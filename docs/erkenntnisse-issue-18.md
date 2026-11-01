# Erkenntnisse — Issue #18 „Fehlerpfad der Personensuche — Fehlermeldung wird nicht erreicht“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #18 (Branch
`feature/18-fehlerpfad-personensuche`, aufgebaut auf PR #19). Zweck: den
MAJOR-Befund aus der Abnahme zu Issue #4 festhalten und die Lehre als Regel
überführen.

Belegte Gates (Ende): Frontend **31 Dateien / 145 Tests** grün (kein
`expected fail` mehr) · `tsc` (app + spec), `lint`, `sheriff`, Build clean ·
Architektur-Review **OK** (0 BLOCKER, 0 MAJOR, 1 MINOR, 2 NIT) · Abnahme
**PASS**, 4/4 ACs belegt.

---

## 1. Behobenes Problem

### 1.1 Die Fehlermeldung der Suche war unerreichbar

- **Symptom:** Ist das Nutzerverzeichnis nicht erreichbar, brach die
  Suchkomponente während des Renderns ab (`ResourceValueError`) — die Meldung
  „Nutzerverzeichnis nicht erreichbar." kam nie an.
- **Ursache:** `httpResource.value()` wirft im Fehlerzustand. `results()` las
  den Wert ungeschützt; die Vorlage ruft `results()` auf — der Fehler
  landete mitten im Change Detection.
- **Nachbesserung:** `core/users/user-search.ts` — `results()` prüft zuerst
  `searchResource.error()` und liefert in dem Fall `NO_USERS`. Der Wert der
  Resource wird nur gelesen, wenn es keinen Fehler gibt.
- **Lehre:** Ein `computed` über einer `httpResource` muss den Fehlerzustand
  **vor** dem Wertabruf prüfen. `value()` ist im Fehlerzustand keine
  Fehlerquelle, sondern eine Falle.

### 1.2 Der Befund war sichtbar, aber nicht als Test festgehalten

- **Symptom:** Der Fehlerpfad war seit Issue #1 ungetestet — die Abnahme zu
  #4 hat ihn als MAJOR gemeldet, obwohl er **nicht** durch #4 verursacht war.
- **Ursache:** Die Tests deckten nur den Erfolgsfall ab. Ein Fehlerfall, der
  erst zur Laufzeit als Wurf auffällt, braucht eine eigene Beobachtung.
- **Nachbesserung:** Die beiden Tripwire-Tests (`it.fails`) aus der Abnahme zu
  #4 sind jetzt normale Tests; zusätzlich prüft `core/users/user-search.spec.ts`
  den Dienst direkt (Fehler gemeldet, keine Treffer, **kein Wurf**).
- **Lehre:** „Der Fehlerpfad ist ungetestet" ist kein Kleingelb, sondern ein
  MAJOR: Der Nutzer sieht in genau diesem Moment die Fehlermeldung. Regel im
  Skill `feature-testing` — jeder Fehlerzweig braucht einen Test.

## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| `httpResource.value()` wirft im Fehlerzustand | Kommentar an `core/users/user-search.ts`; `## Gotchas` in Issue #18 |
| Fehlerpfade gehören getestet | `feature-testing` — Fehlerzweig braucht einen Test (bereits als Grundsatz vorhanden, hier bestätigt) |

## 3. Abhängigkeit zu PR #19

Der Fix hängt an den Dateien aus Issue #4 (`core/users/user-search.ts` mit
geteilter Zustand, `user-picker.shared-state.spec.ts` mit den Tripwires).
Deshalb baut der Branch `feature/18-fehlerpfad-personensuche` auf
`feature/4-user-search-instanz-strategie` auf (PR #19, noch offen). Der PR zu
#18 ist gegen `main` eröffnet, damit die CI (die nur bei PRs gegen `main`
läuft) greift; der Diff gegen `main` enthält bis zum Merge von #19 zusätzlich
die Änderungen aus #4. Merge-Reihenfolge: **#19 zuerst**, dann #18 — oder
umgekehrt, der Stand ist in beiden Fällen grün.

## 4. Offene Kleinbefunde

| # | Was | Wo |
|---|---|---|
| Issue #20 | Nach einem Fehler löst derselbe Suchbegriff keinen zweiten Request aus (das Signal ändert sich nicht) — die naheliegendste Nutzeraktion bleibt ohne Wirkung. Als Charakterisierungstest festgehalten; das Setzen desselben Begriffs löst weiterhin **keinen** Request aus — der Weg führt über `retry()` (Issue #20) | `frontend/src/app/core/users/user-search.spec.ts` |
| Issue #21 | Ist **Nextcloud** nicht erreichbar, liefert `api/users/search` 200 mit `[]` → der Nutzer sieht „Keine Person gefunden." statt einer Fehlermeldung. Die Meldung erscheint nur, wenn das Backend selbst unerreichbar ist | `backend/.../adapters/outbound/nextcloud/NextcloudUserDirectory.java` |
| MINOR aus dem Review | `docs/erkenntnisse-issue-18.md` behauptete die Gate-Ergebnisse und eine PR-Nummer vor deren Existenz — nach dem Lauf berichtigt | `docs/erkenntnisse-issue-18.md` |

## 5. Umgebung

Frontend-Gates im Container (`node:22`), weil das Node auf diesem Rechner
(22.22.1) unter dem Minimum der Angular-CLI (≥ 22.22.3) liegt — siehe
`docs/erkenntnisse-issue-4.md`, Abschnitt 5.
