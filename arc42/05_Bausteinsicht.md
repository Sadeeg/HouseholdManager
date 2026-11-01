# 5. Bausteinsicht

## Backend (`de.householdmanager`)

```text
                        ┌───────────────────────────────┐
   HTTP-Request ───────▶│ adapters/inbound/rest          │
                        │  HouseholdController           │
                        │  dto/ (Request, Response)       │
                        │  GlobalExceptionHandler        │
                        └───────────────┬───────────────┘
                                        │ spricht nur den Port an
                                        ▼
                        ┌───────────────────────────────┐
                        │ application/port/in             │
                        │  HouseholdUseCase (Interface)   │
                        ├───────────────────────────────┤
                        │ application/usecase             │
                        │  HouseholdUseCaseImpl          │
                        │  (Orchestrierung, Invariante)  │
                        ├───────────────────────────────┤
                        │ application/port/out            │
                        │  HouseholdRepository (Interface)│
                        └───────────────┬───────────────┘
                                        │ implementiert von
                                        ▼
                        ┌───────────────────────────────┐
                        │ adapters/outbound/persistence  │
                        │  HouseholdRepositoryAdapter     │
                        │  HouseholdJpaEntity            │
                        │  HouseholdJpaRepository        │
                        └───────────────┬───────────────┘
                                        ▼
                                    PostgreSQL 17

   domain/model: Household, InvalidHouseholdNameException  (framework-frei)
   config/ApplicationConfig: Composition Root (Clock-Bean)
   HouseholdManagerApplication: Einstiegspunkt (@SpringBootApplication)
```

**Regeln:** `adapters → application → domain`. `domain` importiert kein
Spring, kein JPA, kein Jackson. `application` darf `@Service` und
`@Transactional` nutzen, aber keine HTTP- oder JPA-Typen.

## Frontend (`src/app`)

```text
app.routes.ts ──lazy──▶ features/households/routes.ts
                              │
                              ├── HouseholdsPage        (feature: smart Component)
                              │      │  inject()
                              │      ▼
                              │   HouseholdsData         (data: httpResource)
                              │      │
                              │      ▼
                              │   HouseholdApi           (data: HttpClient)
                              │
                              └── HouseholdItem         (ui: dumb Component, Inputs)
                                   PageHeader            (shared: fachneutral)

core/config/api.config.ts    (app-weit: API-Basispfad)
```

**Regeln:** Ein Feature benutzt nur eigene Module (Ausnahme `shared`,
`core`); ein Modul benutzt nur tiefere Schichten
(`feature → ui, data, model`; `ui → model`; `data → model`). Geprüft von
Sheriff (`frontend/sheriff.config.ts`).

### Frontend: der Abschnitt „Telegram-Chats"

Der Abschnitt ist ein **dumb UI-Component** im Feature `households`
(`ui/telegram-chat-section.ts`), genau wie der Müllkalender-Abschnitt: er
kennt kein Backend, bekommt die Chats per Input und meldet Aktionen per
Output. Das Feature `households` bleibt der einzige Ort — die Chats gehören
zum **Haushalt** (Freischalt-Code und Chat-Liste sind haushaltsbezogen), nicht
zu einem eigenen Feature-Ordner.

Der Bot **antwortet im Chat**, nicht in der Oberfläche: die Oberfläche zeigt
den Code, den Chat-Namen und die Chat-Art und bietet das Trennen an — sie
sendet **keine** Nachricht an den Bot. Der Code ist damit ein **Anzeige-
Wert**, kein Formularfeld mit Gültigkeitsprüfung im Browser.

## Bausteine je Kontext

Die Tabelle nennt die **Art** eines Bausteins und seine Verantwortung —
nicht den Feature-Verlauf. Detaillierte Entwürfe einzelner Features stehen
im jeweiligen GitHub-Issue.

| Baustein | Art | Verantwortung |
|-----------|-----|----------------|
| `Household` (domain) | Fachmodell | Daten des Haushalts, keine Technik |
| `InvalidHouseholdNameException` | Fach-Exception | Unzulässiger Name — framework-frei, wird am Adapter zu 400 |
| `HouseholdUseCase` | Port (ein-) | Was die Anwendung kann |
| `HouseholdRepository` | Port (aus-) | Was die Anwendung braucht |
| `HouseholdUseCaseImpl` | Use Case | Orchestrierung, Validierung, Transaktionsgrenze |
| `HouseholdController` | Adapter (ein-) | HTTP ↔ Use Case |
| `GlobalExceptionHandler` | Adapter (ein-) | Fachfehler → `ProblemDetail` + Statuscode |
| `HouseholdRequest` / `HouseholdResponse` | DTO | Transportform der Ein- und Ausgabe (`dto/`) |
| `HouseholdRepositoryAdapter` | Adapter (aus-) | Fachmodell ↔ JPA, Ort des Mappings |
| `HouseholdJpaEntity` | Persistenzmodell | Tabellenabbildung (`household`) |
| `HouseholdJpaRepository` | Spring-Data-Repo | Technisches Detail des Persistenz-Adapters |
| `HouseholdManagerApplication` | Einstiegspunkt | `@SpringBootApplication`, Start der Anwendung |
| `HouseholdsPage` | Feature (smart) | Datenfluss, Zustand |
| `HouseholdsData` | Data | `httpResource`, **route-scoped** (in `routes.ts` bereitgestellt) |
| `HouseholdApi` | Data | HTTP-Client, **app-weit** (`providedIn: 'root'`) |
| `Household` (frontend) | Modell | Typ des Features (`model/household.model.ts`) |
| `HouseholdItem` | UI (dumb) | Darstellung eines Haushalts |
| `PageHeader` | Shared | Seitenkopf |
| `TelegramChat` (domain) | Fachmodell | Verbundener Chat: Chat-ID, Anzeigename, Chat-Art, Haushalt, freischaltende Person |
| `NotificationDelivery` (domain) | Fachmodell | Zustellvermerk: Ziel, Inhalt, Versuchszählung, Fehler, Status |
| `TelegramChatUseCase` | Port (ein-) | Freischaltung, Anzeige und Trennen der Chats eines Haushalts |
| `NotificationDispatchUseCase` | Port (ein-) | Benachrichtigung über Mülltermine; Auslöser für den 18:00-Uhr-Lauf |
| `TelegramChatRepository` | Port (aus-) | Persistenz der verbundenen Chats |
| `NotificationDeliveryRepository` | Port (aus-) | Persistenz der Zustellvermerke (Versuchszählung) |
| `TelegramBot` | Port (aus-) | Versand einer Nachricht an eine Chat-ID; **protokollfrei** |
| `TelegramChatUseCaseImpl` | Use Case | Mitgliedschaft prüfen, Code prüfen, Chat anlegen/entfernen |
| `NotificationDispatchUseCaseImpl` | Use Case | Zeitfenster bestimmen, Nachricht bauen, zustellen, zählen |
| `TelegramChatRepositoryAdapter` | Adapter (aus-) | Fachmodell ↔ JPA, Ort des Mappings |
| `NotificationDeliveryRepositoryAdapter` | Adapter (aus-) | Fachmodell ↔ JPA, Ort des Mappings |
| `TelegramBotAdapter` | Adapter (aus-) | Telegram-Bot-API; endet an der Adaptergrenze (ADR 0007) |
| `TelegramBotInputAdapter` | Adapter (ein-) | Long-Poll; nimmt Chat-Nachrichten entgegen und löst den Use Case aus |
| `NotificationDispatchJob` | Adapter (ein-) | 18:00-Uhr-Lauf; löst genau einen Use Case aus (ADR 0005-Muster) |
| `NotificationRetryJob` | Adapter (ein-) | stündlicher Wiederholungslauf; löst genau einen Use Case aus |
| `TelegramChatController` | Adapter (ein-) | HTTP ↔ Use Case (`api/households/{householdId}/telegram/…`) |
| `TelegramConfig` | Konfiguration | Bot-Token (`TelegramToken`-Record) und `RestClient` der Bot-API — **keine** Bean in `ApplicationConfig` |
| `TelegramChatSection` | UI (dumb) | Abschnitt „Telegram-Chats" auf der Haushaltsseite; reiner Input/Output |

### Kontext „Benachrichtigung"

Der Kontext hat **eigenes Fachmodell, eigene Tabellen und eigene Ports**; er
liest die Mülltermine über den In-Port des Müllkalender-Kontexts (ADR 0007).

```text
                ┌────────────────────────────────────────────┐
 Telegram  ───▶ │ adapters/inbound/telegram  (Bot-Eingang)    │
 (Chat-Nachricht)│  TelegramBotInputAdapter (Long-Poll)       │
                │  reicht die Nachricht an den Use Case      │
                └────────────────────┬───────────────────────┘
                                     │ spricht nur den Port an
                                     ▼
                ┌────────────────────────────────────────────┐
                │ application/port/in                         │
                │  TelegramChatUseCase (Interface)            │
                │  NotificationDispatchUseCase (Interface)    │
                ├────────────────────────────────────────────┤
                │ application/usecase                         │
                │  TelegramChatUseCaseImpl                    │
                │  NotificationDispatchUseCaseImpl            │
                │  (Orchestrierung, Invariante, Zeitregeln)   │
                ├────────────────────────────────────────────┤
                │ application/port/out                        │
                │  TelegramChatRepository (Interface)         │
                │  NotificationDeliveryRepository (Interface) │
                │  TelegramBot (Interface)                    │
                │  WasteCollectionUseCase (In-Port Müllkal.) │
                └────────────────────┬───────────────────────┘
                                     │ implementiert von
                                     ▼
                ┌────────────────────────────────────────────┐
                │ adapters/outbound/persistence              │
                │  TelegramChatRepositoryAdapter              │
                │  NotificationDeliveryRepositoryAdapter      │
                │ adapters/outbound/telegram                  │
                │  TelegramBotAdapter (Bot-API)               │
                │  TelegramApiClient                          │
                └────────────────────────────────────────────┘
```

Die eingehenden Adapter des Kontexts sind **vier**: der Bot-Eingang
(`TelegramBotInputAdapter`), der REST-Adapter (`TelegramChatController`)
und die beiden Scheduler (`NotificationDispatchJob`,
`NotificationRetryJob`). Sie sprechen nur die Ports an und tragen keine
Fachlogik (ADR 0005, ADR 0007).

**Die Zeitregeln liegen in der Anwendung, nicht im Adapter:** „morgen",
„kommende Woche" (Montag–Sonntag), der Auslösetag (Sonntag) und die
Versuchszählung sind Domänenregeln. Sie stehen im Use Case und rechnen mit
der `Clock`-Bean. Der Telegram-Adapter kennt keine Datumsregel; er sendet
Text an eine Chat-ID.

### Muster über alle Contexts

Diese Bausteinarten wiederholen sich je Fach-Kontext. Sie stehen hier
**einmal** — nicht je Feature:

| Muster | Ausprägung | Regel |
|--------|-----------|-------|
| **Use Case je Fach-Vorgang** | `HouseholdUseCase`, `TaskUseCase`, `MembershipUseCase`, `WasteCollectionUseCase` | Orchestrierung + Invariante; Transaktionsgrenze |
| **Fach-Exception je Regelverletzung** | `InvalidHouseholdNameException`, `WasteFeedUnavailableException`, `NotChatOwnerException`, `TelegramChatNotFoundException` | framework-frei; der Adapter übersetzt sie in einen Statuscode |
| **Repository-Port + Adapter je Aggregat** | `HouseholdRepository` → `HouseholdRepositoryAdapter` | Mapping ausschließlich im Adapter |
| **Controller je Kontext** | `HouseholdController`, `TaskController`, `MemberController`, `WasteCollectionController` | `api/<resource>`; DTO-Records in `dto/` |
| **In-Port für Context-Zugriff** | `MembershipUseCase.requireMembership`, `WasteCollectionUseCase.overview`, `WasteCollectionUseCase.collectionsOf` | ein Context greift **nie** auf die Tabellen eines anderen zu (ADR 0003, 0007) |
| **Scheduler-Adapter** | `WasteSyncJob`, `NotificationDispatchJob`, `NotificationRetryJob` | löst genau einen Use Case aus, enthält keine Fachlogik (ADR 0005, 0007) |
| **Workspace-Auswahl** | `WorkspaceUseCase`, `WorkspaceSelectionRepository` | app-weit in `core/workspace`; Auswahl wird je Person gemerkt (ADR 0004) |
| **Feature-Ordner je Kontext** | `features/households/`, `features/tasks/`, `features/waste/` | `routes.ts`; route-scoped Data-Services |
| **core-Bausteine** | `core/workspace/`, `core/users/` | app-weit, eine Instanz (`providedIn: 'root'`) |
| **shared-Komponenten** | `shared/components/user-picker`, `page-header` | fachneutral, ohne Fachlogik |
| **Fremdprotokoll im Adapter** | `IcsWasteCalendarFeed` (ICS), `TelegramBotAdapter` (Bot-API) | der Port bleibt protokoll- und bibliotheksfrei (ADR 0006, 0007) |
| **Zustellvermerk im Fachkern** | `NotificationDelivery` | Versuchszählung und Fehlerzustand sind persistent — ein Adapter zählt nicht |

### Datenzuordnung

| Tabelle | Kontext | Besonderheit |
|---------|---------|--------------|
| `household` | Haushalt | trägt die Adresse einer externen Quelle als untypisiertes Attribut — der Kontext kennt keine Fremdregeln |
| `household_member` | Haushalt | Mitgliedschaft; geprüft über den In-Port des Haushalt-Kontexts |
| `workspace_selection` | Haushalt | Workspace-Merkung je Person (ADR 0004) |
| `task`, `label`, `task_label` | Aufgabe | Reihenfolge je Spalte über ein `position`-Feld |
| `waste_collection`, `waste_sync_state` | Müllkalender | der Stand gehört dem **Lauf**, nicht dem Termin |
| `telegram_activation_code` | Benachrichtigung | je Haushalt **eine** Zeile: der Code (UNIQUE) und die Person, die ihn angefordert hat — die freischaltende Person der daraus entstehenden Verbindungen |
| `telegram_chat` | Benachrichtigung | je Haushalt viele Chats; je Chat genau eine freischaltende Person; UNIQUE über (household_id, chat_id) |
| `notification_delivery` | Benachrichtigung | Zustellvermerk je Nachricht; trägt die Versuchszählung |

**Fachliche Konstanten im `domain`:** `Household.MAX_NAME_LENGTH`,
`WasteCalendar.SYNC_WINDOW_DAYS` (42) und `WasteCalendar.MAX_URL_LENGTH` (500).
Das Fenster gibt der Use Case dem Feed-Port vor — eine Quelle der Wahrheit.
Die Konstante steht im Fachmodell, damit Use Case und Adapter dieselbe
Grenze kennen.

Im Benachrichtigungs-Kontext liegen dieselben Regeln: die **Versuchszählung**
(maximal drei Versuche, `NotificationDelivery.MAX_ATTEMPTS`) ist eine
fachliche Grenze und steht im Fachmodell, nicht im Adapter.

Die **Codeform** hat zwei Seiten, und die liegen an verschiedenen Orten: die
**Erzeugung** des Codes (Ziffern, nicht vorhersagbar) liegt beim
`ActivationCodeGenerator` im Persistenz-Adapter, die **Erkennung** im Chat
(eine Nachricht aus Ziffern oder `/code <Code>`) liegt im Use Case
`TelegramChatUseCaseImpl` — sie ist eine fachliche Entscheidung („ist das
eine Freischaltung?"), keine Telegram-Kenntnis. Beide Seiten sprechen
dieselbe Form (Ziffern), sonst entsteht ein Code, den der Bot nicht erkennt.

Der Generator liefert einen Code, den **noch kein Haushalt trägt**: der Code
ist über alle Haushalte eindeutig, ein zufälliger Treffer wäre ein Fehler des
Servers. Ohne konfigurierten Bot-Token wird **gar kein** Code erzeugt — der
Code liefe ins Leere; die Oberfläche weist stattdessen auf den fehlenden
Token hin.

**Der Freischalt-Code trägt die freischaltende Person.** Der Bot kennt keine
Person; deshalb entsteht der Code beim ersten Anfordern **im Namen** eines
Mitglieds und speichert dessen Kennung (`telegram_activation_code.activated_by`,
übernommen in `TelegramChat.activatedBy`). Die Freischaltung über diesen Code
gehört der Person, die ihn angefordert hat. Die Spaltengrenzen des
Benachrichtigungs-Kontexts stehen als Konstanten im Fachmodell
(`TelegramChat.MAX_CHAT_NAME_LENGTH`, `TelegramChat.MAX_CHAT_ID_LENGTH`,
`TelegramChat.MAX_ACTIVATED_BY_LENGTH`, `NotificationDelivery.MAX_CHAT_ID_LENGTH`,
`NotificationDelivery.MAX_CONTENT_LENGTH`) und werden von den JPA-Entities
gespiegelt.

**Migrationen:** jede Schema-Änderung ist eine **neue** `V`-Migration;
bestehende Dateien werden nie editiert. `ddl-auto: validate` verlangt
Übereinstimmung zwischen Entity und Migration.

## Testebenen

| Test | Ebene | Werkzeug | Prüft |
|------|-------|----------|-------|
| `HouseholdUseCaseImplTest` | Use Case | JUnit 5 + Mockito, **ohne** Spring | Orchestrierung, Invariante, Zeitstempel (`Clock` fest) |
| `HouseholdControllerTest` | Adapter (ein-) | `@WebMvcTest` + `@MockitoBean` | HTTP-Übersetzung, `@Valid`, Fehlerabbildung |
| `HouseholdRepositoryAdapterTest` | Adapter (aus-) | `@DataJpaTest` + Testcontainers (PostgreSQL 17) | Mapping, Migration, `createdAt` beim Update |
| `households-page.spec.ts` | Frontend (Feature) | Vitest + `HttpTestingController` | Liste, Leerfall-Hinweis |
| `app.spec.ts` | Frontend (App-Shell) | Vitest | App-Shell rendert |

Die Ebenen-Tabelle gilt für **alle** Features — die konkreten Testnamen
stehen im jeweiligen Issue unter `## Tests`.

> **Offen:** Für `domain` und für die Frontend-`data`-Schicht gibt es noch
> keinen eigenen Test — die Logik wird dort über die Nachbarschichten
> abgedeckt.

> **Offen:** Bausteine für die Contexts Ausgabe, Termin.
