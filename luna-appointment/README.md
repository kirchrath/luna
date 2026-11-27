# luna-appointment

Element-Service für **Termine** (Port 7085, Produktion unter `/api/luna-appointment`).
Konventionen, Tech-Stack und Betrieb stehen in [AGENTS.md](../AGENTS.md).

## Zweck

`luna-appointment` verarbeitet die Termine, die `luna-core` aus Eingaben erzeugt:

```
luna_core_appointment ──► CoreAppointmentProjection ──► Appointment ──► luna_appointment_appointment (Kafka)
                          (nur neuere Versionen)                    └──► /topic/appointments (STOMP-Signal)
```

1. Ein `AppointmentEventDto` von `luna-core` (Topic `luna_core_appointment`) wird gelesen; die **Projection**
   `CoreAppointmentProjection(id, coreAppointmentId, version)` lässt nur neuere Versionen durch.
2. Beim ersten Event entsteht ein geplanter **Termin** `Appointment(source = CORE, sourceId = coreAppointmentId, …)`.
   Neuere Versionen übernehmen alle Inhalte – **nur solange der Termin nicht vom Benutzer bearbeitet** wurde
   (`edited`; Bearbeiten, Absagen und Wiederherstellen setzen es).
3. Nach jedem Anlegen und jeder Änderung wird **nach dem Commit** der vollständige Stand als `AppointmentEventDto`
   auf `luna_appointment_appointment` veröffentlicht und ein Signal `AppointmentChangedDto` an `/topic/appointments`
   gesendet.

Fehler werden über `luna_appointment_appointment_retry` wiederholt (Standard 10-mal, 10 s) und landen danach in
`luna_appointment_dlt`; nicht lesbare Events direkt im DLT. Die Core-DTOs werden aus
`src/main/resources/kafka-api/luna-core.yaml` generiert.

## Termin

| Feld | Typ | Herkunft |
|---|---|---|
| `title` | Text (Pflicht, max. 500) | Core-Event / Benutzer |
| `start` | Zeitpunkt (Pflicht) | Core-Event / Benutzer |
| `end` | Zeitpunkt (optional, nicht vor `start`) | Core-Event / Benutzer |
| `allDay` | ja/nein | Core-Event / Benutzer |
| `location` | Text (max. 500) | Core-Event / Benutzer |
| `description` | Text (max. 10.000) | Core-Event / Benutzer |
| `status` | `SCHEDULED`, `CANCELLED` | neu immer `SCHEDULED` |
| `cancelledDate` | Zeitpunkt | beim Absagen |
| `source`, `sourceId` | `CORE` + ID des Core-Termins | Core-Event |
| `area` | `PERSONAL`, `WORK` (Lebensbereich) | Core-Event, danach **unveränderlich** (kein Update-Endpunkt ändert ihn, neuere Core-Versionen auch nicht) |
| `edited` | ja/nein | `true` nach jeder Änderung durch den Benutzer |

**Ganztägige Termine** beginnen um 00:00 Ortszeit; `end` ist der letzte Tag (ebenfalls 00:00).

**Effektives Ende** (`effective_end`, nur intern): der Zeitpunkt, zu dem der Termin vorbei ist – `end`, ohne Ende
`start`, bei ganztägigen Terminen das Ende des letzten Tages. Es trennt kommende von vergangenen Terminen
(laufende Termine zählen als kommend) und bestimmt die Überschneidung mit einem Zeitraum.

## REST-API

Spezifikation: [openapi.yaml](openapi.yaml). Veröffentlichte Events: [kafka-api.yaml](kafka-api.yaml).

| Methode und Pfad | Zweck |
|---|---|
| `GET /appointments?period=&area=&page=&size=` | `UPCOMING` (Standard, früheste zuerst), `PAST` (späteste zuerst) oder `ALL`; optional nach Lebensbereich gefiltert |
| `GET /appointments/range?from=&to=&area=` | alle Termine, die sich mit dem Zeitraum überschneiden (z. B. ein Monat), früheste zuerst; optional nach Lebensbereich gefiltert |
| `GET /appointments/{id}` | Details (`404`, wenn unbekannt) |
| `PUT /appointments/{id}` | bearbeiten; mit `version` (`409` bei Konflikt, `400` bei Ende vor Beginn) |
| `POST /appointments/{id}/cancel` | absagen (bereits abgesagt: unverändert) |
| `POST /appointments/{id}/restore` | wiederherstellen (bereits geplant: unverändert) |

Optionale Felder fehlen in den Antworten, wenn sie leer sind.

**Frontend:** Bereich „Termine“ (`/calendar`, Details unter `/calendar/:id`), siehe
[frontend/docs](../frontend/docs/README.md#termine-schritt-7c).

## Betrieb

- **Stack:** [`swarm/luna-appointment.yml`](../swarm/luna-appointment.yml), Image
  `luna-appointment:${LUNA_APPOINTMENT_VERSION}`.
- **Secrets:** `luna_db_password` (`spring.datasource.password`), `kafka_luna_appointment_password`
  (`app.kafka.password`).
- **Kafka:** Benutzer `luna-appointment`; liest `luna_core_appointment`, `luna_appointment_appointment_retry` und
  `luna_appointment_dlt`, schreibt `luna_appointment_*`.

## Entwicklung

```bash
cd luna-appointment
./mvnw verify               # bauen, testen, Coverage prüfen
./mvnw verify -Popenapi     # openapi.yaml und kafka-api.yaml neu erzeugen
./mvnw package jib:dockerBuild
```
