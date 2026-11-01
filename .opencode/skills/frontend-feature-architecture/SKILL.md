---
name: Frontend-Feature-Architektur
description: Use for every change in the Angular frontend of HouseholdManager. Encodes the feature-based architecture after Manfred Steyer - the architecture matrix (feature, ui, data, util), the two rules (a feature may only use its own modules plus shared, a module may only use lower layers), the folder layout, smart versus dumb components, routing per feature and the Sheriff tags that enforce the boundaries. Load this when adding a feature, a component, a service, a model or a route.
---

# Frontend — Feature-basierte Architektur

Das Frontend folgt der **feature-basierten Architektur von Manfred Steyer**
(„Modern Architectures with Angular", angulararchitects.io): ein Feature ist
der Baustein, nicht die Datei. Die Features entsprechen bounded contexts aus
dem Strategic Design.

Verbindliche Quelle: `AGENTS.md`. Angular-Handwerk (Signals, `httpResource`,
Signal Forms, zoneless): Skill `angular-developer`.

## Die Architektur-Matrix

Steyers Matrix kennt vier Modul-Arten. Sie bilden **Schichten** — jede Schicht
darf nur die darunterliegenden benutzen:

| Schicht | Inhalt | Beispiel im Projekt |
|---------|--------|---------------------|
| `feature` | Use Case, **smart** Component, spricht mit dem Backend | `features/households/households-page.ts` |
| `data` | Services, die mit dem Backend reden | `features/households/data/` |
| `model` | Modell des Features (Typen) | `features/households/model/household.model.ts` |
| `ui` | **dumb**/presentational Components, wiederverwendbar, ohne Backend | `features/households/ui/household-item.ts` |
| `util` | generische Hilfsfunktionen | `shared/util/` |

Dazu `shared` (Code für alle Features) und `core` (app-weite Angelegenheiten).

> **Ergänzung gegenüber Steyers Matrix:** Das Modell liegt in einem eigenen
> `model/`-Ordner. Ohne diese Trennung müsste die `ui`-Schicht für ihre
> Typen auf die `data`-Schicht zugreifen — und damit die Regel „dumb
> Components reden nicht mit dem Backend" aufweichen. Mit `model` bleibt die
> `data`-Schicht für `data`/`feature` reserviert.

## Die zwei Regeln

```text
1. Ein Feature darf nur seine eigenen Module benutzen — Ausnahme: shared.
2. Ein Modul darf nur Module tieferer Schichten benutzen.
   feature → ui, data      ui → (nichts)      data → (nichts)
```

Verboten sind damit:

- Feature A importiert Feature B (kein `features/haus/...` in
  `features/urlaub/...`).
- Ein `ui`-Component ruft das Backend auf oder kennt den Data-Service.
- Ein `data`-Service importiert aus `ui` oder `feature`.

## Ordnerstruktur

```text
src/app/
├── core/                          # app-weit, eine Instanz
│   └── config/api.config.ts       # Basis-Pfad, globale Einstellungen
│
├── shared/                        # von mehreren Features benutzt, fachneutral
│   ├── components/page-header/
│   └── util/
│
└── features/                      # ein Ordner pro Feature
    └── households/
        ├── routes.ts               # Routen des Features (lazy)
        ├── households-page.ts|html|scss   # Feature-Schicht (smart)
        ├── model/                  # Modell des Features (Typen)
        │   └── household.model.ts
        ├── data/                   # Daten-Schicht
        │   ├── household-api.ts
        │   └── households-data.ts
        └── ui/                     # UI-Schicht (dumb)
            └── household-item.ts|html
```

Regeln zum Ablegen:

1. **Kennt nur ein Feature den Code?** → `features/<feature>/`.
2. **Wird er von mehreren Features benutzt und ist fachneutral?**
   → `shared/`.
3. **App-weit, genau eine Instanz** (Konfiguration, HTTP-Basis, Auth)?
   → `core/`.
4. **Greift auf das Backend zu?** → `data/` **innerhalb** des Features.
5. **Nur Anzeige, keine Daten?** → `ui/` innerhalb des Features oder
   `shared/components/`.
6. **Nur Typen des Features?** → `model/` innerhalb des Features.

## Smart vs. dumb

| | smart Component | dumb Component |
|---|---|---|
| Schicht | `feature` | `ui` / `shared` |
| Daten | holt sie über den Data-Service | bekommt sie per Input |
| Ausgabe | schreibt/ändert Daten | meldet per Output |
| Wissen | kennt den Use Case | kennt nur seine Darstellung |
| Beispiel | `HouseholdsPage` | `HouseholdItem`, `PageHeader` |

Ein dumb Component darf **nicht** `inject(HouseholdsData)` machen — das ist
der häufigste Architekturbruch und wird von Sheriff verhindert.

## Routing

- Jedes Feature hat **eine** `routes.ts`; die App lädt sie lazy:

  ```ts
  { path: 'households',
    loadChildren: () => import('./features/households/routes').then((m) => m.HOUSEHOLD_ROUTES) }
  ```

- Feature-Services werden **route-scoped** bereitgestellt
  (`providers: [HouseholdsData]` in der Feature-Route): Der Zustand lebt nur
  so lange, wie das Feature aktiv ist, und lädt beim erneuten Betreten neu.
- Nur Komponenten, die nicht in eine Feature-Route passen, liegen in der
  App-Route.

## Datenhaltung im Frontend

- Lesen: `httpResource` im Data-Service (`data/`), Ergebnis als Signal.
- Schreiben: `HttpClient` im API-Client (`data/`).
- Abgeleiteter Zustand: `computed()` im Data-Service oder in der
  smart Component — nie in einem dumb Component.
- Formulare: **Signal Forms** (Angular 22).

### Zwischenstände gehören in die Datenschicht

Ein UI-Interaktionsschritt, der eine Serverantwort abwartet (Verschieben,
Sortieren, Umschalten), zeigt **sofort** den neuen Zustand an und speichert
danach. Dieser Zwischenstand liegt im **Data-Service**, nicht in der Seite:

```text
Data-Service:  pendingMove (Signal) → board (computed) → applyMove(tasks, move)
Seite:         onDrop meldet nur den Zug — sie verschiebt nichts selbst.
```

Regeln:

- Die lokale Normalisierung **spiegelt die des Servers** (Zielspalte ohne
  das Element, Position begrenzt auf die Länge der Zielspalte, beide
  betroffenen Spalten neu nummerieren). Sonst weicht die Anzeige bis zur
  Antwort von der Wahrheit ab.
- Der Zwischenstand gilt, solange die geladenen Daten unverändert sind; die
  Serverantwort macht ihn wirkungslos, der **Fehlerpfad verwirft** ihn.
- Der zugehörige Test prüft den Zustand **vor** der Antwort. Ein Test, der
  erst nach dem Reload zählt, ist blind für genau diesen Fehler.

**CDK verschiebt kein Modell.** `CdkDragDrop` meldet den Zug; das Verschieben
des Eintrags macht die Anwendung. Wer das dem CDK überlässt, sieht die Karte
in die Ausgangsbahn zurückfallen.

### `core` heißt: genau eine Instanz

`core/` ist für app-weite Angelegenheiten mit **einer** Instanz. Ein Dienst
in `core/` wird deshalb `providedIn: 'root'` gestellt. Wird er in einer
Komponente per `providers: [...]` erzeugt, ist er faktisch ein `shared`-Dienst
mit eigenem Zustand — dann gehört er nach `shared/` (oder der Grund für den
geteilten Zustand steht als Kommentar an der Datei).

**Entscheidung zu `core/users/user-search.ts` (Issue #4):** Die Kapsel bleibt
in `core/` und ist `providedIn: 'root'` — **eine** Instanz für die ganze App.
Begründung: Das Nutzerverzeichnis ist Infrastruktur wie die Auth (app-weit,
kein Fachwissen über ein Feature); ein Umzug nach `shared/` würde Backend-Zugriff
dort hineinbringen, den `shared/README.md` ausschließt. Der Zustand (Query,
Treffer) ist damit **geteilt**: alle Suchfelder sehen denselben Stand, ein
Request pro Suchbegriff. Damit jedes Feld frisch startet, setzt der
`UserPicker` den Zustand beim Anlegen und nach einer Auswahl zurück (`reset()`);
Treffer werden erst ab zwei Zeichen gezeigt. Wer einen weiteren Verwender
aufnimmt, muss diese Reset-Regel beachten — sonst erbt er den Zustand eines
anderen Feldes.

## Naming (Angular 22)

- Dateien **ohne** Rollen-Suffix: `households-page.ts` → `class HouseholdsPage`.
- Models behalten `.model.ts`: `household.model.ts` → `interface Household`.
- Komponenten-Dateien heißen wie die Klasse: `households-page.ts|html|scss`.
- Der Ordnungsweg trägt die Rolle (`core/`, `features/`, `shared/`) — nicht
  der Dateiname.

## Sheriff — Grenzen erzwingen

Die Regeln 1 und 2 werden per `sheriff.config.ts` maschinell geprüft. Sheriff
arbeitet mit Tags:

| Tag | Bedeutung |
|-----|-----------|
| `domain:<feature>` | Zugehörigkeit zu einem Feature |
| `type:feature` | Smart Component |
| `type:ui` | Dumb Component |
| `type:data` | Daten-Schicht |
| `type:model` | Modell des Features |
| `type:core` | app-weit |
| `type:shared` | geteilt |

Regel-Konzept (aus Steyers Sheriff-Artikeln):

```text
domain:<f>  →  nur domain:<f>            (kein Feature importiert ein anderes)
type:feature → type:ui, type:data, type:model
type:ui     → type:model
type:data   → type:model
```

Befehle:

```bash
npm run sheriff        # prüft die Grenzen (Dependency-Rules)
npm run sheriff:list   # zeigt die Module und ihre Tags
npm run lint           # ESLint inkl. Sheriff-Plugin
```

Nach jeder neuen Datei: `npm run sheriff` — bei Verstoß wird die Struktur
repariert, nicht die Regel aufgeweicht. Ein **neues Feature** muss in
`sheriff.config.ts` eingetragen werden (`domain:<feature>` plus `feature`, `model`,
`data`, `ui`): mit `autoTagging: false` wird ein Feature ohne Eintrag nicht geprüft,
und ein grüner Lauf bedeutet dann nichts.

## Fallen

- **Zoneless:** Effekte laufen erst nach `detectChanges()`. Im Test den
  HTTP-Request **nach** `fixture.detectChanges()` erwarten.
- **Ein Fehlerzustand braucht eine sichtbare Handlung.** „Dieselbe Eingabe
  noch einmal" ist keine Handlung, die der Nutzer kennen kann — der
  Zustand braucht einen Knopf, der ihn verlässt (z. B. „Erneut versuchen").
- **`httpResource` feuert nur bei URL-Änderung.** Für einen erneuten Versuch
  mit derselben URL `reload()` benutzen, nicht auf ein Signal warten. Achtung:
  `reload()` setzt den Fehlerzustand **nicht** zurück — bei gleicher URL
  bleibt der alte Stream; die Fehlermeldung blendet man über einen laufenden
  Versuch aus (`!isLoading()`), nicht über einen gelöschten Fehler.
- **Feature-Service in der Route providen**, sonst lebt der Zustand in der
  App und überlebt das Verlassen des Features.
- **`shared` ist kein Ablageort.** „Wird vielleicht später gebraucht" heißt
  `feature`.
- **Kein `any` an der Feature-Grenze:** Das Model im `data/`-Ordner ist die
  clientseitige Sicht — sie darf bewusst schmaler sein als das
  Backend-Modell.
- **Ein bestehendes UI-Muster wird nicht nachgebaut.** Overlay, Liste,
  Bestätigungsdialog, Seitenkopf: erst `shared/components/` und die
  vorhandenen Features prüfen, dann bauen. Zwei Varianten desselben Musters
  sind zwei Fehlerquellen (siehe Skill `ux-design`, „Muster zuerst suchen").
- **Layout ist nicht testbar.** jsdom sieht keine Geometrie — Position,
  Überlappung und Verdeckung brauchen eine Sichtprüfung (Screenshot), keinen
  Unit-Test.
