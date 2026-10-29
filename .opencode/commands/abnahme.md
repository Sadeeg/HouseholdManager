---
description: Abnahme eines implementierten Features durch den kritischen Tester (PASS/FAIL)
---

Führe die **Abnahme** eines implementierten Features durch.

Feature / Issue:
$ARGUMENTS

Vorgehen:

1. **Sollbild holen.** Issue über die GitHub-Tools lesen (`issue_read` mit
   `method: "get"`): Akzeptanzkriterien, `## Tests`, `## Verify`,
   `## Out of Scope`. Steht keine Issue-Nummer im Argument, nachfragen.
   Lade die Skill `feature-testing` (Testebenen, Definition von
   „vollständig getestet").

2. **Kritischen Tester starten.** Dispatche den Subagenten
   `kritischer-tester` (eigene Session, frischer Kontext) mit:

   ```text
   Prüfe das Feature aus Issue <Nr> adversarial gegen seine
   Akzeptanzkriterien. Schreibe fehlende Tests (nur Testdateien),
   führe alle Tests aus und melde PASS oder FAIL mit Belegen.
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
     Implementierungsarbeit.

4. **Kein Selbsturteil.** Besteht der Tester nicht, gilt das Feature als
   nicht fertig — auch wenn die Tests grün sind.

## Kommentar-Format

```markdown
<!-- abnahme: status=PASS pruefer=kritischer-tester -->

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
- Findings immer mit Ort (Datei:Zeile oder Testname) und Beleg.
