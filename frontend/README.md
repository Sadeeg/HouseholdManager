# HouseholdManager — Frontend

Angular-22-App (zoneless, Signals, Vitest). Vollständige Projekt-Doku steht
im [README](../README.md) und in [`AGENTS.md`](../AGENTS.md).

## Skripte

| Befehl | Wirkung |
|--------|---------|
| `npm start` | Dev-Server auf http://localhost:4200, `/api` wird auf `http://localhost:8080` geproxyt (`proxy.conf.json`) |
| `npm run build` | Production-Build nach `dist/household-manager/browser` |
| `npm test` | Vitest (kein Watch) |
| `npm run lint` | ESLint (Angular-Regeln + Sheriff-Plugin) |
| `npm run sheriff` | prüft die Modulgrenzen der Feature-Architektur |
| `npm run sheriff:list` | zeigt die Module mit ihren Tags |
| `npx tsc --noEmit -p tsconfig.app.json` | Type-Check App |
| `npx tsc --noEmit -p tsconfig.spec.json` | Type-Check Tests |

## Struktur (Feature-Architektur, Manfred Steyer)

```text
src/app/
  core/
    config/api.config.ts        App-weite Konfiguration
  shared/
    components/page-header/     Von mehreren Features benutzt, fachneutral
  features/households/
    routes.ts                   Feature-Routen (lazy)
    households-page.ts|html|scss  Smart Component (Feature-Schicht)
    model/household.model.ts    Modell des Features
    data/household-api.ts       HTTP-Zugriff
    data/households-data.ts     httpResource, route-scoped Service
    ui/household-item.ts|html    Dumb Component (UI-Schicht)
```

Regeln: ein Feature benutzt nur eigene Module (Ausnahme `shared`, `core`);
ein Modul benutzt nur tiefere Schichten. Die Regeln stehen in
`sheriff.config.ts` und werden von `npm run sheriff` geprüft. Details:
[ADR 0002](../docs/adr/0002-frontend-feature-architecture.md).

Konventionen (Angular 22): Dateinamen ohne Rollen-Suffix, Modelle mit
`.model.ts`, `core/` / `features/` / `shared/` als Ordnungsweg, Standalone
ohne `NgModule`, `signal()`/`computed()`, `httpResource` für Lesezugriffe.

## Node-Version

Angular CLI 22 verlangt Node ≥ 22.22.3 (oder 24.15+ / 26+). Ältere
Node-Versionen brechen schon `ng new`/`ng build` ab.
