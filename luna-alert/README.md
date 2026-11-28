# luna-alert

Element-Service für **Warnungen** (Alerts) (Port 7083, Produktion unter `/api/luna-alert`).
Konventionen, Tech-Stack und Betrieb stehen in [AGENTS.md](../AGENTS.md).

## Zweck

`luna-alert` verarbeitet die Alerts, die `luna-core` aus Eingaben erzeugt, und verwaltet ihren Status:

```
luna_core_alert ──► CoreAlertProjection ──► Alert ──► luna_alert_alert (Kafka)
                    (nur neuere Versionen)        └──► /topic/alerts (STOMP-Signal)
```

1. Ein `AlertEventDto` von `luna-core` (Topic `luna_core_alert`) wird gelesen; die **Projection**
   `CoreAlertProjection(id, coreAlertId, version)` lässt nur neuere Versionen durch.
2. Beim ersten Event entsteht ein offener **Alert** `Alert(source = CORE, sourceId = coreAlertId, …)`.
   Der Inhalt wird **nicht vom Benutzer bearbeitet**; neuere Versionen übernehmen daher immer Titel, Meldung, Schwere
   und betroffenes System. Der Status bleibt erhalten (kein `edited`).
3. Nach jedem Anlegen und jeder Änderung wird **nach dem Commit** der vollständige Stand als `AlertEventDto` auf
   `luna_alert_alert` veröffentlicht und ein Signal `AlertChangedDto` an `/topic/alerts` gesendet.

Fehler werden über `luna_alert_alert_retry` wiederholt (Standard 10-mal, 10 s) und landen danach in `luna_alert_dlt`;
nicht lesbare Events direkt im DLT. Die Core-DTOs werden aus `src/main/resources/kafka-api/luna-core.yaml` generiert.

## Alert

| Feld | Typ | Herkunft |
|---|---|---|
| `title`, `message` | Text | Core-Event |
| `severity` | `INFO`, `WARNING`, `CRITICAL` | Core-Event |
| `affectedSystem` | Text (optional) | Core-Event |
| `status` | `OPEN`, `ACKNOWLEDGED`, `RESOLVED` | neu immer `OPEN` |
| `acknowledgedDate`, `resolvedDate` | Zeitpunkt | beim Bestätigen bzw. Beheben |
| `source`, `sourceId` | `CORE` + ID des Core-Alerts | Core-Event |
| `area` | `PERSONAL`, `WORK` (Lebensbereich) | Core-Event, danach **unveränderlich** (kein Update-Endpunkt ändert ihn, neuere Core-Versionen auch nicht) |

Für die Sortierung nach Schwere speichert der Service zusätzlich `severity_rank` (`CRITICAL` 0, `WARNING` 1,
`INFO` 2), weil die Schwere selbst als Text gespeichert ist.

## REST-API

Spezifikation: [openapi.yaml](openapi.yaml). Veröffentlichte Events: [kafka-api.yaml](kafka-api.yaml).

| Methode und Pfad | Zweck |
|---|---|
| `GET /alerts?status=&status=&area=&page=&size=` | Liste, optional nach einem oder mehreren Status und nach Lebensbereich gefiltert; schwerste zuerst, dann neueste |
| `GET /alerts/{id}` | Details (`404`, wenn unbekannt) |
| `POST /alerts/{id}/acknowledge` | Bestätigen (`OPEN` → `ACKNOWLEDGED`; bereits bestätigt: unverändert; behoben: `409`) |
| `POST /alerts/{id}/resolve` | Beheben (aus `OPEN` oder `ACKNOWLEDGED`; bereits behoben: unverändert) |
| `POST /alerts/{id}/reopen` | Wieder öffnen (→ `OPEN`, Zeitpunkte werden gelöscht; bereits offen: unverändert) |

Optionale Felder fehlen in den Antworten, wenn sie leer sind.

**Frontend:** Bereich „Warnungen“ (`/alerts`, Details unter `/alerts/:id`), siehe
[frontend/docs](../frontend/docs/README.md#warnungen-und-notizen-schritt-7b).

## Betrieb

- **Stack:** [`swarm/luna-alert.yml`](../swarm/luna-alert.yml), Image `luna-alert:${LUNA_ALERT_VERSION}`.
- **Secrets:** `luna_db_password` (`spring.datasource.password`), `kafka_luna_alert_password` (`app.kafka.password`).
- **Kafka:** Benutzer `luna-alert`; liest `luna_core_alert`, `luna_alert_alert_retry` und `luna_alert_dlt`,
  schreibt `luna_alert_*`.

## Entwicklung

```bash
cd luna-alert
./mvnw verify               # bauen, testen, Coverage prüfen
./mvnw verify -Popenapi     # openapi.yaml und kafka-api.yaml neu erzeugen
./mvnw package jib:dockerBuild
```
