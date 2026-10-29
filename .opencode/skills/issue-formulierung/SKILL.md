---
name: Issue-Formulierung
description: Use whenever a HouseholdManager issue text is written or rewritten so that issues always look the same. Defines title format, label vocabulary, section order, and the verify block. Load this in every round of the requirements loop and whenever an existing issue is reformatted.
---

# Issue-Formulierung

Setzt den Hausstil. Diese Skill wird in **jeder** Runde des
Anforderung-Loops angewendet — sie bestimmt das Aussehen, nie den Inhalt.

Die vollständigen Vorlagen, das Label-Vokabular und die Beispiele stehen in
`references/hausstil.md`. Diese Datei wird gelesen, wenn ein konkreter
Abschnitt oder ein konkretes Beispiel gebraucht wird.

## Kurzregeln

- Sprache: Deutsch. Commit-Sprache und Bezeichner im Code bleiben englisch.
- Title: Epic `[Epic N] <Titel>`, Phase `[N.M] <Layer> — <Kurztitel>`,
  Einzelissue `feat(scope): <Titel>`. Details in `references/hausstil.md`.
- Labels: nur aus dem Vokabular in `references/hausstil.md` — im Repo
  existieren aktuell nur die GitHub-Default-Labels. Keine erfundenen
  Labels, keine erfundenen Issue-Typen (gültig: Task, Bug, Feature).
- Section-Reihenfolge ist fix:

```text
## Goal
## Context
## Scope
## Out of Scope
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

`Context`, `Implementation`, `Begriffe`, `Referenz`, `Depends on`,
`Offene Punkte` und `Gotchas` sind optional. Alle anderen Abschnitte sind
Pflicht — `Depends on` nur, wenn es Abhängigkeiten gibt.

`## Offene Punkte` enthält zwei getrennte Listen:

```text
**User-Entscheidungen**

1. <Entscheidung> — Empfehlung: <…>, Alternative: <…>

**Annahmen**

1. `_Annahme: <…>` — Begründung: <…>
```

Leere Listen werden weggelassen. Sind beide leer, entfällt der Abschnitt.

## Wiederholbarkeit

- Jedes Issue beginnt mit `## Goal` in zwei bis vier Sätzen. Kein
  Feature-Name im Goal, sondern der Effekt für den Nutzer.
- Jeder Bullet in `## Scope` beginnt mit einem Verb und benennt das
  Ergebnis, nicht die Datei. Die Datei steht in `## Files`.
- `## Files` enthält echte Pfade, keine Beschreibungen wie "Frontend-Teil".
- `## Acceptance Criteria` ist eine Checkliste mit `- [ ]`, je Zeile genau
  eine prüfbare Aussage. Keine Sammelkriterien mit "und".
- `## Verify` enthält lauffertige Kommandos als Codeblock, plus einen
  manuellen Test in einem Satz.
- `## Out of Scope` ist eine Liste. Leer ist nicht erlaubt — notfalls
  "bewusst nicht Teil dieses Issues: …".

## Struktur des Issues im Chat

Das fertige Issue wird als Markdown ausgegeben, ohne umgebende Codefences.
Der Zustand steht **nicht** im Issue-Text, sondern getrennt darüber:

```text
Issue: #12 https://github.com/Sadeeg/HouseholdManager/issues/12
Status: READY_FOR_AGENT
```

Dann eine Leerzeile, dann der Issue-Text.
