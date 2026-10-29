# <Conventional-Commit-art>: <Titel>

Closes #<Issue-Nr>

## Ziel

<Was kann der Nutzer danach, was er vorher nicht konnte? Ein bis zwei
Sätze — Ziel, nicht Implementierung.>

## Was geändert wurde

- <Baustein/Funktion> — <was>
- <Baustein/Funktion> — <was>

## Gestaltung (bei UI-Änderungen)

<Welche Design-DNA-Muster wurden verwendet bzw. neu dokumentiert? Bei neuem
Muster: Verweis auf `docs/design/design-dna.md`.>

## Nachweis der Gates

| Gate | Kommando | Ergebnis |
|------|----------|----------|
| Backend-Tests | `cd backend && ./mvnw -B test` | Tests run: <n>, Failures: 0 |
| Frontend Type-Check | `npx tsc --noEmit -p tsconfig.app.json` | keine Fehler |
| Frontend Tests | `npm test` | <n> passed |
| Lint | `npm run lint` | clean |
| Modulgrenzen | `npm run sheriff` | keine Issues |
| Build | `npm run build` | erfolgreich |

## Checkliste

- [ ] Issue umgesetzt (alle Akzeptanzkriterien abgedeckt)
- [ ] Tests: jedes Akzeptanzkriterium hat einen belegenden Test
- [ ] Architektur-Review: `OK` (Reviewer `architektur-reviewer`)
- [ ] Abnahme: `PASS` (Tester `kritischer-tester`)
- [ ] arc42 aktualisiert (falls Architektur betroffen)
- [ ] Design-DNA aktualisiert (falls neues UI-Muster)
- [ ] Geänderter Stand ist lauffähig (Build + Tests grün)
- [ ] Keine Secrets, keine Debug-Reste, keine Build-Artefakte im Diff

## Hinweise für das Review

<Was ist dir beim Implementieren aufgefallen? Wo ist Unsicherheit? Welche
Entscheidung könnte der Reviewer hinterfragen?>
