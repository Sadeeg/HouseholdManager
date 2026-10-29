# ADR 0002 — Frontend: Feature-basierte Architektur (Manfred Steyer)

- **Status:** angenommen
- **Datum:** 2026-10-29
- **Entscheidung:** Das Frontend von HouseholdManager folgt der **feature-basierten Architektur von Manfred Steyer** — Features als bounded contexts, Module in der Architektur-Matrix, Grenzen per Sheriff erzwungen.

## Kontext

Ein einzelnes `core/`-Ordner mit allen Services und Komponenten wächst
unkontrolliert: niemand weiß, welcher Code zu welchem Feature gehört, und
Features beginnen sich gegenseitig zu importieren.

Steyers Ansatz („Modern Architectures with Angular", angulararchitects.io)
setzt auf **Strategic Design** aus dem Domain-Driven Design: das System wird
in bounded contexts (= Features) zerlegt, jeder Context hat seine eigenen
Module, und zwei Regeln halten die Kopplung niedrig:

1. Ein Context darf nur mit seinen eigenen Modulen kommunizieren —
   Ausnahme: der `shared`-Bereich.
2. Ein Modul darf nur Module **tieferer** Schichten benutzen.

Die Architektur-Matrix hat vier Modul-Arten: `feature` (smart Components),
`ui` (dumb Components), `data` (Model + Services) und `util`. Die Matrix
wird durch Ordner abgebildet und durch **Sheriff** (Projekt von
SoftArc/Steyer) maschinell geprüft.

## Entscheidung

```text
src/app/
├── core/                  # app-weit: Konfiguration, HTTP-Basis, später Auth
├── shared/                # von mehreren Features genutzt, fachneutral
└── features/
    └── households/
        ├── routes.ts       # Feature-Routen (lazy)
        ├── households-page.ts   # feature-Schicht (smart)
        ├── model/          # Modell des Features (Typen)
        ├── data/           # data-Schicht (HTTP-Zugriff)
        └── ui/             # ui-Schicht (dumb Components)
```

Ergänzung gegenüber der Matrix: das Feature-Modell liegt in einem eigenen
`model/`-Ordner. Grund: sonst müsste die `ui`-Schicht auf die `data`-Schicht
zugreifen (Typen), was die Regel „dumb Components reden nicht mit dem
Backend" aufweichen würde. Mit `model` bleibt die data-Schicht für
`data`/`feature` erreichbar, `ui` sieht nur noch Typen.

Die Regeln werden in `sheriff.config.ts` über Tags kodiert:

| Tag | Bedeutung |
|-----|-----------|
| `domain:<feature>` | Zugehörigkeit zu einem Feature |
| `type:feature` / `type:ui` / `type:data` / `type:model` | Schicht |
| `type:core` / `type:shared` | app-weit bzw. geteilt |

Zusätzlich:

- Feature-Services werden **route-scoped** bereitgestellt — der Zustand
  lebt nur so lange wie das Feature.
- Jedes Feature hat eine eigene `routes.ts`; die App lädt sie lazy.

## Konsequenzen

**Positiv**

- Die Zugehörigkeit des Codes ist am Ordner ablesbar.
- Grenzverletzungen fallen beim Linten auf, nicht in der Review
  (`npm run sheriff`, `npm run lint`).
- Features sind unabhängig erweiterbar und einzeln lazy ladbar.
- `ui`-Components bleiben ehrlich: kein Backend-Zugriff, keine Outputs
  ohne Notwendigkeit.

**Negativ / Kosten**

- ESLint + Sheriff müssen mitinstalliert und gepflegt werden
  (`@softarc/sheriff-core`, `@softarc/eslint-plugin-sheriff`).
- Ein neues Feature braucht mehr Ordner als ein flaches Layout.
- Steyers Matrix stammt aus einem Nx-/Multi-Context-Umfeld; im
  Einzelprojekt sind bewusst nur die relevanten Kategorien umgesetzt.

**Neutral**

- Die Grenzen zwischen `core` und `shared` sind eine Projektentscheidung:
  `core` = genau eine Instanz und app-weit, `shared` = mehrfach genutzt.
