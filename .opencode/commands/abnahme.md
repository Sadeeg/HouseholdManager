---
description: Abnahme eines implementierten Features durch den kritischen Tester (PASS/FAIL)
---

Führe die **Abnahme** eines implementierten Features durch.

Feature / Issue:
$ARGUMENTS

Vorgehen:

1. **Sollbild und Vorlauf holen.** Issue über die GitHub-Tools lesen
   (`issue_read`, `method: "get"`): Akzeptanzkriterien, `## Tests`,
   `## Verify`, `## Out of Scope`. Dazu **die Kommentare**
   (`method: "get_comments"`): Gibt es bereits ein Abnahme-Urteil?

   - **Erste Abnahme:** ohne vorherigen `<!-- abnahme: … -->`-Marker.
   - **Wiederholung:** mit vorherigem `FAIL`. Dann nur die dort offenen
     ACs/Befunde prüfen — es gibt **höchstens eine** Wiederholung; ein
     zweites `FAIL` wird eskaliert, nicht erneut getestet.

   Steht keine Issue-Nummer im Argument, nachfragen. Lade die Skill
   `feature-testing` (Testebenen, Definition von „vollständig getestet").

2. **Kritischen Tester starten.** Dispatche den Subagenten
   `kritischer-tester` (eigene Session, frischer Kontext) mit:

   ```text
   Prüfe das Feature aus Issue <Nr> adversarial gegen seine
   Akzeptanzkriterien. Schreibe fehlende Tests (nur Testdateien),
   führe alle Tests aus und melde PASS oder FAIL mit Belegen.

   <bei Wiederholung:>
   Wiederholung. Prüfe ausschließlich diese offenen Punkte aus dem letzten
   Abnahme-Kommentar — keine neue Abdeckungsmatrix, keine neue
   Härtungsrunde:
   <AC-/Befundliste mit Ort und Beleg>
   ```

   Der Tester arbeitet **unabhängig** — er kennt deine Umsetzung nicht und
   urteilt selbst. Warte auf sein Urteil, arbeite nicht parallel daran.

3. **Urteil auswerten.**

   - **PASS** → Kommentar an das Issue hängen (siehe Format unten). Das
     Feature ist damit fertig. Der Statusmarker im Body bleibt unberührt —
     er gehört dem Anforderungs-Loop (`IN_PROGRESS`, `READY_FOR_AGENT`,
     `BLOCKED`); die Abnahme hat einen **eigenen** Marker im Kommentar.
   - **FAIL** → Kommentar hängen. Die Befunde zurückgeben: der Tester
     schreibt die Tests **nicht** in den Produktionscode, das ist
     Implementierungsarbeit. Danach genau **eine** Wiederholung; ein
     zweites `FAIL` wird dem Auftraggeber vorgelegt, nicht erneut getestet.

4. **Kein Selbsturteil.** Besteht der Tester nicht, gilt das Feature als
   nicht fertig — auch wenn die Tests grün sind. Ein `PASS` in der
   Wiederholung bezieht sich nur auf die geprüften offenen Punkte; alles
   davor Geprüfte bleibt gültig.

## Kommentar-Format

```markdown
<!-- abnahme: status=PASS pruefer=kritischer-tester runde=<1|2> -->

## Abnahme — <Feature-Titel>

**Urteil:** PASS | FAIL
**Geprüfte Akzeptanzkriterien:** <n> · **belegt:** <n> · **Lücken:** <n>

| AC | Test | Belegt |
|----|------|--------|
| AC1 | `…Test.createHousehold_trimsName` | ja |
| AC3 | – | **nein** |

**Kommandos**

- `backend: ./mvnw -B test` → Tests run: 7, Failures: 0
- `frontend: npm test` → 4 passed

**Befunde**

- [BLOCKER] …
- [MAJOR] …
```

Regeln:

- Das Urteil ist das des Testers, nicht deins — auch wenn es dir nicht
  passt.
- Kein PASS, solange ein Akzeptanzkriterium unbelegt ist.
- **Wiederholung ist Delta, kein neuer Durchgang.** Nur die offenen Punkte,
  keine zweite Härtungsrunde, keine zusätzlichen Tests über den Befund
  hinaus. Höchstens eine Wiederholung.
- Findings immer mit Ort (Datei:Zeile oder Testname) und Beleg.
