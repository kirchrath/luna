# AGENTS.md

Leitfaden für KI-Agenten (Claude Code, Codex, Copilot u. a.), die an **Luna** arbeiten.
Diese Datei ist die zentrale Quelle für Arbeitsweise, Konventionen und Projektkontext.
Sie wird gepflegt, sobald sich am Projekt etwas Wesentliches ändert.

> [!IMPORTANT]
> **Oberste Regel: Niemals Annahmen treffen.**
> Bei jeder Unklarheit – egal wie klein – **nachfragen** und **konkrete Vorschläge zur Auswahl** machen.
> Die Entscheidung trifft der Mensch; erst danach umsetzen. Details siehe [Grundprinzip 3](#grundprinzipien).

## Projektstatus

Luna ist eine **zentrale Anwendung zur Verwaltung des Alltags** – für **einen einzelnen Benutzer**.

Geplante Bereiche:

- Aufgaben & To-dos
- Termine & Kalender
- Einkauf & Haushalt
- Finanzen & Verträge
- Monitoring: Heimnetz & Server, Smart Home & Energie, Gesundheit & Gewohnheiten sowie Luna selbst
  (Logs, Metriken, Health-Checks)

**Phase: Konzeption.** Das erste Feature ist festgelegt (siehe unten). Keine Fachfunktionen implementieren,
solange sie nicht ausdrücklich beauftragt sind.

Bisher existieren die Dokumentation, die Infrastruktur (`swarm/`) und das Grundgerüst von `luna-core`
(ohne Fachfunktionen).

Noch offen (bitte hier eintragen, sobald entschieden):

- **Reihenfolge / erstes Feature:** **Verarbeitung in `luna-core`** mit der manuellen Eingabe als erster Quelle,
  in kleinen, einzeln beauftragten Schritten:
  1. Kafka im Stack (`swarm/kafka.yml`) mit TLS, SASL/SCRAM und ACLs (erledigt)
  2. Eingang: REST-Endpunkt für die manuelle Eingabe; eingehendes Event speichern (erledigt)
  3. Analyse per Spring AI (Entscheidung über den Element-Typ) (erledigt)
  4. Ausgang: erzeugte Elemente qualifizieren, speichern und auf `luna_core_<typ>` publizieren; `luna-core` im Stack (erledigt)
  5. Backend: List-Endpunkt in `luna-core` für die letzten Eingaben (Status und daraus entstandene Elemente) (erledigt)
  6. Frontend: Seite für die manuelle Eingabe (visuelle Planung in [frontend/docs](frontend/docs/README.md)),
     in Teilschritten:
     - 6a Grundgerüst mit Layout, Navigation und Platzhaltern (erledigt)
     - 6b Anmeldung: Realm `luna` per Realm-Import im Stack, Client fürs Frontend, OIDC mit
       `oidc-client-ts` + `react-oidc-context` (erledigt)
     - 6c Schnelleingabe mit Liste der letzten Eingaben, aufgeteilt in:
       - 6c-1 Push-Signal in `luna-core` per STOMP/WebSocket bei Statuswechsel einer Eingabe (erledigt)
       - 6c-2 Frontend: generierter API-Wrapper, MobX-Store, Seite und STOMP-Abonnement
     - 6d nginx-Image und `swarm/frontend.yml`
     - 6e E2E-Tests mit Playwright
  7. Danach weitere Quellen (z. B. E-Mail)

  Keiner dieser Schritte ist ohne ausdrücklichen Auftrag umzusetzen.

Betrieb: siehe Abschnitt [Betrieb](#betrieb) (Heimnetz, Docker Stack).

Bei offenen Punkten keine Entscheidungen eigenmächtig treffen, sondern Vorschläge zur Auswahl vorlegen (siehe Grundprinzip 3).

### Backends und ihr Zweck

| Backend | Zweck | Details |
|---|---|---|
| `luna-core` | **Event-Analyse:** empfängt Events aus unterschiedlichen Quellen, analysiert sie per **Spring AI** und entscheidet, welche neuen Elemente (z. B. Aufgabe, Alert) bzw. Events daraus entstehen. | [luna-core/README.md](luna-core/README.md) |

## Grundprinzipien

1. **Klein und nachvollziehbar.** Lieber mehrere kleine, fokussierte Änderungen als eine große.
2. **Erst verstehen, dann ändern.** Vor Änderungen den bestehenden Code und diese Datei lesen.
3. **Niemals Annahmen treffen.** Diese Regel gilt ausnahmslos – auch für scheinbar kleine Details wie Namen,
   Typen, Pfade, Versionen oder Standardwerte. Bei jeder Unklarheit **nachfragen** und **konkrete Vorschläge zur
   Auswahl** vorlegen (mit kurzer Begründung bzw. Vor- und Nachteilen, ggf. einer Empfehlung). Optionen werden
   **immer anklickbar zur Auswahl** angeboten (z. B. über einen Auswahl-Dialog), nicht als Text, den der Benutzer
   abtippen muss; freie Eingabe bleibt als zusätzliche Möglichkeit offen. Die Entscheidung
   trifft der Mensch; erst danach umsetzen. Was nicht entschieden ist, wird als offen (_TODO_) markiert,
   nicht stillschweigend festgelegt.
4. **Einfachheit vor Abstraktion.** Keine spekulativen Erweiterungspunkte, keine unnötigen Abhängigkeiten.
5. **Ehrlich berichten.** Fehlgeschlagene Tests, übersprungene Schritte und offene Punkte klar benennen.
6. **Persönliche Ansprache.** Den Benutzer mit seinem Namen ansprechen. Ist der Name nicht bekannt,
   nachfragen und keinen Namen annehmen.

## Tech-Stack

### Backends (`luna-core/` und weitere)

| Thema | Entscheidung |
|---|---|
| Sprache | Java **25** |
| Framework | Spring Boot **4.1.1 oder neuer** |
| Build-Tool | **Maven** (mit Maven Wrapper `mvnw`) |
| Basispaket / groupId | `de.kirchrath.luna` |
| Datenbank | **PostgreSQL** |
| Persistenz / ORM | **Spring Data JDBC**, Entities sind **Records** |
| Schema-Migration | **Flyway** (versionierte SQL-Migrationen unter `src/main/resources/db/migration`) |
| Authentifizierung | **OIDC** mit **Keycloak** als IdP; Backend ist Spring-Security-**Resource-Server** (JWT) |
| API-Dokumentation | **springdoc-openapi** (Code-first) |
| Container-Image | **Jib** (Maven-Plugin), Basis-Image **`amazoncorretto:25`**, nur **`linux/amd64`** |
| Tests | **JUnit 5**, **Spring Boot Test**, **Testcontainers** (PostgreSQL, Keycloak, Kafka) |
| Service-Kommunikation | **Kafka** – Kommunikation zwischen Services **ausschließlich** über Kafka |
| KI / Event-Analyse | **Spring AI 2.1.0** (in `luna-core`); Anbieter konfigurierbar über `spring.ai.model.chat` (Ollama, Anthropic Claude, OpenAI-kompatibel), kein Standard; nicht genutzte Modellarten mit `none` abgeschaltet |
| Mapping | **MapStruct 1.6.3** (Mapper in `converter`) |
| Lombok | Version von Spring Boot verwaltet; **nur** `@Slf4j`, `@Builder(toBuilder = true)` und `@With` |
| Architekturtests | **ArchUnit 1.5.1** (`archunit-junit5`) |
| Testabdeckung | **JaCoCo**, **100 % in allen Zählern** (Instruction, Line, Branch, Complexity, Method, Class), Build bricht bei Unterschreitung ab |

- Backends werden primär in Java mit Spring Boot geschrieben. Abweichungen nur nach Rücksprache.
- **Mehrere Backends:** Luna besteht aus mehreren Backends. Das erste ist **`luna-core`**.
  - Jedes Backend liegt in einem **eigenen Ordner auf oberster Ebene**, benannt nach dem Service (z. B. `luna-core/`).
  - **Benennung:** `artifactId` = Ordnername (z. B. `luna-core`), Java-Paket = `de.kirchrath.luna.` + Service-Name
    ohne `luna-` (z. B. `de.kirchrath.luna.core`).
  - Jedes Backend ist ein **unabhängiges Maven-Projekt** mit eigener `pom.xml` und eigenem `mvnw`
    (keine Parent-POM, keine Multi-Modul-Struktur).
  - Jedes Backend bekommt einen **eigenen festen Port** (siehe [Services & Ports](#services--ports)),
    damit alle Services lokal gleichzeitig laufen können.
  - Neue Backends in der Port-Tabelle, der Repository-Struktur und im Stack eintragen.
- **Kommunikation zwischen Services:** Backends kommunizieren untereinander **immer und ausschließlich
  über Kafka**. Keine direkten REST- oder sonstigen synchronen Aufrufe zwischen Backends.
  Das Frontend ruft die Backends weiterhin über den generierten REST-Wrapper auf (siehe [Frontend](#frontend-frontend)).
  - **Topic-Namen:** `luna_<erzeuger>_<inhalt>` – `<erzeuger>` ist der Service, der in das Topic schreibt
    (Service-Name ohne `luna-`), `<inhalt>` der Inhalt; beides **Englisch**, kleingeschrieben.
    Beispiele: `luna_mail_email` (ein Mail-Service liefert E-Mails), `luna_core_task` (`luna-core` erzeugt Aufgaben).
    Retry-Topics: `luna_<konsument>_<inhalt>_retry` (z. B. `luna_core_email_retry`), Dead-Letter-Topic je Service:
    `luna_<konsument>_dlt` (z. B. `luna_core_dlt`).
  - **Topic-Namen kommen immer aus der `application.yml`** (z. B. `app.kafka.topics.<inhalt>`), nie aus dem Code.
  - **Typisiert senden:** `KafkaTemplate<UUID, <Basis-Event>>` mit UUID-Key-Serializer und JSON-Value-Serializer
    (ohne Typ-Header; der Typ ergibt sich aus dem Topic und der Property `type`). Alle veröffentlichten Events implementieren ein
    **sealed Interface** (z. B. `ElementEventDto`) mit `UUID id()`; der Kafka-Key ist immer `id()` des Events.
    Das Interface trägt den Discriminator **`type`** (Werte = Namen des zugehörigen DTO-Enums, als Konstanten in
    `<Enum>.Constants`) per `@JsonTypeInfo`/`@JsonSubTypes` sowie `@Schema(oneOf, discriminatorProperty,
    discriminatorMapping)`, damit Typ und Untertypen in `kafka-api.yaml` veröffentlicht werden.
  - **Kafka-API:** Jeder Service, der per Kafka veröffentlicht, erzeugt im `GenerateOpenApiTest` zusätzlich
    **`<backend>/kafka-api.yaml`** (OpenAPI) mit allen veröffentlichten DTOs und dem Topic je Event (eingecheckt).
    Jedes Feld dieser DTOs trägt `@Schema(requiredMode = …)` (`REQUIRED` bzw. `NOT_REQUIRED`).
- **Push an das Frontend:** Backends melden Änderungen per **STOMP über WebSocket** (Spring
  `@EnableWebSocketMessageBroker`, einfacher Broker, Endpunkt `/ws`, Topics unter `/topic/…`; Pfade aus der
  `application.yml`). Nachrichten sind **Signale** (DTO mit ID), die Daten lädt der Client per REST.
  Gesendet wird erst **nach dem Commit** (Spring-Ereignis + `@TransactionalEventListener(AFTER_COMMIT)`).
  Das JWT wird nur am WebSocket-Endpunkt auch als Query-Parameter `access_token` akzeptiert.
- **OpenAPI (Code-first):** Die Spezifikation wird mit springdoc aus den Controllern erzeugt.
  Controller und DTOs sauber annotieren, damit die Spec vollständig und präzise ist
  (Operation-IDs, Schemas, Response-Codes) – sie ist der Vertrag für den Frontend-Wrapper.
- **Docker-Image:** Jedes Backend stellt **immer** ein Docker-Image bereit, gebaut mit Jib.
  - Kein eigenes `Dockerfile` für Backends; die Image-Konfiguration liegt in der `pom.xml`.
  - Images werden **lokal** in den Docker-Daemon gebaut (`jib:dockerBuild`), nur für `linux/amd64`.
    Keine Registry konfigurieren (siehe [Betrieb](#betrieb)).
- **Datenbank:** Alle Backends nutzen dieselbe Luna-Datenbank, aber jedes Backend hat ein **eigenes Schema**.
  Schemaänderungen ausschließlich über neue Flyway-Migrationen; bestehende Migrationen nie ändern.
- **CORS:** Lokal ruft das Frontend die Backends direkt auf; die Backends erlauben daher lokal CORS für
  `http://localhost:5173`. In Produktion ist kein CORS nötig (gleiche Origin über Traefik).

### Regeln für Java-Services

#### Package-Struktur

Im Haupt-Package (z. B. `de.kirchrath.luna.core`) liegt **nur** `Application.java`. Alle weiteren Klassen liegen
in diesen Sub-Packages:

| Package | Inhalt |
|---|---|
| `config` | Konfigurationen |
| `inbound` | Handler und Controller |
| `data.entity` | Entities (Records) |
| `data.repository` | Spring-Data-JDBC-Repositories |
| `dto` | DTOs (Records) |
| `converter` | Converter (`@Component`, `<X>Converter`) und MapStruct-Mapper (`<X>Mapper`) zwischen `data` und `dto` |
| `exception` | Exceptions |
| `outbound` | Clients zu externen Systemen |
| `services` | **die komplette Logik** des Systems, in `@Component`- oder `@Service`-Klassen |

#### Entities

- ORM ist **Spring Data JDBC**; Entities sind **Records**.
- Jede Entity hat: `id` (**`UUID`**), `createdDate` und `lastModifiedDate` (**`OffsetDateTime`**) sowie
  `version` (**`Long`**).
  Diese Felder werden **per Annotation oder per Lifecycle-Event** gesetzt, nie manuell in der Fachlogik.
  - **ID:** Jede Entity implementiert **`EntityWithId<T>`** (`data.entity`) mit `id()` und `withId(UUID)`
    (liefert eine Kopie mit neuer ID). Der zentrale Listener **`EntityIdCallback`** (`config`,
    `BeforeConvertCallback`) setzt vor dem Speichern per `UuidProvider` eine neue ID, wenn `id()` `null` ist.
  - **Audit-Felder:** `@CreatedDate`, `@LastModifiedDate` (Auditing mit dem `TimeProvider`) und `@Version`.
- **Enums** für Entities und DTOs sind **getrennt** (z. B. `LunaElementType` in `data.entity`,
  `LunaElementTypeDto` in `dto`) und werden über Converter aufeinander abgebildet (siehe unten).
- **Enums** werden in der Datenbank als **String** gespeichert. Die erlaubten Werte werden **nicht** als
  Constraint in der Migration gepflegt.

#### Converter und Architekturregeln

- **`data` und `dto` kennen sich nicht:** Nichts in `data` darf auf `dto` referenzieren und umgekehrt.
- Abbildungen zwischen beiden erledigen **Converter-Komponenten** im Package **`converter`**:
  - `<X>Converter` – `@Component`, wird von `services` genutzt und kann zusätzliche Logik kapseln.
  - `<X>Mapper` – MapStruct-Interface mit `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)`,
    wird nur vom zugehörigen Converter genutzt.
  - Converter fallen unter die 100-%-Abdeckung; ausgenommen sind nur die von MapStruct generierten
    `*MapperImpl`-Klassen (enthalten bei Enums einen unerreichbaren `default`-Zweig).
- **ArchUnit-Tests in allen Services:** Jeder Service hat einen **`ArchitectureTest`** im Haupt-Testpaket
  (`@AnalyzeClasses` auf das Basispaket, ohne Testklassen). Architekturregeln werden dort als `@ArchTest`-Methoden
  nach dem üblichen Test-Namensschema und mit given/when/then ergänzt.
  Bisherige Regeln: `data` → `dto` verboten, `dto` → `data` verboten sowie die Regeln aus
  [Service-Schichten und Transaktionen](#service-schichten-und-transaktionen).

#### Service-Schichten und Transaktionen

- **`<X>DomainService`** (Package `services`, `@Transactional`) ist die **Transaktionsgrenze**. Er darf nur von
  `<X>ApplicationService`s aufgerufen werden.
- **`<X>ApplicationService`** (Package `services`) **leitet nur weiter** und enthält **keine Logik**; er existiert
  als Schicht für spätere Kompositionen. Je Anwendungsfall (öffentliche Methode) ruft er **genau eine**
  DomainService-Methode auf. Als Felder (per Konstruktor injiziert) hat er **nur DomainServices**. Parameter und
  Rückgabetypen (inkl. generischer Typargumente) sind nur DTO-artige Typen: `dto`, `java.*`, primitive Typen sowie
  Spring Datas `Pageable` und `Page` – **keine Entities**. Scheduler arbeiten daher z. B. mit IDs.
- Die **Logik** liegt im DomainService. Aufrufe externer Systeme (z. B. KI-Anbieter) kapseln Clients im Package
  `outbound`, die der DomainService nutzt (der Aufruf läuft damit innerhalb der Transaktion).
- ApplicationServices werden **nur** von Handlern und Controllern (`inbound`) sowie von Schedulern
  (**`<X>Scheduler`** im Package `services`) aufgerufen.
- **Repositories** dürfen nur von DomainServices und von Komponenten niedrigerer Hierarchie genutzt werden; diese
  Komponenten tragen **`@Transactional(propagation = Propagation.MANDATORY)`**, damit sie nur innerhalb einer
  offenen Transaktion laufen.
- **Keine eigenen Queries** (`@Query`), wenn es mit normalen Repository-Methoden (abgeleitete Methoden, `Limit`,
  `Sort`, `Pageable`) oder per Criteria/`JdbcAggregateTemplate` realisierbar ist.
- Alle Regeln dieses Abschnitts sind im `ArchitectureTest` abgesichert.

#### Fehlerbehandlung

- **Kafka-Handler (Consumer):** Exceptions, die bis zum Handler gelangen, werden über ein **Retry-Topic**
  wiederholt. Anzahl der Wiederholungen je Topic konfigurierbar, **Standard 10**. Danach landet das Event auf einem
  **Dead-Letter-Topic** – je Ursprungstopic konfigurierbar, Standard ist ein gemeinsames DLT je Service
  (Namen siehe Topic-Namen). Umsetzung mit dem ersten Kafka-Consumer eines Services.
- **Scheduler:** Fehler spiegeln sich im **Status der verarbeiteten Entität** wider (z. B. `attempts`, `FAILED`).
- **REST:** Ist der Client für den Fehler verantwortlich, antwortet der Service mit einem passenden
  **Client-Error (4xx)**; Server-Errors helfen dem Anwender nicht weiter und sind zu vermeiden.

#### Keine „Magic Strings“

- **Interne** Zeichenketten nur als **gut benannte Konstanten**.
- **Externe** Werte (Topics, Prompts/Ressourcen, URLs, Benutzer-/Anwendungsnamen) kommen **ausschließlich aus der
  `application.yml`** (gebunden per `@ConfigurationProperties`, keine `@Value`-Literale im Code). Werte, die bereits
  als Property existieren, werden referenziert (z. B. `${spring.application.name}`) statt wiederholt.
- **Review-Checkliste:**
  - Steht irgendwo ein String-Literal, das kein Log-/Fehlertext und keine benannte Konstante ist?
  - Wird ein Topic, Prompt, Pfad oder Name im Code statt in der `application.yml` festgelegt?
  - Ist ein Wert doppelt gepflegt, obwohl er per Platzhalter referenziert werden könnte?

#### Lombok

- Lombok wird **nur** für diese Annotationen verwendet: **`@Slf4j`** (Logging), **`@Builder(toBuilder = true)`**
  und **`@With`** (z. B. an Record-Komponenten von Entities statt handgeschriebener `with…`-Methoden).
- Alle anderen Lombok-Features (z. B. `@Data`, `@Getter`, `@RequiredArgsConstructor`, `@Value`) werden nicht
  genutzt; Konstruktoren für Dependency Injection werden explizit geschrieben.

#### Keine statischen Methoden

- Statische Methoden dürfen im **Produktionscode** nicht direkt verwendet werden, damit alles testbar bleibt.
  **In Tests sind statische Methodenaufrufe erlaubt** (z. B. `Path.of`, AssertJ, MockMvc-Builder).
- **Erlaubt sind reine Factory-Methoden** ohne Seiteneffekte und ohne Nichtdeterminismus, z. B. `List.of`,
  `Set.of`, `Map.of`, `Optional.of`/`ofNullable`/`empty`. Verboten bleiben Aufrufe, die von Zeit, Zufall oder
  Umgebung abhängen – dafür gibt es Provider (siehe unten).
- **Logging** ausschließlich über Lombok **`@Slf4j`** (kein direkter Aufruf von `LoggerFactory`).
- IDs kommen aus einem **`UuidProvider`** (statt `UUID.randomUUID()`).
- Zeitpunkte kommen aus einem **`TimeProvider`** (statt `OffsetDateTime.now()`, `Instant.now()` o. ä.).
  Er liefert `OffsetDateTime` mit dem Offset der **Zeitzone des Servers**.
  Im Stack übernehmen die Backend-Container die Zeitzone **vom Host**: `/etc/localtime` wird read-only
  eingebunden (siehe [Betrieb](#betrieb)).
- `UuidProvider` und `TimeProvider` liegen im Package **`services`**.
- Für weitere statische Aufrufe ebenso einen injizierbaren Provider anlegen.

#### DTOs

- **Alles, was an Daten das System verlässt, ist ein DTO.** Klassennamen enden **immer auf `Dto`**.
- DTOs sind **Records**. Es gibt drei Arten:

1. **List-Dtos** – Antworten auf List-Anfragen, Name `<Entity>ListDto`:
   - `items`: Liste von `<Entity>ListItemDto`
   - `totalPages`, `totalCount`
   - Ein `<Entity>ListItemDto` enthält die **ID** der Entität und **nur die Daten, die für die Darstellung in
     einer Liste nötig sind**.
2. **Entity-Dtos** – Name `<Entity>Dto`, enthält **nicht** die ID der Entität, sondern:
   - `data`: `<Entity>DataDto` mit den eigentlichen Daten
   - `audit`: `AuditDto` mit `createdDate`, `lastModifiedDate` (`OffsetDateTime`) und `version` (`Long`)
3. **Eingebettete Dtos** – Records, die weder ein List-Dto noch direkt eine Entität repräsentieren
   (Name endet ebenfalls auf `Dto`).

- **Anlegen von Entitäten:** Endpunkte, die eine Entität anlegen, antworten in **allen Services** mit einem
  **`ResourceReferenceDto`**, dessen einzige Property `id` (`UUID`) ist.

Beispiel für eine Entität `Task` (Felder und Typen nur zur Veranschaulichung, keine Festlegung):

```java
record TaskListDto(List<TaskListItemDto> items, int totalPages, long totalCount) {}
record TaskListItemDto(UUID id, String title) {}
record TaskDto(TaskDataDto data, AuditDto audit) {}
record TaskDataDto(String title, String description) {}
```

- **Pagination** von List-Anfragen über die Query-Parameter `page` (**ab 0**) und `size`.
  Der Controller nimmt ein Spring-`Pageable` entgegen (Standard/Obergrenze über `spring.data.web.pageable.*`,
  zu große Werte werden gekappt; `sort` ist in der OpenAPI ausgeblendet) und reicht es über den ApplicationService
  an den DomainService weiter; die Sortierung legt der DomainService fest.

#### API / OpenAPI

- Die API wird per **OpenAPI** veröffentlicht.
- Ein **spezieller Test** erzeugt die Spezifikation und schreibt sie nach **`<backend>/openapi.yaml`**
  (z. B. `luna-core/openapi.yaml`). Die Datei ist eingecheckt; aus ihr wird der Frontend-Wrapper generiert.
  - Der Test heißt **`GenerateOpenApiTest`** und liegt in einem **eigenen Quellverzeichnis `src/openapi/java`**
    (neben `src/main` und `src/test`).
  - Er läuft **nur mit dem Maven-Profil `openapi`** (`./mvnw verify -Popenapi`); normale Testläufe verändern
    `openapi.yaml` nicht. Das Profil bindet `src/openapi/java` per `build-helper-maven-plugin` als Testquelle ein.
  - Titel, Version und Server stehen per `@OpenAPIDefinition` an `Application.java`: Titel z. B. `Luna Core API`,
    Version = Projekt-Version (`spring.application.version: @project.version@` in `application.yml`),
    Server = Pfad-Präfix in Produktion (z. B. `/api/luna-core`).
  - Ohne Token erreichbar sind `/v3/api-docs/**`, `/v3/api-docs.yaml`, die Swagger-UI (`/swagger-ui.html`,
    `/swagger-ui/**`) und `/actuator/health`.

#### Konfiguration

- Es gibt immer `application.yml` sowie die Profile **`application-local.yml`** und **`application-prod.yml`**.
- In `application.yml` sind **alle** verfügbaren Properties definiert. Werte, die durch ein Profil gesetzt werden
  müssen, sind dort bewusst definiert, aber **leer** gelassen.

#### Tests

- **100 % Testabdeckung** in **allen** JaCoCo-Zählern (Instruction, Line, Branch, Complexity, Method, Class),
  per JaCoCo-`check` erzwungen.
  Ausgenommen: `Application.java`, die Packages `config`, `inbound`, `data`, `dto` und `exception` sowie die
  von MapStruct generierten `*MapperImpl`-Klassen.
- Testnamen nach dem Muster **`thatActionShouldResultIntoX`**. Einzige Ausnahme: der Haupt-Test
  `ApplicationTests.contextLoads()` wie in Spring-Initializr-Projekten.
- Tests, die den Spring-Kontext starten, nutzen PostgreSQL über **Testcontainers** (`config/TestcontainersConfig`
  im Test-Quellbaum, `@ServiceConnection`, Image `postgres:18.7`). Der Container setzt wie die echte Konfiguration
  `currentSchema=<schema>` in der JDBC-URL (`withUrlParam`), sonst findet Spring Data JDBC die Tabellen nicht. Kafka-Integrationstests nutzen ebenso
  **Testcontainers** (Image `apache/kafka:4.4.0`, `@ServiceConnection`), kein Embedded Kafka.
- Jeder Test folgt den drei Sektionen **given / when / then**, jeweils als Kommentar markiert:

```java
@Test
void thatCreatingATaskShouldResultIntoAPersistedTask() {
    // given
    ...
    // when
    ...
    // then
    ...
}
```

### Frontend (`frontend/`)

| Thema | Entscheidung |
|---|---|
| Framework | **React** |
| Routing | **React Router** |
| Sprache | **TypeScript**, `strict` plus Zusatzprüfungen (u. a. `noUncheckedIndexedAccess`) |
| Build-Tool | **Vite** |
| Paketmanager | **npm** |
| Node.js | aktuelle **LTS (Node 24)**, festgehalten in `.nvmrc` und `engines` |
| CSS | **Bulma 1.x** über **Sass** (Anpassung über Sass-Variablen) |
| UI-Komponenten | **keine UI-Bibliothek** – eigene Komponenten in `src/ui` |
| API-Zugriff | generierter Wrapper in `src/api/<backend>/` (**openapi-typescript** + **openapi-fetch**) |
| State | **MobX** mit **mobx-react-lite**; Stores unter `src/stores/`, ein `RootStore` per React-Context (`StoresProvider`, `useStores()`) |
| i18n | **react-i18next**; Sprachen **Deutsch (Standard/Fallback)** und **Englisch** |
| Tests | **Vitest** + **Testing Library**; E2E mit **Playwright** |
| Testabdeckung | **100 %** (Statements, Branches, Functions, Lines), per Vitest-Coverage (v8) erzwungen; ausgenommen `main.tsx`, Typdefinitionen und der generierte API-Wrapper |
| Codequalität | **ESLint** (typescript-eslint `strictTypeChecked`, React-Hooks) und **Prettier** (`npm run lint`) |
| Icons | **lucide-react**; Logo als eigene Komponente |
| Schriften | **Inter** und **Space Grotesk** (Überschriften), lokal über `@fontsource-variable` |
| Design | Ruhige, graue Oberfläche: Hintergrund **`#f3f4f6`** (Bulma `--bulma-background`), Primärfarbe **`#4a5260`** (Bulma `$primary`); kräftige Farbe nur für Highlights: Petrol **`#1f7a8c`** (Bulma `$link`); dunkler Modus automatisch nach Geräteeinstellung |
| Install-Skripte | npm 11 führt Install-Skripte von Abhängigkeiten nur nach Freigabe aus (`allowScripts` in `package.json`). Jedes neue Skript **prüfen** und bewusst freigeben (`true`) oder ablehnen (`false`); abgelehnt sind `@parcel/watcher` und `fsevents` (bringen fertige Binaries mit, die Skripte würden nur nativ kompilieren) |
| Versionen | in `package.json` **exakt gepinnt** (`.npmrc`: `save-exact`), `package-lock.json` eingecheckt; TypeScript **5.9.x** (neuere Versionen werden von typescript-eslint und openapi-typescript noch nicht unterstützt) |

- **Keine UI-Komponentenbibliothek** (kein MUI, Ant Design, Chakra, shadcn o. ä.).
  Wiederverwendbare Komponenten werden im Projekt erstellt und unter **`src/ui`** abgelegt.
  - Vor dem Bau einer neuen Komponente prüfen, ob in `src/ui` bereits eine passende existiert oder erweitert werden kann.
  - Komponenten in `src/ui` sind fachneutral (keine Geschäftslogik, keine API-Aufrufe) und nutzen Bulma-Klassen.
- **Backend-Zugriff:** Eigene Backends werden **ausschließlich** über den aus der OpenAPI-Spec generierten
  TypeScript-Wrapper unter **`src/api`** aufgerufen.
  - **Ein Unterordner pro Backend** (z. B. `src/api/luna-core/`) mit eigenem Client und eigenen Typen;
    jedes Backend wird separat generiert.
  - Die **Basis-URL jedes Backends ist konfigurierbar** (Vite-Umgebungsvariable pro Backend):
    lokal direkt der Backend-Port (z. B. `http://localhost:7081`), in Produktion das Pfad-Präfix
    (z. B. `/api/luna-core`). Keine URLs fest im Code.
  - Keine direkten `fetch`/`axios`-Aufrufe an eigene Backends außerhalb von `src/api`.
  - **Jede Kommunikation mit einem Backend läuft über MobX-Stores** (ausgenommen die OIDC-Anmeldung).
    Komponenten rufen den API-Wrapper nie direkt auf, sondern lesen Daten und Ladezustand aus Stores
    (`observer`) und lösen Aktionen über Store-Methoden aus. Alle Stores erzeugt der `RootStore`.
  - Reiner UI-Zustand (z. B. Sprache, eingeklappte Navigation) bleibt in React-State bzw. i18next.
  - Generierter Code wird **eingecheckt** und nie von Hand geändert. Bei API-Änderungen: Backend anpassen,
    Spec erzeugen, Wrapper neu generieren und im selben Commit mit einchecken.
- **Docker-Image:** Das Frontend wird als eigenes **nginx-Image** ausgeliefert (Dockerfile im Frontend-Projekt,
  nur `linux/amd64`, lokal gebaut). nginx liefert ausschließlich die statischen Dateien aus;
  API-Aufrufe routet **Traefik direkt an das jeweilige Backend**.
- **Texte:** Keine fest codierten UI-Texte; alle Texte über react-i18next. Neue Schlüssel immer in
  **Deutsch und Englisch** anlegen.
- **Barrel-Dateien:** Jeder Ordner unter `src/` hat ein `index.ts`, das alle Dateien und Unterordner exportiert.
  Von außen wird ein Ordner **nur über sein `index.ts`** importiert (z. B. `from '../ui'`); innerhalb eines Ordners
  importieren sich Dateien **direkt** (z. B. `from './Sidebar'`), damit keine zirkulären Importe entstehen.
  ESLint (`no-restricted-imports`) verbietet Importe an einem `index.ts` vorbei; ausgenommen sind Stylesheets.
- **Anmeldung (OIDC):** `oidc-client-ts` + `react-oidc-context` gegen den Keycloak-Client `luna-frontend`.
  - Die ganze App ist geschützt: Ohne Anmeldung leitet `AuthGate` sofort zu Keycloak weiter und kehrt danach zur
    angeforderten Seite zurück (Pfad im OIDC-`state`). Abmelden beendet auch die Keycloak-Sitzung.
  - Tokens im **sessionStorage**, Erneuerung automatisch per Refresh-Token.
  - Konfiguration über `VITE_OIDC_AUTHORITY` und `VITE_OIDC_CLIENT_ID` in **`.env.development`** und
    **`.env.production`**. Diese beiden Dateien sind **eingecheckt** und enthalten **nur öffentliche Werte** – eine
    bewusste Ausnahme von der Regel „keine `.env`-Dateien committen“; Secrets gehören nie hinein.
  - Router und Stores entstehen erst nach der Anmeldung (`App`), damit der Router die wiederhergestellte Adresse liest.
  - In Tests ersetzt `fakeAuth()` (`src/test`) den `AuthContext`; `react-oidc-context` wird nicht gemockt.
- **Routen** sind englisch (`/`, `/tasks`, `/calendar`, `/shopping`, `/finance`, `/monitoring`) und stehen in
  `src/navigation.ts`.
- **Sprache:** Start auf Deutsch; die Wahl im DE/EN-Umschalter wird im `localStorage` gemerkt.
- **Tests** folgen denselben Konventionen wie im Backend: Namen `that … should result into …`, Abschnitte
  `// given`, `// when`, `// then`.

### Ende-zu-Ende

- **Playwright** für E2E-Tests über Frontend und Backend hinweg.

## Services & Ports

Jeder Service hat einen festen lokalen Port, damit alle gleichzeitig gestartet werden können.
Ports für Backends werden **ab 7080 fortlaufend** vergeben (7080 ist durch Keycloak belegt).

| Service | Port lokal | Pfad-Präfix (Produktion) |
|---|---|---|
| Frontend (Vite-Dev-Server) | 5173 | `/` |
| PostgreSQL | 7432 | – |
| Kafka | 7094 | – |
| Keycloak | 7080 | `/auth` (Keycloak mit relativem Pfad `/auth` konfiguriert, **ohne** StripPrefix) |
| `luna-core` | 7081 | `/api/luna-core` |
| _nächstes Backend_ | 7082 | `/api/<backend-name>` |

## Betrieb

Luna läuft im **Heimnetz** als **Docker Stack** (Docker Swarm, `docker stack deploy`) auf **einem Knoten**.

| Thema | Entscheidung |
|---|---|
| Orchestrierung | Docker Stack (Swarm), **ein Knoten** |
| Architektur | nur **`linux/amd64`** |
| Container-Registry | **keine** – Images werden lokal auf dem Knoten gebaut |
| Reverse Proxy | **Traefik** im Stack (Routing über Service-Labels; `/api/<backend-name>` an das jeweilige Backend **mit StripPrefix**, alles andere ans Frontend) |
| Domain | **`luna.internal`** (nur im Heimnetz) |
| Namensauflösung | vorerst über die **hosts-Datei** der einzelnen Geräte |
| HTTPS | eigene CA mit **step-ca** im Stack; Traefik bezieht und erneuert Zertifikate per **ACME** von step-ca |
| Identity Provider | **Keycloak** läuft im Luna-Stack |
| Datenbank | **eine PostgreSQL-Instanz**; getrennte Datenbanken für Luna und Keycloak; in der Luna-Datenbank **ein Schema pro Backend** |

Dienste im Stack: Traefik, step-ca, Frontend (nginx), alle Backends (Jib-Images), Keycloak, PostgreSQL, Kafka.

- **Stack-Dateien** liegen unter `swarm/`. Ein Stack `luna`, mehrere Dateien:
  `swarm/infrastructure.yml` (Traefik, step-ca, PostgreSQL, Keycloak), `swarm/kafka.yml` (Kafka und `kafka-init`),
  `swarm/luna-core.yml` (Backend `luna-core`), später weitere für Frontend und Backends (je Backend eine Datei
  `swarm/<backend>.yml`).
  Deploy gemeinsam: `docker stack deploy -c swarm/infrastructure.yml -c swarm/kafka.yml -c swarm/luna-core.yml [-c …] luna`
  (Pflichtvariablen siehe README).
  Kafka steht in einer eigenen Datei, weil sein Zertifikat erst erzeugt werden kann, wenn step-ca läuft.
- **Image-Versionen** sind **exakt gepinnt** (aktuell Traefik `v3.7.19`, step-ca `0.30.2`, PostgreSQL `18.7`,
  Keycloak `26.8.2`, Kafka `apache/kafka:4.4.0`). Updates nur bewusst per Commit.
- **Secrets** sind **externe Docker Secrets** (`docker secret create` auf dem Server).
  Backends lesen sie per **`spring.config.import=optional:configtree:/run/secrets/`** (Profil `prod`); das Secret
  wird dazu mit dem **Property-Namen als Ziel** eingebunden (z. B. `target: spring.datasource.password`).
- **Backend-Images** werden im Stack über eine Versions-Variable referenziert (z. B. `luna-core:${LUNA_CORE_VERSION}`),
  nie über `latest`.
- Die Infrastruktur-Datei dient **auch der lokalen Entwicklung**; PostgreSQL (7432) und Keycloak (7080) sind
  daher auch auf dem Server veröffentlicht. Die Keycloak-URL (Issuer) ist über `LUNA_KEYCLOAK_URL` umschaltbar.
- **Kafka** (`apache/kafka`, KRaft-Modus, ein Knoten) läuft in `swarm/kafka.yml`, ist wie PostgreSQL und
  Keycloak auch auf dem Server unter **7094** veröffentlicht (lokale Entwicklung) und speichert seine Daten im
  Volume `kafka-data`. Backends im Stack nutzen `kafka:9094`; die für externe Clients mitgeteilte Adresse ist über
  `LUNA_KAFKA_HOST` umschaltbar (Standard `localhost:7094`, auf dem Server z. B. `luna.internal:7094`).
- **Kafka-Sicherheit:** **immer TLS** und **SASL/SCRAM-SHA-512** (auch lokal); nur der Controller-Listener lauscht
  ohne TLS auf `localhost` im Container.
  - Zertifikat von step-ca, **1 Jahr** gültig, über einen eigenen Provisioner `kafka`; erzeugt per
    `swarm/kafka/create-certificate.sh <n>` als **versionierte Secrets** `kafka_keystore_v<n>` /
    `kafka_keystore_password_v<n>`, ausgewählt über `LUNA_KAFKA_CERT_VERSION`.
  - **Ein Kafka-Benutzer je Service** (Name = Service-Name, Passwort-Secret `kafka_<service mit _>_password`),
    Superuser `admin` (`kafka_admin_password`).
  - **ACLs je Service:** schreiben (inkl. automatischer Topic-Anlage beim ersten Schreiben) nur auf Topics mit dem
    eigenen Präfix `luna_<service ohne luna->_`; lesen nur **ausdrücklich freigegebene** Topics;
    Consumer-Gruppe = Service-Name.
  - Benutzer und ACLs pflegt der Init-Service **`kafka-init`** idempotent bei jedem Deploy aus `LUNA_KAFKA_SERVICES`
    in `swarm/kafka.yml`.
  - Kafka-Tests (Testcontainers) laufen **ohne** TLS/SASL.
- **Zeitzone:** **Alle** Container (Infrastruktur und Backends) binden `/etc/localtime:/etc/localtime:ro`
  vom Host ein; Logs und `TimeProvider` nutzen so die Zeitzone des Servers.
- Traefik leitet HTTP auf HTTPS um; **kein Traefik-Dashboard**.
- PostgreSQL legt beim ersten Start per Init-Skript die Datenbanken `keycloak` und `luna` mit je einem Benutzer an.
- Der **Keycloak-Realm** für Luna heißt **`luna`** (Issuer lokal `http://localhost:7080/auth/realms/luna`,
  im Stack `https://luna.internal/auth/realms/luna`). Er wird aus **`swarm/keycloak/luna-realm.json`** per
  `--import-realm` angelegt – **nur wenn er noch nicht existiert**; spätere Änderungen an der Datei zusätzlich in der
  Admin-Konsole nachziehen. Der Import enthält **keine Benutzer und keine Secrets**.
  - Client **`luna-frontend`**: öffentlich, nur Authorization Code mit **PKCE (S256)**, Weiterleitungen nur an
    `https://luna.internal/*` und `http://localhost:5173/*`.
  - Realm: Anmeldeseite Deutsch (Standard) und Englisch, „Angemeldet bleiben“, keine Registrierung.
- **Schema-Namen** in der Luna-Datenbank: Service-Name mit `_` statt `-` (z. B. `luna_core`).
- Traefik entfernt das Pfad-Präfix vor der Weiterleitung; Backends kennen ihr Präfix nicht und haben
  lokal und in Produktion dieselben Pfade.

- Das Root-Zertifikat von step-ca muss auf jedem Gerät installiert werden, das Luna nutzt.
- **Backends → Keycloak:** Im Stack holen Backends die Schlüssel **intern per HTTP**
  (`jwk-set-uri: http://keycloak:8080/auth/realms/luna/protocol/openid-connect/certs`); die `issuer-uri` bleibt
  `https://luna.internal/auth/realms/luna` und wird im Token geprüft. Dafür ist kein Truststore nötig.
- **Backends → Kafka:** TLS mit dem Root-Zertifikat von step-ca, das **zur Laufzeit** aus dem read-only
  eingebundenen step-ca-Volume gelesen wird (`/step/certs/root_ca.crt`, PEM). Kein eingechecktes Zertifikat,
  kein Truststore im Image.
- Innerhalb des Stacks löst `luna.internal` über einen **Netzwerk-Alias am Traefik-Service** auf Traefik auf.
- **Lokale Entwicklung:** Backends erreichen Keycloak direkt unter `http://localhost:7080/auth` (ohne Traefik/TLS).
  Die Keycloak-/Issuer-URL ist daher je Umgebung konfigurierbar und nie fest im Code.
- Secrets (DB-Passwörter, Keycloak-Admin, Kafka-Passwörter und -Keystore, CA-Schlüssel und -Passwort) als **Docker Secrets** bzw. außerhalb des Repos
  bereitstellen – nie im Stack-File oder Repo.
- **Spätere Ausbaustufe (nicht umsetzen, solange nicht beauftragt):** Umstieg auf eine Unterdomain der echten
  Domain (Registrar domainprovider.de, Nameserver bei **deSEC**) mit **Let's Encrypt** per DNS-Challenge.
- Sollte später ein zweiter Knoten oder eine zweite Architektur dazukommen, wird eine Registry nötig –
  dann zuerst Vorschläge vorlegen.

## Repository-Struktur

Monorepo mit mehreren Backends (je ein Top-Level-Ordner) und einem Frontend:

```
.
├── AGENTS.md        # Diese Datei – Regeln für Agenten
├── README.md        # Projektbeschreibung für Menschen
├── LICENSE          # MIT-Lizenz
├── swarm/           # Docker-Stack-Dateien (infrastructure.yml, kafka.yml, <backend>.yml) und Configs/Skripte
├── luna-core/       # Erstes Backend: Event-Analyse (Spring Boot, Maven, Jib), Port 7081
├── <backend-name>/  # Weitere Backends nach demselben Muster
└── frontend/        # React-Frontend (Vite, npm, nginx-Image)
    ├── docs/        # Visuelle Planung: Wireframes (SVG) und Festlegungen zum Aufbau der Anwendung
    └── src/
        ├── api/     # Generierte Wrapper, ein Unterordner pro Backend (eingecheckt, nicht manuell bearbeiten)
        └── ui/      # Wiederverwendbare, fachneutrale UI-Komponenten auf Basis von Bulma
```

Bei neuen Top-Level-Verzeichnissen diese Übersicht aktualisieren.

## Entwicklungsablauf

### Setup, Build, Test

Backends (Beispiel `luna-core`, benötigt Java 25 und Docker für Testcontainers):

```bash
cd luna-core

# Bauen, testen, Coverage prüfen
./mvnw verify

# OpenAPI-Spezifikation neu erzeugen (schreibt luna-core/openapi.yaml)
./mvnw verify -Popenapi

# Docker-Image lokal bauen (Jib) -> luna-core:<version> und luna-core:latest
./mvnw package jib:dockerBuild
```

Lokal starten: Profil `local` ist Standard; das DB-Passwort kommt aus der Umgebungsvariable
`SPRING_DATASOURCE_PASSWORD` (Spring-Standardnamen, keine Platzhalter in den yml-Dateien).

Frontend (benötigt Node 24, siehe `frontend/.nvmrc`):

```bash
cd frontend
npm ci && npm run lint && npm run coverage && npm run build
npm run dev   # http://localhost:5173
```

Noch nicht angelegt:

```bash
# API-Wrapper eines Backends aus OpenAPI neu generieren
# <befehl>

# E2E-Tests (Playwright)
# <befehl>
```

Vor jedem Commit müssen Build, Tests und Linter (sofern vorhanden) grün sein.

### Tests

- Neue Funktionalität bekommt Tests; Bugfixes einen Regressionstest.
- Integrationstests gegen echte Abhängigkeiten über Testcontainers statt Mocks, wo sinnvoll.
- Tests niemals deaktivieren oder überspringen, nur um eine Pipeline grün zu bekommen.

## Code-Konventionen

- **Sprache im Code:** **Englisch**. Kommentare sind Teil des Codes und daher **immer Englisch** – das gilt für
  alle Kommentare (auch Javadoc, TODO-Kommentare und die `given`/`when`/`then`-Kommentare in Tests) sowie für
  Bezeichner und Commit-Messages.
- **Sprache der Dokumentation:** Deutsch oder Englisch – innerhalb eines Dokuments konsistent bleiben.
- Formatierung und Stil folgen dem jeweiligen Standard-Formatter/Linter des Stacks.
- **Code muss für Menschen gut lesbar sein.** Kommentare veralten schnell und werden daher **nur** gesetzt, wenn der
  Code selbst wirklich zu kompliziert ist, um ohne Erklärung verstanden zu werden. Dann erklären sie das *Warum*.
  Ausnahme: die `given`/`when`/`then`-Kommentare in Tests.
- Keine toten Code-Pfade, auskommentierten Blöcke oder Debug-Ausgaben committen.

## Git & Commits

- Nicht direkt auf `main` arbeiten; Feature-Branches verwenden.
- **Nach dem Merge eines PRs immer einen neuen Branch erstellen.** Auf dem Branch eines gemergten PRs wird nicht
  weitergearbeitet, und er wird auch nicht neu aufgesetzt. Neue Arbeit beginnt immer auf einem **neuen Branch vom
  aktuellen `main`**. Grund: Beim Merge können neue Commit-IDs entstehen (z. B. „Rebase and merge“); weitere
  Commits auf dem alten Branch führen dann zu Merge-Konflikten mit inhaltlich bereits übernommenen Änderungen,
  und das Neu-Aufsetzen erfordert einen Force-Push.
- Commit-Messages **immer** nach [Conventional Commits](https://www.conventionalcommits.org/):
  `<type>(<optional scope>): <description>`, Beschreibung im Imperativ, Englisch, kleingeschrieben.
  - Typen: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`
  - Scopes: Name des Backends (z. B. `luna-core`), `frontend`, `api`, `ui`, `e2e` (bei Bedarf erweitern)
  - Breaking Changes mit `!` nach dem Typ/Scope und/oder `BREAKING CHANGE:` im Footer kennzeichnen.
  - Beispiele: `feat(frontend): add login page`, `fix(luna-core): handle empty response`, `docs: update AGENTS.md`
- Ein Commit = eine logische Änderung.
- Keine History von fremden Branches umschreiben; kein Force-Push ohne ausdrückliche Freigabe.
- Pull Requests nur auf ausdrücklichen Wunsch erstellen.

## Sicherheit

- Keine Secrets (API-Keys, Tokens, Passwörter, Client-Secrets, `.env`-Dateien) committen.
  Ausnahme: `frontend/.env.development` und `frontend/.env.production` mit ausschließlich öffentlichen Werten.
- Neue Abhängigkeiten nur mit Begründung hinzufügen und auf Pflege/Lizenz prüfen.
- Externe Eingaben immer validieren.

## Pflege dieser Datei

Wenn ein Agent etwas lernt, das zukünftigen Agenten hilft (Build-Befehle, Fallstricke, Konventionen),
soll er diese Datei entsprechend ergänzen – knapp und konkret.
