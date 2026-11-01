# AGENTS.md — HouseholdManager

Verbindliche Arbeitsanweisungen für Agenten in diesem Repo. Diese Datei wird
bei **jedem** Turn geladen und enthält deshalb **nur Entscheidungen** — keine
Erklärungen, keine Beispiele, keine Wiederholung der Skills. Details stehen
in `.opencode/skills/`, `docs/` und `arc42/`.

## Projekt

Haushaltsverwaltung: Mitglieder, Aufgaben, Ausgaben und Termine in einem
Haushalt. Zweck, Rollen und Fachvokabular werden mit dem ersten Epic
festgelegt und hier ergänzt.

| Teil | Stack | Pfad |
|------|-------|------|
| Backend | Spring Boot **4.1.2** (Framework 7), Java **25**, JPA, Flyway 12, PostgreSQL 17 | `backend/` |
| Frontend | Angular **22** (zoneless, Signals, Vitest), TypeScript 6 | `frontend/` |
| Infrastruktur | Docker Compose, nginx, Dockerfiles | `compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` |

Versionen stehen in `backend/pom.xml` und `frontend/package.json` — nicht hier
und nicht im Gedächtnis.

## Arbeitsweise

Der Kontext ist das Betriebsmittel: Was hineinkommt, bleibt bis zum
Sessionende. Lange Kontexte kosten Geld und Präzision — **ein kurzer,
präziser Kontext schlägt einen langen.**

1. **Grep vor Read.** Erst `grep`/`find`, dann nur die Fundstelle lesen
   (`offset`/`limit`). Nie eine ganze Datei, wenn eine Stelle reicht.
2. **Erst Dateinamen, dann Inhalt.** Kein `ls -R` über `node_modules/`,
   `target/`, `dist/`.
3. **Tool-Ausgaben kürzen** (`| head -30`, `| grep -E "ERROR|Tests run"`).
   Lange Läufe in `/tmp/<thema>.log`, dann gezielt greppen.
4. **Keine Lockfiles, Build-Artefakte oder generierten Dateien lesen**
   (`package-lock.json`, `target/`, `dist/`, `node_modules/`, `.angular/`).
5. **Gepatchten Code nicht erneut lesen** — Verifikation über Test oder Grep.
6. **Skill einmal laden.** Große Referenzdateien nur öffnen, wenn genau diese
   Frage zu beantworten ist — dann eine Datei, ein Abschnitt.
7. **Subagent nur für breite Suche**, mit exakten Pfaden und Antwortgrenze
   (≤ 300 Wörter). Nie zwei Agenten auf denselben Dateien. **Nie zwei
   Subagenten in einer Nachricht starten** — der lokale Router hat nur einen
   Slot; Ergebnis abwarten, dann den nächsten.
8. **Große Implementierungen trennen**: Backend- und Frontend-Teil an
   `backend-dev` bzw. `frontend-dev` delegieren, statt beide Stack im
   Hauptkontext zu halten. Das Ergebnis ist eine Zusammenfassung, kein
   Dateidump.
9. **GitHub-Antworten begrenzen** (`fields`, kleines `perPage`, keine
   vollständigen Issue-Bodies).
10. **Ein Schritt pro Zug**, danach max. 3 Sätze Status. Der Status ersetzt
    die Historie, er wiederholt sie nicht.
11. **Handover statt Verlauf.** Reicht der Kontext nicht: Stand in
    `.opencode/state.md` (≤ 40 Zeilen) schreiben, Session neu starten.
    **Ein Feature = eine Session**; nach jeder Runde prüfen, ob ein Schnitt
    möglich ist.

## Struktur

```text
backend/src/main/java/de/householdmanager/  domain · application/{port,usecase} · adapters · config
frontend/src/app/                           core/ · shared/ · features/<feature>/
arc42/            Architekturdokumentation (12 Kapitel, ARC-Agent)
docs/adr/         Architekturentscheidungen (Quelle der Wahrheit)
docs/design/design-dna.md   Gestaltungssprache
docs/release.md   Release-Ablauf
.opencode/state.md          Handover zwischen Sessions
```

Schichtaufbau, Ordnerregeln und Testebenen stehen in den Skills
`backend-ports-adapters` und `frontend-feature-architecture` (Entscheid:
[ADR 0001](docs/adr/0001-backend-ports-and-adapters.md),
[ADR 0002](docs/adr/0002-frontend-feature-architecture.md)).

## Skills

Der Katalog (Name + Beschreibung) wird automatisch geladen — hier steht
**keine** Skill-Liste. Maßgeblich ist die `description` im `SKILL.md`.

- Projekt-Skills liegen in `.opencode/skills/`; Herkunft und Pflege:
  `.opencode/skills/README.md`.
- `.opencode/vendor-skills/` ist **nicht registriert** und wird nie
  automatisch geladen. Nur bei nachweislichem Bedarf zurückholen.
- Agenten: `architektur-reviewer` (Architektur-Gate), `kritischer-tester`
  (Test-Gate), `ux-designer` (UX-Entwurf), `arc-agent` (pflegt `arc42/`),
  `backend-dev` / `frontend-dev` (Implementierung je Stack).

## Definition of Done (Feature)

Ein Feature ist **erst fertig**, wenn **alle sechs** Punkte erfüllt sind:

1. **Implementiert** — alle Akzeptanzkriterien des Issues sind umgesetzt,
   in der Schicht, in der sie hingehören (Ports & Adapters,
   Feature-Architektur).
2. **Vollständig getestet** — **jedes** `AC<n>` hat einen Test, der es
   belegt; Fehlerpfade und Grenzwerte sind getestet. Bei UI-Anteil kommen
   **Sichtprüfung** und **manueller Pfad im Browser** dazu (Skill
   `feature-testing`).
3. **Grün** — `./mvnw -B test` und `npm test` grün, dazu `npm run lint` und
   `npm run sheriff`. **Der Implementierer führt sie aus** — der
   Architektur-Reviewer arbeitet statisch und übernimmt die Ergebnisse; das
   Abnahme-Gate prüft sie unabhängig.
4. **Architektur-Review OK** — `architektur-reviewer` hat geprüft. Bei
   `KORREKTUREN` wird der Korrekturauftrag an den Implementierer delegiert
   und erneut geprüft — bis `OK` oder nach spätestens 3 Runden.
5. **Abnahme PASS** — `kritischer-tester` hat adversarial gegen die
   Akzeptanzkriterien geprüft und `PASS` gemeldet.
6. **Nachbereitung** — Erfahrungen in `docs/erkenntnisse-issue-<n>.md`,
   wiederkehrende Fehler als Regel in `AGENTS.md` oder im zuständigen Skill;
   offene Kleinbefunde als Follow-up-Issues.

```text
/review <Issue-Nr>       # Architektur-Gate: review → Korrekturen delegieren → review
/abnahme <Issue-Nr>      # Test-Gate: adversarial Prüfung der Akzeptanzkriterien
```

Urteile werden als Kommentar am Issue protokolliert
(`<!-- review: status=OK … -->`, `<!-- abnahme: status=PASS … -->`).

**Ein Feature ohne beide Gates ist nicht fertig** — auch wenn alle Tests grün
sind. Ein selbst erteiltes Urteil zählt nicht: Urteile kommen von den
Subagenten, Korrekturen vom Implementierer. Nach `FAIL`/`KORREKTUREN`:
Befunde beheben, dann das betroffene Gate erneut laufen lassen.

## Konventionen

Sprache: Issue-Texte und Kommentare auf **Deutsch**, Identifier im Code
**englisch**. Der Issue-Text ist die Quelle für die Umsetzung, diese Datei
für *wie* gebaut wird.

### Authentifizierung (Nextcloud/OIDC)

Nextcloud ist der Identity Provider **und** das Nutzerverzeichnis.

| Thema | Festlegung |
|-------|-----------|
| Protokoll | OAuth2/OIDC, **JWT-Access-Tokens** (RFC 9068), Signaturprüfung über JWKS des Issuers (`nextcloud.oidc.issuer-uri`) |
| Backend | Resource Server, **kein Client-Secret** |
| Client-Bindung | `nextcloud.oidc.client-id` — sonst passt ein Token jedes anderen Clients desselben Issuers |
| Frontend | `angular-oauth2-oidc`, Code-Flow, **public** Client |
| Konfiguration | `.env` (Properties, in `.gitignore`) — **niemals** Secrets committen, `.env.example` ist die Vorlage |
| Nutzerverzeichnis | Nextcloud OCS-API, Service-User + App-Password |

**Ohne konfigurierten Issuer startet die API ohne Authentifizierung** — mit
lauter Warnung im Log. Das ist der Zustand eines frischen Setups, kein
Produktivbetrieb.

Ports: `application/port/in/CurrentUserPort`, `application/port/out/UserDirectory`
— die Anwendung kennt **keine** Security-Klassen. Fachliche Validierung
greift unabhängig von der Anmeldung. CORS:
`householdmanager.security.allowed-origins` (Default `http://localhost:4200`).

### Backend

- **Schichten:** `domain` (Fachkern, keine Framework-Imports) →
  `application` (Ports, Use Cases) → `adapters` (REST, JPA). Details:
  Skill `backend-ports-adapters`.
- Controller: `@RestController @RequestMapping("api/<resource>")` — **ohne
  führenden Slash**.
- DTOs als **Records** in `adapters/inbound/rest/dto/`; `@JsonProperty` nur
  bei abweichendem JSON-Key (Jackson 3, Package bleibt
  `com.fasterxml.jackson.annotation`).
- Fehler: `GlobalExceptionHandler` mit `ProblemDetail`; Use Cases werfen,
  Controller geben kein `ResponseEntity` zurück.
- Transaktionen: `@Transactional` an der Use Case, `readOnly = true` beim
  Lesen. `open-in-view: false` — die Entity verlässt den Adapter nicht.
- Schema: Flyway, **keine** Änderung bestehender `V`-Dateien; `ddl-auto:
  validate` verlangt Übereinstimmung von Entity und Migration.
- **Anwendung setzt die Invariante:** Zeitfenster, Grenzen und Reihenfolge
  gehören in die Use Case; der Adapter liefert die Daten der Quelle.
- **Architekturregeln sind Tests:** `ArchitectureRulesTest` (ArchUnit) erzwingt
  Abhängigkeitsrichtung, Transaktions-Ort, Entity-/Controller-Platz,
  DTO-Records, Ports als Interfaces und Konstruktor-Injektion. **Wer eine
  Regel ändert, ändert den Test mit** — sonst driften Prosa und Test auseinander.
- **Zustände getrennt halten:** „veraltet" braucht *letzten Versuch* **und**
  *letzten erfolgreichen Lauf*; „noch kein Lauf" ist ein eigener Zustand.

### Frontend

- Feature = Ordner mit `routes.ts`; Feature-Services **route-scoped** (nicht
  `root`), dazu `provideRouter(routes, withAutoCleanupInjectors())` — sonst
  zeigt `httpResource` beim erneuten Betreten den alten Stand.
- Angular 22, **zoneless**, Standalone. Dateinamen **ohne** Rollen-Suffix
  (`households-page.ts` → `class HouseholdsPage`), Models mit `.model.ts`.
- Reaktivität: `signal()`/`computed()`; Lesen per `httpResource` im
  Data-Service, Schreiben per `HttpClient` im API-Client; Formulare als
  **Signal Forms**.
- **Signal Forms:** `maxLength()` schneidet beim Paste still ab — eigene
  `validate()`-Regel mit `maxLengthError()` auf der **getrimmten** Länge.
- **Design-DNA ist bindend** (`docs/design/design-dna.md`, Tokens in
  `_design-tokens.scss`): Dark Mode Standard, Akzent trägt Bedeutung,
  Monospace für Daten, Radius 4px, 1px-Linien statt Schatten, **keine
  Hex-Literale** — nur `var(--hm-…)`. Komponenten aus `shared/components/`
  wiederverwenden.
- Grenzen: `npm run sheriff`; Stil: `npm run lint`.
- Tests: Vitest + TestBed + `provideHttpClientTesting()`.

### Boot-4-Fallen

- **Dev Services brechen den Start:** `spring.docker.compose.enabled: false`;
  nur Profil `local` schaltet Compose an.
- **Lombok ab JDK 23:** `<proc>full</proc>` + Lombok in
  `annotationProcessorPaths`.
- **Test-Slices umgezogen:** `@WebMvcTest` →
  `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest` →
  `org.springframework.boot.data.jpa.test.autoconfigure`.
- **Testcontainers 2.x:** Modul `org.testcontainers:testcontainers-postgresql`,
  `PostgreSQLContainer` nicht mehr generisch.
- **Jackson 3** ist Default; die **H2-Console** fehlt in Boot 4.

## Issue-Workflow

Issues sind der Arbeitsstand für Anforderungen. Ein Einstieg, kein Command
pro Runde: `/anforderung <Rohidee|Issue-Nr>` (3 Runden, Grilling, Gate).
Details: `.opencode/README.md`.

## Branch, Commit, Push und PR

- **Ein Feature = ein Branch = ein Issue = ein PR.**
- Branch `feature/<issue-nr>-<kurzname>`, kebab-case, **frisch von `main`**
  (`git fetch origin && git switch -c … origin/main`). **Kein Direct-Push
  auf `main`** — `main` erhält nur gemergte PRs.
- **Committen erst, wenn die Gates bestanden sind** (DoD). Englisch,
  Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`,
  `chore:`), Body mit **Warum**, wenn nicht offensichtlich.
- Vor dem Commit: `git diff` prüfen — keine Debug-Reste, keine Secrets,
  keine Build-Artefakte.
- **Jeder gepushte Stand baut und testet grün** (siehe Verify). Rot wird
  nicht gepusht; Ausnahme sind reine Doku-Änderungen.
- Ablauf mit Vorprüfungen, Gate-Nachweis und PR-Vorlage: Command `/pr`.
- **Kein Merge ohne grünes CI und erfüllte Gates** — Merge macht der
  Auftraggeber.

## Verify-Kommandos

```bash
# Backend: Kompilieren + Tests (Adapter-Test braucht Docker/Testcontainers)
cd backend && ./mvnw -B test

# Backend ohne lokales JDK (Docker)
docker run --rm --user "$(id -u):$(id -g)" --group-add "$(getent group docker | cut -d: -f3)" \
  -e HOME=/tmp -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$PWD/backend":/app -w /app maven:3.9-eclipse-temurin-25 mvn -B test

# Frontend: Type-Check, Tests, Lint, Modulgrenzen, Production-Build
cd frontend
npx tsc --noEmit -p tsconfig.app.json && npx tsc --noEmit -p tsconfig.spec.json
npm test && npm run lint && npm run sheriff && npm run build
```

Jedes Issue nennt im `## Verify`-Block **nur** die relevanten Kommandos plus
einen manuellen Test in einem Satz. Skill-Dateien wiederholen diese Befehle
**nicht** — sie verweisen hierher.

## Release

Ablauf, Versions-Bump, Tag, Deploy und Rollback: `docs/release.md`.

## Gotchas

- Angular 22 verlangt Node ≥ 22.22.3 (bzw. 24.15+ / 26+).
- Angular 22 ist zoneless: Effekte laufen erst nach `detectChanges()` — im
  Test den HTTP-Request erst **danach** erwarten.
- `npm test` läuft über Vitest (kein Karma).
- **Geometrie ist in jsdom nicht prüfbar:** Breakpoints und Umbruch brauchen
  die Sichtprüfung am laufenden Stack.
- Maven im Docker-Container braucht ein beschreibbares `-Dmaven.repo.local`;
  Testcontainers braucht Socket **und** Docker-Gruppe (`--group-add`).
- **Boot 4 modularisiert die Starter:** `RestClient.Builder` kommt vom
  Starter `spring-boot-starter-restclient` — ohne ihn startet die App nicht.
- **Boot 4 + Security 7.1:** `JwtDecoders.fromIssuerLocation(...)` für
  JWT-Access-Tokens; die Introspection-API ist neu, `OAuth2TokenIntrospectionClient`
  gibt es nicht mehr.
- **Jackson 3 ist strikt:** unbekannte Felder scheitern
  (`@JsonIgnoreProperties(ignoreUnknown = true)`).
- **Der OIDC-Principal steckt in `getPrincipal()`**, nicht in der
  `Authentication`.
- **Der Composition Root liefert keine Beans:** Verdrahtung liegt in
  `config/ClockConfig`, `config/IcsClientConfig` — sonst Bean-Zyklus.
