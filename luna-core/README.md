# luna-core

Erstes Backend von Luna (Port 7081, Produktion unter `/api/luna-core`).
Konventionen, Tech-Stack und Betrieb stehen in [AGENTS.md](../AGENTS.md).

## Zweck

`luna-core` ist die **Event-Analyse** von Luna. Kurz gesagt:

> **Events kommen rein, werden analysiert und erzeugen neue Events.**

- `luna-core` **empfängt Events aus unterschiedlichen Quellen** (andere Services).
- Jedes Event wird **analysiert**. Die Analyse wird mit **Spring AI** umgesetzt.
- Aus der Analyse entscheidet `luna-core`, **welche neuen Elemente bzw. Events** entstehen.

### Beispiele

| Eingehendes Event | Ergebnis der Analyse |
|---|---|
| Ein Service liefert ein Event über eine E-Mail, deren Inhalt etwas zu erledigen verlangt | neue **Aufgabe** |
| Die E-Mail enthält einen wichtigen Hinweis zu einem Systemstatus | neuer **Alert** |
| Die E-Mail ist ein Unzustellbarkeitsbericht (Non-Delivery-Report) | neuer **Alert** |

## Funktionsliste

> [!NOTE]
> Die Liste ist ein erster Entwurf und **muss noch kuratiert werden**. Kein Punkt ist zur Umsetzung freigegeben.

| # | Funktion | Status |
|---|---|---|
| 1 | Events aus unterschiedlichen Quellen empfangen | _TODO_ |
| 2 | Eingehende Events per Spring AI analysieren | _TODO_ |
| 3 | Anhand der Analyse entscheiden, welche neuen Elemente entstehen (z. B. Aufgabe, Alert) | _TODO_ |
| 4 | Neue Events für die entstandenen Elemente erzeugen | _TODO_ |

## Entscheidungen

- **Eingang:** Events kommen über **Kafka** oder per **manueller Eingabe im Frontend**
  (das Frontend ruft `luna-core` wie alle Backends über den generierten REST-Wrapper auf).
  Weitere Message Broker erst in einer **späteren Ausbaustufe** (nicht umsetzen, solange nicht beauftragt).
- **Erste Quelle:** **manuelle Eingabe** im Frontend.
- **Ausgang:** Entstandene Elemente (z. B. Aufgabe, Alert) werden als Events **an die zuständigen Fach-Backends
  gesendet**, die sie speichern. `luna-core` speichert diese Elemente nicht selbst.
- **Ausgangskanal:** `luna-core` publiziert die erzeugten Events auf **Kafka-Topics**; die Fach-Backends
  konsumieren sie.
- **Event-Format:** **eigenes JSON-Format** (ein- und ausgehend), definiert als Records. Aufbau wie die
  Entity-Dtos, zusätzlich mit ID. Auf Root-Ebene stehen also genau `id`, `audit` und `data`:

  ```json
  { "id": "…", "audit": { … }, "data": { … } }
  ```

  - `audit` ist exakt das **`AuditDto`** (`createdDate`, `lastModifiedDate`, `version`).
  - **Typ und Quelle** stehen nicht im Event, sondern ergeben sich aus dem **Topic**.
  - Die **Verknüpfung** zum auslösenden Event wird nur in der Datenbank von `luna-core` gespeichert.
- **Spring AI:** Modell bzw. Anbieter ist **konfigurierbar** (per Property/Profil umschaltbar).
  Zunächst unterstützt: **Ollama** (lokal), **Anthropic Claude** und **OpenAI-kompatible** Endpunkte.
- **Spring-AI-Standard:** **kein** voreingestellter Anbieter; in `application.yml` leer, je Umgebung explizit setzen.
- **Kafka-Betrieb:** Image **`apache/kafka:4.4.0`** in `swarm/kafka.yml`, Port **7094**, TLS und SASL/SCRAM
  (siehe [AGENTS.md](../AGENTS.md#betrieb)). `luna-core` meldet sich als Benutzer `luna-core` an, darf auf
  `luna_core_*` schreiben und liest nur ausdrücklich freigegebene Topics.
- **Persistenz:** `luna-core` speichert die **eingegangenen Events** und die **erzeugten Events**
  (inkl. Verknüpfung, welche ausgehenden Events aus welchem Eingang entstanden sind).
  Analyseergebnisse und Prompts werden **nicht** in der Datenbank gespeichert.
- **Element-Typen** (englischer Name für Topics und Code):

  | Element | Name | Ausgangs-Topic |
  |---|---|---|
  | Aufgabe | `task` | `luna_core_task` |
  | Alert | `alert` | `luna_core_alert` |
  | Termin | `appointment` | `luna_core_appointment` |
  | Rechnung | `invoice` | `luna_core_invoice` |
  | Vertrag | `contract` | `luna_core_contract` |
  | Notiz | `note` | `luna_core_note` |
- **Manuelle Eingabe:** `data` enthält einen **Freitext** und einen **optionalen Typ-Hinweis**
  (einer der Element-Typen), der die Entscheidung der KI bei Bedarf übersteuert.
- **Topics:** **ein Topic je Quelle** für den Eingang und **ein Topic je Element-Typ** für den Ausgang.
  Namensschema `luna_<erzeuger>_<inhalt>` (siehe [AGENTS.md](../AGENTS.md#backends-luna-core-und-weitere)):
  Eingang z. B. `luna_mail_email`, Ausgang `luna_core_<element-typ>`, z. B. `luna_core_task`.
- **Zuordnung zu Fach-Backends:** wird erst entschieden, wenn das jeweilige Fach-Backend konzipiert wird.

## Abgrenzung

Ausdrücklich **nicht** Teil von `luna-core`:

- **Fachdaten speichern:** Aufgaben, Termine, Alerts usw. werden weder in `luna-core` gespeichert noch über
  `luna-core` abgefragt.
- **Quellen abfragen:** `luna-core` holt keine Daten selbst (kein IMAP, kein Polling); Quell-Services liefern Events.
- **Benachrichtigungen:** `luna-core` verschickt keine Push- oder Mail-Benachrichtigungen an den Benutzer.
- **Fachliche Folgeaktionen:** `luna-core` führt keine Aktionen aus (z. B. Mail beantworten), sondern erzeugt
  nur Events.

## Offene Punkte

Noch nicht entschieden (bitte hier eintragen, sobald entschieden):

- **Event-Daten:** Inhalt von `data` je Element-Typ und je weiterer Quelle (Feldnamen und Typen). _TODO_
- **Zuordnung zu Fach-Backends:** Welches Backend ist für welchen Element-Typ zuständig?
  (Entscheidung bei Konzeption des jeweiligen Fach-Backends) _TODO_
