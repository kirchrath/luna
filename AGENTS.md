# AGENTS.md

Leitfaden für KI-Agenten (Claude Code, Codex, Copilot u. a.), die an **Luna** arbeiten.
Diese Datei ist die zentrale Quelle für Arbeitsweise, Konventionen und Projektkontext.
Sie wird gepflegt, sobald sich am Projekt etwas Wesentliches ändert.

## Projektstatus

Luna ist ein neues Projekt im Aufbau. Bisher existieren nur `README.md`, `LICENSE` (MIT) und diese Datei.

Noch offen (bitte hier eintragen, sobald entschieden):

- **Ziel / Zweck:** _TODO_
- **Zielplattform / Deployment:** _TODO_

Bei offenen Punkten keine Entscheidungen eigenmächtig treffen, sondern Vorschläge zur Auswahl vorlegen (siehe Grundprinzip 3).

## Tech-Stack

### Backend

- **Sprache:** Java **25**
- **Framework:** Spring Boot **4.1.1 oder neuer**
- Backends werden primär in Java mit Spring Boot geschrieben. Abweichungen nur mit Begründung und nach Rücksprache.
- Jedes Backend stellt eine **OpenAPI-Spezifikation** bereit; sie ist die Grundlage für den Frontend-API-Wrapper.
- Jedes Backend stellt **immer ein Docker-Image** bereit, gebaut mit **[Jib](https://github.com/GoogleContainerTools/jib)**
  über das Build-Plugin.
  - Kein eigenes `Dockerfile` für Backends; die Image-Konfiguration liegt in der Jib-Plugin-Konfiguration des Builds.
  - Das Basis-Image muss eine Java-25-Laufzeit enthalten.

### Frontend

- **Framework:** React
- **Routing:** React Router
- **CSS:** [Bulma](https://bulma.io/) als einzige CSS-UI-Bibliothek
- **Keine UI-Komponentenbibliothek** (kein MUI, Ant Design, Chakra, shadcn o. ä.).
  Stattdessen werden **wiederverwendbare Komponenten im Projekt** erstellt und unter
  **`src/ui`** im Frontend-Projekt abgelegt.
  - Vor dem Bau einer neuen Komponente prüfen, ob in `src/ui` bereits eine passende existiert oder erweitert werden kann.
  - Komponenten in `src/ui` sind fachneutral (keine Geschäftslogik, keine API-Aufrufe) und nutzen Bulma-Klassen.
- **Backend-Zugriff:** Eigene Backends werden **ausschließlich** über einen **TypeScript-API-Wrapper** aufgerufen,
  der **aus der OpenAPI-Spezifikation generiert** wird und unter **`src/api`** im Frontend-Projekt liegt.
  - Keine direkten `fetch`/`axios`-Aufrufe an eigene Backends außerhalb von `src/api`.
  - Generierten Code nicht von Hand ändern; bei API-Änderungen die Spezifikation anpassen und den Wrapper neu generieren.

## Grundprinzipien

1. **Klein und nachvollziehbar.** Lieber mehrere kleine, fokussierte Änderungen als eine große.
2. **Erst verstehen, dann ändern.** Vor Änderungen den bestehenden Code und diese Datei lesen.
3. **Keine Annahmen treffen.** Bei offenen oder mehrdeutigen Punkten nicht selbst entscheiden, sondern
   **konkrete Vorschläge zur Auswahl** vorlegen (mit kurzer Begründung bzw. Vor- und Nachteilen, ggf. einer
   Empfehlung) und die Entscheidung dem Menschen überlassen. Erst nach der Entscheidung umsetzen.
4. **Einfachheit vor Abstraktion.** Keine spekulativen Erweiterungspunkte, keine unnötigen Abhängigkeiten.
5. **Ehrlich berichten.** Fehlgeschlagene Tests, übersprungene Schritte und offene Punkte klar benennen.

## Repository-Struktur

```
.
├── AGENTS.md    # Diese Datei – Regeln für Agenten
├── README.md    # Projektbeschreibung für Menschen
└── LICENSE      # MIT-Lizenz
```

Im Frontend-Projekt gilt:

```
src/
├── api/   # Aus OpenAPI generierter TypeScript-API-Wrapper (nicht manuell bearbeiten)
└── ui/    # Wiederverwendbare, fachneutrale UI-Komponenten auf Basis von Bulma
```

Bei neuen Top-Level-Verzeichnissen diese Übersicht aktualisieren.

## Entwicklungsablauf

### Setup, Build, Test

_Noch nicht definiert._ Sobald die Projekte angelegt sind, hier die exakten Befehle eintragen, z. B.:

```bash
# Backend bauen und testen
# <befehl>

# Frontend: Abhängigkeiten installieren, bauen, testen
# <befehl>

# API-Wrapper aus OpenAPI neu generieren
# <befehl>
```

Vor jedem Commit müssen Build, Tests und Linter (sofern vorhanden) grün sein.

### Tests

- Neue Funktionalität bekommt Tests; Bugfixes einen Regressionstest.
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
  - Breaking Changes mit `!` nach dem Typ/Scope und/oder `BREAKING CHANGE:` im Footer kennzeichnen.
  - Beispiele: `feat(frontend): add login page`, `fix(api): handle empty response`, `docs: update AGENTS.md`
- Ein Commit = eine logische Änderung.
- Keine History von fremden Branches umschreiben; kein Force-Push ohne ausdrückliche Freigabe.
- Pull Requests nur auf ausdrücklichen Wunsch erstellen.

## Sicherheit

- Keine Secrets (API-Keys, Tokens, Passwörter, `.env`-Dateien) committen.
- Neue Abhängigkeiten nur mit Begründung hinzufügen und auf Pflege/Lizenz prüfen.
- Externe Eingaben immer validieren.

## Pflege dieser Datei

Wenn ein Agent etwas lernt, das zukünftigen Agenten hilft (Build-Befehle, Fallstricke, Konventionen),
soll er diese Datei entsprechend ergänzen – knapp und konkret.
