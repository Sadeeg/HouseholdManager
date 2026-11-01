# 6. Laufzeitsicht

## Das eine Muster: HTTP-Request durch die Schichten

Jeder eingehende Request läuft durch dieselben Stationen. Die Abbildung
zeigt den **Haushalt**-Ablauf als Beispiel — die Struktur gilt für jeden
Fach-Kontext.

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

**Transaktionsgrenze:** die Use Case. Der Adapter erbt die Transaktion, er
öffnet keine eigene. Lesende Use Cases sind `@Transactional(readOnly = true)`.

**Leerfall:** ein gültiger, aber leerer Bestand ist **kein** Fehler. Die UI
zeigt einen Hinweis statt einer leeren Fläche.

## Eingehende Adapter

Drei Arten — alle lösen genau einen Use Case aus und enthalten keine
Fachlogik:

| Art | Beispiel | Auslöser |
|-----|----------|----------|
| **HTTP-Controller** | `HouseholdController`, `TaskController`, `WasteCollectionController` | Request |
| **Scheduler** | `WasteSyncJob` | Cron, konfiguriert im Composition Root (ADR 0005) |
| **Geplanter Abruf** | Teil des `WasteSyncJob` | je Haushalt, isoliert |

**Fehlerisolierung:** ein fehlerhafter Haushalt stoppt den Lauf nicht. Der
Adapter protokolliert, fährt mit dem nächsten fort und beendet den Job ohne
Fehler.

## Ausgehende Adapter

| Art | Beispiel | Muster |
|-----|----------|--------|
| **Persistenz** | `HouseholdRepositoryAdapter` | Mapping im Adapter, Entity nie außerhalb |
| **HTTP** | `NextcloudUserDirectory`, `IcsWasteCalendarFeed` | `RestClient` mit Timeout; Fehler → Fachfehler |
| **Fremdbibliothek** | `ical4j` in `IcsWasteCalendarFeed` | Bibliothek endet an der Adaptergrenze, der Port bleibt bibliotheksfrei (ADR 0006) |

## Zugriff zwischen Kontexten

Ein Context greift **nie** auf die Tabellen eines anderen zu. Der Zugriff
läuft über den **In-Port** des anderen Contexts:

```text
WasteCollectionUseCase
      │  prüft zuerst die Mitgliedschaft
      ▼
MembershipUseCase.requireMembership()      (In-Port des Haushalt-Kontexts)
      │
      ▼
… keine gemeinsame Tabelle, kein Join über Context-Grenzen
```

Diese eine Richtung ist **einseitig** (ADR 0003): Müllkalender → Haushalt,
nie umgekehrt.

## Lesen und Anzeigen

```text
1. Route /households → loadChildren → features/households/routes.ts
2. Route stellt HouseholdsData bereit (route-scoped) → httpResource startet
3. HouseholdsPage rendert; Fehlerfall → Fehlerhinweis („Bitte später erneut
   versuchen").
```

**Lebenszyklus der route-scoped Provider:** `provideRouter(routes,
withAutoCleanupInjectors())` zerstört den Injector einer Route, sobald sie
verlassen wird. Damit ist ein Feature beim erneuten Betreten eine frische
Instanz — und `httpResource` liest erneut. Ohne diese Option überlebt die
Instanz den Wegwechsel, `httpResource` feuert bei unveränderter URL nicht, und
die Anzeige zeigte den alten Stand (Arc42 09, „Weitere Entscheidungen").

**Workspace-Wechsel:** die Data-Services laden mit neuer `householdId` neu;
die Seite lädt nicht neu.

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
| Externer Dienst nicht erreichbar | kein HTTP-Fehler, sondern ein **veralteter Stand** — der Bestand bleibt, die Anzeige weist den Stand aus |

> **Offen:** Formular zum Anlegen (Signal Forms) ist das nächste Feature;
> Fehlerdarstellung und Mehrfachauslösen sind dann mitzubestimmen.

> **Hinweis:** Die konkreten Abläufe einzelner Features (Aufgaben-Tafel,
> Müllkalender, Workspace-Auswahl) stehen in den jeweiligen GitHub-Issues
> unter `## UX` und `## Acceptance Criteria`. arc42 hält nur die
> **gemeinsamen Muster** fest.
