---
description: Architektur-Review-Gate: Runde 1 vollständig, Runden 2–3 nur offene K-Befunde — max. 3 Runden
---

Führe das **Architektur-Review-Gate** aus. Ein Feature gilt erst als fertig,
wenn dieses Gate `OK` meldet.

Scope (Issue-Nummer oder Branch/Dateien):
$ARGUMENTS

Vorgehen:

1. **Sollbild und Rundenstand holen.** Issue über die GitHub-Tools lesen
   (`issue_read`, `method: "get"`): Ziel, Akzeptanzkriterien,
   `## Out of Scope`. Dazu **die Kommentare** (`method: "get_comments"`):

   - **Runde** = höchste `runde` im Marker
     `<!-- review-findings: runde=<n> … -->` + 1. Ohne Marker: Runde 1.
   - **Offene `K`-Befunde** aus dem letzten `review-findings`-Kommentar
     (Abschnitt „Blockierend"). Der Chatverlauf zählt nicht — nach einer
     Kompaktierung ist der Issue-Kommentar die einzige Quelle.

   Ist Runde > 3: kein weiteres Gate. Offene Befunde offenlegen, den
   Auftraggeber entscheiden lassen, hier abbrechen.

   Lade die Skill `code-review-gate` (Kategorien, Severity, Urteilsform).

2. **Reviewer starten.** Dispatche den Subagenten `architektur-reviewer`
   (eigene Session, frischer Kontext, read-only) mit:

   ```text
   Runde <n> für Issue <Nr>. Prüfe <Issue-Nr / Scope> gegen die Regeln aus
   AGENTS.md, den ADRs und den Skills backend-ports-adapters,
   frontend-feature-architecture und feature-testing.

   Runde 1: vollständiger Durchgang, geschlossene Befundliste mit K-/N-IDs.
   Runde 2–3: ausschließlich diese offenen K-Befunde prüfen und den
   Korrektur-Diff auf Regressionen — keine neuen MINOR/NIT:
   <K-Liste aus dem Befund-Kommentar, je ID + Ort + Soll>

   Liefere das Urteil OK oder KORREKTUREN; bei KORREKTUREN den
   ausführbaren Korrekturauftrag. Ändere keine Dateien.
   ```

   Warte auf sein Urteil.

3. **Befundstand sofort protokollieren.** Nach **jeder** Runde — nicht erst
   bei `OK` — einen eigenen Befund-Kommentar am Issue anlegen (nicht den
   vorigen überschreiben, die Historie bleibt lesbar):

   ```markdown
   <!-- review-findings: issue=<n> runde=<r> status=<OK|KORREKTUREN> -->

   ## Review-Befunde — Runde <r>

   **Blockierend**
   | ID | Severity | Befund | Status |
   |----|----------|--------|--------|
   | K1 | MAJOR | … (`Datei:Zeile`) | behoben / offen |

   **Nicht blockierend (Follow-up)**
   | ID | Severity | Befund |
   |----|----------|--------|
   | N1 | NIT | … |
   ```

   Ohne diesen Kommentar findet die Folgerunde die Befunde nicht wieder.

4. **Bei `KORREKTUREN` — delegieren.** Der Reviewer führt nicht aus. Du
   delegierst **nur die blockierenden `K`-Befunde** an den Implementierer
   (Subagent `general`):

   ```text
   Setze den folgenden Korrekturauftrag um. Ändere ausschließlich
   Produktionscode, Tests und Doku so, wie der Auftrag es verlangt — keine
   eigenmächtigen Erweiterungen, kein Refactoring. Führe danach nur die
   betroffenen Tests aus (-Dtest=…, --include=…), keinen vollen Suite-Lauf.
   Berichte je K-Befund: erledigt (mit Datei) oder offen (mit Begründung).

   <Korrekturauftrag des Reviewers, nur K-Befunde>
   ```

   `N`-Befunde (MINOR/NIT) werden **nicht** umgesetzt: ein Follow-up-Issue
   je Befund anlegen (`gh issue create`) oder am Feature-Issue sammeln.

   Warte auf das Ergebnis und **prüfe die gemeldeten Änderungen kurz selbst**
   (`git diff --stat`, gezielte Greps) — der Implementierer meldet, der
   Reviewer urteilt.

5. **Erneut reviewen.** Dispatche `architektur-reviewer` mit `Runde <r+1>`
   und der `K`-Liste aus dem Befund-Kommentar (Schritt 2).
   - `OK` → Schritt 6.
   - `KORREKTUREN` → Schritt 3, dann Schritt 4 (max. **3 Runden insgesamt**).
     Nach Runde 3: Befunde offenlegen, nicht weiterdelegieren — der
     Auftraggeber entscheidet.

6. **Protokollieren.** Urteil als Kommentar am Issue:

   ```markdown
   <!-- review: status=OK pruefer=architektur-reviewer -->

   ## Architektur-Review — <Feature-Titel>

   **Urteil:** OK
   **Runden:** <n> · **Korrekturen umgesetzt:** <n>

   | Befund | Severity | Status |
   |--------|----------|--------|
   | <Kurztitel> | MAJOR | behoben (`Datei`) |

   **Follow-ups:** <Links auf N-Befunde>

   **Gates**

   - `backend: ./mvnw -B test` → BUILD SUCCESS (8 Tests)
   - `frontend: npm test` → 4 passed
   - `frontend: npm run lint` → clean
   - `frontend: npm run sheriff` → keine Issues
   ```

   Ist das Issue nicht erreichbar, gib das Protokoll im Chat aus.

7. **Nach `OK`:** Das Architektur-Gate ist bestanden. Die **Abnahme**
   (`/abnahme <Issue-Nr>`) ist ein eigenes Gate und muss danach laufen — ein
   `OK` hier ersetzt sie nicht.

## Regeln

- **Ein Gate, ein Urteil.** Kein „grundsätzlich ok, aber …" — entweder OK
  oder KORREKTUREN mit Auftrag.
- **Der Reviewer urteilt, du delegierst.** Korrekturen werden nicht vom
  Reviewer ausgeführt und nicht vom Reviewer selbst abgenommen.
- **Kein PASS mit offenen BLOCKERn.** Auch nicht, wenn die Korrektur
  „offenbar harmlos" wirkt.
- **Harte Grenze:** 3 Runden **insgesamt**, gezählt am
  `review-findings`-Kommentar des Issues. Danach eskalieren statt endlos
  delegieren. Ein erneuter `/review`-Aufruf beginnt keine neue Zählung.
- **Nicht blockierend heißt nicht abarbeiten.** MINOR/NIT gehen als
  Follow-up raus und werden in diesem Durchlauf nicht umgesetzt.
- **Kontext sparen:** Reports zusammenfassen, keine Logdateien in die
  Nachricht kopieren.
