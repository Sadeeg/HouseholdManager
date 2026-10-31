# 6. Laufzeitsicht

## Szenario 1 — Haushalt anlegen

```text
1. Nutzer sendet POST /api/households {"name":"Haushalt Müller"}
2. HouseholdController prüft @NotBlank (Grenze), ruft HouseholdUseCase
3. HouseholdUseCaseImpl.trimmt den Namen und prüft die Invariante
   (nicht leer, höchstens 120 Zeichen) → sonst InvalidHouseholdNameException
4. Use Case setzt createdAt (Clock) und ruft den Port HouseholdRepository.save
5. HouseholdRepositoryAdapter mappt das Fachmodell auf HouseholdJpaEntity
   und speichert sie (Transaktion der Use Case)
6. Adapter mappt zurück → Use Case → Controller → HouseholdResponse (201)
```

**Fehlerpfad:** Fachfehler → `InvalidHouseholdNameException` →
`GlobalExceptionHandler` → `ProblemDetail` mit **400**. Andere
`IllegalArgumentException` bleiben Programmierfehler (5xx) und werden
**nicht** als Benutzerfehler getarnt.

## Szenario 2 — Haushalte auflisten

```text
1. GET /api/households
2. Controller → HouseholdUseCase.listHouseholds() (@Transactional readOnly)
3. Port → Adapter → findAll() → Map auf Fachmodell
4. Response: HouseholdResponse[]
5. Frontend: HouseholdsPage liest HouseholdsData (httpResource, eager)
   → Zustände: isLoading / error / hasValue → rendert HouseholdItem je Haus
```

**Leerfall:** `hasValue()` mit leerem Array → die UI zeigt
„Es ist noch kein Haushalt angelegt." (Hinweis statt Leere).

## Szenario 3 — Feature öffnen (Frontend)

```text
1. Route /households → loadChildren → features/households/routes.ts
2. Route stellt HouseholdsData bereit (route-scoped) → httpResource startet
3. HouseholdsPage rendert; Fehlerfall → Fehlerhinweis („Bitte später erneut
   versuchen"). `HouseholdsData.reload()` existiert, ist im UI aber noch nicht
   als Aktion verdrahtet.
```

**Lebenszyklus der route-scoped Provider:** `provideRouter(routes,
withAutoCleanupInjectors())` zerstört den Injector einer Route, sobald sie
verlassen wird. Damit ist ein Feature beim erneuten Betreten eine frische
Instanz — und `httpResource` liest erneut. Ohne diese Option überlebt die
Instanz den Wegwechsel, `httpResource` feuert bei unveränderter URL nicht, und
die Anzeige zeigte den alten Stand (Arc42 09, „Weitere Entscheidungen").

## Szenario 4 — Aufgaben-Tafel im gewählten Workspace (Issue #1, entworfen)

```text
1. Person wählt den Haushalt im Workspace-Wähler (core/workspace); Auswahl wird
   beim nächsten Besuch wiederhergestellt
2. GET /api/households/{householdId}/tasks → TaskUseCase prüft zuerst die
   Mitgliedschaft (CurrentUserPort); Nicht-Mitglied → 403 → Navigationspunkt
   „AUFGABEN“ erscheint nicht (AC12)
3. Board rendert drei Bahnen in gespeicherter Reihenfolge (`position`, A15);
   „überfällig“ = dueDate < heutiger lokaler Tag
   und Status ≠ DONE — ohne Backend-Beteiligung auf der Karte berechnet (AC22)
4. Drag & Drop → PATCH /api/households/{householdId}/tasks/{taskId} {status};
   Fehler → Karte springt in die Ausgangsbahn zurück, Toast (AC19)
5. Workspace-Wechsel: die Data-Services laden mit neuer householdId neu;
   die Seite lädt nicht neu (AC23)
```

**Fehlerpfad:** Nicht-Mitglied / fremder Haushalt → Fachfehler →
`ProblemDetail` **403**; Löschaktionen wirken nur nach Bestätigung im Dialog.

## Szenario 5 — Geplanter Tageslauf (Issue #10)

```text
1. WasteSyncJob (@Scheduled, ADR 0005) ruft WasteCollectionUseCase.syncAllHouseholds()
2. Use Case liest über den Haushalt-In-Port alle Haushalte mit hinterlegter Quelle
3. je Haushalt: Quelle lesen → WasteCalendarFeed.fetch(url, von, bis)
   → IcsWasteCalendarFeed (HTTP, Timeout) → ical4j (CalendarBuilder, RRULE-Auflösung)
   → WasteEvent
4. Use Case bestimmt das Fenster [heute, heute+42] (LocalDate.now(clock.withZone(Berlin)))
   und gibt es dem Port mit; ein Ereignis mit Uhrzeit zählt auf seinem
   Kalendertag in Europe/Berlin
5. Use Case ersetzt den Bestand des Haushalts (löschen + einfügen, eine Transaktion)
6. Use Case schreibt den Stand: last_attempt_at = jetzt; last_synced_at = jetzt (Erfolg)
```

**Fehlerpfad (R7/AC6):** Timeout, HTTP-Fehler oder kein lesbares ICS →
`WasteFeedUnavailableException` → der Use Case protokolliert, lässt den
Bestand **unverändert**, schreibt nur `last_attempt_at` und fährt mit dem
nächsten Haushalt fort. Der Job endet nicht mit Fehler.

**Leerfall (R8/AC13):** Quelle erreichbar, aber keine Termine im Fenster →
`last_attempt_at` und `last_synced_at` werden gesetzt, der Bestand wird
**geleert**. Die Anzeige nennt den Grund („Für die nächsten sechs Wochen liegen
keine Termine vor").

**Unterscheidung:** ein Dokument ohne `BEGIN:VCALENDAR` ist ein **Fehler**
(Bestand bleibt), ein gültiges `VCALENDAR` ohne passende Termine ist ein
**Leerfall** (Bestand wird geleert) — ADR 0006.

## Szenario 6 — Quelle speichern (Issue #10)

```text
1. PUT /api/households/{householdId}/waste/source  {"url":"https://…ics"}
2. WasteCollectionController → WasteCollectionUseCase.setSource(userId, householdId, url)
3. Use Case prüft zuerst die Mitgliedschaft (MembershipUseCase) → Nicht-Mitglied: 403
4. Use Case prüft die Adresse (nicht leer, höchstens 500 Zeichen, http/https)
   → sonst InvalidWasteCalendarUrlException → 400
5. Use Case speichert die Adresse über den Haushalt-In-Port (updateWasteCalendarUrl)
   — Transaktion 1 endet mit dem Methodenaufruf
6. Der Controller ruft danach den zweiten Use Case auf: syncHousehold(householdId)
   — Transaktion 2, eigener HTTP-Timeout von 5 s für den Abruf der Quelle.
   Kein Ereignis, kein Listener, kein @Async (ADR 0005).
   Die HTTP-Antwort wartet auf das Ergebnis: der Nutzer sieht die Termine
   direkt nach dem Speichern. Ein Fehler des Laufs wird isoliert (R7) — das
   Speichern bleibt erfolgreich, der Stand gilt als veraltet (R13).
   Der Tageslauf wird nicht abgewartet (R6/AC11)
```

**Fehlerpfad:** nicht angemeldet → 401; kein Mitglied → 403; unbekannter
Haushalt → 404; unzulässige Adresse → 400. Fehler der Quelle → **kein**
HTTP-Fehler, sondern ein veralteter Stand (ADR 0005, Fehlerisolierung).

## Szenario 7 — Termine anzeigen (Issue #10)

```text
1. GET /api/households/{householdId}/waste
2. Controller → WasteCollectionUseCase.overview(userId, householdId)
3. Use Case prüft die Mitgliedschaft (403) und liest Quelle (Haushalt-In-Port),
   Stand (waste_sync_state) und Termine (waste_collection, aufsteigend nach Datum)
4. Response: Quelle, Stand (letzter erfolgreicher Lauf), Veraltet-Flag, Termine
5. Frontend: WasteData (httpResource, route-scoped) → WastePage rendert Panel und Liste
```

**Leerfälle:** ohne Quelle → `sourceUrl = null`, keine Termine, kein Stand
(„Noch keine Synchronisation", R16); Quelle ohne Termine im Fenster → Quelle
und Stand stehen, die Liste ist leer (R15).

## Szenario 8 — Quelle entfernen (Issue #10)

```text
1. DELETE /api/households/{householdId}/waste/source (nach Bestätigung im Dialog)
2. Use Case prüft die Mitgliedschaft, löscht die Adresse (Haushalt-In-Port)
   und den Stand (waste_sync_state)
3. Die gespeicherten Termine bleiben in der Datenbank bestehen, werden aber
   nicht mehr angezeigt (Annahme 5 im Issue)
```

## Zeitverhalten

| Vorgang | Dauer (lokal) | Anmerkung |
|---------|----------------|-----------|
| Seite öffnen + Liste laden | < 300 ms | Dev-Setup, Postgres lokal |
| API-Aufruf (lokal) | < 50 ms | ohne Netzwerk |
| Anlegen (inkl. Persistenz) | < 100 ms | eine Transaktion |

> **Offen:** Zielwerte mit dem Auftraggeber bestätigen und um
> Performance-Szenarien in 10 ergänzen.

## Fehlerverhalten im Frontend

| Fall | Verhalten |
|------|-----------|
| Backend nicht erreichbar | `httpResource.error()` → Fehlerhinweis in der Seite |
| 400 vom Backend (ungültiger Name) | Fehlerhinweis; Validierung zusätzlich im Formular (Signal Forms) |
| Leere Liste | Hinweistext statt leerer Fläche |

> **Offen:** Formular zum Anlegen (Signal Forms) ist das nächste Feature;
> Fehlerdarstellung und Mehrfachauslösen sind dann mitzubestimmen.
