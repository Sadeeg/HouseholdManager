# Technik-Checkliste HouseholdManager

Details für Runde 3. Verbindlich ist `AGENTS.md` — bei Widerspruch gilt es.
Stack: Spring Boot 4.1.2 / Java 25, Angular 22. Boot-4-Fallen, Angular-22- und
Verify-Details stehen in `AGENTS.md` (`§Boot-4-Fallen`, `§Frontend`, `§Verify`,
`§Gotchas`) und werden hier nicht wiederholt.

## Backend

| Thema | Konvention |
|-------|-----------|
| Pfad | `backend/src/main/java/de/householdmanager/` |
| Package | `de.householdmanager.<schicht>` — `domain.model`, `application.port.in|out`, `application.usecase`, `adapters.inbound.rest`, `adapters.outbound.persistence`, `config` |
| Architektur | **Ports & Adapters**: `adapters → application → domain` (Skill `backend-ports-adapters`, ADR 0001) |
| Fachmodell | `domain/model/` — **keine** Spring-/JPA-Annotationen |
| Persistenzmodell | `adapters/outbound/persistence/<X>JpaEntity.java`, Mapping im `*RepositoryAdapter` |
| Ports | ausgehend `application/port/out/<X>Repository.java`, eingehend `application/port/in/<X>UseCase.java` |
| Use Case | `application/usecase/<X>UseCaseImpl.java`, `@Transactional` dort, `readOnly = true` beim Lesen |
| Controller | `@RestController @RequestMapping("api/<resource>")` — **ohne** führenden Slash |
| DTO | `adapters/inbound/rest/dto/` als **Record**; `@JsonProperty` nur bei abweichendem JSON-Key |
| JSON | Jackson 3; Annotation-Package `com.fasterxml.jackson.annotation` |
| Fehler | `GlobalExceptionHandler` mit `ProblemDetail`; Use Case wirft, Controller gibt kein `ResponseEntity` zurück |
| `open-in-view` | `false` — die Entity verlässt den Adapter nicht |
| Schema | `ddl-auto: validate` — Entity und Migration müssen übereinstimmen |

### Fehler-Mapping

| Exception | HTTP |
|-----------|------|
| `IllegalArgumentException` | 400 |
| `RuntimeException` mit "not found" | 404 |
| Zugriff verweigert | 403 |
| Konflikt | 409 |

## Datenmodell und Migration

- Pfad `backend/src/main/resources/db/migration/V<n>__<snake_case>.sql`.
- Bestehende V-Dateien werden **nie** editiert; nächste freie Nummer im Ordner
  ermitteln.
- Im Issue festhalten: Tabelle, Spalten mit Typ, Constraints, Indizes, ob
  nullable und warum, Fremdschlüssel (z. B. `household_id`), ob
  `ON DELETE CASCADE` nötig ist.
- Bezugsgebundene Entitäten tragen `@ManyToOne House household` mit
  `optional = false`, sofern der Stack das vorsieht.
- Postgres: `BIGSERIAL` für IDs, `TIMESTAMPTZ` für Zeitstempel.
- Kein Schema betroffen → im Issue ausdrücklich „Keine Schema-Änderung —
  <Begründung>".

## API-Vertrag

| Methode | Pfad | Request | Response | Fehler |
|---------|------|---------|----------|--------|
| `GET` | `api/<ressource>` | – | `<Ressource>Response[]` | 401, 403 |

- Pfadpräfix folgt der bestehenden Konvention; wo keine existiert, wird sie mit
  dem Issue festgelegt. Pfadvariablen: `@PathVariable("name")`.
- Fehler über die zentrale Fehlerbehandlung; Statuscodes siehe Mapping.
- Bezug zum Kontext: Dienste holen den aktuellen Kontext (z. B. aktiven
  Haushalt) aus dem Request; vor jedem `save()` die Zuordnung setzen
  (`entity.setHousehold(currentHousehold())`).
- **Der Vertrag passt zum Zielort.** Kein Vertrag, der gegen die Realität des
  Repos läuft — Widersprüche werden hier aufgelöst, nicht beim Bauen. Beispiel:
  „der Renderer fragt die Datenbank nicht ab" ist mit einem Renderer unvereinbar,
  der Bildbytes aus der DB lädt; Formulierung präzisieren („fragt keine
  Fachlogik ab") oder Bytes übergeben.

## Frontend

| Thema | Konvention |
|-------|-----------|
| Pfad | `frontend/src/app/{core,shared,features/<feature>}` |
| Architektur | **Feature-basiert** — Feature = Ordner mit `routes.ts`; Grenzen per Sheriff |
| Feature-Schicht | `features/<f>/<f>-page.ts` (smart, spricht mit dem Data-Service) |
| Modell | `features/<f>/model/<x>.model.ts` (Suffix bleibt) |
| Daten | `features/<f>/data/<thing>-api.ts` (HttpClient), `<f>-data.ts` (`httpResource`) |
| UI-Schicht | `features/<f>/ui/<thing>.ts` — dumb Components, nur Inputs/Outputs |
| Geteilt | `shared/components/`, `shared/util/` — fachneutral, mehrfach genutzt |
| App-weit | `core/config/` — genau eine Instanz |
| Komponente | standalone, zoneless, `ChangeDetectionStrategy.OnPush` |
| Dateiname | **ohne** Rollen-Suffix: `households-page.ts` → `class HouseholdsPage` |
| DI | `inject()` statt Konstruktor-Injektion |
| State | `signal()` + `computed()`; `resource`/`httpResource` für Serverdaten |
| Template | `@if` / `@for` / `@empty` |
| Forms | **Signal Forms** für neue Formulare; Pflicht/Länge als Validator, nicht als Template-Attribut |
| Routing | `app.routes.ts` → lazy `loadChildren` auf die Feature-`routes.ts` |
| Styling | SCSS, Tokens (`var(--hm-…)`), keine Hex-Literale |

- Feature-Route in `features/<feature>/routes.ts`, in `app.routes.ts` lazy per
  `loadChildren`; eine Komponente ohne Feature-Route (z. B. Login) per
  `loadComponent`; Pfad kebab-case. Feature-Services route-scoped.
- Das Issue nennt Komponentenname, Route und Signale, nicht das vollständige
  Template.

## Dateiliste — Pfadmuster

| Artefakt | Pfadmuster |
|----------|-----------|
| Fachmodell | `domain/model/<Entity>.java` |
| Outbound-Port | `application/port/out/<Entity>Repository.java` |
| Inbound-Port | `application/port/in/<Entity>UseCase.java` |
| Use Case | `application/usecase/<Entity>UseCaseImpl.java` |
| Backend-DTO | `adapters/inbound/rest/dto/<Entity>Response.java` |
| Controller | `adapters/inbound/rest/<Entity>Controller.java` |
| Persistenz-Entity | `adapters/outbound/persistence/<Entity>JpaEntity.java` |
| JPA-Repository | `adapters/outbound/persistence/<Entity>JpaRepository.java` |
| Port-Implementierung | `adapters/outbound/persistence/<Entity>RepositoryAdapter.java` |
| Migration | `db/migration/V<n>__<snake_case>.sql` |
| Frontend-Feature | `features/<feature>/<feature>-page.ts` |
| Frontend-Modell | `features/<feature>/model/<thing>.model.ts` |
| Frontend-Daten | `features/<feature>/data/<thing>-api.ts`, `<feature>-data.ts` |
| Frontend-UI | `features/<feature>/ui/<thing>.ts` |
| Frontend-Route | `features/<feature>/routes.ts` (in `app.routes.ts` eingehängt) |
| Dev-Proxy | `frontend/proxy.conf.js` (Ziel über `BACKEND_URL`) |
| Compose | `compose.yml` (Ports, Healthchecks, `depends_on`) |
| Architektur-Doku | `arc42/<NN>_<Titel>.md`, `docs/adr/<n>-<slug>.md` |
| Design-DNA | `docs/design/design-dna.md`, `frontend/src/styles/_design-tokens.scss` |

- **Verifizieren:** Jeder bestehende Pfad muss im Repo existieren
  (`grep`/`find`, nicht raten). Neue Pfade können nicht existieren — sie gelten
  nach Konvention und werden als **Neu** markiert mit dem Zusatz: „Pfad nach
  Namenskonvention, beim Bauen anpassen, falls die Konvention nicht greift."
  Ein Pfad, der weder existiert noch der Konvention folgt, ist `TECH` → 3.
- Format im Issue: Pfad + Klammer + was dort passiert.
- **Sofort in die Dateiliste, wenn berührt:** `compose.yml` (Ports,
  Healthchecks, Startreihenfolge — ohne Healthcheck/`depends_on:
  service_healthy` startet nginx nicht), `frontend/proxy.conf.js`
  (`BACKEND_URL`), `arc42/` und `docs/adr/` **im selben Commit wie der Code**
  (nachträgliche Doku ist ein MAJOR-Befund), `docs/design/design-dna.md` (neues
  UI-Muster).

## Tests

| AC | Ebene | Ort | Fälle |
|----|--------|-----|-------|
| AC1 | Use Case | `backend/src/test/java/de/householdmanager/application/usecase/XUseCaseImplTest.java` | … |
| AC2 | Controller | `…/adapters/inbound/rest/XControllerTest.java` | … |
| AC3 | Persistenz | `…/adapters/outbound/persistence/XRepositoryAdapterTest.java` | … |
| AC4 | Frontend | `frontend/src/app/features/<f>/<f>-page.spec.ts`, `…/ui/<thing>.spec.ts` | … |
| AC5 | manuell | — (Sichtprüfung, Export, Gerät) | … |

- Backend: JUnit 5 + Mockito, Testklasse je Produktionsklasse, gleiches
  Package. Controller-Test mit Mock auf dem **Port**; Persistenz-Test mit
  Testcontainers (Modulname und Slice-Pakete: `AGENTS.md §Boot-4-Fallen`).
- Frontend: Vitest + TestBed, `vi.fn()`, `provideHttpClientTesting()`,
  `provideRouter([])`; zoneless-Reihenfolge: `AGENTS.md §Gotchas`.
- **Jedes AC hat hier eine Zeile** — und keine Zeile ohne AC. Rein manuelle
  Prüfung (Export neben Musterdokument) wird als `manuell` markiert und im
  Verify-Block als konkreter Schritt benannt. Diese Zuordnung deckt beim Bauen
  übersprungene AC auf.

## Verify

Nur die für das Issue relevanten Kommandos — `AGENTS.md §Verify` und
`issue-formulierung` (Hausstil). Plus ein manueller Test-Satz. Mit UI-Anteil
nennt der Block drei Dinge: **Stack starten** (Befehl, Ports, `.env`,
`BACKEND_URL`), **Datenbank-Zustand** (frischer Aufbau nötig, z. B. `NOT NULL`
ohne Backfill?) und **manueller Pfad** als konkrete Handlung im Browser, nicht
„Funktion prüfen". Bei belegtem Port: `BACKEND_PORT` in `.env` und Dev-Proxy
per `BACKEND_URL` darauf richten.

## Risikofelder

- **Hibernate + `ddl-auto: validate`**: Entity-Änderung ohne Migration lässt die
  App nicht starten.
- **Migrationen im Docker-Build**: `mvn compile` ohne `clean` kann gelöschte
  Migrationen in `target/` zurücklassen.
- Boot 4 + Jackson 3, Testcontainers 2.x, Flyway 12, Angular 22 zoneless:
  `AGENTS.md §Boot-4-Fallen`, `§Gotchas`.

## Erprobte Fehlerquellen (Issue #1)

| Fehlerquelle | Festlegung in Runde 3 |
|--------------|-----------------------|
| **Stille Verwerfung** — Wert außerhalb der Grenze fällt ohne Meldung weg | Fehlerfall als AC: „Ein Label mit 61 Zeichen wird abgelehnt und benannt." |
| **Migration ohne Backfill** — `NOT NULL` auf bestehender Tabelle | „Entwicklungsdatenbank wird neu aufgebaut" als Voraussetzung + Verify |
| **Overlay-Muster** — eigener Dialog statt vorhandenem Muster | Verweis auf `shared/components/confirm-dialog` in der Dateiliste |
| **Nur-Endpunkt-Feature** — Endpunkt existiert, UI nicht | Einstieg und Datenvoraussetzung je AC |
| **Nicht startender Stack** — nginx findet Backend nicht | Healthcheck + `depends_on: service_healthy` + Port-Overrides |
