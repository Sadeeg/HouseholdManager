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

> **Offen:** Bausteine für die Contexts Ausgabe, Termin.

## Zielbild: Context Aufgabe und Haushaltsmitgliedschaft (Issue #1, entworfen — nicht implementiert)

### Backend

| Baustein (Ziel) | Schicht | Anmerkung |
|-----------|-------|-----------|
| `Task`, `TaskStatus` (TODO/DOING/DONE), `HouseholdMembership` | `domain/model` | framework-frei; Frist ist ein Datum ohne Uhrzeit |
| `TaskUseCase` (anlegen, bearbeiten, löschen, Status und Position wechseln (A15), Erledigte löschen), `MembershipUseCase` (Mitglieder auflisten, ernennen, entfernen) | `application/port/in` | Use Cases prüfen **zuerst** die Mitgliedschaft der angemeldeten Person (`CurrentUserPort` + `HouseholdMemberRepository`); Nicht-Mitglieder → Fachfehler → 403 |
| `TaskRepository`, `HouseholdMemberRepository` | `application/port/out` | Label-Identität: normalisierter Name (lowercase, getrimmt) eindeutig je Haushalt; gespeichert wird die zuerst gespeicherte Schreibweise |
| `TaskController` (`api/households/{householdId}/tasks`), `MemberController` (`api/households/{householdId}/members`) | `adapters/inbound/rest` | DTO-Records in `dto/`; Fehler über `GlobalExceptionHandler` |
| `TaskJpaEntity`, `HouseholdMemberJpaEntity`, `LabelJpaEntity`, `TaskLabelJpaEntity` + Adapter | `adapters/outbound/persistence` | Mapping im Adapter; Zuweisung speichert die Nutzer-ID aus `UserDirectory` |
| Migrationen `V2__…` (`household.created_by` NOT NULL + `household_member`) und `V3__…` (`task` mit `position`-Feld für die Reihenfolge je Spalte (A15), `label`, `task_label`) | `db/migration` | V1 wird nicht editiert; `created_by` NOT NULL ohne Backfill — frische Dev-DB (A14) |
| `WorkspaceUseCase` (gemerkten Haushalt lesen, Haushalt merken), `WorkspaceSelectionRepository` | `application/port/in`, `application/port/out` | Merkung der Workspace-Auswahl je Person (ADR 0004); `selectWorkspace` prüft **zuerst** die Mitgliedschaft über `MembershipUseCase.requireMembership` — In-Port des Haushalt-Kontexts, nicht dessen Tabellen |
| `WorkspaceController` (`api/me/workspace`): `GET` die gemerkte Haushaltskennung (404, wenn nie einer gewählt wurde), `PUT` setzt sie (403 ohne Mitgliedschaft) | `adapters/inbound/rest` | DTO-Record `dto/WorkspaceRequest`; Person über `CurrentUserPort`; dient dem Workspace-Wähler in `core/workspace/` |
| `WorkspaceSelectionJpaEntity` (`workspace_selection`), `WorkspaceSelectionJpaRepository` + Adapter | `adapters/outbound/persistence` | `user_id` Primärschlüssel, `household_id` Fremdschlüssel mit `ON DELETE CASCADE`; Mapping im Adapter |
| Migration `V4__workspace_selection.sql` (Tabelle `workspace_selection`: `user_id`, `household_id`, `updated_at`) | `db/migration` | V1–V3 bleiben unverändert |
| `UserSearchUseCase` (`searchUsers(query)` → `UserRef` mit `id` + Anzeigename) | `application/port/in` | liest über den Out-Port `UserDirectory` (Nextcloud OCS); keine eigene Tabelle, keine Migration, keine E-Mail im Ergebnis |
| `UserSearchController` (`api/users/search?q=`) | `adapters/inbound/rest` | DTO-Record `UserRefResponse` (`id`, `displayName`); liefert die Vorschläge für `shared/components/user-picker` — Zuständige auswählen (AC8), Mitglieder ernennen (AC11) |

Der Aufgaben-Kontext nutzt die Mitgliedschaft über den In-Port des
Haushalt-Kontexts (`MembershipUseCase`), nicht über dessen Tabellen.

> **Stand:** Die Workspace-Auswahl (`api/me/workspace`, Migration V4) und die
> Personensuche (`api/users/search`) sind als Backend-Bausteine umgesetzt; der
> REST-Pfad `api/users/search` ist eine bewusste Erweiterung gegenüber dem
> API-Vertrag von Issue #1 (Begründung in 3).

### Frontend

| Baustein (Ziel) | Schicht | Anmerkung |
|-----------|-------|-----------|
| `core/workspace/` — `WorkspaceService` + Workspace-Wähler im App-Rahmen | `core` | app-weit, eine Instanz; hält den gewählten Haushalt, lädt die Haushalte der Person, wählt beim nächsten Besuch wieder |
| `features/tasks/` — `routes.ts` (auth_guard + Workspace vorausgesetzt), `tasks-page`, `model/`, `data/` (`httpResource` auf die WorkspaceId skoped), `ui/` (board-column, task-card, label-chip, task-dialog, confirm-dialog) | Feature | Reihenfolge je Spalte folgt `position` (A15); „überfällig“ wird im Frontend aus `dueDate` + Status berechnet (lokaler Tag, keine Uhrzeit); Label-Farbe = Hash des normalisierten Namens → `--hm-label-1…8` |
| Mitglieder-Abschnitt in `features/households/` (inline im Listeneintrag) | Feature | keine neue Route |
| `shared/components/page-header` — Erweiterung um einen Aktionen-Slot | `shared` | von Tafel und Haushaltsverwaltung genutzt |

**Geplant (Tests):** `TaskUseCaseImplTest` (ohne Spring, `Clock` fest für
Grenzdaten), `TaskControllerTest` (`@WebMvcTest`),
`TaskRepositoryAdapterTest` (`@DataJpaTest` + Testcontainers),
`MembershipUseCaseImplTest`, `tasks-page.spec.ts` (Vitest, Leerzustände,
Drag-Fehlerpfad AC19).

> **Offen:** Markdown-Vorschau als `shared/components`-Komponente
> (fachneutral) oder in `features/tasks/ui` — Entscheidung in Runde 3.
> **Offen:** Anzeigename der Zugewiesenen: Live-Auflösung über
> `UserDirectory` (Verzeichnisabhängigkeit je Request) oder Namens-Snapshot
> bei Zuweisung — Entscheidung in Runde 3.
