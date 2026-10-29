# Technik-Checkliste HouseholdManager

Kurzfassung der für Runde 3 relevanten Konventionen. Verbindlich ist
`AGENTS.md` — bei Widerspruch gilt `AGENTS.md`.

Stack dieses Projekts: **Spring Boot 4.1.2 / Java 25** im Backend,
**Angular 22** im Frontend. Beim Ändern des Stacks wird diese Datei
mitgezogen.

## Backend

| Thema | Konvention |
|-------|-----------|
| Pfad | `backend/src/main/java/de/householdmanager/` |
| Package | `de.householdmanager.<schicht>` — `domain.model`, `application.port.in|out`, `application.usecase`, `adapters.inbound.rest`, `adapters.outbound.persistence`, `config` |
| Architektur | **Ports & Adapters**: `adapters → application → domain` (siehe Skill `backend-ports-adapters`, ADR 0001) |
| Fachmodell | `domain/model/` — **keine** Spring-/JPA-Annotationen |
| Persistenzmodell | `adapters/outbound/persistence/<X>JpaEntity.java` — Mapping im `*RepositoryAdapter` |
| Port (ausgehend) | `application/port/out/<X>Repository.java` |
| Port (eingehend) | `application/port/in/<X>UseCase.java` |
| Use Case | `application/usecase/<X>UseCaseImpl.java`, `@Transactional` dort |
| Controller | `@RestController @RequestMapping("api/<resource>")` — **ohne führenden Slash** |
| DTO | `adapters/inbound/rest/dto/` als **Record**; `@JsonProperty` nur bei abweichendem JSON-Key |
| JSON | Jackson **3** (`tools.jackson.*`), Annotation-Package `com.fasterxml.jackson.annotation` |
| Fehler | `adapters/inbound/rest/GlobalExceptionHandler` mit `ProblemDetail`; Use Cases werfen, Controller geben kein `ResponseEntity` zurück |
| Transaktion | `@Transactional` an der Use Case, `@Transactional(readOnly = true)` fürs Lesen |
| `open-in-view` | `false` — die Entity darf den Adapter nicht verlassen |
| Schema | `ddl-auto: validate` — Persistenz-Entity und Migration müssen übereinstimmen |

### Fehler-Mapping

| Exception | HTTP |
|-----------|------|
| `IllegalArgumentException` | 400 |
| `RuntimeException` mit "not found" | 404 |
| Zugriff verweigert | 403 |
| Konflikt | 409 |

### Boot-4-Sonderheiten

| Thema | Konvention |
|-------|-----------|
| Flyway | `spring-boot-starter-flyway` **plus** `flyway-database-postgresql` |
| Dev Services | `spring-boot-docker-compose` + `compose.yml`; Profil `local` mit `spring.docker.compose.file: ../compose.yml`. **Standardmäßig aus** (`spring.docker.compose.enabled: false`), sonst bricht der Start ohne Compose-Datei |
| Testcontainers | **2.x**: `org.testcontainers:testcontainers-postgresql`, `…:testcontainers-junit-jupiter` |
| Test-Slices | `@WebMvcTest` aus `org.springframework.boot.webmvc.test.autoconfigure` (Modul `spring-boot-starter-webmvc-test`) |
| Mocks | `@MockitoBean` (nicht `@MockBean` — in Boot 4 entfernt) |
| Lombok | braucht ab JDK 23 `<proc>full</proc>` + `annotationProcessorPaths` im `maven-compiler-plugin` |
| H2-Console | nicht mehr enthalten — nicht darauf bauen |
| Observability | `spring-boot-starter-opentelemetry` (Boot 4), Actuator für Health/Info |

## Migrationen

- Pfad: `backend/src/main/resources/db/migration/V<n>__<beschreibung>.sql`
- Bestehende V-Dateien werden **nie** editiert.
- Nächste freie Nummer:

  ```bash
  ls backend/src/main/resources/db/migration/ | sort -V | tail -3
  ```

- Postgres: `BIGSERIAL` für IDs, `TIMESTAMPTZ` für Zeitstempel.

## Frontend

| Thema | Konvention |
|-------|-----------|
| Pfad | `frontend/src/app/{core,shared,features/<feature>}` |
| Architektur | **Feature-basiert (Steyer)** — Feature = Ordner mit `routes.ts`; Grenzen per Sheriff (`frontend/sheriff.config.ts`) |
| Feature-Schicht | `features/<f>/<f>-page.ts` — smart Component, spricht mit dem Data-Service |
| Modell | `features/<f>/model/<x>.model.ts` (behält den Suffix) |
| Daten-Schicht | `features/<f>/data/` — API-Client (`HttpClient`) + Data-Service (`httpResource`) |
| UI-Schicht | `features/<f>/ui/` — dumb Components, nur Inputs/Outputs |
| Geteilt | `shared/components/`, `shared/util/` — fachneutral, von mehreren Features benutzt |
| App-weit | `core/config/` — genau eine Instanz (Konfiguration, HTTP-Basis) |
| Komponente | standalone, zoneless (kein `zone.js`), `ChangeDetectionStrategy.OnPush` |
| Dateiname | **ohne** Rollen-Suffix: `households-page.ts` → `class HouseholdsPage` |
| Model | `*.model.ts` (behält den Suffix), Interface `Household` |
| DI | `inject()` statt Konstruktor-Injektion |
| State | `signal()` + `computed()`; `resource`/`httpResource` für Serverdaten |
| Template | `@if` / `@for` / `@empty` |
| Forms | **Signal Forms** (stable ab Angular 22) für neue Formulare |
| Routing | `app.routes.ts` → lazy `loadChildren` auf die Feature-`routes.ts` |
| Styling | SCSS, Component-Styles mit Variablen aus `src/styles.scss` |
| Tests | Vitest + TestBed + `provideHttpClientTesting()` |

### Angular-22-Sonderheiten

| Thema | Konvention |
|-------|-----------|
| Zoneless | Default; kein `zone.js` in `package.json` |
| Testrunner | Vitest — `ng test` ist der vitest-Runner, nicht Karma |
| Node | ≥ 22.22.3 bzw. 24.15+ / 26+ |
| Effekte | laufen erst nach `detectChanges()` — im Test den HTTP-Request **danach** erwarten |
| `httpResource` | eager; Request-Funktion liefert die URL, `value()` erst nach `hasValue()` lesen |

## Tests

| Ebene | Konvention |
|--------|-----------|
| Backend | JUnit 5 + Mockito, Testklasse je Produktionsklasse, gleiches Package |
| Controller | `@WebMvcTest` + `@MockitoBean` |
| Verboten | `@MockBean` (in Boot 4 entfernt) |
| Frontend | Vitest + TestBed + `provideHttpClientTesting()` |
| E2E | Playwright, `e2e/<name>.spec.ts` (noch nicht eingerichtet) |

## Kommandos

```bash
# Backend kompilieren + Tests
cd backend && ./mvnw -B test

# Backend ohne lokales JDK
docker run --rm -v "$PWD/backend":/app -w /app \
  maven:3.9-eclipse-temurin-25 mvn -B test

# Frontend Type-Check
cd frontend
npx tsc --noEmit -p tsconfig.app.json
npx tsc --noEmit -p tsconfig.spec.json

# Frontend Tests
npm test

# Frontend Production-Build
npm run build
```

## Risikofelder

- **Boot 4 + Jackson 3**: `com.fasterxml.jackson.databind` existiert nicht
  mehr — `tools.jackson.databind`. Annotationen bleiben unter
  `com.fasterxml.jackson.annotation`.
- **Testcontainers 2.x**: Modulnamen geändert, alte Koordinaten 404.
- **Flyway 12**: braucht explizit `flyway-database-postgresql`, sonst findet
  Flyway keine Datenbank-Unterstützung.
- **Angular 22 zoneless**: `effect()`/`resource()` laufen nicht synchron beim
  Erzeugen — Tests brauchen `fixture.detectChanges()` vor `expectOne(...)`.
- **Hibernate + `ddl-auto: validate`**: Entity-Änderung ohne Migration
  lässt die App nicht starten.
- **Migrationen im Docker-Build**: `mvn compile` ohne `clean` kann gelöschte
  Migrationen in `target/` zurücklassen.
