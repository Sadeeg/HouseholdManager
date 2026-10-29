---
description: Architektur-Review-Gate: Review ausführen, Korrekturen an den Implementierer delegieren, erneut reviewen — bis OK
---

Führe das **Architektur-Review-Gate** aus. Ein Feature gilt erst als fertig,
wenn dieses Gate `OK` meldet.

Scope (Issue-Nummer oder Branch/Dateien):
$ARGUMENTS

Vorgehen:

1. **Sollbild holen.** Issue über die GitHub-Tools lesen (`issue_read` mit
   `method: "get"`): Ziel, Akzeptanzkriterien, `## Out of Scope`. Ohne
   Argument: nachfragen, welches Issue gemeint ist. Lade die Skill
   `code-review-gate` (Kategorien, Severity, Urteilsform).

2. **Reviewer starten.** Dispatche den Subagenten `architektur-reviewer`
   (eigene Session, frischer Kontext, read-only) mit:

   ```text
   Prüfe <Issue-Nr / Scope> gegen die Regeln aus AGENTS.md, den ADRs und
   den Skills backend-ports-adapters, frontend-feature-architecture und
   feature-testing. Führe die Gates aus (mvn test, npm test, npm run lint,
   npm run sheriff). Liefere das Urteil OK oder KORREKTUREN; bei
   KORREKTUREN den ausführbaren Korrekturauftrag. Ändere keine Dateien.
   ```

   Warte auf sein Urteil.

3. **Bei `KORREKTUREN` — delegieren.** Der Reviewer führt nicht aus. Du
   delegierst die Korrekturen an den Implementierer (Subagent `general`):

   ```text
   Setze den folgenden Korrekturauftrag um. Ändere ausschließlich Produktions-
   code, Tests und Doku so, wie der Auftrag es verlangt — keine
   eigenmächtigen Erweiterungen, kein Refactoring. Führe danach die betroffenen
   Gates aus (mvn test / npm test / npm run lint / npm run sheriff) und
   berichte je Korrektur: erledigt (mit Datei) oder offen (mit Begründung).

   <Korrekturauftrag des Reviewers>
   ```

   Warte auf das Ergebnis und **prüfe die gemeldeten Änderungen kurz selbst**
   (`git diff --stat`, gezielte Greps) — der Implementierer meldet, der
   Reviewer urteilt.

4. **Erneut reviewen.** Dispatche `architektur-reviewer` ein zweites Mal mit
   demselben Scope und dem Hinweis, welche Korrekturen umgesetzt wurden.
   - `OK` → Schritt 5.
   - `KORREKTUREN` → Schritt 3 (max. **3** Runden). Danach: Befunde
     offenlegen, nicht weiterdelegieren — der Auftraggeber entscheidet.

5. **Protokollieren.** Urteil als Kommentar am Issue:

   ```markdown
   <!-- review: status=OK pruefer=architektur-reviewer -->

   ## Architektur-Review — <Feature-Titel>

   **Urteil:** OK
   **Runden:** <n> · **Korrekturen umgesetzt:** <n>

   | Befund | Severity | Status |
   |--------|----------|--------|
   | <Kurztitel> | MAJOR | behoben (`Datei`) |

   **Gates**

   - `backend: ./mvnw -B test` → BUILD SUCCESS (8 Tests)
   - `frontend: npm test` → 4 passed
   - `frontend: npm run lint` → clean
   - `frontend: npm run sheriff` → keine Issues
   ```

   Ist das Issue nicht erreichbar, gib das Protokoll im Chat aus.

6. **Nach `OK`:** Das Architektur-Gate ist bestanden. Die **Abnahme**
   (`/abnahme <Issue-Nr>`) ist ein eigenes Gate und muss danach laufen — ein
   `OK` hier ersetzt sie nicht.

## Regeln

- **Ein Gate, ein Urteil.** Kein „grundsätzlich ok, aber …" — entweder OK
  oder KORREKTUREN mit Auftrag.
- **Der Reviewer urteilt, du delegierst.** Korrekturen werden nicht vom
  Reviewer ausgeführt und nicht vom Reviewer selbst abgenommen.
- **Kein PASS mit offenen BLOCKERn.** Auch nicht, wenn die Korrektur
  „offenbar harmlos" wirkt.
- **Harte Grenze:** 3 Runden. Danach eskalieren statt endlos delegieren.
- **Kontext sparen:** Reports zusammenfassen, keine Logdateien in die
  Nachricht kopieren.
