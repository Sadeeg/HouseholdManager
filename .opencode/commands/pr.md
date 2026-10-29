---
description: Feature committen, branch pushen und PR gegen main eröffnen (nach bestandenen Gates)
---

Erstelle den Pull Request für das umgesetzte Feature.

Branch / Issue:
$ARGUMENTS

Voraussetzung: Die Gates sind bestanden — Implementierung, vollständige
Tests, **Architektur-Review `OK`** und **Abnahme `PASS`**. Prüfe das, bevor
du etwas tust: Im Issue müssen die Kommentare
`<!-- review: status=OK … -->` und `<!-- abnahme: status=PASS … -->` vorliegen.
Fehlen sie, **brich ab** und nenne die fehlenden Gates — nicht committen.

Vorgehen:

1. **Zustand prüfen.** `git status`, `git branch --show-current`, `git log
   --oneline origin/main..HEAD`. Der Branch muss `feature/<nr>-<name>` sein
   und auf `main` basieren. Steht auf `main`, lege zuerst den Feature-Branch
   an (`git fetch origin && git switch -c feature/<nr>-<name> origin/main`
   und die Änderungen mitnehmen).

2. **Lauffähigkeit nachweisen (verbindlich).** Erst pushen, wenn alles grün
   ist:

   ```bash
   cd backend && ./mvnw -B test > /tmp/pr-backend.log 2>&1
   grep -E "Tests run:|BUILD" /tmp/pr-backend.log | tail -5

   cd frontend
   npx tsc --noEmit -p tsconfig.app.json && npx tsc --noEmit -p tsconfig.spec.json
   npm test && npm run lint && npm run sheriff && npm run build
   ```

   Ist etwas rot: **nicht committen, nicht pushen** — berichte, was rot ist.

3. **Diff prüfen.** `git diff --stat origin/main...HEAD` und gezielt
   `git diff origin/main...HEAD` lesen. Keine Debug-Reste, keine Secrets,
   keine Build-Artefakte.

4. **Committen** (falls noch nicht committed): Conventional-Commit-Art im
   Betreff (`feat:`, `fix:`, `docs:` …), englisch, Body mit **Warum**.

5. **Pushten:** `git push -u origin <branch>`.

6. **PR eröffnen** mit den GitHub-Tools (`create_pull_request`):

   ```text
   owner: Sadeeg
   repo: HouseholdManager
   base: main
   head: <branch>
   title: <Conventional-Commit-Art>: <Titel>
   body: <Beschreibung nach .github/pull_request_template.md>
   ```

   Die Beschreibung enthält: **Ziel**, **Was geändert wurde**, **Closes
   #<Issue-Nr>**, die **Gate-Tabelle** mit Kommandos und Ergebnissen, die
   abgehakte **Checkliste** und — bei UI-Änderungen — den Verweis auf die
   Design-DNA.

7. **Bericht:** PR-URL, Branch, Commit, Issue-Nr, Gate-Ergebnisse. Und der
   Hinweis, dass der PR **nicht gemergt** wird — das macht der Auftraggeber
   nach Sichtung des CI-Laufs.

Regeln:

- **Kein Push ohne grünen Stand.** Ein nicht lauffähiger Zustand landet
  nicht im Repo.
- **Kein Merge.** Der PR wird eröffnet und offen gelassen.
- **Ein Issue = ein Branch = ein PR.** Kein Branch ohne Issue.
- Beim Pushen von Dokumentationsänderungen ohne Code gilt die
  Lauffähigkeitsprüfung nicht — der Rest des Ablaufs bleibt gleich.
