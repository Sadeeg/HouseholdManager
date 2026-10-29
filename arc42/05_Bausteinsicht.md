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

## Testebenen

| Test | Ebene | Werkzeug | Prüft |
|------|-------|----------|-------|
| `HouseholdUseCaseImplTest` | Use Case | JUnit 5 + Mockito, **ohne** Spring | Orchestrierung, Invariante, Zeitstempel (`Clock` fest) |
| `HouseholdControllerTest` | Adapter (ein-) | `@WebMvcTest` + `@MockitoBean` | HTTP-Übersetzung, `@Valid`, Fehlerabbildung |
| `HouseholdRepositoryAdapterTest` | Adapter (aus-) | `@DataJpaTest` + Testcontainers (PostgreSQL 17) | Mapping, Migration, `createdAt` beim Update |
| `households-page.spec.ts` | Frontend (Feature) | Vitest + `HttpTestingController` | Liste, Leerfall-Hinweis |
| `app.spec.ts` | Frontend (App-Shell) | Vitest | App-Shell rendert |

> **Offen:** Für `domain` und für die Frontend-`data`-Schicht gibt es noch
> keinen eigenen Test — die Logik wird dort über die Nachbarschichten
> abgedeckt.

> **Offen:** Bausteine für die Contexts Aufgabe, Ausgabe, Termin.
