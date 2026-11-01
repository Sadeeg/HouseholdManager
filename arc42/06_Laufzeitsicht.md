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
| **HTTP-Controller** | `HouseholdController`, `TaskController`, `WasteCollectionController`, `TelegramChatController` | Request |
| **Scheduler** | `WasteSyncJob`, `NotificationDispatchJob`, `NotificationRetryJob` | Cron, konfiguriert im Composition Root (ADR 0005, 0007) |
| **Geplanter Abruf** | Teil des `WasteSyncJob` | je Haushalt, isoliert |
| **Bot-Eingang** | `TelegramBotInputAdapter` | Chat-Nachricht aus Telegram (Long-Poll) |

**Fehlerisolierung:** ein fehlerhafter Haushalt stoppt den Lauf nicht. Der
Adapter protokolliert, fährt mit dem nächsten fort und beendet den Job ohne
Fehler.

### Der Benachrichtigungslauf (18:00 Uhr)

Der Benachrichtigungslauf läuft nach **demselben Muster** wie der Tageslauf
(ADR 0005, ADR 0007):

```text
1. NotificationDispatchJob (Cron 18:00 Europe/Berlin) löst genau einen
   Use Case aus — keine Fachlogik im Adapter
2. Use Case bestimmt den Tag aus der Clock: „morgen" und „kommende Woche"
   (Montag–Sonntag); sonntags zusätzlich der Montag
3. Use Case liest die Termine über den In-Port des Müllkalender-Kontexts
   (Anzeigestand des letzten erfolgreichen Laufs — ein veralteter Stand
   wird nicht verworfen)
4. Use Case legt je Haushalt und Nachricht einen Zustellvermerk an und
   versendet ihn über den TelegramBot-Port
5. Fehlgeschlagener Versand: Fehler wird protokolliert, der Vermerk zählt
   den Versuch; ein weiterer Versuch folgt beim nächsten stündlichen Lauf
6. Nach dem dritten Fehlversuch: kein weiterer Versuch — die Verbindung
   des Chats bleibt bestehen
```

**Fehlerisolierung auf zwei Ebenen:** ein fehlgeschlagener **Chat** stoppt
die übrigen Chats desselben Haushalts nicht; ein fehlgeschlagener
**Haushalt** stoppt die übrigen Haushalte nicht. Das Vorbild ist die
Fehlerisolierung je Haushalt aus ADR 0005.

### Der Wiederholungslauf (stündlich)

Der dritte Zeitgeber folgt demselben Muster: `NotificationRetryJob` löst
genau einen Use Case aus (`NotificationDispatchUseCase.retryFailed()`), der
**keine** Fachlogik im Adapter. Der Use Case sucht alle Zustellvermerke, die
noch einen Versuch brauchen (`attempt_count < 3`, nicht abgeschlossen), und
versucht sie erneut — frühestens eine Stunde nach dem letzten Versuch. Die
Auswahl der Vermerke liegt im Use Case, die Taktung in der Konfiguration
(`householdmanager.telegram.retry-cron`, Vorgabe `0 0 * * * *`) — beide Läufe
werden in `config/ApplicationConfig` neben dem Tageslauf eingetragen
(ADR 0007, O2).

### Der Bot-Eingang (Long-Poll)

Der **Empfang** läuft nicht über HTTP, sondern über einen Abfragelauf:

```text
1. Beim ApplicationReadyEvent startet TelegramBotInputAdapter einen
   Daemon-Thread ("telegram-long-poll") — ohne konfigurierten Token
   startet er gar nicht
2. Der Thread fragt Telegram aktiv ab (getUpdates) und reicht je
   Nachricht den Text an TelegramChatUseCase.handleMessage(chatId, text)
3. Der Use Case entscheidet, ob es ein Freischalt-Code ist (Ziffernfolge
   oder "/code <Code>") und schaltet den Chat frei; alles andere wird
   ignoriert — der Bot schweigt
4. Der Code bestimmt den Haushalt: der Bot kennt keine andere Zuordnung.
   Ein unbekannter Code und ein Code, der zu einem anderen Haushalt als
   dem des Chats gehört, bekommen **dieselbe** Antwort — einen Hinweis,
   wie ein gültiger Code beschafft wird. Die Antwort verrät nicht, ob
   der Code existiert, und nennt nie den Haushaltsnamen
5. Antwortet der Bot, geht die Antwort über den TelegramBot-Port zurück
   in denselben Chat
6. Beim ContextClosedEvent endet der Abfragelauf — er darf den Shutdown
   nicht blockieren; ein Abfragefehler beendet den Lauf nicht
```

Damit gibt es **keinen** öffentlich erreichbaren Endpunkt für Telegram —
nur Egress (ADR 0007, O1).

## Ausgehende Adapter

| Art | Beispiel | Muster |
|-----|----------|--------|
| **Persistenz** | `HouseholdRepositoryAdapter` | Mapping im Adapter, Entity nie außerhalb |
| **HTTP** | `NextcloudUserDirectory`, `IcsWasteCalendarFeed` | `RestClient` mit Timeout; Fehler → Fachfehler |
| **Fremdbibliothek** | `ical4j` in `IcsWasteCalendarFeed` | Bibliothek endet an der Adaptergrenze, der Port bleibt bibliotheksfrei (ADR 0006) |
| **Nachrichtenkanal** | `TelegramBotAdapter` + `TelegramApiClient` | Bot-API endet an der Adaptergrenze, der Port bleibt protokollfrei (ADR 0007) |

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

Der Benachrichtigungs-Kontext schließt sich diesem Muster an: er liest die
Termine über den **In-Port des Müllkalender-Kontexts**, nicht über dessen
Tabellen (ADR 0007). Die Richtung ist einseitig — Benachrichtigung →
Müllkalender. Der Müllkalender weiß nichts von Telegram.

```text
NotificationDispatchUseCase
      │ liest die Termine des Anzeigestands
      ▼
WasteCollectionUseCase (In-Port des Müllkalender-Kontexts)
      │
      ▼
… keine gemeinsame Tabelle, kein Join über Context-Grenzen
```

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
