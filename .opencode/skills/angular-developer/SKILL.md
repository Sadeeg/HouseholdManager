---
name: angular-developer
description: "Use for Angular 22 code: Signals, httpResource, Signal Forms, zoneless, DI, routing, testing, accessibility."
---

# Angular Developer — HouseholdManager

**Kontext:** Angular **22**, **zoneless**, Standalone, TypeScript 6, Vitest.
Der Projektstand ist verbindlich: `frontend/package.json`, `AGENTS.md §Frontend`.
Das hier ist **Referenzmaterial** — die Projektregeln stehen in
`frontend-feature-architecture` (Feature-Grenzen) und `AGENTS.md`.

> **Kein `ng new`.** Das Projekt existiert. Dieser Skill enthält keine
> Anleitung zum Anlegen einer Anwendung — bei Bedarf `npx ng generate`.

## Was hier gilt

| Thema | Festlegung |
|-------|-----------|
| Reaktivität | `signal()` / `computed()`; Lesen per `httpResource`, Schreiben per `HttpClient` |
| Formulare | **Signal Forms** (`validate()` mit eigener `maxLengthError()` — `maxLength()` schneidet beim Paste still ab) |
| Zonen | zoneless — Effekte laufen erst nach `detectChanges()` |
| Services | **route-scoped**, nicht `root` (mit `withAutoCleanupInjectors()`) |
| Styles | Tokens `var(--hm-…)`, keine Hex-Literale — `docs/design/design-dna.md` |
| Tests | Vitest + TestBed + `provideHttpClientTesting()` |

## Referenzen

**Nur die eine Datei öffnen, die genau die Frage betrifft.** Weitere
Eigenrecherche kostet mehr als die Datei.

### Reaktivität und Daten

| Thema | Datei |
|-------|-------|
| Signal-Grundlagen, `computed`, `untracked` | `references/signals-overview.md` |
| Abhängiger Zustand (`linkedSignal`) | `references/linked-signal.md` |
| Asynchron (`resource`, `httpResource`) | `references/resource.md` |
| `effect` — und wann **nicht** | `references/effects.md` |
| HTTP-Client, Interceptor | `references/http-client.md` |

### Formulare

| Thema | Datei |
|-------|-------|
| **Signal Forms** (der Standard) | `references/signal-forms.md` |
| Reactive Forms (bestehender Altbestand) | `references/reactive-forms.md` |

### Komponenten

| Thema | Datei |
|-------|-------|
| Anatomie, Templates, `@if`/`@for`/`@switch` | `references/components.md` |
| Signal-Inputs, Outputs | `references/inputs.md` · `references/outputs.md` |
| Queries, Content Projection | `references/queries-and-projection.md` |
| Host-Bindings | `references/host-elements.md` |
| Naming (Intent over Role) | `references/naming-conventions.md` |
| Lazy Loading, `@defer` | `references/deferrable-views.md` |

### Dependency Injection

| Thema | Datei |
|-------|-------|
| Grundlagen, `inject()` | `references/di-fundamentals.md` |
| Services, `providedIn` | `references/creating-services.md` |
| Provider, `InjectionToken` | `references/defining-providers.md` |
| Hierarchische Injectors, `skipSelf` | `references/hierarchical-injectors.md` |

### Routing

| Thema | Datei |
|-------|-------|
| Routen definieren | `references/define-routes.md` |
| Lazy Loading | `references/loading-strategies.md` |
| `RouterLink`, `Router` | `references/navigate-to-routes.md` |
| Guards | `references/route-guards.md` |
| `<router-outlet>` | `references/show-routes-with-outlets.md` |

### Tests

| Thema | Datei |
|-------|-------|
| TestBed, async Muster | `references/testing-fundamentals.md` |
| Component Harnesses | `references/component-harnesses.md` |
| Router-Tests | `references/router-testing.md` |

### Darstellung und Barrierefreiheit

| Thema | Datei |
|-------|-------|
| Pipes | `references/pipes.md` |
| Component-Styles | `references/component-styling.md` |
| Angular Aria (Accordion, Listbox, Combobox, Tabs …) | `references/angular-aria.md` |

### Weiter

| Thema | Datei |
|-------|-------|
| CLI (`ng generate`) | `references/cli.md` |
| Umgebungskonfiguration | `references/environment-configuration.md` |

## Regeln

- **Bestehende Konventionen im Repo zuerst.** `grep` nach einem vorhandenen
  Beispiel im Feature, statt eine neue Form zu erfinden.
- **Keine neue Technik ohne Anlass.** Braucht ein Feature etwas, das es im
  Projekt noch nicht gibt, ist das eine **Rückfrage**, kein Selbstentscheid
  (siehe `anforderung-loop` §Runde 3).
- **Framework-Referenz ist kein Projektentscheid.** Was hier steht, sagt
  *wie Angular es kann* — was wir *wollen*, entscheidet `AGENTS.md`.
- **Nicht im Projekt verwendet** und nur für andere Fälle relevant
  (Tailwind, SSR/Hydration, Animationen, Migrationen, E2E, MCP):
  `references/` enthält die Dateien weiterhin — **öffne sie nur, wenn die
  Aufgabe genau das verlangt.**
