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
