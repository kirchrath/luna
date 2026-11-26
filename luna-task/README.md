# luna-task

Element-Service für **Aufgaben** (Port 7082, Produktion unter `/api/luna-task`).
Konventionen, Tech-Stack und Betrieb stehen in [AGENTS.md](../AGENTS.md).

## Zweck

`luna-task` verarbeitet die Aufgaben, die `luna-core` aus Eingaben erzeugt, und verwaltet sie als eigene Aufgaben:

> **Aufgaben-Events von `luna-core` kommen rein, werden gespeichert und jede Änderung wird weitergemeldet.**

## Verarbeitung der Core-Events

```
luna_core_task ──► CoreTaskProjection ──► Task ──► luna_task_task (Kafka)
                   (nur neuere Versionen)      └──► /topic/tasks (STOMP-Signal)
```

1. Ein `TaskEventDto` von `luna-core` (Topic `luna_core_task`, Key = Core-ID) wird gelesen.
2. Die **Projection** `CoreTaskProjection(id, coreTaskId, version)` merkt sich je Core-Aufgabe die zuletzt
   verarbeitete Version (`audit.version` des Core-Events). Events mit gleicher oder älterer Version werden verworfen.
3. Beim ersten Event entsteht eine offene **Aufgabe** `Task(source = CORE, sourceId = coreTaskId, …)`.
   Neuere Versionen übernehmen Titel, Beschreibung, Fälligkeit und Priorität – **nur solange die Aufgabe nicht vom
   Benutzer bearbeitet** wurde (`edited = false`). Bei bearbeiteten Aufgaben wird die Version nur in der Projection
   vermerkt.
4. Nach jedem Anlegen und jeder Änderung wird **nach dem Commit** der vollständige Stand als `TaskEventDto`
   (`id`, `audit`, `data`; Key = Task-ID) auf `luna_task_task` veröffentlicht und ein Signal `TaskChangedDto` an
   `/topic/tasks` gesendet.

Fehler bei der Verarbeitung werden über das Retry-Topic `luna_task_task_retry` wiederholt (Standard: 10-mal im
Abstand von 10 s, `app.kafka.retry.*`), danach landet das Event im DLT `luna_task_dlt`. Nicht lesbare Events gehen
ohne Wiederholung direkt ins DLT.

Die Java-Typen der Core-Events werden beim Build aus `src/main/resources/kafka-api/luna-core.yaml` generiert
(Package `de.kirchrath.luna.task.dto.core`). Die Datei ist eine Kopie von `luna-core/kafka-api.yaml` und wird vom
`GenerateOpenApiTest` in `luna-core` geschrieben.

## Aufgabe

| Feld | Typ | Herkunft |
|---|---|---|
| `title` | Text (Pflicht, max. 500) | Core-Event / Benutzer |
| `description` | Text (max. 10.000) | Core-Event / Benutzer |
| `dueDate` | Zeitpunkt | Core-Event / Benutzer |
| `priority` | `LOW`, `NORMAL`, `HIGH` | Core-Event / Benutzer |
| `status` | `OPEN`, `DONE` | neu immer `OPEN` |
| `completedDate` | Zeitpunkt | beim Erledigen |
| `source`, `sourceId` | `CORE` + ID der Core-Aufgabe | Core-Event |
| `edited` | ja/nein | `true` nach jeder Änderung durch den Benutzer |

## REST-API

Spezifikation: [openapi.yaml](openapi.yaml). Veröffentlichte Events: [kafka-api.yaml](kafka-api.yaml).

| Methode und Pfad | Zweck |
|---|---|
| `GET /tasks?status=&page=&size=` | Liste, optional nach Status gefiltert; sortiert nach Fälligkeit (ohne Fälligkeit zuletzt), dann Eingang |
| `GET /tasks/{id}` | Details (`404`, wenn unbekannt) |
| `PUT /tasks/{id}` | Titel, Beschreibung, Fälligkeit, Priorität ändern; mit `version` (`409` bei Konflikt) |
| `POST /tasks/{id}/complete` | Erledigen (bereits erledigt: unverändert) |
| `POST /tasks/{id}/reopen` | Wieder öffnen (bereits offen: unverändert) |

Optionale Felder (`description`, `dueDate`, `completedDate`, `sourceId`) fehlen in den Antworten, wenn sie leer
sind (`spring.jackson.default-property-inclusion: non_null`).

**Frontend:** Seiten „Aufgaben“ (`/tasks`) und Details/Bearbeiten (`/tasks/:id`), siehe
[frontend/docs](../frontend/docs/README.md#aufgaben-schritt-7a-3).

## Konfiguration

| Property | Bedeutung |
|---|---|
| `app.kafka.topics.core-task` | Eingang `luna_core_task` |
| `app.kafka.topics.core-task-retry` | Retry-Topic `luna_task_task_retry` |
| `app.kafka.topics.dlt` | Dead-Letter-Topic `luna_task_dlt` |
| `app.kafka.topics.task` | Ausgang `luna_task_task` |
| `app.kafka.retry.retries` / `delay` | Wiederholungen (Standard 10) und Abstand (Standard 10 s) |
| `app.websocket.topics.tasks` | STOMP-Topic für Änderungssignale (`/topic/tasks`) |

Datenbank-Schema `luna_task`; Kafka-Benutzer und Consumer-Gruppe `luna-task`.

## Betrieb

- **Stack:** [`swarm/luna-task.yml`](../swarm/luna-task.yml), Image `luna-task:${LUNA_TASK_VERSION}`
  (`./mvnw package jib:dockerBuild`), Traefik leitet `/api/luna-task` (ohne Präfix) an Port 7082 weiter.
  Schritte siehe [README](../README.md).
- **Secrets:** `luna_db_password` (`spring.datasource.password`) und `kafka_luna_task_password` (`app.kafka.password`).
- **Kafka:** Benutzer `luna-task`; liest `luna_core_task`, `luna_task_task_retry` und `luna_task_dlt`, schreibt
  `luna_task_*`. `kafka-init` legt die lesbaren Topics an, falls sie fehlen. Fehlen Rechte oder Topics beim Start,
  versucht der Consumer es alle 10 s erneut (`spring.kafka.listener.auth-exception-retry-interval`).

## Entwicklung

```bash
cd luna-task
./mvnw verify               # bauen, testen, Coverage prüfen
./mvnw verify -Popenapi     # openapi.yaml und kafka-api.yaml neu erzeugen
./mvnw package jib:dockerBuild
```

## Offen (_TODO_)

- Events werden nach dem Commit direkt gesendet: Ist Kafka in diesem Moment nicht erreichbar, geht das Event verloren
  (kein Outbox-Muster).
