# AGENTS.md — HouseholdManager

Verbindliche Arbeitsanweisungen für Agenten in diesem Repo. Gilt für
**HouseholdManager** und ersetzt die allgemeineren Vorgaben aus
übergeordneten Verzeichnissen.

> **Diese Datei wird bei jedem Turn geladen.** Sie enthält nur
> Entscheidungen. Details stehen in den verlinkten Dateien und werden bei
> Bedarf geladen, nicht vorab.

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

## Kontext-Disziplin (klassisches lokales Modell, 256k)

Gearbeitet wird mit einem **kleinen lokalen Modell** (Qwen3.8-27B, quantisiert,
via `llama-router`, 256k Kontext — Varianten `q38-256k-q3/q4`). Zwei Folgen:

1. **Der Context ist das Betriebsmittel.** Alles, was hineinkommt, bleibt bis
   zum Sessionende im Fenster. Der Context ist kein Scratchpad, sondern
   Arbeitsspeicher für die *nächste* Entscheidung.
2. **Kleine Modelle leiden unter langen Kontexten.** Die Aufmerksamkeit
   verschiebt sich zur Mitte („lost in the middle"): lange Verläufe führen zu
   Fehlern und Halluzinaten, nicht nur zu Kosten. **Ein kurzer, präziser
   Kontext schlägt einen langen.**

Konsequenz: **Arbeitsschritte klein halten, nie den ganzen Verlauf mitführen.**

| # | Regel |
|---|-------|
| 1 | **Grep vor Read.** Erst `grep`/`find` mit Pfad-Muster, dann nur die Fundstelle mit `read` (mit `offset`/`limit`) lesen. Nie eine ganze Datei lesen, wenn eine Stelle reicht. |
| 2 | **Erst Dateinamen, dann Inhalt.** `find <pfad> -name '*muster*'` statt Verzeichnisbaum. Nie `ls -R` über `node_modules/`, `target/`, `dist/`. |
| 3 | **Tool-Ausgaben immer kürzen.** `| head -30`, `| tail -20`, `| grep -E "ERROR\|Tests run"`. Rohausgaben von `cat`, `mvn`, `npm`, `docker build` sind verboten. |
| 4 | **Lange Läufe: Logdatei statt Bildschirm.** Output nach `/tmp/<thema>.log`, dann gezielt greppen. Aus einem 3000-zeiligen Maven-Log gehören 5 Zeilen in den Kontext. |
| 5 | **Keine Lockfiles, keine Build-Artefakte, keine generierten Dateien.** `package-lock.json`, `target/`, `dist/`, `node_modules/`, `.angular/` nie lesen. |
| 6 | **Schreiben statt zitieren.** Dateien mit `write`/`edit` anlegen oder ändern; den Inhalt nicht vorher in die Nachricht kopieren. |
| 7 | **Ein Schritt pro Zug.** Suchen → entscheiden → umsetzen → prüfen → **max. 3 Sätze Status**. Der Status ersetzt die Historie, er wiederholt sie nicht. |
| 8 | **Gepatchten Code nicht erneut lesen.** Nach `write`/`edit` gilt der Inhalt als bekannt — Verifikation über Test/Grep. |
| 9 | **Skill einmal laden.** Eine geladene Skill gilt für die Session. Große Referenzdateien (43 Angular-Referenzen, Spring-Referenzen) nur öffnen, wenn genau diese Frage beantwortet werden muss — dann **eine** Datei, ein Abschnitt. |
| 10 | **Subagent nur für breite Suche.** `explore` mit exakten Pfaden, einer engen Frage und Antwortgrenze (≤ 300 Wörter, Pfade als Beleg). Nie zwei Agenten auf dieselben Dateien. |
| 11 | **MCP-Antworten begrenzen.** GitHub-Tools mit `fields: ["number","title"]`, kleines `perPage`, Kommentare nur bei Bedarf lesen. |
| 12 | **Handover-Datei statt Verlauf.** Reicht der Context nicht mehr: Stand in `.opencode/state.md` (max. 40 Zeilen) schreiben, Session neu starten, dort weitermachen. |
| 13 | **Diese Datei bleibt kurz.** AGENTS.md und System-Prompts landen in **jedem** Turn. Regeln kurz, keine Beispiele, die ohnehin im Repo stehen. |

**Nicht in den Context:** vollständige Dateiinhalte nach dem Schreiben ·
Stacktraces (nur `Caused by:`-Kette, max. 5 Zeilen) · Maven-/npm-Download-Listen ·
Issue- oder Dateilisten > 50 Einträge · nicht relevante Skill-Referenzen.

> **Faustregel:** Antwortet eine Suche mit mehr als ~200 Zeilen, war sie zu
> breit — enger suchen, nicht mehr ausgeben.

## Struktur

```text
backend/src/main/java/de/householdmanager/
  domain/model/              Fachkern, ohne Framework-Imports
  application/port/in|out/   Inbound-/Outbound-Ports
  application/usecase/       Use Cases (Transaktionsgrenze)
  adapters/inbound/rest/     HTTP: Controller, DTOs, Fehlerübersetzung
  adapters/outbound/persistence/  JPA: Entity, Repo, Mapping
  config/                    Composition Root

frontend/src/app/
  core/                      app-weit, eine Instanz
  shared/                    von mehreren Features benutzt, fachneutral
  features/<feature>/
    routes.ts                lazy; Feature-Services route-scoped
    <feature>-page.ts        smart Component
    model/ data/ ui/         Modell, Datenzugriff, dumb Components

arc42/                       Architekturdokumentation (12 Kapitel),
                             gepflegt vom ARC-Agenten
docs/adr/                    Architekturentscheidungen (ADRs)
docs/design/design-dna.md    Gestaltungssprache (Dark Mode, Tokens, Muster)
frontend/src/styles/         Design-Tokens (_design-tokens.scss)
.github/workflows/ci.yml     CI: Build, Tests, Lint, Modulgrenzen, Stack
.github/pull_request_template.md  PR-Vorlage
```

**Backend:** Ports & Adapters, `adapters → application → domain` —
[ADR 0001](docs/adr/0001-backend-ports-and-adapters.md), Skill
`backend-ports-adapters`.
**Frontend:** Feature-Architektur (Manfred Steyer) — [ADR 0002](docs/adr/0002-frontend-feature-architecture.md),
Skill `frontend-feature-architecture`. Grenzen werden von **Sheriff**
geprüft (`frontend/sheriff.config.ts`).
**arc42:** wird vom ARC-Agenten (`@arc-agent`) zwischen fachlicher und
technischer Runde gepflegt; ADRs sind die Quelle der Wahrheit für
Entscheidungen, arc42/09 verweist darauf.

## Skills

| Skill | Wann |
|-------|------|
| `backend-ports-adapters` | jede Backend-Änderung: Schichten, Ports, Mapping, Transaktionen |
| `frontend-feature-architecture` | jede Frontend-Änderung: Feature-Struktur, Grenzen, Smart/Dumb, Sheriff |
| `feature-testing` | Tests schreiben/prüfen: Testebene, AC-Abdeckung, Anti-Patterns |
| `code-review-gate` | Review-Kategorien, Severity, OK/KORREKTUREN, Format des Korrekturauftrags |
| `ux-design` | UX-Entwurf: Methode, Issue-Abschnitt `## UX`, Design-DNA |
| `angular-developer` · `angular-new-app` | Frontend-Handwerk: Signals, `httpResource`, Signal Forms, zoneless, Vitest |
| `vitest` | Frontend-Testwerkzeug (Vitest 5): Mocking, Coverage, Filter |
| `spring-boot` · `spring-framework` · `spring-data-jpa` · `spring-flyway` · `spring-web` · `spring-security` · `spring-testing` | Backend-Handwerk |
| `hexagonal-architecture` · `spring-versions` | Methodik bzw. Versionsfragen (Versionsangaben sind **nicht** die Projekt-Version) |
| `anforderung-loop` (+ `-intake`/`-fachlich`/`-technisch`/`-gate`) · `issue-formulierung` · `issue-github` | Issues anlegen und verfeinern |

**Agent:** `architektur-reviewer` (Subagent) — Architektur-Gate
(`OK`/`KORREKTUREN`, Korrekturauftrag), `kritischer-tester` (Subagent) —
Test-Gate (`PASS`/`FAIL`), `ux-designer` (Subagent) — UX-Entwurf als
`## UX` im Issue, `arc-agent` (Subagent) — pflegt `arc42/` zwischen
fachlicher und technischer Runde. Siehe „Definition of Done".

Herkunft und Aktualisierung: `.opencode/skills/README.md`. Vollständige
Konventionstabellen: `.opencode/skills/anforderung-technisch/references/technik-checkliste.md`.

## Definition of Done (Feature)

Ein Feature ist **erst fertig**, wenn **alle sechs** Punkte erfüllt sind:

1. **Implementiert** — alle Akzeptanzkriterien des Issues sind umgesetzt,
   in der Schicht, in der sie hingehören (Ports & Adapters, Feature-Architektur).
2. **Vollständig getestet** — **jedes** `AC<n>` hat einen Test, der es
   belegt; Fehlerpfade und Grenzwerte sind getestet. Bei UI-Anteil kommen
   **Sichtprüfung** (Layout ist im Test nicht sichtbar) und der **manuelle
   Pfad im Browser** dazu. Definition und Testebenen: Skill `feature-testing`.
3. **Grün** — `./mvnw -B test` und `npm test` laufen grün; zusätzlich
   `npm run lint` und `npm run sheriff` (Architektur-Grenzen).
4. **Architektur-Review OK** — der Subagent `architektur-reviewer` hat
   geprüft und **`OK`** gemeldet. Bei `KORREKTUREN` wird der
   **Korrekturauftrag an den Implementierer delegiert** (nicht vom Reviewer
   ausgeführt) und erneut geprüft — bis `OK` oder nach spätestens 3 Runden.
5. **Abnahme PASS** — der Subagent `kritischer-tester` hat unabhängig
   geprüft und `PASS` gemeldet. Er hat **andere Augen** als der
   Implementierer: er urteilt adversarial gegen die Akzeptanzkriterien,
   schreibt fehlende Tests (nur Testdateien) und meldet `PASS` oder `FAIL`.
6. **Nachbereitung** — die Erfahrungen aus dem Feature stehen in
   `docs/erkenntnisse-issue-<n>.md`, und die wiederkehrenden Fehler sind als
   Regel in `AGENTS.md` oder im zuständigen Skill **überführt**; offene
   Kleinbefunde liegen als Follow-up-Issues vor.

```text
/review <Issue-Nr>       # Architektur-Gate: review → Korrekturen delegieren → review
/abnahme <Issue-Nr>      # Test-Gate: adversarial Prüfung der Akzeptanzkriterien
@architektur-reviewer    # Reviewer direkt
@kritischer-tester       # Tester direkt
```

Die Urteile werden als **Kommentar am Issue** protokolliert
(`<!-- review: status=OK … -->`, `<!-- abnahme: status=PASS … -->`).

**Ein Feature ohne beide Gates ist nicht fertig** — auch wenn alle Tests
grün sind. Ein selbst erteiltes Urteil zählt nicht: Urteile kommen von den
Subagenten, Korrekturen vom Implementierer.

Nach `FAIL`/`KORREKTUREN`: Befunde beheben, dann das betroffene Gate erneut
laufen lassen. Grüne Tests allein sind kein Fertig-Beleg.

## Konventionen

Sprache: Issue-Texte und Kommentare auf **Deutsch**, Identifier im Code
**englisch**. Der Issue-Text ist die Quelle für die Umsetzung, diese Datei
für *wie* gebaut wird.

### Authentifizierung (Nextcloud/OIDC)

Nextcloud ist der Identity Provider **und** das Nutzerverzeichnis — wie in
Constructly.

| Thema | Festlegung |
|-------|-----------|
| Protokoll | OAuth2/OIDC, **JWT-Access-Tokens** (RFC 9068) — Signaturprüfung über den JWKS des Issuers (`nextcloud.oidc.issuer-uri`) |
| Backend | Resource Server (`spring-boot-starter-oauth2-resource-server`), **kein Client-Secret** nötig |
| Client-Bindung | `nextcloud.oidc.client-id` — ohne sie würde ein Token jedes anderen Clients desselben Issuers passen |
| Frontend | `angular-oauth2-oidc`, Code-Flow, **public** Client (nur Client-ID, kein Secret) |
| Konfiguration | `.env` (Properties-Format, in `.gitignore`). **Niemals** Secrets committen — `.env.example` ist die Vorlage |
| Nutzerverzeichnis | Nextcloud OCS-API, Service-User + App-Password (`householdmanager-svc`) |

**Ohne konfigurierten Issuer startet die API ohne Authentifizierung** —
mit lauter Warnung im Log. Das ist der Zustand eines frischen Setups, kein
Produktivbetrieb.

Struktur (Ports & Adapters):

```text
application/port/in/CurrentUserPort          eingehender Port: wer ist angemeldet?
application/port/out/UserDirectory           ausgehender Port: Nutzerverzeichnis
adapters/inbound/security/…CurrentUser      liest den SecurityContext
adapters/inbound/rest/AuthController         GET /api/auth/me
adapters/outbound/nextcloud/…UserDirectory  OCS-API (Suche + Details)
```

Regeln:

- Die Anwendung kennt **keine** Security-Klassen: `CurrentUserPort` und
  `UserDirectory` sind die einzigen Einstiegspunkte.
- Fachliche Validierung bleibt unabhängig von der Auth: die Regeln greifen
  auch bei fehlender Anmeldung (401).
- CORS-Origin des Frontends: `householdmanager.security.allowed-origins`
  (Default `http://localhost:4200`).

### Backend

- **Schichten:** Fachmodell in `domain`, Orchestrierung in
  `application/usecase`, Technik in `adapters`. `domain` importiert nie
  Spring/JPA/Jackson; `application` kennt keine HTTP- oder JPA-Typen.
- Controller: `@RestController @RequestMapping("api/<resource>")` — **ohne
  führenden Slash**.
- DTOs: `adapters/inbound/rest/dto/` als **Records**; `@JsonProperty` nur bei
  abweichendem JSON-Key (Jackson 3, Annotation-Package bleibt
  `com.fasterxml.jackson.annotation`).
- Fehler: `adapters/inbound/rest/GlobalExceptionHandler` mit `ProblemDetail`;
  Use Cases werfen, Controller geben kein `ResponseEntity` zurück.
- Transaktionen: `@Transactional` an der Use Case, `@Transactional(readOnly = true)`
  fürs Lesen.
- `open-in-view: false` — die Entity darf den Adapter nicht verlassen.
- Schema: Flyway-Migrationen, **keine** Änderung bestehender `V`-Dateien;
  `ddl-auto: validate` verlangt Übereinstimmung von Entity und Migration.

### Frontend

- Feature = Ordner mit `routes.ts`; Feature-Services **route-scoped**
  (nicht `root`).
- Angular 22, **zoneless**, Standalone. Dateinamen **ohne** Rollen-Suffix
  (`households-page.ts` → `class HouseholdsPage`), Models behalten
  `.model.ts`; der Ordnungsweg trägt die Rolle (`core/`, `features/`, `shared/`).
- Reaktivität: `signal()` / `computed()`; Lesen per `httpResource` im
  Data-Service, Schreiben per `HttpClient` im API-Client; Formulare als
  **Signal Forms**.
- **Design-DNA ist bindend**: Tokens aus
  `frontend/src/styles/_design-tokens.scss`, beschrieben in
  [`docs/design/design-dna.md`](docs/design/design-dna.md). Dark Mode ist
  Standard, Akzent trägt Bedeutung, Monospace für Daten, Radius 4px,
  1px-Linien statt Schatten. **Keine Hex-Literale** im Code — nur
  `var(--hm-…)`. Komponenten aus `shared/components/` wiederverwenden.
- Modulgrenzen: `npm run sheriff`; Stil: `npm run lint`.
- Tests: Vitest + TestBed + `provideHttpClientTesting()`.

### Boot-4-Fallen

- **Dev Services brechen den Start**: `spring-boot-docker-compose` ist ab
  Boot 4 automatisch aktiv. `application.yml` setzt
  `spring.docker.compose.enabled: false`; nur Profil `local` schaltet es an.
- **Lombok ab JDK 23**: Annotation Processing ist aus → `maven-compiler-plugin`
  braucht `<proc>full</proc>` + Lombok in `annotationProcessorPaths`.
- **Test-Slices umgezogen**: `@WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure`,
  `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure`.
- **Testcontainers 2.x**: Modul `org.testcontainers:testcontainers-postgresql`,
  `PostgreSQLContainer` ist nicht mehr generisch.
- **Jackson 3** ist Default; die **H2-Console** ist in Boot 4 nicht enthalten.

## Issue-Workflow

Issues sind der Arbeitsstand für Anforderungen. Ein Einstieg, kein Command pro
Runde: `/anforderung <Rohidee|Issue-Nr>` (3 Runden, Grilling, Gate). Details:
`.opencode/README.md`.

## Branch, Commit, Push und PR

**Ein Feature = ein Branch = ein Issue = ein PR.**

### Branch

- Für **jedes** Feature wird ein Branch angelegt — Ausnahme sind reine
  Dokumentations-Änderungen.
- Name: `feature/<issue-nr>-<kurzname>` (z. B. `feature/12-haushalt-anlegen`),
  kebab-case, lowercase.
- Basis ist immer `main`; der Branch wird **frisch aus `main` gezogen**
  (`git fetch origin && git switch -c feature/… main`).
- **Kein Direct-Push auf `main`** — `main` erhält nur gemergte PRs.

### Commit

- **Committen wird erst, wenn die Gates bestanden sind** (siehe
  „Definition of Done"): Implementierung, Tests, Architektur-Review `OK`
  und Abnahme `PASS`.
- Commit-Sprache: **Englisch**. Format: Conventional Commits
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`) mit kurzem
  Imperativ im Betreff, Body mit **Warum**, wenn es nicht offensichtlich ist.
- Ein Commit = ein zusammenhängender Schritt. Der Branch bleibt lauffähig
  (siehe unten) — keine halben Stände committen.
- Vor dem Commit: `git diff` prüfen, keine Debug-Reste, keine Secrets
  (`.env`, Tokens), keine Build-Artefakte (siehe `.gitignore`).

### Lauffähigkeit (verbindlich)

**Jeder gepushte Stand baut und testet grün.** Ein Branch, der auf `main`
landet, muss lauffähig sein — im Development-Setup **und** in CI.

```bash
# Backend
cd backend && ./mvnw -B test
# Frontend
cd frontend
npx tsc --noEmit -p tsconfig.app.json && npx tsc --noEmit -p tsconfig.spec.json
npm test && npm run lint && npm run sheriff && npm run build
```

- **Grün ist die Bedingung für Push und PR.** Ist etwas rot, wird nicht
  gepusht — erst beheben.
- Ausnahmen gibt es nur für **reine Doku-Änderungen** (kein Code berührt).
- Ein Branch mit Zwischenständen, die nicht lauffähig sind, ist verboten:
  kein „WIP auf main“-Zustand, kein „Fix later“-Push.
- CI (`.github/workflows/ci.yml`) prüft dasselbe bei jedem PR und bei jedem
  Push auf `main` — **der PR ist nicht mergebar, solange CI rot ist**.

### Push und PR

1. Nach bestandenen Gates: committen (siehe oben).
2. Branch pushen: `git push -u origin feature/<issue-nr>-<kurzname>`.
3. PR gegen `main` eröffnen (Command `/pr` oder GitHub-Tools
   `create_pull_request`). Der PR enthält:
   - **Titel**: Conventional-Commit-Art + Titel (`feat: Haushalt im UI anlegen`)
   - **Beschreibung**: Ziel, was geändert wurde, **Closes #<Issue-Nr>**,
     Nachweis der Gates (Kommandos + Ergebnis), Screenshots bei UI-Änderungen
   - **Checkliste**: Gates, alle abgehakt
4. Der PR bleibt offen, bis die Gates bestanden sind und das Review
   erfolgt ist. Merge erfolgt durch den Auftraggeber.

**Kein Merge ohne grünes CI und erfüllte Gates.**

> **Einmalige Repo-Einstellung:** Branch-Schutz für `main` aktivieren
> (Settings → Branches → Rule: „Require a pull request" + „Require status
> checks" auf `CI / backend`, `CI / frontend`, `CI / stack`). Damit ist ein
> Direct-Push technisch unmöglich und ein PR ohne grünes CI nicht mergebar.

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

Jedes Issue nennt im `## Verify`-Block **nur** die Kommandos, die für es
relevant sind, plus einen manuellen Test in einem Satz.

## Release

Ein Feature ist ausgeliefert, wenn sein PR gemergt ist. Der Ablauf:

1. **Versions-Bump** — `backend/pom.xml` und `frontend/package.json` auf die
   neue Version setzen (in beiden dieselbe Nummer).
2. **Tag** — `git tag -a v<version> -m "<version>"`, dann
   `git push origin v<version>`.
3. **Bau** — `docker compose -f compose.yml build` baut die Images
   `householdmanager-backend` und `householdmanager-frontend`.
4. **Deploy** — auf dem Zielsystem `docker compose up -d --build`; der Stack
   bringt Postgres (5432), Backend (8080) und Frontend (4200) hoch und startet
   das Backend erst, wenn die Datenbank gesund ist.
5. **Rollback** — auf den vorherigen Tag zurücksetzen
   (`git checkout v<vorherige Version>`), erneut bauen und deployen.

Ein Publish in eine Container-Registry ist nicht eingerichtet: die Images
werden auf dem Zielsystem gebaut. Nach einem Deploy immer mit `--build`
starten — ohne baut Docker das zuletzt vorhandene Image nicht neu.

## Gotchas

- Angular 22 verlangt Node ≥ 22.22.3 (bzw. 24.15+ / 26+).
- Angular 22 ist zoneless: Effekte laufen erst nach `detectChanges()` — im Test
  den HTTP-Request erst **danach** erwarten.
- `npm test` läuft über Vitest (kein Karma).
- Maven-Builds im Docker-Container brauchen ein beschreibbares
  `-Dmaven.repo.local`, sonst ist der Cache nach jedem Lauf weg.
- Testcontainers braucht den Docker-Socket **und** die Docker-Gruppe
  (`--group-add`) — der Socket gehört `root:docker`.
- Hibernate 7 + `ddl-auto: validate`: Jede Entity-Änderung braucht eine
  Migration, sonst startet die App nicht.
- **Boot 4 modularisiert die Starter**: `RestClient.Builder` ist ein Bean des
  Starters `spring-boot-starter-restclient` — ohne ihn startet die App nicht.
- **Boot 4 + Security 7.1**: Für JWT-Access-Tokens `JwtDecoders.fromIssuerLocation(...)`
  (Discovery + JWKS); die Introspection-API ist neu
  (`opaqueToken(opaque -> opaque.introspectionUri(...).clientId(...))`),
  `OAuth2TokenIntrospectionClient` gibt es nicht mehr.
- **Jackson 3 ist strikt**: unbekannte Felder lassen die Deserialisierung
  scheitern (`@JsonIgnoreProperties(ignoreUnknown = true)`); Annotationen
  kommen weiterhin aus `com.fasterxml.jackson.annotation`.
- **Der OIDC-Principal steckt in `getPrincipal()`**, nicht in der
  `Authentication` selbst.
