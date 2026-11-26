# Luna E2E-Tests

Ende-zu-Ende-Tests mit Playwright gegen eine eigene Test-Umgebung (Docker Compose), die alle Teile von Luna so
verbindet wie der Stack: Traefik, Frontend (nginx-Image), `luna-core` und `luna-task` (Jib-Images), Keycloak, PostgreSQL
und Kafka.
Statt eines KI-Anbieters antwortet ein Stub mit festen Antworten (jede Eingabe wird zu einer Aufgabe).

## Voraussetzungen

- Docker (mit Compose), Java 25 (für die Images von `luna-core` und `luna-task`), Node.js 24 (siehe `.nvmrc`)
- Port **7180** frei (Traefik der Test-Umgebung)

## Befehle

```bash
cd e2e
npm ci
npx playwright install chromium   # einmalig: Browser für die gepinnte Playwright-Version

npm test            # Images bauen, Umgebung starten, testen, Umgebung entfernen
npm run env:up      # nur Umgebung bauen und starten (z. B. zum Debuggen unter http://localhost:7180)
npm run env:down    # Umgebung samt Volumes entfernen
npm run lint        # TypeScript, ESLint und Prettier
```

| Variable                    | Wirkung                                                                                                     |
| --------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `E2E_SKIP_BUILD=true`       | Images nicht neu bauen (sie müssen als `luna-frontend:e2e`, `luna-core:e2e` und `luna-task:e2e` existieren) |
| `E2E_KEEP_ENVIRONMENT=true` | Umgebung nach `npm test` stehen lassen, z. B. zur Fehlersuche                                               |
| `E2E_CHROMIUM_EXECUTABLE`   | Pfad zu einem vorhandenen Chromium, falls `playwright install` nicht möglich ist                            |

Traces sind standardmäßig aus; für die Fehlersuche einzelne Tests mit `npx playwright test <datei> --trace on` erneut
starten. Der HTML-Report liegt danach in `playwright-report/`.

## Aufbau

| Pfad                  | Inhalt                                                                                                                       |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `docker-compose.yml`  | Test-Umgebung (Projekt `luna-e2e`, erreichbar unter `http://localhost:7180`)                                                 |
| `traefik/dynamic.yml` | Routing wie im Stack, ohne TLS: `/api/luna-core` und `/api/luna-task` (mit StripPrefix), `/auth`, Rest ans Frontend          |
| `ai-stub/stub.py`     | OpenAI-kompatibler Stub für `luna-core`                                                                                      |
| `environment/`        | Bauen, Starten, Warten und Vorbereiten von Keycloak (Weiterleitung auf Port 7180, Testbenutzer)                              |
| `tests/`              | Szenarien: Anmelden/Abmelden, Schnelleingabe mit Push-Signal, Aufgaben (Push, Bearbeiten, Erledigen), Navigation und Sprache |

- Das Frontend-Image wird mit `--build-arg MODE=e2e` gebaut und nutzt `frontend/.env.e2e`.
- `luna-task` läuft mit und muss bereit sein (`/api/luna-task/actuator/health`).
- Der KI-Stub übernimmt eine Markierung wie `[e2e-…]` aus der Eingabe in den Titel der Aufgabe, damit jeder Test
  seine eigene Aufgabe findet.
- Keycloak importiert den Realm aus `swarm/keycloak/luna-realm.json`; die zusätzliche Weiterleitung auf
  `http://localhost:7180/*` setzt nur die Test-Umgebung über die Admin-API, der Realm im Repo bleibt unverändert.
- Alle Werte (Passwörter, Testbenutzer `e2e`) sind Testwerte ohne Bedeutung außerhalb dieser Umgebung.
- Jeder Test läuft in Chromium als Desktop und als Smartphone (Pixel 7).
