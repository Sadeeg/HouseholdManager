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

| Q11 | Eine Person bedient das System ohne Maus oder ohne Farbwahrnehmung | Jeder Bahnwechsel hat ein Tastatur-Äquivalent; Status und Kennzeichnungen stehen als Text da, nie nur als Farbe | hoch | `ux-design`, Design-DNA (Barrierefreiheit) |
| Q12 | Ein externer Dienst ist nicht erreichbar oder liefert Müll | Keine gespeicherten Daten gehen verloren; die Anzeige weist den Stand als **veraltet** aus; der Lauf bricht nicht ab | hoch | `WasteCollectionUseCaseImplTest`, `IcsWasteCalendarFeedTest` |
| Q13 | Ein Lieferant führt eine neue Art ein | Sie erscheint in der Liste, ohne dass Code geändert werden muss | mittel | `IcsWasteCalendarFeedTest` (Adapter, ical4j) |
| Q14 | Ein zeitgesteuerter Lauf wird geprüft | Der Test läuft ohne Wartezeit, ohne Netz und ohne Datenbank — die Zeit ist eine `Clock`, der Job ein dünner Adapter | hoch | `WasteCollectionUseCaseImplTest`, `WasteSyncJobTest` |
| Q15 | Die Anwendung startet, obwohl eine externe Quelle nicht erreichbar ist | Der Start ist unauffällig; der Fehler erscheint erst im Lauf, je Kontext isoliert protokolliert | mittel | `WasteSyncJobTest` |
| Q16 | Eine Benachrichtigung kann nicht zugestellt werden | Der Fehler endet als **einzelner** Zustellvermerk; der Versand an die übrigen Chats läuft weiter; nach dem dritten Versuch stellt der Lauf das **Weiterprobieren** ein, ohne die Verbindung zu lösen | hoch | `NotificationDispatchUseCaseImplTest` (dritte Versuche, Isolation je Chat) |
| Q17 | Der 18:00-Uhr-Lauf wird geprüft | Der Test läuft ohne Wartezeit, ohne Netz und ohne Datenbank — die Zeit ist eine `Clock`, der Job ein dünner Adapter; der Wochentag entscheidet die Nachrichtenart | hoch | `NotificationDispatchUseCaseImplTest`, `NotificationDispatchJobTest` |
| Q18 | Eine neue fachliche Regel ändert sich | Sie berührt **eine** Klasse im `domain`/`application` des Benachrichtigungs-Kontexts; der Müllkalender und die Telegram-Adapter bleiben unberührt | hoch | Ports & Adapters, Architekturregeln (Contextgrenze) |
| Q19 | Telegram ist nicht erreichbar oder lehnt eine Nachricht ab | Der Start ist unauffällig; ein **Zustellfehler** zählt den Versuch und wird beim nächsten stündlichen Lauf wiederholt, ein **Protokollfehler** nicht; die übrigen Chats und Haushalte laufen weiter | hoch | `NotificationDispatchUseCaseImplTest`, `TelegramBotInputAdapterTest` |
| Q20 | Die Anwendung wird heruntergefahren | Der Long-Poll-Thread endet mit dem Shutdown und blockiert ihn nicht; ein Neustart verliert den persistenten Versuchszähler nicht | hoch | `TelegramBotInputAdapterTest`, `NotificationDeliveryRepositoryAdapterTest` |

## Nicht-funktionale Anforderungen

| Thema | Anforderung | Stand |
|-------|--------------|-------|
| Performance | Liste lädt lokal < 300 ms (siehe 06) | Zielwert — Messung noch offen (siehe 06) |
| Sicherheit | Keine unauthentifizierte API | **offen** — siehe 08 |
| Datenschutz | Haushaltsdaten nur für Mitglieder des Haushalts sichtbar | entworfen: Mitgliedschaftsprüfung im Use Case (siehe 08) — Umsetzung ausstehend |
| Wartbarkeit | Änderungen an der Fachlogik ohne Frontend-Änderung | durch Schichtregeln angestrebt |
| Nachvollziehbarkeit | Entscheidungen als ADR, Anforderungen als Issue | vollständig |
| Bedienbarkeit | Ein Feature ohne Einarbeitung nutzbar | pro Feature zu prüfen (Abnahme) |
| Aktualität | Der Stand weist Veraltung **aus**, statt stillschweigend veraltete Daten zu zeigen; die Oberfläche leitet die Veraltung nicht aus dem Zeitstempel ab | umgesetzt — das Veraltet-Flag kommt vom Server |
| Fremdformat | Das ICS-Format bleibt im ausgehenden Adapter; die Anwendung kennt keine Fremdbibliothek und kein Fremdformat | umgesetzt (ADR 0006) |

> **Offen:** Prioritäten bestätigen, Schwellenwerte für Performance und
> Verfügbarkeit festlegen.
