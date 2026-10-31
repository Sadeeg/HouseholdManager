# Erkenntnisse — Issue #11 (Testgenauigkeit aus der Abnahme von #10)

Follow-up ohne Verhaltensänderung: fünf Befunde der Abnahme von Issue #10,
die alle dasselbe Muster haben — **ein Test behauptet mehr, als er beweist**.

## Was belegt war und was nicht

| Befund | Der Fehler im Test | Der Beleg danach |
|---|---|---|
| AC3 Fensterfilter | Use Case filtert nie selbst getestet; nur der Adapter hielt das Fenster | `syncHousehold_discardsTermsOutsideTheWindow` — Stub-Feed liefert terms unabhängig vom Fenster, die Assertion kann nur durch den Filter entstehen |
| AC4 Reihenfolge | Component-Test prüft die Durchgabeordnung: was durchgereicht wird, bleibt grün | Data-Ebene-Test: unsortierte Antwort, Behauptung gegen `terms()` — kann falsche Sortierung widerlegen |
| AC7 kein Aufgabenbezug | Reflexion über Feldtypen widerlegt keine zur Laufzeit erzeugte Aufgabe | `WasteSyncLeavesTasksUntouchedTest` — echte DB, Aufgabe vorher da, nach dem Lauf unverändert, keine zusätzliche Zeile |
| AC23 Abbrechen | Bewiesen war die Wiederherstellung des Feldes, nicht der fehlende Schreibpfad | `httpMock.verify()` im Testkörper: eine offene PUT-Anfrage wirft |
| AC-Labels | Bezeichnungen gegenüber dem Issue verschoben | Labels folgen wieder der Nummerierung von Issue #10 |

## Überführt (Regeln im Skill `feature-testing`)

1. **Negativbeweis gehört mit `verify()` geschrieben.** „Kein request“ ist eine
   Aussage über die ganze Fläche, nicht über eine URL: `expectNone` prüft eine
   konkrete Adresse, `verify()` wirft bei jeder offenen Anfrage — und das ist
   der Beleg, dass der Schreibpfad nicht lief. (Der alte Stand hatte ein
   `verify()` im `afterEach`; das war zufällig derselbe Beweis, aber nicht der
   Behauptung im Test zugeordnet.)
2. **Ein Strukturtest ist kein Verhaltensbeweis.** Feldtypen per Reflexion
   können eine zur Laufzeit erzeugte Aufgabe nicht widerlegen. Als
   Architektur-Guard ist ein Strukturtest akzeptabel — dann darf er sich nicht
   als Bezeichnung eines AC ausgeben.
3. **Der Test muss auf der Ebene entscheiden, nicht durchgeben.** Ein Test, der
   die Ausgabe einer Sortierung prüft, ist grün, wenn die Sortierung fehlt und
   die Eingabe zufällig schon richtig steht. Die Behauptung muss auf der Ebene
   stehen, die die Reihenfolge *erzeugt* (hier: `terms()`, nicht das DOM).
4. **Zoneless ohne Component:** Effekte laufen erst, wenn sie ausdrücklich
   ausgelöst werden (`TestBed.flushEffects()`). Ein Data-Test ohnefixture
   braucht genau das, bevor `expectOne` etwas findet.

## Merkwürdigkeiten

- Der erste Data-Test war grün, obwohl die deutsche Kollation nicht geprüft
  war: `Restmülltonne`/`Singletonne` hat unter Default-Kollation dieselbe
  Reihenfolge wie im Code-Point-Vergleich — ein Test, der aus dem falschen
  Grund grün ist. Der vom kritischen Tester ergänzte Fall (`Ähm-Tonne` vor
  `Biotonne`) unterscheidet die Kollation.
- Befund 5 war im Arbeitsstand nur noch zur Hälfte vorhanden: drei von fünf
  Bezeichnungen waren durch das Bündeln (Issue #15) bereits richtig; es fehlte
  die Korrektur des No-Household-Tests, der als AC14 geführt wurde, obwohl der
  Fall ein UX-Zustand ohne Akzeptanzkriterium ist. Die Abnahme von #15 zeigte
  dieselbe Klasse von Fehlern in `waste-calendar-section.spec.ts` (Pflegen war
  als AC23, der Zähler als AC18 bezeichnet) und im Confirm-Fall von
  `households-page.spec.ts` (entfernt-erst-nach-Bestätigung ist AC9 +
  UX-Schritt 7, nicht AC23) — alle Labels stehen jetzt auf der Nummerierung
  von Issue #10.

## Nicht durchgeführt

Sichtprüfung und Browserpfad entfielen — keine UI-Änderung, nur Testdateien.

## Verify

```bash
cd frontend
npx tsc --noEmit -p tsconfig.spec.json && npm test && npm run lint && npm run sheriff
```

Ergebnis: 19 Dateien / 123 Tests grün (122 wie im Stand von #15, +1 Kollations-
fall auf der Data-Ebene). Backend unverändert; die beiden belegenden Klassen
(`WasteCollectionUseCaseImplTest`, `WasteSyncLeavesTasksUntouchedTest`) laufen
grün gegen Testcontainers.

## Gates

- Architektur-Review: **OK** (1 MINOR — Reihenfolge-Behauptung im neuen Kommentar, korrigiert).
- Abnahme: **PASS** (5 Befunde, 5 belegt; kein tautologischer Test, Mutation geprüft).
