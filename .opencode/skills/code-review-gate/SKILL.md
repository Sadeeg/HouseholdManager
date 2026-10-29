---
name: Code-Review-Gate
description: Use when reviewing a change in HouseholdManager before it counts as done. Defines the review categories (correctness, backend architecture, frontend architecture, tests, errors/data, security, conventions), the severity levels, the OK/KORREKTUREN gate verdict, and the format of the correction order that gets delegated back to the implementer. Load this when the architektur-reviewer runs or when you need to prepare, interpret or execute a correction order.
---

# Code-Review-Gate

Ein Review ist ein **Gate**, kein Meinungsaustausch: Am Ende steht genau ein
Urteil — `OK` oder `KORREKTUREN` — und im Fall von Korrekturen ein
**Korrekturauftrag**, der ohne Rückfrage umgesetzt werden kann.

Die Korrekturen werden **nicht vom Reviewer gemacht**, sondern
**zurückdelegiert**: Der Reviewer prüft, der Implementierer korrigiert, der
Reviewer prüft erneut. Der Reviewer ist read-only für Produktionscode.

## Was geprüft wird

| # | Kategorie | Woran es hängt |
|---|-----------|----------------|
| 1 | **Korrektheit** | Logik, Randfälle, Fehlerpfade, `null`/leer, Nebenwirkungen |
| 2 | **Architektur Backend** | Ports & Adapters: Abhängigkeitsrichtung `adapters → application → domain`, keine Framework-Typen in `domain`/`application`, Mapping nur im Adapter, Transaktion an der Use Case, keine Vermischung von Fach- und Persistenzlogik |
| 3 | **Architektur Frontend** | Feature-Architektur: Feature = Ordner mit `routes.ts`, Schichten `feature → ui/data/model`, keine Feature-Grenze überschritten, dumb Components ohne Backend-Zugriff, route-scoped Services, Namenskonventionen |
| 4 | **Tests** | Hat **jedes** `AC<n>` einen belegenden Test? Fehler- und Grenzfälle? Test auf der richtigen Ebene? Tautologie? Details: Skill `feature-testing` |
| 5 | **Fehlerbehandlung & Daten** | Fachfehler → HTTP-Status, `ProblemDetail`, Validierung an der Grenze **und** als Invariante, Flyway-Migration nötig und korrekt, keine Änderung bestehender `V`-Dateien |
| 6 | **Sicherheit** | Eingaben unvalidiert?, Secrets im Code, fehlende Autorisierung, Injection (SQL/Pfad/Template), Daten im Log |
| 7 | **Konventionen & Doku** | `AGENTS.md`, Issue-Hausstil, ADR-Pflicht bei Architekturänderungen, keine toten Codes, keine Debug-Reste |

Die Gates laufen zusätzlich: `npm run lint`, `npm run sheriff`,
`./mvnw -B test`, `npm test`.

## Severity

| Level | Bedeutung | Folge |
|-------|-----------|--------|
| **BLOCKER** | Verletzt eine verbindliche Regel (Architektur, Sicherheit, Korrektheit, fehlende Migration) oder das Feature ist falsch | muss korrigiert werden, Gate bleibt KORREKTUREN |
| **MAJOR** | Deutliche Abweichung von Konvention oder Lücke (unvollständige Fehlerbehandlung, fehlender Test für ein AC, Doku fehlt) | muss korrigiert werden |
| **MINOR** | Kleiner Verstoß ohne Verhaltensfolge (Namenskonvention, Struktur, Kommentar) | Korrektur empfohlen, blockiert das Gate nicht |
| **NIT** | Geschmack, Optimierung, Idee | nur nennen, wenn es den Code klarer macht; blockiert nie |

**Signal-to-Noise:** Nenne nur Befunde, die du **belegen** kannst (Ort +
Beleg). Ein sauberes Diff ist `OK` — keine erfundenen Nits. Jeder
nicht-belegbare Befund wird weggelassen.

## Gate-Vertrag

```text
REVIEW: OK | KORREKTUREN
Scope: <Dateien/Feature, geprüft gegen Issue #N>
Befunde: <n> (BLOCKER <n> · MAJOR <n> · MINOR <n> · NIT <n>)
Geprüfte Gates: <mvn test | npm test | npm run lint | npm run sheriff> → <Ergebnis>
```

- `OK` → keine BLOCKER/MAJOR-Befunde, alle Gates grün.
- `KORREKTUREN` → mindestens ein BLOCKER oder MAJOR, oder ein Gate ist rot.

## Korrekturauftrag (das, was delegiert wird)

Bei `KORREKTUREN` liefert der Reviewer einen Auftrag, der **ohne Rückfrage
umsetzbar** ist:

```text
KORREKTUR AUFTRAG — <Issue-Nr> / <Feature>
Regel: <welche Regel aus AGENTS.md, ADR oder Skill verletzt ist>

K1 [BLOCKER] <Kurztitel>
   Ort:      <Datei:Zeile>
   Problem:  <was falsch ist, konkret>
   Soll:     <die erwartete Änderung>
   Nachweis: <woran erkennbar, dass es erledigt ist>

K2 [MAJOR] …
```

Regeln für den Auftrag:

- **Kleinste Korrektur.** Kein Refactoring, keine Umbenennungen „außer Haus",
  kein Feature-Ausbau. Nur, was die Regel verlangt.
- **Keine neuen Anforderungen.** Was nicht im Issue steht, ist kein Befund
  (außer es verletzt eine verbindliche Regel).
- **Pro Korrektur:** Ort, Problem, Soll, Nachweis. Keine Romane.
- **Reihenfolge:** erst die Produktionskorrekturen, dann die Tests, dann die
  Doku.
- Der Auftrag wird **delegiert**, nicht selbst umgesetzt — an den
  Implementierer (siehe `/review`).

## Was der Reviewer nicht macht

- **Keine Tests schreiben** — das ist Aufgabe des Testers (`kritischer-tester`)
  und des Implementierers. Der Reviewer *fordert* fehlende Tests ein.
- **Keinen Produktionscode ändern.** Read-only.
- **Keine Feature-Entscheidungen.** „Sollten wir das als Epic machen?" ist
  eine Frage an den Auftraggeber, kein Befund.
- **Keine Geschmacksdiskussion.** MINOR/NIT sind Hinweise, keine Korrekturen.

## Grenze zum Test-Review

| | Architektur-Review (dieser) | Abnahme/Test-Agent (`kritischer-tester`) |
|---|---|---|
| Frage | Ist die Lösung **richtig gebaut**? | Ist das Verhalten **belegt**? |
| Urteil | OK / KORREKTUREN | PASS / FAIL |
| Blick | Regeln, Schichten, Verträge, Konventionen | Akzeptanzkriterien, Testabdeckung, Fehlerpfade |
| Korrigiert | Produktionscode (delegiert) | fehlende Tests (schreibt selbst) |

Beide Gates sind nötig: Ein review kann „sauber“ sein, während ein
Akzeptanzkriterium unbelegt ist — und umgekehrt.
