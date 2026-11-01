# Prüfgrundlage — die Regeln, gegen die der Reviewer prüft

**Die ausführbaren Regeln stehen in**
`backend/src/test/java/de/householdmanager/architecture/ArchitectureRulesTest.java`
— sie laufen in CI und ersetzen den Großteil der Greps in diesem Dokument.
Der Reviewer prüft **nicht** selbst, was der Test bereits erzwingt; er
nutzt das Testergebnis und prüft nur, was ein Test nicht ausdrücken kann.

Kompakte Fassung der Projekt-Regeln. **Quelle bleibt `AGENTS.md`** plus die
Skills `backend-ports-adapters` / `frontend-feature-architecture` — dieses
Dokument ist die Kurzform für den Reviewer, nicht die Wahrheit. Bei Widerspruch
gilt `AGENTS.md`.

Geprüft wird ausschließlich, was der Diff betrifft. Nicht betroffene Dateien
sind **kein** Befund.

| Regel | ausführbar als |
|-------|----------------|
| `domain` ohne Frameworks | `ArchitectureRulesTest` |
| `application` ohne Adapter/Technik | `ArchitectureRulesTest` |
| `domain` kennt `application` nicht | `ArchitectureRulesTest` |
| `@Transactional` nur in `application/usecase` | `ArchitectureRulesTest` |
| `@Entity` nur in `adapters/outbound/persistence` | `ArchitectureRulesTest` |
| `@RestController` nur in `adapters/inbound/rest` | `ArchitectureRulesTest` |
| DTOs sind Records | `ArchitectureRulesTest` |
| Ports sind Interfaces | `ArchitectureRulesTest` |
| keine Feld-Injektion (`@Autowired`) | `ArchitectureRulesTest` |
| Feature-Grenzen im Frontend | `npm run sheriff` |

**Driftprüfung:** Wurde eine Regel hier oder in `AGENTS.md` geändert und
nicht der zugehörige Test, ist das ein **Befund** (MAJOR) — die ausführbare
und die beschriebene Form müssen zusammenpassen.

## Backend — Abhängigkeitsrichtung `adapters → application → domain`

- `domain` importiert **nichts** aus Spring, JPA, Jackson, Hibernate.
- `application` importiert nur `domain` und die eigenen Port-Interfaces.
  Erlaubt: Spring-**Annotationen** (`@Service`, `@Transactional`) in
  `application/usecase`. Verboten: JPA-, HTTP- und Jackson-Typen.
- `adapters` implementieren die Ports aus `application/port/**` und
  übersetzen an der Grenze (HTTP ↔ Fachmodell, Persistenz ↔ Fachmodell).
- Transaktionen liegen an der **Use Case**, nicht im Adapter; Lesen mit
  `@Transactional(readOnly = true)`. `open-in-view: false` — die Entity
  verlässt den Adapter nicht.
- DTOs sind **Records** in `adapters/inbound/rest/dto/`; kein verschachteltes
  Record im Controller.
- Fehler: Use Case wirft, `GlobalExceptionHandler` übersetzt nach
  `ProblemDetail`; der Controller gibt kein `ResponseEntity` zurück.
- Flyway: **neue** `V`-Migration, bestehende `V`-Dateien bleiben unverändert;
  Entity und Migration müssen zusammenpassen (`ddl-auto: validate`).
- Validierung an der Grenze (`@Valid`) **und** als Invariante im Fachmodell.
- Die Anwendung setzt die Invariante (Zeitfenster, Grenzen, Reihenfolge);
  der Adapter liefert nur die Daten der Quelle.
- Zustände getrennt: „veraltet" = letzter Versuch **und** letzter
  erfolgreicher Lauf; „noch kein Lauf" ist ein eigener Zustand.

```bash
grep -rn "^import \(org\.springframework\|jakarta\.\|com\.fasterxml\|tools\.jackson\)" \
  backend/src/main/java/de/householdmanager/domain \
  backend/src/main/java/de/householdmanager/application
grep -rn "@Transactional" backend/src/main/java/de/householdmanager
grep -rn "@Autowired" backend/src/main/java/de/householdmanager/adapters
```

## Frontend — Feature-Architektur

- Feature = Ordner mit `routes.ts`; Schichten `feature → ui/data/model`.
  Ein Feature darf **nur** sein eigenes Modul plus `shared/` nutzen.
- Feature-Services sind **route-scoped**, nicht `root`; dazu
  `provideRouter(routes, withAutoCleanupInjectors())`.
- Dumb Components (`ui/`) greifen **nicht** auf `data/` zu.
- Reaktivität: `signal()`/`computed()`, kein Subject; Lesen per
  `httpResource` im Data-Service, Schreiben per `HttpClient`.
- Signal Forms: `maxLength()` schneidet beim Paste still ab — es braucht eine
  eigene `validate()`-Regel mit `maxLengthError()` auf der getrimmten Länge.
- Design-Tokens: **keine Hex-Literale**, nur `var(--hm-…)`; Komponenten aus
  `shared/components/` wiederverwenden.
- Dateinamen ohne Rollen-Suffix (`households-page.ts` → `class HouseholdsPage`),
  Models mit `.model.ts`.
- Kein Feature greift auf ein anderes Feature zu (`sheriff` prüft das).

```bash
grep -rn "providedIn: 'root'\|providedIn: \"root\"" frontend/src/app --include=*.ts
grep -rn "#[0-9a-fA-F]\{3,8\}" frontend/src/app --include=*.scss
grep -rn "import .*from '.*features/[a-z-]*/" frontend/src/app/features --include=*.ts
```

## Tests

- **Jedes `AC<n>` braucht einen belegenden Test**; Fehler- und Grenzfälle
  gehören getestet.
- Richtige Ebene: `domain` ohne Spring, Use Case gegen gemockten Port ohne
  Spring, `adapters/inbound` mit `@WebMvcTest`, `adapters/outbound` mit
  `@DataJpaTest` + Testcontainers-Postgres.
- Boot 4: `@MockitoBean` statt `@MockBean`; Test-Slices aus
  `org.springframework.boot.webmvc.test.autoconfigure` bzw.
  `org.springframework.boot.data.jpa.test.autoconfigure`.
- Testcontainers 2.x: `PostgreSQLContainer` ist **nicht** mehr generisch.
- Keine Tautologie (der Test prüft die Implementierung, nicht sich selbst).

## Konventionen, Daten, Sicherheit

- Keine Secrets im Code, keine unvalidierten Eingaben, keine Injection
  (SQL/Pfad/Template), keine personenbezogenen Daten im Log.
- Keine toten Codes, keine `TODO`/`FIXME`, keine Debug-Prints.
- Architekturänderung → ADR nötig; Issue-Hausstil wie in
  `issue-formulierung`.
- Sprache: Identifier englisch, Issue-Texte und Kommentare deutsch.

```bash
grep -rn "TODO\|FIXME\|console\.log\|System\.out" backend/src frontend/src
```
