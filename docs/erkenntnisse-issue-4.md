# Erkenntnisse — Issue #4 „Instanz-Strategie der Nutzersuche klären“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #4 (Branch
`feature/4-user-search-instanz-strategie`). Zweck: die Entscheidung festhalten,
die den MINOR-Befund aus dem Architektur-Review zu #1 auflöst, und die
wiederkehrenden Fehler als Regeln überführen.

Belegte Gates (Ende): Frontend **30 Dateien / 133 Tests** grün (davon 7 neu,
2 als `it.fails`-Tripwire für einen vorbestegenden Defekt) · `tsc` (app +
spec), `lint`, `sheriff`, Build clean · Architektur-Review **OK** (0 BLOCKER,
0 MAJOR, 3 MINOR, 1 NIT) · Abnahme **PASS**, 3/3 ACs belegt.

---

## 1. Die Entscheidung

**Befund aus #1 (MINOR, nicht blockierend):** `frontend/src/app/core/users/user-search.ts`
lag in `core/` („app-weit, eine Instanz“), wurde aber pro konsumierender
Komponente erzeugt (`providers: [UserSearch]` im `UserPicker`). Die Zuordnung
zu `core` war damit nicht stimmig.

**Entschieden: Variante B — die Kapsel bleibt in `core/` und wird
root-provided (`providedIn: 'root'`).** Begründung, an der Datei und im Skill
festgehalten:

- **Regel:** ADR 0002 — „`core` = genau eine Instanz und app-weit, `shared` =
  mehrfach genutzt." Ein Dienst in `core/` wird deshalb `providedIn: 'root'`
  gestellt (Präzedenzfall: `core/workspace/workspace-service.ts`).
- **Ort:** Das Nutzerverzeichnis ist Infrastruktur wie die Auth — app-weit,
  von jedem Feature gebraucht, ohne Fachwissen über ein Feature.
- **Gegen die Alternative `shared/`:** `frontend/src/app/shared/README.md`
  verbietet Backend-Zugriff in `shared/`. Die Suche spricht mit dem Backend;
  sie gehört deshalb nicht dorthin.
- **Folge:** Query und Treffer sind **geteilt** — eine `httpResource`, ein
  Request pro Suchbegriff in der App statt einer pro Suchfeld.

**Absicherung des geteilten Zustands:** Der `UserPicker` setzt den Zustand im
Konstruktor und nach einer Auswahl zurück (`reset()`); `results` bleibt unter
zwei Zeichen leer. Damit startet jedes Feld so frisch wie zuvor — der
Nutzer sieht dieselbe Bedienung.

## 2. Behobene Probleme

### 2.1 `core`-Dienst wurde pro Komponente erzeugt

- **Symptom:** Schicht und Instanz-Strategie widersprachen sich — die Datei lag
  in `core/`, erzeugt wurde sie pro Komponente.
- **Ursache:** Der `UserPicker` (`shared/components/user-picker/`) hat den
  Dienst per `providers: [UserSearch]` erzeugt, um seinen Query-Zustand
  getrennt zu halten. Das war funktional korrekt, aber nicht stimmig zur
  Schichtdefinition.
- **Nachbesserung:** `providedIn: 'root'`, `providers: []` entfernt, der
  Zustand wird geteilt und über `reset()` beim Anlegen wieder geleert.
- **Lehre:** Ein Dienst in `core/` ist per Definition root-provided. Wer einen
  eigenen Zustand braucht, muss ihn **ableiten** (z. B. im Konstruktor
  zurücksetzen), nicht eine zweite Instanz erzeugen. Regel im Skill
  `frontend-feature-architecture` („`core` heißt: genau eine Instanz").

### 2.2 Die Begründung im Kommentar trug nicht

- **Symptom:** Der erste Kommentarentwurf begründete die Exklusivität mit
  „beide stehen in verschiedenen Features und damit nie gleichzeitig offen“.
- **Ursache:** Der Schluss stimmte, die Begründung nicht — verschiedene
  Features bedeuten keine Ausschließlichkeit.
- **Nachbesserung:** Die Begründung nennt jetzt den tragenden Grund: auf der
  Seite „Haushalte" ist genau ein Haushalt aufgeklappt
  (`expandedHouseholdId` — ein Signal), im Aufgaben-Board genau ein
  Task-Formular, die Routen sind getrennt. Belegt in
  `households-page-user-picker.spec.ts`.
- **Lehre:** Eine Begründung, die nur plausibel klingt, wird beim nächsten
  Feature zur falschen Prämisse. Der Grund muss die **tatsächliche**
  Invariante benennen.

## 3. Überführte Regeln

| Lehre | Regel |
|---|---|
| `core` = genau eine Instanz | `frontend-feature-architecture` „`core` heißt: genau eine Instanz" (Abschnitt um die Entscheidung zu `UserSearch` ergänzt) |
| shared ≠ Ablageort, kein Backend-Zugriff | `frontend/src/app/shared/README.md` (Regel + benannte Ausnahme „dumb Component → `core`-Dienst") |
| Begründung muss die reale Invariante nennen | `frontend-feature-architecture` (Abschnitt `core` + Kommentarregel an der Datei) |

## 4. Offene Kleinbefunde

| # | Was | Wo |
|---|---|---|
| Issue #18 | **MAJOR aus der Abnahme:** Der Fehlerpfad der Suche ist kaputt — `searchResource.value()` wirft im Fehlerzustand `ResourceValueError`, die Meldung „Nutzerverzeichnis nicht erreichbar." wird nie erreicht. Vorbestehend (gegen `origin/main` verifiziert), nicht durch #4 verursacht; als `it.fails`-Tripwire abgesichert — **erledigt durch Issue #18** (Branch `feature/18-fehlerpfad-personensuche`) | `frontend/src/app/core/users/user-search.ts` |
| MINOR aus dem Review | `arc42/05_Bausteinsicht.md` führt `core/users/` nicht — im Zuge von #4 nachgetragen | `arc42/05_Bausteinsicht.md` |
| MINOR aus dem Review | `shared/README.md` nannte `components/user-picker/` nicht und verschwieg den Umweg `shared → core` — im Zuge von #4 nachgetragen | `frontend/src/app/shared/README.md` |

## 5. Umgebung (Praktisches)

Das Node auf diesem Rechner (22.22.1) liegt unter dem Minimum der
Angular-CLI (≥ 22.22.3) — `npm test` bricht dort ab. Die Frontend-Gates
laufen im Container:

```bash
cd frontend && docker run --rm -v "$PWD":/app -w /app \
  -u "$(id -u):$(id -g)" -e HOME=/tmp -e CI=true node:22 \
  sh -c 'npx ng test --watch=false'
```

`npx vitest` direkt funktioniert **nicht** (kein Angular-Linker →
„JIT compilation failed for service [class BrowserXhr]").
