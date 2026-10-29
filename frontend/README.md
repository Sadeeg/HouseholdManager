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

## Design

Die Gestaltung folgt der **Design-DNA**
([`../docs/design/design-dna.md`](../docs/design/design-dna.md)): modern,
**Dark Mode als Standard**, mit Terminal-Anmutung — Monospace für Daten,
scharfe Kanten (Radius 4px), 1px-Linien statt Schatten, Akzentfarbe nur
als Bedeutung.

Die Tokens liegen in `src/styles/_design-tokens.scss` und sind die einzige
Quelle für Farben, Maße und Schriftgrößen:

```scss
// in Component-Stylesheets — niemals Hex-Literale
color: var(--hm-text-muted);
border: 1px solid var(--hm-border);
border-left: 2px solid var(--hm-accent-line);
```

| Datei | Inhalt |
|-------|--------|
| `src/styles/_design-tokens.scss` | Flächen, Linien, Text, Akzente, Typografie, Maße, Effekte, helle Variante |
| `src/styles.scss` | Basis-Styles: Tokens einbinden, Grid-Hintergrund, Fokus, Typografie |
| `src/app/shared/components/` | Wiederverwendbare UI-Bausteine im DNA-Stil |

Ein **neues** UI-Muster wird zuerst entworfen (UX-Runde, Skill `ux-design`),
dann in der DNA dokumentiert — nicht spontan im Code erfunden.

## Node-Version

Angular CLI 22 verlangt Node ≥ 22.22.3 (oder 24.15+ / 26+). Ältere
Node-Versionen brechen schon `ng new`/`ng build` ab.
