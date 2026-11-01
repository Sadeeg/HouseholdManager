# Severity — die Stufen der Befunde

Einzige Quelle für beide Gates (Architektur-Review, Abnahme). Der Tester liest
nur diese Datei; der Reviewer liest `SKILL.md` und diese Datei.

| Level | Bedeutung | Folge |
|-------|-----------|--------|
| **BLOCKER** | Verletzung einer verbindlichen Regel (Architektur, Sicherheit, Korrektheit, fehlende Migration) oder das Feature ist falsch | muss korrigiert werden, Gate bleibt KORREKTUREN |
| **MAJOR** | deutliche Abweichung von Konvention oder Lücke (unvollständige Fehlerbehandlung, fehlender Test für ein AC, Doku fehlt, fehlender Einstieg in der UI, stilles Verwerfen eines Werts ohne Meldung) | muss korrigiert werden |
| **MINOR** | kleiner Verstoß ohne Verhaltensfolge (Namenskonvention, Struktur, Kommentar) | Korrektur empfohlen, blockiert das Gate nicht |
| **NIT** | Geschmack, Optimierung, Idee | nur nennen, wenn es den Code klarer macht; blockiert nie |

**Signal-to-Noise:** Nenne nur Befunde, die du **belegen** kannst (Ort +
Beleg). Ein sauberes Diff ist `OK` — keine erfundenen Nits. Jeder
nicht-belegbare Befund wird weggelassen.
