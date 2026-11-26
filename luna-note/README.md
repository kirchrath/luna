# luna-note

Element-Service für **Notizen** (Port 7084, Produktion unter `/api/luna-note`).
Konventionen, Tech-Stack und Betrieb stehen in [AGENTS.md](../AGENTS.md).

## Zweck

`luna-note` verarbeitet die Notizen (Zusammenfassungen), die `luna-core` aus Eingaben erzeugt:

```
luna_core_note ──► CoreNoteProjection ──► Note ──► luna_note_note (Kafka)
                   (nur neuere Versionen)      └──► /topic/notes (STOMP-Signal)
```

1. Ein `NoteEventDto` von `luna-core` (Topic `luna_core_note`) wird gelesen; die **Projection**
   `CoreNoteProjection(id, coreNoteId, version)` lässt nur neuere Versionen durch.
2. Beim ersten Event entsteht eine aktive **Notiz** `Note(source = CORE, sourceId = coreNoteId, …)`. Neuere
   Versionen übernehmen Titel und Zusammenfassung – **nur solange die Notiz nicht vom Benutzer bearbeitet** wurde
   (`edited`; Bearbeiten, Archivieren und Wiederherstellen setzen es).
3. Nach jedem Anlegen und jeder Änderung wird **nach dem Commit** der vollständige Stand als `NoteEventDto` auf
   `luna_note_note` veröffentlicht und ein Signal `NoteChangedDto` an `/topic/notes` gesendet.

Fehler werden über `luna_note_note_retry` wiederholt (Standard 10-mal, 10 s) und landen danach in `luna_note_dlt`;
nicht lesbare Events direkt im DLT. Die Core-DTOs werden aus `src/main/resources/kafka-api/luna-core.yaml` generiert.

## Notiz

| Feld | Typ | Herkunft |
|---|---|---|
| `title` | Text (Pflicht, max. 500) | Core-Event / Benutzer |
| `summary` | Text (Pflicht, max. 10.000) | Core-Event / Benutzer |
| `status` | `ACTIVE`, `ARCHIVED` | neu immer `ACTIVE` |
| `archivedDate` | Zeitpunkt | beim Archivieren |
| `source`, `sourceId` | `CORE` + ID der Core-Notiz | Core-Event |
| `edited` | ja/nein | `true` nach jeder Änderung durch den Benutzer |

## REST-API

Spezifikation: [openapi.yaml](openapi.yaml). Veröffentlichte Events: [kafka-api.yaml](kafka-api.yaml).

| Methode und Pfad | Zweck |
|---|---|
| `GET /notes?status=&page=&size=` | Liste, optional nach Status gefiltert; neueste zuerst |
| `GET /notes/{id}` | Details (`404`, wenn unbekannt) |
| `PUT /notes/{id}` | Titel und Zusammenfassung ändern; mit `version` (`409` bei Konflikt) |
| `POST /notes/{id}/archive` | Archivieren (bereits archiviert: unverändert) |
| `POST /notes/{id}/restore` | Wiederherstellen (bereits aktiv: unverändert) |

Optionale Felder fehlen in den Antworten, wenn sie leer sind.

**Frontend:** Bereich „Notizen“ (`/notes`, Details unter `/notes/:id`), siehe
[frontend/docs](../frontend/docs/README.md#warnungen-und-notizen-schritt-7b).

## Betrieb

- **Stack:** [`swarm/luna-note.yml`](../swarm/luna-note.yml), Image `luna-note:${LUNA_NOTE_VERSION}`.
- **Secrets:** `luna_db_password` (`spring.datasource.password`), `kafka_luna_note_password` (`app.kafka.password`).
- **Kafka:** Benutzer `luna-note`; liest `luna_core_note`, `luna_note_note_retry` und `luna_note_dlt`,
  schreibt `luna_note_*`.

## Entwicklung

```bash
cd luna-note
./mvnw verify               # bauen, testen, Coverage prüfen
./mvnw verify -Popenapi     # openapi.yaml und kafka-api.yaml neu erzeugen
./mvnw package jib:dockerBuild
```

## Offen (_TODO_)

- Events werden nach dem Commit direkt gesendet: Ist Kafka in diesem Moment nicht erreichbar, geht das Event verloren
  (kein Outbox-Muster). Gilt ebenso für `luna-alert` und `luna-task`.
