# AGENTS.md

Leitfaden für KI-Agenten (Claude Code, Codex, Copilot u. a.), die an **Luna** arbeiten.
Diese Datei ist die zentrale Quelle für Arbeitsweise, Konventionen und Projektkontext.
Sie wird gepflegt, sobald sich am Projekt etwas Wesentliches ändert.

## Projektstatus

Luna ist ein neues Projekt im Aufbau. Bisher existieren nur `README.md`, `LICENSE` (MIT) und diese Datei.

Noch offen (bitte hier eintragen, sobald entschieden):

- **Ziel / Zweck:** _TODO_
- **DNS-Anbieter für die Let's-Encrypt-DNS-Challenge:** _TODO_

Betrieb: siehe Abschnitt [Betrieb](#betrieb) (Heimnetz, Docker Stack).

Bei offenen Punkten keine Entscheidungen eigenmächtig treffen, sondern Vorschläge zur Auswahl vorlegen (siehe Grundprinzip 3).

## Grundprinzipien

1. **Klein und nachvollziehbar.** Lieber mehrere kleine, fokussierte Änderungen als eine große.
2. **Erst verstehen, dann ändern.** Vor Änderungen den bestehenden Code und diese Datei lesen.
3. **Keine Annahmen treffen.** Bei offenen oder mehrdeutigen Punkten nicht selbst entscheiden, sondern
   **konkrete Vorschläge zur Auswahl** vorlegen (mit kurzer Begründung bzw. Vor- und Nachteilen, ggf. einer
   Empfehlung) und die Entscheidung dem Menschen überlassen. Erst nach der Entscheidung umsetzen.
4. **Einfachheit vor Abstraktion.** Keine spekulativen Erweiterungspunkte, keine unnötigen Abhängigkeiten.
5. **Ehrlich berichten.** Fehlgeschlagene Tests, übersprungene Schritte und offene Punkte klar benennen.

## Tech-Stack

### Backend (`backend/`)

| Thema | Entscheidung |
|---|---|
| Sprache | Java **25** |
| Framework | Spring Boot **4.1.1 oder neuer** |
| Build-Tool | **Maven** (mit Maven Wrapper `mvnw`) |
| Basispaket / groupId | `de.kirchrath.luna` |
| Datenbank | **PostgreSQL** |
| Schema-Migration | **Flyway** (versionierte SQL-Migrationen unter `src/main/resources/db/migration`) |
| Authentifizierung | **OIDC** mit **Keycloak** als IdP; Backend ist Spring-Security-**Resource-Server** (JWT) |
| API-Dokumentation | **springdoc-openapi** (Code-first) |
| Container-Image | **Jib** (Maven-Plugin), Basis-Image **`amazoncorretto:25`**, nur **`linux/amd64`** |
| Tests | **JUnit 5**, **Spring Boot Test**, **Testcontainers** (PostgreSQL, Keycloak) |

- Backends werden primär in Java mit Spring Boot geschrieben. Abweichungen nur nach Rücksprache.
- **OpenAPI (Code-first):** Die Spezifikation wird mit springdoc aus den Controllern erzeugt.
  Controller und DTOs sauber annotieren, damit die Spec vollständig und präzise ist
  (Operation-IDs, Schemas, Response-Codes) – sie ist der Vertrag für den Frontend-Wrapper.
- **Docker-Image:** Jedes Backend stellt **immer** ein Docker-Image bereit, gebaut mit Jib.
  - Kein eigenes `Dockerfile` für Backends; die Image-Konfiguration liegt in der `pom.xml`.
  - Images werden **lokal** in den Docker-Daemon gebaut (`jib:dockerBuild`), nur für `linux/amd64`.
    Keine Registry konfigurieren (siehe [Betrieb](#betrieb)).
- **Datenbank:** Schemaänderungen ausschließlich über neue Flyway-Migrationen; bestehende Migrationen nie ändern.

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
| API-Zugriff | generierter Wrapper in `src/api` (**openapi-typescript** + **openapi-fetch**) |
| i18n | **react-i18next**; Sprachen **Deutsch (Standard/Fallback)** und **Englisch** |
| Tests | **Vitest** + **Testing Library**; E2E mit **Playwright** |

- **Keine UI-Komponentenbibliothek** (kein MUI, Ant Design, Chakra, shadcn o. ä.).
  Wiederverwendbare Komponenten werden im Projekt erstellt und unter **`src/ui`** abgelegt.
  - Vor dem Bau einer neuen Komponente prüfen, ob in `src/ui` bereits eine passende existiert oder erweitert werden kann.
  - Komponenten in `src/ui` sind fachneutral (keine Geschäftslogik, keine API-Aufrufe) und nutzen Bulma-Klassen.
- **Backend-Zugriff:** Eigene Backends werden **ausschließlich** über den aus der OpenAPI-Spec generierten
  TypeScript-Wrapper unter **`src/api`** aufgerufen.
  - Keine direkten `fetch`/`axios`-Aufrufe an eigene Backends außerhalb von `src/api`.
  - Generierter Code wird **eingecheckt** und nie von Hand geändert. Bei API-Änderungen: Backend anpassen,
    Spec erzeugen, Wrapper neu generieren und im selben Commit mit einchecken.
- **Docker-Image:** Das Frontend wird als eigenes **nginx-Image** ausgeliefert (Dockerfile im Frontend-Projekt,
  nur `linux/amd64`, lokal gebaut). nginx liefert die statischen Dateien aus und leitet API-Aufrufe ans Backend weiter.
- **Texte:** Keine fest codierten UI-Texte; alle Texte über react-i18next. Neue Schlüssel immer in
  **Deutsch und Englisch** anlegen.

### Ende-zu-Ende

- **Playwright** für E2E-Tests über Frontend und Backend hinweg.

## Betrieb

Luna läuft im **Heimnetz** als **Docker Stack** (Docker Swarm, `docker stack deploy`) auf **einem Knoten**.

| Thema | Entscheidung |
|---|---|
| Orchestrierung | Docker Stack (Swarm), **ein Knoten** |
| Architektur | nur **`linux/amd64`** |
| Container-Registry | **keine** – Images werden lokal auf dem Knoten gebaut |
| Reverse Proxy | **Traefik** im Stack (Routing über Service-Labels) |
| HTTPS | **Let's Encrypt** per **DNS-Challenge** (keine offenen Ports nach außen nötig) |
| Identity Provider | **Keycloak** läuft im Luna-Stack |
| Datenbank | **eine PostgreSQL-Instanz** mit **getrennten Datenbanken und Benutzern** für Luna und Keycloak |

Dienste im Stack: Traefik, Frontend (nginx), Backend (Jib-Image), Keycloak, PostgreSQL.

- Secrets (DB-Passwörter, Keycloak-Admin, DNS-API-Token) als **Docker Secrets** bzw. außerhalb des Repos
  bereitstellen – nie im Stack-File oder Repo.
- Sollte später ein zweiter Knoten oder eine zweite Architektur dazukommen, wird eine Registry nötig –
  dann zuerst Vorschläge vorlegen.

## Repository-Struktur

Monorepo mit genau einem Backend und einem Frontend:

```
.
├── AGENTS.md        # Diese Datei – Regeln für Agenten
├── README.md        # Projektbeschreibung für Menschen
├── LICENSE          # MIT-Lizenz
├── backend/         # Spring-Boot-Backend (Maven, Jib)
└── frontend/        # React-Frontend (Vite, npm, nginx-Image)
    └── src/
        ├── api/     # Aus OpenAPI generierter Wrapper (eingecheckt, nicht manuell bearbeiten)
        └── ui/      # Wiederverwendbare, fachneutrale UI-Komponenten auf Basis von Bulma
```

Bei neuen Top-Level-Verzeichnissen diese Übersicht aktualisieren.

## Entwicklungsablauf

### Setup, Build, Test

_Die Projekte sind noch nicht angelegt._ Sobald sie existieren, hier die exakten Befehle eintragen:

```bash
# Backend bauen und testen
# <befehl>

# Backend-Docker-Image lokal bauen (Jib)
# <befehl>

# Frontend: Abhängigkeiten installieren, bauen, testen
# <befehl>

# API-Wrapper aus OpenAPI neu generieren
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

- **Sprache im Code:** Englisch (Bezeichner, Kommentare, Commit-Messages).
- **Sprache der Dokumentation:** Deutsch oder Englisch – innerhalb eines Dokuments konsistent bleiben.
- Formatierung und Stil folgen dem jeweiligen Standard-Formatter/Linter des Stacks.
- Kommentare erklären das *Warum*, nicht das *Was*.
- Keine toten Code-Pfade, auskommentierten Blöcke oder Debug-Ausgaben committen.

## Git & Commits

- Nicht direkt auf `main` arbeiten; Feature-Branches verwenden.
- Commit-Messages **immer** nach [Conventional Commits](https://www.conventionalcommits.org/):
  `<type>(<optional scope>): <description>`, Beschreibung im Imperativ, Englisch, kleingeschrieben.
  - Typen: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`
  - Scopes: `backend`, `frontend`, `api`, `ui`, `e2e` (bei Bedarf erweitern)
  - Breaking Changes mit `!` nach dem Typ/Scope und/oder `BREAKING CHANGE:` im Footer kennzeichnen.
  - Beispiele: `feat(frontend): add login page`, `fix(backend): handle empty response`, `docs: update AGENTS.md`
- Ein Commit = eine logische Änderung.
- Keine History von fremden Branches umschreiben; kein Force-Push ohne ausdrückliche Freigabe.
- Pull Requests nur auf ausdrücklichen Wunsch erstellen.

## Sicherheit

- Keine Secrets (API-Keys, Tokens, Passwörter, Client-Secrets, `.env`-Dateien) committen.
- Neue Abhängigkeiten nur mit Begründung hinzufügen und auf Pflege/Lizenz prüfen.
- Externe Eingaben immer validieren.

## Pflege dieser Datei

Wenn ein Agent etwas lernt, das zukünftigen Agenten hilft (Build-Befehle, Fallstricke, Konventionen),
soll er diese Datei entsprechend ergänzen – knapp und konkret.
