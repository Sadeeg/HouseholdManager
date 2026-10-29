# 10. Qualitätsanforderungen

## Qualitätsszenarien

| Nr | Szenario | Erwartung | Priorität | Nachweis |
|----|----------|-----------|-----------|----------|
| Q1 | Eine Fachregel ändert sich | Die Änderung betrifft **eine** Klasse im `domain`/`application`; keine Adapternderung, kein Frontend-Touch | hoch | `backend-ports-adapters` (Abhängigkeitsregel) |
| Q2 | Ein Entwickler prüft die Umsetzung | Er liest das Issue (Ziel, ACs, Tests, Verify) und findet ohne Architekturkenntnis die betroffenen Dateien | hoch | Issue-Hausstil (`.opencode/skills/issue-formulierung/references/hausstil.md`) |
| Q3 | Eine neue fachliche Regel wird ergänzt | Sie ist **ohne** Datenbank testbar (Use-Case-Test ohne Spring) | hoch | `HouseholdUseCaseImplTest` |
| Q4 | Ein neues Feature kommt | Es ist ein eigener Ordner mit `routes.ts`, lazy ladbar, ohne Eingriff in bestehende Features | hoch | Sheriff-Regeln, `npm run sheriff` |
| Q5 | Die Datenbank wird ersetzt | Nur die ausgehenden Adapter ändern sich; Use Cases bleiben unberührt | mittel | Ports & Adapters |
| Q6 | Ein Fehlerfall tritt auf | Der Nutzer sieht eine verständliche Meldung; intern bleibt der Fehler ein 5xx, fachlich ein 4xx | hoch | `GlobalExceptionHandler` + Tests |
| Q7 | Die Liste ist leer | Die Oberfläche zeigt einen Hinweis, keine leere Fläche | mittel | `households-page.spec.ts` |
| Q8 | Der Auftraggeber liest die Dokumentation | arc42 beschreibt Bausteine, Abläufe und offene Punkte ohne Code-Snippets | mittel | arc42/ |
| Q9 | Eine Anforderung ist unklar | Der Prozess stellt genau **eine** Frage pro Zug und endet mit bestätigtem Verständnis | hoch | `anforderung-loop` (Grilling, Stop-Kriterien) |
| Q10 | Ein Feature gilt als fertig | Zwei unabhängige Gates (`OK` + `PASS`) liegen vor; Urteile kommen von Subagenten | hoch | `/review`, `/abnahme` |

| Q11 | Eine Person bedient das Board ohne Maus oder ohne Farbwahrnehmung | Jeder Bahnwechsel hat ein Tastatur-Äquivalent; Status und Kennzeichnungen stehen als Text da, nie nur als Farbe | hoch | Issue #1 `## UX` (Barrierefreiheit), AC1/AC13/AC22 |

## Nicht-funktionale Anforderungen

| Thema | Anforderung | Stand |
|-------|--------------|-------|
| Performance | Liste lädt lokal < 300 ms (siehe 06) | Zielwert — Messung noch offen (siehe 06) |
| Sicherheit | Keine unauthentifizierte API | **offen** — siehe 08 |
| Datenschutz | Haushaltsdaten nur für Mitglieder des Haushalts sichtbar | entworfen (Issue #1): Mitgliedschaftsprüfung im Use Case (siehe 08) — Umsetzung ausstehend |
| Wartbarkeit | Änderungen an der Fachlogik ohne Frontend-Änderung | durch Schichtregeln angestrebt |
| Nachvollziehbarkeit | Entscheidungen als ADR, Anforderungen als Issue | vollständig |
| Bedienbarkeit | Ein Feature ohne Einarbeitung nutzbar | pro Feature zu prüfen (Abnahme) |

> **Offen:** Prioritäten bestätigen, Schwellenwerte für Performance und
> Verfügbarkeit festlegen.
