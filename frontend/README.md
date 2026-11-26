# Luna Frontend

React-Frontend von Luna (Vite, TypeScript, Bulma). Aufbau und Verhalten sind in [docs/README.md](docs/README.md)
geplant (Wireframes).

## Voraussetzungen

- Node.js **24** (siehe `.nvmrc`, z. B. `nvm use`)
- npm (Versionen in `package.json` sind exakt gepinnt, `package-lock.json` ist eingecheckt)
- Install-Skripte von Abhängigkeiten laufen nur nach Freigabe in `allowScripts` (`package.json`); neue Skripte
  vor der Freigabe prüfen (`npm install-scripts ls`).

## Befehle

```bash
cd frontend

npm ci              # Abhängigkeiten exakt nach package-lock.json installieren
npm run dev         # Dev-Server auf http://localhost:5173
npm run build       # Typprüfung und Produktions-Build nach dist/
npm run test        # Tests (Vitest)
npm run coverage    # Tests mit Abdeckung, bricht unter 100 % ab
npm run lint        # ESLint und Prettier-Prüfung
npm run format      # Formatierung mit Prettier anwenden
npm run generate:api  # API-Wrapper aus ../luna-core/openapi.yaml neu erzeugen
```

## Docker-Image

```bash
docker build --platform linux/amd64 -t luna-frontend:<version> .   # Version aus package.json
```

Mehrstufig: `node:24.22.3-alpine` baut mit `.env.production` (oder mit `--build-arg MODE=e2e` für die E2E-Tests
mit `.env.e2e`), ausgeliefert wird
`nginxinc/nginx-unprivileged:1.30.6-alpine` (ohne Root, Port 8080). Die nginx-Konfiguration in `nginx/` regelt
SPA-Fallback, Caching und Sicherheits-Header inklusive Content-Security-Policy (nur eigene Origin). Im Stack läuft
das Image über [`swarm/frontend.yml`](../swarm/frontend.yml).

## Anmeldung

Die App ist vollständig geschützt und meldet über Keycloak (Realm `luna`, Client `luna-frontend`) an. Lokal muss
Keycloak unter `http://localhost:7080/auth` laufen (siehe [README im Hauptverzeichnis](../README.md), Schritt 13 für
das Konto). Die Adressen stehen in `.env.development` (lokal) und `.env.production` (Stack); beide enthalten nur
öffentliche Werte.

## Aufbau

| Pfad                          | Inhalt                                                                                                                                            |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| `src/auth/`                   | Anmeldung: `AuthGate` (leitet ohne Anmeldung zu Keycloak), `createOidcConfig`                                                                     |
| `src/ui/`                     | Fachneutrale Komponenten auf Basis von Bulma (`AppShell`, `Sidebar`, `LanguageSwitch`, `Placeholder`, `Logo`, `UserMenu`, `StatusScreen`)         |
| `src/layout/`                 | Anwendungsrahmen mit Kopfzeile und Navigation (`AppLayout`)                                                                                       |
| `src/pages/`                  | Seiten (`QuickInputPage`, `ComingSoonPage`)                                                                                                       |
| `src/navigation.ts`           | Pfade und Bereiche der Navigation                                                                                                                 |
| `src/App.tsx`                 | Router und Stores, erst nach der Anmeldung erzeugt                                                                                                |
| `src/routes.tsx`              | Routen (React Router)                                                                                                                             |
| `src/i18n.ts`, `src/locales/` | Übersetzungen (Deutsch Standard, Englisch); die gewählte Sprache wird im Browser gemerkt                                                          |
| `src/api/luna-core/`          | Wrapper für `luna-core`: generiertes `schema.d.ts`, Client mit Token-Middleware, STOMP-Push-Verbindung, Typ-Aliase                                |
| `src/stores/`                 | MobX-Stores: `RootStore` und `createRootStore`, `InboundEventStore` (Schnelleingabe, Liste, Live-Aktualisierung), `StoresProvider`, `useStores()` |
| `src/services/storage.ts`     | Einstellungen im `localStorage` (Sprache, eingeklappte Navigation)                                                                                |
| `src/styles/main.scss`        | Bulma: Hintergrund `#f3f4f6`, Primärfarbe `#4a5260`, Highlight Petrol `#1f7a8c` (`$link`), Schriften Inter und Space Grotesk, Layout              |

- **Importe:** Jeder Ordner unter `src/` exportiert seine Dateien über ein `index.ts`; von außen nur darüber
  importieren, innerhalb eines Ordners direkt. ESLint prüft das.
- **Icons:** `lucide-react`; das Logo (Vollmond mit Kratern) ist eine eigene Komponente.
- **Dunkler Modus:** folgt automatisch der Geräteeinstellung (Bulma 1.x).
- **Tests:** Namen nach dem Muster `that … should result into …`, jeweils mit `// given`, `// when`, `// then`.
