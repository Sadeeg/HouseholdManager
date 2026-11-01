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

### Muster über alle Contexts

Diese Bausteinarten wiederholen sich je Fach-Kontext. Sie stehen hier
**einmal** — nicht je Feature:

| Muster | Ausprägung | Regel |
|--------|-----------|-------|
| **Use Case je Fach-Vorgang** | `HouseholdUseCase`, `TaskUseCase`, `MembershipUseCase`, `WasteCollectionUseCase` | Orchestrierung + Invariante; Transaktionsgrenze |
| **Fach-Exception je Regelverletzung** | `InvalidHouseholdNameException`, `WasteFeedUnavailableException` | framework-frei; der Adapter übersetzt sie in einen Statuscode |
| **Repository-Port + Adapter je Aggregat** | `HouseholdRepository` → `HouseholdRepositoryAdapter` | Mapping ausschließlich im Adapter |
| **Controller je Kontext** | `HouseholdController`, `TaskController`, `MemberController`, `WasteCollectionController` | `api/<resource>`; DTO-Records in `dto/` |
| **In-Port für Context-Zugriff** | `MembershipUseCase.requireMembership` | ein Context greift **nie** auf die Tabellen eines anderen zu (ADR 0003) |
| **Scheduler-Adapter** | `WasteSyncJob` | löst genau einen Use Case aus, enthält keine Fachlogik (ADR 0005) |
| **Workspace-Auswahl** | `WorkspaceUseCase`, `WorkspaceSelectionRepository` | app-weit in `core/workspace`; Auswahl wird je Person gemerkt (ADR 0004) |
| **Feature-Ordner je Kontext** | `features/households/`, `features/tasks/`, `features/waste/` | `routes.ts`; route-scoped Data-Services |
| **core-Bausteine** | `core/workspace/`, `core/users/` | app-weit, eine Instanz (`providedIn: 'root'`) |
| **shared-Komponenten** | `shared/components/user-picker`, `page-header` | fachneutral, ohne Fachlogik |

### Datenzuordnung

| Tabelle | Kontext | Besonderheit |
|---------|---------|--------------|
| `household` | Haushalt | trägt die Adresse einer externen Quelle als untypisiertes Attribut — der Kontext kennt keine Fremdregeln |
| `household_member` | Haushalt | Mitgliedschaft; geprüft über den In-Port des Haushalt-Kontexts |
| `workspace_selection` | Haushalt | Workspace-Merkung je Person (ADR 0004) |
| `task`, `label`, `task_label` | Aufgabe | Reihenfolge je Spalte über ein `position`-Feld |
| `waste_collection`, `waste_sync_state` | Müllkalender | der Stand gehört dem **Lauf**, nicht dem Termin |

**Fachliche Konstanten im `domain`:** `Household.MAX_NAME_LENGTH`,
`WasteCalendar.SYNC_WINDOW_DAYS` (42) und `WasteCalendar.MAX_URL_LENGTH` (500).
Das Fenster gibt der Use Case dem Feed-Port vor — eine Quelle der Wahrheit.
Die Konstante steht im Fachmodell, damit Use Case und Adapter dieselbe
Grenze kennen.

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
