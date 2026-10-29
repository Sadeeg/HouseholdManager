# Hausstil HouseholdManager — Issues

Maßgeblich sind die Issues, die in `Sadeeg/HouseholdManager` entstehen. Die
Formate unten gelten für neue Issues; mit dem ersten Epic wird diese Datei
gegen die bestehenden Issues geprüft und angepasst.

## Title

| Art | Format | Beispiel |
|-----|--------|----------|
| Epic | `[Epic N] <Titel>` | `[Epic 1] Haushaltsverwaltung — Grundgerüst` |
| Phase | `[N.M] <Layer> — <Kurztitel>` | `[1.2] Backend — HaushaltService + Repository` |
| Migration | `[N.M] V<nr> — <Tabellenname>` | `[1.1] V1 — household Tabelle` |
| Einzelissue | `feat(scope): <Titel>` | `feat(household): Haushalt anlegen und Mitglieder verwalten` |
| Bug | `fix(scope): <Titel>` | `fix(ausgaben): doppelte Aggregation bei Monatswechsel` |

Layer-Werte: `Backend`, `Frontend`, `Tests`, `Docs`.

Phasen-Nummerierung ist fortlaufend je Epic (`1.1`, `1.2`, …) und wird nicht
wiederverwendet. Neue Epic-Nummer = höchste bestehende + 1.

Während der Arbeit steht zusätzlich `[WIP] ` vor dem Titel — der fällt beim
Finalisieren weg.

## Labels

**Im Repo existieren aktuell nur die GitHub-Default-Labels:**

```
accessibility   bug            documentation   duplicate      enhancement
good first issue   help wanted   invalid      question       wontfix
```

Regeln:

- **Vor dem Setzen prüfen**, ob das Label existiert (`get_label`). Ein Label,
  das es im Repo nicht gibt, wird **nicht** erfunden, sondern weggelassen —
  und im Bericht erwähnt.
- Es gibt **kein** `epic`- und **kein** `phase:<n>`-Label. Epic und Phase
  stehen ausschließlich im **Titelpräfix** (`[Epic N]`, `[N.M]`). Wer Labels
  dafür braucht, legt sie einmalig in den Repo-Einstellungen an.
- Ein Issue bekommt höchstens drei Themen-Labels.
- **Issue-Typ** (GitHub-Feld, nicht Label): `Task` für Arbeitseinheiten,
  `Feature` für Wünsche, `Bug` für Fehler. `Feature` ist ein Typ, kein
  Label. Gültige Typen: `Task`, `Bug`, `Feature` — prüfen mit
  `list_issue_types`.
- Ein `enhancement`-Label ist das Themen-Label für ein Feature-Issue,
  `bug` für Fehler, `documentation` für reine Doku-Themen.

## Section-Reihenfolge

```markdown
## Goal

## Context

## Scope

## Out of Scope

## UX

## Begriffe

## Referenz

## Files

## Implementation

## Acceptance Criteria

## Tests

## Verify

## Depends on

## Offene Punkte

## Gotchas
```

Pflicht: `Goal`, `Scope`, `Out of Scope`, `Files`, `Acceptance Criteria`,
`Tests`, `Verify`. Optional: `Context`, `UX` (bei UI-Anteil), `Implementation`,
`Begriffe`, `Referenz`, `Depends on`, `Offene Punkte`, `Gotchas`.

`## UX` steht nach `## Out of Scope` und vor `## Begriffe`: Erst das Was und
Wofür, dann die Oberfläche, dann die Begriffe und die Dateien.

`## Begriffe` steht vor `## Files`, weil es die Benennung des Features festlegt,
bevor über Dateien gesprochen wird. `## Referenz` steht direkt danach, weil
es die übernommenen Eigenschaften des Vorbilds hält.

`## Offene Punkte` trennt zwei Sorten:

```text
**User-Entscheidungen**

1. Eigene Route neben der bestehenden oder Tab auf der bestehenden Seite?
   Empfehlung: eigene Route, weil Liste und Auswertung verschiedene
   Werkzeuge sind.

**Annahmen**

1. `_Annahme: Beträge werden als ganze Zahl geführt.` — das Betragsfeld ist
   `NUMERIC(12,2)`.
```

User-Entscheidungen betreffen UX, Datenmodell oder Umfang und gehören dem
User. Annahmen sind unbelegte Fakten, die die Umsetzung nicht blockieren.

**Nach dem Grilling bleibt nur die Liste der Annahmen übrig.** Eine
entschiedene User-Entscheidung wandert als Regel, Kriterium oder
Festlegung in den Issue-Text — sie ist dann keine offene Entscheidung
mehr, sondern ein Bestandteil der Anforderung. Eine Liste mit offenen
User-Entscheidungen im fertigen Issue bedeutet: das Grilling ist nicht
zu Ende gelaufen.

Beide Listen leer → Abschnitt weglassen.

## Vorlage Phase-Issue

```markdown
## Goal

<2–4 Sätze: was der Nutzer danach kann, das er vorher nicht konnte.>

## Context

<Ausgangslage: betroffene Seite, Klasse, Route. Wann der Weg hierher
gekommen ist — meist "nach #7 fehlt nur noch …".>

## Scope

- <Verb> <Ergebnis> …
- <Verb> <Ergebnis> …

## Out of Scope

- <bewusst nicht dabei, mit Begründung>

## UX

**Gestaltung:** Dark Mode, Design-DNA (Akzent für Aktion, Monospace für Daten)

### Screen: <Name>

| | |
|---|---|
| Rolle | <wer> |
| Zweck | <was der Nutzer hier erreicht> |
| Eingang / Ausgang | <woher / wohin> |

**Ablauf**

1. <Schritt> — <was der Nutzer tut>

**Elemente**

| Element | Typ | Verhalten | Zustände |
|---------|-----|-----------|----------|
| <Bezeichnung> | <Button / Input / …> | <was passiert> | default, hover, focus, disabled |

**Zustände**

- Leer: <Text> + <Aktion>
- Lade: <Text>
- Fehler: <Text> + <nächste Handlung>

**Regeln**

- <Entscheidung mit Begründung>

## Files

- `backend/src/main/java/de/householdmanager/…/XController.java` — <was>
- `frontend/src/app/features/<feature>/…/x.ts` — <was>

## Implementation

<Zeigt Implementierungsdetails, wenn sie Missverständnisse verhindern.
Kein vollständiger Code — nur die Stellen, an denen es Alternativen gibt.>

## Begriffe

| Fachbegriff | DB-Feld | UI-Label | Literal im Ergebnis |
|--------------|---------|----------|---------------------|
| <Begriff> | `<feld>` | <Label> | „<Literal>" |

## Referenz

| Eigenschaft | übernommen als |
|--------------|---------------|
| <Eigenschaft des Vorbilds> | AC<n> |

## Acceptance Criteria

- [ ] **AC1** <prüfbare Aussage>
- [ ] **AC2** <prüfbare Aussage>

## Tests

| AC | Testart | Ort | Fälle |
|----|---------|-----|-------|
| AC1 | <Testart> | `<Ort>` | <Fälle> |

## Verify

```bash
<kommando>
```

Manuell: <ein Satz, was der Tester in der App tut>.
```

## Vorlage Epic-Issue

```markdown
## Goal

<Was das Epic insgesamt erreicht.>

## Phasen

| # | Phase | Inhalt | Layer | Depends on |
|---|-------|--------|-------|------------|
| 1.1 | V1 — household Tabelle | Schema | Migration | – |
| 1.2 | Entity + Repository | Datenmodell | Backend | 1.1 |
| 1.3 | Service + Controller | Logik | Backend | 1.2 |

## Konventionen

- <die 3–5 Konventionen aus AGENTS.md, die für dieses Epic gelten>

## Verify

```bash
<kommando>
```
```

## Vorlage Feature-Issue (ohne Epic)

Nutzt die Background-Form, wenn es vor allem um Beschreibung und nicht um
Implementierungsdetails geht:

```markdown
## Hintergrund

<2–5 Sätze: Ausgangslage, warum das Thema jetzt ansteht.>

## Scope

### A. <Teilbereich>
- <Punkt>

### B. <Teilbereich>
- <Punkt>

## Out of Scope

- <Punkt>

## Acceptance Criteria

- [ ] **AC1** <prüfbar>

## Verify

```bash
…
```
```

## Verify-Kommandos

Kommandos, die im Repo real funktionieren:

```bash
# Backend: Kompilieren + Tests
cd backend && ./mvnw -B test

# Backend ohne lokales JDK (Docker)
docker run --rm -v "$PWD/backend":/app -w /app \
  maven:3.9-eclipse-temurin-25 mvn -B test

# Frontend Type-Check
cd frontend
npx tsc --noEmit -p tsconfig.app.json
npx tsc --noEmit -p tsconfig.spec.json

# Frontend Tests (Vitest)
npm test

# Frontend Production-Build
npm run build
```

Der Verify-Block nennt **nur** die Kommandos, die für dieses Issue
tatsächlich relevant sind, plus ein manueller Test-Satz. Kein
Kommando-Block, der nur Copy-Paste aus dieser Datei ist.

Ein Verify-Block ohne lauffähiges Kommando (außer dem manuellen Test) ist
eine Lücke der Art `TECH` → runde=3.

## Phrasen-Regeln

- „der Nutzer", „die Rolle X" — nicht „der User" und nicht „Admin" ohne
  Erklärung.
- „muss", „kann", „darf" — keine Modal-Verben, die nichts aussagen.
- Kein Passiv, wo Aktiv klarer ist.
- Keine Füllwörter: „eigentlich", „einfach", „grundsätzlich", „im Prinzip".
- Jede Anforderung ist testbar formuliert. Wenn ein Satz nicht prüfbar
  ist, gehört er nicht ins Issue.
- Akzeptanzkriterien tragen IDs (`AC1`, `AC2`, …), fortlaufend. Die ID
  taucht in `## Tests` wieder auf — ohne sie ist keine Zuordnung möglich.
