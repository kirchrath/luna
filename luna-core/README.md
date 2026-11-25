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
| 1 | Events aus unterschiedlichen Quellen empfangen | manuelle Eingabe umgesetzt; Kafka-Quellen _TODO_ |
| 2 | Eingehende Events per Spring AI analysieren | umgesetzt (Schritt 3) |
| 3 | Anhand der Analyse entscheiden, welche neuen Elemente entstehen (z. B. Aufgabe, Alert) | umgesetzt (Schritte 3 und 4) |
| 4 | Neue Events für die entstandenen Elemente erzeugen | umgesetzt (Schritt 4) |

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
  (inkl. Verknüpfung, welche ausgehenden Events aus welchem Eingang entstanden sind). Jedes erzeugte Event wird
  **vor** der Veröffentlichung mit Typ, Verweis auf das Original-Event und einem **Status** gespeichert, damit es
  in späteren Schritten weiter qualifiziert werden kann. Prompts und Begründungen der KI werden **nicht** in der
  Datenbank gespeichert.
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

## Umsetzung

### Manuelle Eingabe (Schritt 2)

- **Endpunkt:** `POST /inbound-events/manual` (OperationId `createManualInboundEvent`), nur mit JWT.
- **Request:** `ManualInputDataDto` mit `text` (Pflicht, nicht leer, max. 10.000 Zeichen) und optionalem
  `typeHint` (`LunaElementTypeDto`: `TASK`, `ALERT`, `APPOINTMENT`, `INVOICE`, `CONTRACT`, `NOTE`).
- **Antwort:** `202 Accepted` mit `ResourceReferenceDto` (`{"id": "…"}`); `400` bei ungültiger Eingabe,
  `401` ohne gültiges Token.
- **Speicherung:** Tabelle `inbound_event` mit `id`, `source` (`EventSource`, z. B. `MANUAL`), `data` (JSONB,
  hier der `ManualInputDataDto` als JSON) und den Audit-Feldern.
- **Security:** CSRF ist deaktiviert und die API zustandslos (keine Sessions), da sie nur per Bearer-Token
  aufgerufen wird.

### Klassifizierung per Spring AI (Schritt 3)

- **Ablauf:** `ClassificationScheduler` läuft periodisch (`app.classification.interval`, Standard `30s`) und
  holt über `ClassificationApplicationService` die ältesten eingehenden Events mit Status `RECEIVED` (höchstens
  `app.classification.batch-size`, Standard `10`). Je Event klassifiziert der `ClassificationDomainService` in
  **einer** Transaktion: Er fragt über den Client `ElementClassificationClient` (`outbound`, Spring AI), welche
  Element-Typen entstehen (**null bis mehrere**), qualifiziert und veröffentlicht sie (siehe Schritt 4) und setzt
  das eingehende Event auf `ANALYZED` – bzw. speichert den Fehlversuch. Der ApplicationService leitet nur weiter.
- **Typ-Hinweis:** Ist bei der manuellen Eingabe ein `typeHint` gesetzt, wird er der KI mitgegeben und ist immer
  im Ergebnis enthalten; die KI kann weitere Typen ergänzen.
- **Fehler:** Schlägt ein KI-Aufruf fehl (Klassifizierung oder Qualifizierung), wird `attempts` erhöht; ab `app.classification.max-attempts`
  (Standard `3`) wird das eingehende Event `FAILED`, sonst beim nächsten Lauf erneut versucht.
- **Status:** eingehende Events `RECEIVED` → `ANALYZED` bzw. `FAILED` (`InboundEventStatus`); erzeugte Events
  `PUBLISHED` (`OutboundEventStatus`).
- **Speicherung:** Tabelle `outbound_event` mit `id`, `inbound_event_id` (Fremdschlüssel), `type`
  (`LunaElementType`), `status` und den Audit-Feldern; `inbound_event` hat zusätzlich `status` und `attempts`.
- **Prompt:** `src/main/resources/prompts/classification.st` (Englisch); `NOTE` steht für Inhalte, die
  zusammengefasst werden sollen (die Zusammenfassung selbst folgt später); die Antwort der KI wird als
  `ClassificationResultDto` (Liste von Typen) gelesen.
- **Enums:** `LunaElementTypeDto` (API, Package `dto`) und `LunaElementType` (Entity, Package `data.entity`)
  sind getrennt und werden über `LunaElementTypeConverter` (mit MapStruct-Mapper `LunaElementTypeMapper`,
  Package `converter`) aufeinander abgebildet.

### KI-Anbieter konfigurieren

Spring AI **2.1.0** mit den Startern für Ollama, Anthropic und OpenAI. Der Anbieter wird über
`spring.ai.model.chat` gewählt; der Wert ist in `application.yml` bewusst leer und muss je Umgebung gesetzt
werden – **ohne Anbieter startet `luna-core` nicht**. Alle übrigen Modellarten (Embedding, Bild, Audio,
Moderation, Decision) sind mit `none` abgeschaltet.

| Anbieter | `SPRING_AI_MODEL_CHAT` | Weitere Umgebungsvariablen (Beispiele) |
|---|---|---|
| Ollama | `ollama` | `SPRING_AI_OLLAMA_BASE_URL`, `SPRING_AI_OLLAMA_CHAT_MODEL` |
| Anthropic Claude | `anthropic` | `SPRING_AI_ANTHROPIC_API_KEY`, `SPRING_AI_ANTHROPIC_CHAT_MODEL` |
| OpenAI-kompatibel | `openai` | `SPRING_AI_OPENAI_API_KEY`, `SPRING_AI_OPENAI_BASE_URL`, `SPRING_AI_OPENAI_CHAT_MODEL` |

API-Keys nie ins Repo; im Stack als Docker Secret `luna_core_ai_api_key` bereitstellen (siehe unten).

### Qualifizieren und Veröffentlichen (Schritt 4)

**Ablauf** – direkt im Anschluss an die Klassifizierung, im `ClassificationDomainService`, **alles in einer
Transaktion**:

1. Klassifizieren wie bisher (null bis mehrere Element-Typen).
2. **Qualifizieren:** je ermitteltem Typ **ein eigener KI-Aufruf** (`ElementQualificationClient`) mit
   typspezifischem Prompt (`app.prompts.<typ>`) und strukturierter Antwort `QualificationResultDto<<Typ>EventDataDto>`
   – einer **Liste aller Elemente** dieses Typs (z. B. zwei Termine aus einer Eingabe).
   Extrahierte Texte in der **Sprache der Eingabe**; relative Angaben („nächsten Dienstag“) bezogen auf den
   **Eingangszeitpunkt** des Events. Fehlen Pflichtfelder (Bean Validation), gilt der Versuch als fehlgeschlagen.
3. Je Element ein `OutboundEvent` (Status **`PUBLISHED`**) und ein `<Typ>Element` mit allen Feldern speichern.
4. Je Element ein `<Typ>EventDto` **synchron** an das Topic aus `app.kafka.topics.<typ>` senden
   (`KafkaTemplate<UUID, ElementEventDto>`, Kafka-Key = ID des `OutboundEvent`).
5. Eingehendes Event auf `ANALYZED`.

Die KI-Aufrufe laufen vor allen Schreibzugriffen; schlägt einer fehl, wird `attempts` erhöht und das Event später
erneut verarbeitet (ab `max-attempts` `FAILED`). **Kafka gilt als immer verfügbar:** Scheitert das Senden, wird die
Transaktion zurückgerollt, ohne dass ein Fehlversuch gezählt wird; der nächste Lauf versucht es erneut.
Restrisiko: Senden erfolgreich, Commit scheitert → später doppelt gesendet; die Fach-Backends erkennen Duplikate
über die ID.

Umgesetzt durch je einen `<Typ>ElementProcessor` (`services`, `@Transactional(propagation = MANDATORY)`, Basis
`ElementProcessor`), der qualifiziert, das `<Typ>Element` speichert und per `ElementEventPublisher` (`outbound`)
sendet; die Abbildung erledigen `<Typ>ElementConverter`/`<Typ>ElementMapper`.

**Datenmodell** (Migration `V3`):

- `outbound_event` bleibt (ID, Verweis auf Eingang, Typ, Status, Audit); Status **`PUBLISHED`** ersetzt
  `CLASSIFIED` (Zwischenstände werden wegen der einen Transaktion nie gespeichert).
- Je Typ eine Tabelle/Entity **`<typ>_element` / `<Typ>Element`** mit eigener ID, `outbound_event_id`
  (Fremdschlüssel, eindeutig), allen Feldern und den Audit-Feldern. Alle Zeitangaben als `OffsetDateTime`
  (`TIMESTAMPTZ`), Beträge als `BigDecimal` (`NUMERIC` ohne Grenze). Pflichtfelder sind **fett**.

| Typ | Entity / Tabelle | Felder |
|---|---|---|
| `TASK` | `TaskElement` / `task_element` | **`title`**, `description`, `dueDate`, **`priority`** (`LOW`/`NORMAL`/`HIGH`, Standard `NORMAL`) |
| `ALERT` | `AlertElement` / `alert_element` | **`title`**, **`message`**, **`severity`** (`INFO`/`WARNING`/`CRITICAL`), `affectedSystem` |
| `APPOINTMENT` | `AppointmentElement` / `appointment_element` | **`title`**, **`start`**, `end`, **`allDay`**, `location`, `description` |
| `INVOICE` | `InvoiceElement` / `invoice_element` | **`issuer`**, `invoiceNumber`, **`amount`**, **`currency`** (ISO 4217), `dueDate`, `description` |
| `CONTRACT` | `ContractElement` / `contract_element` | **`counterparty`**, **`title`**, `startDate`, `endDate`, `cancellationDeadline`, `description` |
| `NOTE` | `NoteElement` / `note_element` | **`title`**, **`summary`** |

**Kafka-Nachrichten:** je Typ `<Typ>EventDto(id, audit, data)` mit `data: <Typ>EventDataDto` (Felder wie oben,
**ohne** Originaltext); `id` und `audit` stammen vom `OutboundEvent`. Alle Events implementieren das sealed
Interface `ElementEventDto`. Die Schemas aller Nachrichten stehen in **`luna-core/kafka-api.yaml`** (erzeugt mit
`./mvnw verify -Popenapi`). Topics `luna_core_task`, `luna_core_alert`,
`luna_core_appointment`, `luna_core_invoice`, `luna_core_contract`, `luna_core_note` (Anlage beim ersten
Schreiben; ACL für `luna-core` auf `luna_core_*` besteht bereits).

**Stack** (`swarm/luna-core.yml`, Image per `./mvnw package jib:dockerBuild`, Version über `LUNA_CORE_VERSION`):

- Erreichbar **nur über Traefik** unter `https://luna.internal/api/luna-core` (StripPrefix), kein veröffentlichter
  Port; Profil `prod`.
- **Kafka:** `kafka:9094`, SASL/SCRAM als Benutzer `luna-core` (Secret `kafka_luna_core_password`), TLS mit dem
  Root-Zertifikat **zur Laufzeit aus dem step-ca-Volume** (`/step/certs/root_ca.crt`, PEM, read-only eingebunden).
- **Keycloak:** Schlüssel intern per HTTP (`jwk-set-uri` =
  `http://keycloak:8080/auth/realms/luna/protocol/openid-connect/certs`), `issuer-uri` bleibt
  `https://luna.internal/auth/realms/luna` und wird im Token geprüft.
- **KI-Anbieter:** Spring-Namen direkt beim Deploy (`SPRING_AI_MODEL_CHAT`, `SPRING_AI_<ANBIETER>_…`), von der
  Stack-Datei durchgereicht; nicht gesetzte Variablen werden nicht übergeben. Der API-Key kommt aus dem Secret
  `luna_core_ai_api_key`, eingebunden unter dem Property-Namen aus `LUNA_AI_API_KEY_PROPERTY`
  (z. B. `spring.ai.anthropic.api-key`; bei Ollama ein Platzhalter).
- **Secrets per configtree:** `spring.config.import=optional:configtree:/run/secrets/` (Profil `prod`); Secrets
  werden mit dem Property-Namen als Ziel eingebunden: `luna_db_password` → `spring.datasource.password`,
  `kafka_luna_core_password` → `app.kafka.password`.
- Zeitzone über `/etc/localtime`.

**Tests:** Unit-Tests je Klasse (100 % Abdeckung), Integrationstest von der Eingabe bis zur Nachricht auf dem Topic
mit PostgreSQL und Kafka per Testcontainers (ohne TLS/SASL), gemocktes `ChatModel`; ArchUnit-Regeln gelten weiter.

### Konfiguration von Topics und Prompts

| Property | Inhalt |
|---|---|
| `app.kafka.topics.<typ>` | Topic je Element-Typ (`task`, `alert`, `appointment`, `invoice`, `contract`, `note`) |
| `app.prompts.classification` | Prompt der Klassifizierung |
| `app.prompts.<typ>` | Prompt der Qualifizierung je Element-Typ |

Der Kafka-Benutzer ist `${spring.application.name}`.

### Lokale Entwicklung

`luna-core` lokal (Profil `local`) braucht neben PostgreSQL und Keycloak auch Kafka aus dem lokalen Stack
(`localhost:7094`, TLS + SCRAM). Umgebungsvariablen beim Start:

| Variable | Wert |
|---|---|
| `SPRING_DATASOURCE_PASSWORD` | Passwort des DB-Benutzers `luna` |
| `APP_KAFKA_PASSWORD` | Kafka-Passwort von `luna-core` (Secret `kafka_luna_core_password`) |
| `SPRING_KAFKA_SSL_TRUST_STORE_LOCATION` | `file:<pfad>/luna-root-ca.crt` (Root-Zertifikat von step-ca, siehe README) |
| `SPRING_AI_MODEL_CHAT` + Anbieter-Variablen | siehe [KI-Anbieter konfigurieren](#ki-anbieter-konfigurieren) |

## Offene Punkte

Noch nicht entschieden (bitte hier eintragen, sobald entschieden):

- **Fehlerformat:** Body von Fehlerantworten (z. B. `400`) ist noch nicht festgelegt und daher nicht in der
  OpenAPI-Spezifikation beschrieben. _TODO_

- **Event-Daten weiterer Quellen:** Inhalt von `data` eingehender Events weiterer Quellen (z. B. E-Mail). _TODO_
- **Zuordnung zu Fach-Backends:** Welches Backend ist für welchen Element-Typ zuständig?
  (Entscheidung bei Konzeption des jeweiligen Fach-Backends) _TODO_
