# Luna Frontend

React-Frontend von Luna (Vite, TypeScript, Bulma). Aufbau und Verhalten sind in [docs/README.md](docs/README.md)
geplant (Wireframes).

## Voraussetzungen

- Node.js **24** (siehe `.nvmrc`, z. B. `nvm use`)
- npm (Versionen in `package.json` sind exakt gepinnt, `package-lock.json` ist eingecheckt)

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
```

## Aufbau

| Pfad                          | Inhalt                                                                                                                           |
| ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| `src/ui/`                     | Fachneutrale Komponenten auf Basis von Bulma (`AppShell`, `Sidebar`, `LanguageSwitch`, `Placeholder`, `Logo`)                    |
| `src/layout/`                 | Anwendungsrahmen mit Kopfzeile und Navigation (`AppLayout`)                                                                      |
| `src/pages/`                  | Seiten (`QuickInputPage`, `ComingSoonPage`)                                                                                      |
| `src/navigation.ts`           | Pfade und Bereiche der Navigation                                                                                                |
| `src/routes.tsx`              | Routen (React Router)                                                                                                            |
| `src/i18n.ts`, `src/locales/` | Übersetzungen (Deutsch Standard, Englisch); die gewählte Sprache wird im Browser gemerkt                                         |
| `src/stores/`                 | MobX-Stores: `RootStore` (erzeugt alle Stores), `StoresProvider` und `useStores()`; jede Backend-Kommunikation läuft über Stores |
| `src/services/storage.ts`     | Einstellungen im `localStorage` (Sprache, eingeklappte Navigation)                                                               |
| `src/styles/main.scss`        | Bulma mit Primärfarbe `#1f7a8c`, Schriften Inter und Space Grotesk, Layout                                                       |

- **Importe:** Jeder Ordner unter `src/` exportiert seine Dateien über ein `index.ts`; von außen nur darüber
  importieren, innerhalb eines Ordners direkt. ESLint prüft das.
- **Icons:** `lucide-react`; das Logo (Vollmond mit Kratern) ist eine eigene Komponente.
- **Dunkler Modus:** folgt automatisch der Geräteeinstellung (Bulma 1.x).
- **Tests:** Namen nach dem Muster `that … should result into …`, jeweils mit `// given`, `// when`, `// then`.
