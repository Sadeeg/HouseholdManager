# Erkenntnisse — Issue #1 „Aufgaben-Board TODO/DOING/DONE mit Drag & Drop“

Aufgeschrieben am 30.10.2026, nach Abschluss von Issue #1 (PR #2, gemergt durch den
Auftraggeber). Zweck: die Reibung dieses Features festhalten — was kaputt war, warum es
kaputt war, wie es nachgebessert wurde, und was davon in Regeln übergeht.

Belegte Gates: Backend 116 Tests grün · Frontend 12 Dateien / 68 Tests grün · `lint`,
`sheriff`, `tsc`, Build clean · Architektur-Review **OK** (Issue #1, Kommentar 6509136018)
· Abnahme **PASS**, 26/26 ACs belegt (Kommentar 6508709817).

---

## 1. Behobene Probleme

### 1.1 Dialog „Aufgabe anlegen“ war nicht erreichbar

- **Symptom:** Das Formular ließ sich öffnen, aber das Fenster war nicht sichtbar bzw.
  nicht bedienbar.
- **Ursache:** Das Overlay (`position: fixed; inset: 0; z-index: var(--hm-z-overlay)`) hing
  am **Backdrop-Button**, der Dialog war ein gewöhnliches Flow-Element ohne eigene
  Position. Damit lag er (a) unter dem vollflächigen Overlay und (b) am Ende des
  Seiteninhalts, außerhalb des Viewports. Dasselbe Muster war im bestehenden
  `shared/components/confirm-dialog` richtig implementiert — es wurde nicht übernommen.
- **Nachbesserung:** `features/tasks/ui/task-form.scss` — Overlay auf dem Wrapper
  `.task-form`, Backdrop und Dialog in derselben Gitterzelle (`grid-area: 1 / 1`),
  `max-height: 100%; overflow-y: auto`, damit ein hoher Dialog (Markdown-Vorschau,
  Labels, Fristen) nie über den Rand wächst.
- **Lehre:** Dialoge sind ein **Muster**, kein Detail. Vor dem ersten eigenen Overlay-Code
  das bestehende Dialog-Muster lesen. Unit-Tests (jsdom) finden solche Fehler nicht.

### 1.2 Mitglieder ernennen: verschobene Aktion, verdeckte Vorschlagsliste, fehlender Button

- **Symptome:** „MITGLIEDER“ überlagerte das Erstellungsdatum
  (`29.10.2026MITGLIEDER`); die Personenvorschläge überdeckten den nächsten Haushalt;
  ein Button zum Zuordnen der ausgewählten Person fehlte.
- **Ursachen:**
  - `households-page.scss`: `.households__row` mit `position: relative` +
    `padding-right`, die Aktion absolut bei `top/right` — genau über dem rechtsbündigen
    Datum des Eintrags.
  - `user-picker.scss`: `.user-picker__results` mit `position: absolute` +
    `z-index: var(--hm-z-dropdown)` — eine Liste ohne Raum im Layout.
  - `member-section.html`: „Ernennen“ wurde erst **nach** der Auswahl gerendert; neben dem
    Suchfeld war keine Aktion sichtbar. Die UX-Spec des Issues beschreibt aber „Suchfeld,
    Aktion ‚Ernennen'“ **und** einen `disabled`-Zustand dieser Aktion — also ist sie
    ständig sichtbar.
- **Nachbesserung:** `.households__row` als Grid `minmax(0, 1fr) auto`, Aktion in Spalte 2
  (`justify-self: end`), `app-member-section` mit `grid-column: 1 / -1`.
  `user-picker__results` im Dokumentfluss (Overlay entfernt). „Ernennen“ als ständige
  Primäraktion neben dem Suchfeld, `disabled` ohne Auswahl (`canAppoint`,
  `member-section.ts:45`), mit Auswahl-Chip, Hinweis „Bereits Mitglied“ und „ABBRECHEN“
  (`clearPerson()`, Zeile 55).
- **Lehre:** Zustands- und Aktions-Tabellen der UX-Spec sind der Bauplan. „Aktion X ist
  `disabled`“ bedeutet: Aktion X existiert sichtbar, nicht nachgelagert.

### 1.3 Drag & Drop: Karte fällt zurück und erscheint verspätet in der anderen Bahn

- **Symptom:** „hackelig“ — die Karte schnappt in die Ausgangsbahn zurück und taucht erst
  nach dem Neuladen in der Zielbahn auf.
- **Ursache:** Zwei Fehler hintsereinander.
  1. `tasks-page.ts` rief `moveItemInArray` **nur** bei Umzügen innerhalb derselben Bahn.
     Beim Bahnwechsel wurde das Modell nicht verschoben → CDK setzte die Karte am Drop-Ziel
     nicht ein und spielte die Rückfall-Animation.
  2. `TasksData.move()` speicherte und rief sofort `reloadTasks()` → die Karte kam erst mit
     der Serverantwort zurück.
- **Nachbesserung:** Der Datenzug übernimmt den Zwischenstand.
  `features/tasks/data/tasks-data.ts`: `pendingMove` (Zeile 63), `board`-Computed
  (Zeile 65), reine Funktion `applyMove` (Zeile 150) als exakter Spiegel der
  Backend-Normalisierung (`TaskUseCaseImpl.moveTask`: Zielspalte ohne die Karte,
  Position `clamp(0 … Länge)`, beide betroffenen Bahnen 0..n neu nummerieren). Der
  Zwischenstand gilt, solange die geladene Serverliste **identisch** ist; die neue Antwort
  macht ihn wirkungslos (Identitätsvergleich), der Fehlerpfad verwirft ihn (Zeile 102).
  `onDrop` meldet nur noch den Zug.
- **Lehre:** CDK verschiebt **kein** Modell. Wer Drag & Drop über „speichern, dann neu
  laden“ löst, bekommt genau dieses Flackern. Der Regressionstest muss die Zielbahn
  **vor** dem HTTP-Flush prüfen — ein Test, der erst nach dem Reload zählt, ist blind für
  genau diesen Fehler.

### 1.4 Ohne Haushalts-Anlage war das Feature nicht testbar

- **Symptom:** „ich kann keine Aufgabe anlegen“ — es gab keinen Haushalt, und `main` hatte
  für `POST api/households` **keine** UI (nur den Endpunkt).
- **Nachbesserung:** Signal-Formular im Seitenkopf und im Leerzustand (Name Pflicht,
  max. 120, inline „Der Name fehlt.“, Toast „Haushalt angelegt.“) und
  `WorkspaceService.adoptHousehold()`: der neue Haushalt wird sofort Workspace (das Backend
  macht die anlegende Person zu Leitung und Mitglied).
- **Lehre:** „Endpunkt vorhanden“ ≠ „Feature nutzbar“. Zu jedem AC, der mit „kann anlegen“
  beginnt, gehört die UI-Aktion und ein testbarer Datenzustand.

### 1.5 Der lokale Stack startete nicht zuverlässig

- **Symptome:** nginx brach mit `host not found in upstream "backend"` ab;
  `http://localhost:4300/api/...` antwortete 502; Backend-Port kollidierte mit dem
  bereits laufenden Dev-Server.
- **Ursachen:** nginx löst Upstream-Namen beim Start auf und wartete nicht auf das Backend;
  das Backend-Image enthält kein `curl`/`wget`, ein üblicher Healthcheck war unmöglich;
  Backend (8080) und `BACKEND_URL` des Dev-Servers (8081) passten nicht zusammen.
- **Nachbesserung:** `compose.yml` — Backend-Healthcheck über bash
  (`exec 3<>/dev/tcp/127.0.0.1/8080`, Zeile 38–43), `frontend.depends_on.backend.condition:
  service_healthy` (Zeile 51–54), Port-Overrides für Postgres/Backend/Frontend.
  `.env` mit `BACKEND_PORT=8081`; Dev-Proxy-Ziel über `BACKEND_URL` überschreibbar
  (`frontend/proxy.conf.js`).
- **Lehre:** Startreihenfolge, Healthchecks und Port-Überschreibbarkeit sind **Teil des
  Features** — ohne erreichbaren Stack ist kein AC manuell prüfbar. Bei schlanken Images ist
  `/dev/tcp` der Healthcheck.

### 1.6 Frische Datenbank war Voraussetzung, nicht Nebenwirkung

`V2__household_owner_and_membership.sql` ergänzt `created_by NOT NULL` an einer bestehenden
Tabelle ohne Backfill (Entscheidung A14 des Issues). Ohne neues DB-Volume startet das
Backend nicht; der Nachweis „Migrationen V1–V4 gegen echte PostgreSQL“ braucht den
Volume-Neubau als festen Schritt.

### 1.7 Der API-Vertrag des Issues war an einer Stelle zu eng

Die Personenauswahl (AC8/AC11) braucht eine Suchschnelle; der Vertrag nannte nur das
Nutzerverzeichnis-Port. Ergänzt: `GET api/users/search?q=` (`UserSearchController`) und
die Gleichsetzung `UnknownUserException → IllegalArgumentException → 400`
(`GlobalExceptionHandler.java:21–24, 72`). Beides ist jetzt als begründete Abweichung
dokumentiert (arc42/03 Abschnitt „Technische Schnittstellen“, PR #2).

- **Lehre:** Vertragslücken early benennen. Eine unbenannte Abweichung wird im Review zum
  Befund, eine benannte zur Entscheidung.

---

## 2. Was die Gates gefunden haben — und der Implementierer nicht

| Wer | Befund | Status |
|---|---|---|
| `architektur-reviewer`, Runde 1 (**MAJOR**) | `arc42/05_Bausteinsicht.md` und `arc42/03_Kontextabgrenzung.md` dokumentierten den Workspace-Context (`api/me/workspace`, `WorkspaceUseCaseImpl`, `WorkspaceSelectionRepository`/Entity/Adapter, `V4__workspace_selection.sql`) und `UserSearchController` nicht. | behoben (Commit `b62652c`), Runde 2 **OK** |
| `kritischer-tester` | Zwei fehlende Tests: `updateTask_removingDueDateAndAssignee_clearsThemPersistently` (Entfernen von Frist/Zuständiger bleibt dauerhaft) und `task-card.spec.ts › never marks a task whose due date was removed`. | geschrieben — Backend 116, Frontend 68 Tests |
| `kritischer-tester` | Testbelastung geprüft: Backend-Mutation `isNull` → `isNotNull` ergab 1 Failure, invertierte Frontend-Badge-Assertion genau 1 Test rot. | Beleg, dass die Tests beißen |
| Review + Abnahme (MINOR) | `core/auth/start-route.guard.ts` ohne Test · Instanz-Strategie `core/users/user-search.ts` (nicht root-provided, Instanz je Komponente) · `UserRefResponse` als nested Record in `UserSearchController.java:31` statt in `adapters/inbound/rest/dto/` | Follow-up-Issues **#3**, **#4**, **#5** |
| eigene Beobachtung | Labels über 60 Zeichen werden kommentarlos verworfen (`TaskUseCaseImpl.java:186`, `Label.MAX_NAME_LENGTH = 60`) — ohne Test, ohne Rückmeldung im UI. Die Titelgrenze (`Task.MAX_TITLE_LENGTH = 200`) wird korrekt validiert. | offen, nicht AC-gefordert |

**Lehre 1:** Zwei unabhängige Blicke sind der eigentliche Fortschritt. Der Reviewer fand
fehlende Architektur-Doku, der Tester fehlende Fehlerpfad-Tests. Ein selbst erteiltes
„passt“ zählt nicht.

**Lehre 2:** Architektur-Doku (arc42, ADR, Design-DNA) gehört in **denselben Schritt** wie
der Code. Sonst entsteht eine zusätzliche Review-Runde — hier Runde 1 → Korrektur → Runde 2.

---

## 3. Wiederkehrende Fallen (Werkzeug und Framework)

**Spring Boot 4 / Java 25**

- Test-Slices in neuen Paketen: `org.springframework.boot.webmvc.test.autoconfigure`,
  `org.springframework.boot.data.jpa.test.autoconfigure`.
- Testcontainers 2.x: Modul `org.testcontainers:testcontainers-postgresql`,
  `PostgreSQLContainer` ist nicht mehr generisch.
- `spring-boot-docker-compose` ist ab Boot 4 automatisch aktiv →
  `spring.docker.compose.enabled: false` (nur Profil `local` schaltet es an).
- Lombok braucht ab JDK 23 `<proc>full</proc>` + Lombok in `annotationProcessorPaths`.
- Starter sind modularisiert: `RestClient.Builder` existiert nur mit
  `spring-boot-starter-restclient`.
- Jackson 3 ist streng: fremde Felder lassen die Deserialisierung scheitern
  (`@JsonIgnoreProperties(ignoreUnknown = true)`), Annotationen bleiben aus
  `com.fasterxml.jackson.annotation`.
- Der OIDC-Principal steckt in `getPrincipal()`, nicht in der `Authentication`;
  JWT-Prüfung über `JwtDecoders.fromIssuerLocation(...)`.
- Hibernate 7 + `ddl-auto: validate`: jede Entity-Änderung braucht eine Migration.
- Die H2-Console ist in Boot 4 nicht enthalten.

**Angular 22 (zoneless, Signals, Vitest) — die Testfallen, die Zeit gekostet haben**

- Effekte laufen erst nach `detectChanges()`: erst rendern, **dann** `httpMock.expectOne(...)`.
- Nach einem `flush()` braucht der darauffolgende Reload einen Makrotask
  (`await new Promise(r => setTimeout(r, 0)); fixture.detectChanges()`); `whenStable()`
  wartet auf den noch nicht geflushzten Request und hängt.
- `httpResource` dedupliziert identische Requests — ein zweiter GET geht nur über
  `.reload()` (es gibt `reload()`, kein `refresh()`).
- `error()` liefert `undefined` → Prüfung als `!!x.error()`, nie `!== null`.
- Signal-Forms-Knoten dürfen keine `required`/`maxlength`-Attribute im Template tragen.
- Ein Klick auf `type="submit"` löst im Test kein Submit aus → Handler über eine
  `type="button"`-Aktion aufrufen.
- `TestBed.configureTestingModule` nur einmal pro Test.

**Umgebung dieses Rechners**

- Kein lokales JDK: Backend-Kommandos laufen in `maven:3.9-eclipse-temurin-25` mit
  `-Dmaven.repo.local=<beschreibbar>` (sonst ist der Cache jeden Lauf weg) plus
  Docker-Socket **und** `--group-add` (Testcontainers — der Socket gehört `root:docker`).
- Node 22.22.1 ist zu alt für Angular 22 (≥ 22.22.3): `tsc`, Tests, Lint, Sheriff und Build
  laufen in `node:22-slim` mit Cache-Mount.
- `npx vitest` direkt umgeht die Angular-Testkonfiguration (`describe is not defined`) →
  immer `npm test`.
- `analyze_context` ist kein GitHub-MCP-Tool; verfügbar sind nur `tools.github.*` und
  `tools.opencode.*`.

---

## 4. Was sich bewährt hat (übertragbar)

1. **Layout visuell prüfen, nicht nur testen.** Unit-Tests laufen in jsdom und kennen keine
   Geometrie. Bewährt: statischer Harness — echte SCSS des Projekts mit dem vorhandenen
   `sass` kompilieren (`--load-path=frontend/src/styles`, wie `angular.json`), Markup aus
   den echten Templates nachbauen, `google-chrome --headless=new --screenshot`, vor/nach
   vergleichen. Beide UI-Befunde (1.1, 1.2) waren damit in einem Durchgang sichtbar.
2. **Auslieferung belegen statt annehmen.** Nach dem Build die ausgelieferten Dateien im
   Container prüfen (`/usr/share/nginx/html` nach der neuen Regel greppen). „Image gebaut“
   ist kein Nachweis — „die Regel liegt im ausgelieferten Bundle“ ist einer.
3. **Zwischenstände gehören in den Datenzug, nicht in die Seite** (1.3). Die Seite
   orchestriert, der Data-Service hält Zustand und Normalisierung.
4. **Ein Fehler pro Schritt, mit Regressionstest, der die alte Variante verletzt** (1.2, 1.3).
5. **Abweichungen offen benennen** (`api/users/search`, `UnknownUser → 400`,
   `task_label`-JoinTable, Label-Reihenfolge über `label.id`, V2 ohne Backfill).
6. **Urteile von außen holen** (`architektur-reviewer`, `kritischer-tester`), Korrekturen
   delegieren, Protokoll als Kommentar am Issue.
7. **Handover statt Verlauf:** `.opencode/state.md` hält Ports, Kommandos, Gate-Stand und
   Offenes — die Session kann neu starten, die Arbeit setzt dort auf.

---

## 5. Offene Nachbesserungen

| # | Was | Wo |
|---|---|---|
| Issue #3 | Start-Route-Guard ohne Test (Scoping, Umleitung auf die Tafel des zuletzt gewählten Haushalts) | `frontend/src/app/core/auth/start-route.guard.ts` |
| Issue #4 | Instanz-Strategie der Nutzersuche klären (`core`-Position, nicht root-provided → eine Instanz je Komponente) — **erledigt**: `UserSearch` ist jetzt `providedIn: 'root'`, der Zustand geteilt, Reset-Regel im Skill und an der Datei | `frontend/src/app/core/users/user-search.ts` |
| Issue #5 | `UserRefResponse` nach `adapters/inbound/rest/dto/` verschieben | `UserSearchController.java:31` |
| Issue #8 | Stille Verwerfung von Labels > 60 Zeichen sichtbar machen (Hinweis im Formular, Test) | `TaskUseCaseImpl.java:186` |
| offen | Branch-Schutz für `main` (PR-Pflicht + Status-Checks) nachweislich nicht gesetzt: PR #2 eröffnete mit `mergeable_state: clean` | GitHub → Settings → Branches |
| offen | Kein E2E-Setup (Playwright) — UI-Layout bleibt bis dahin manuell geprüft | `.github/workflows/ci.yml` |

---

## 6. Regeln für das nächste Feature

1. Vor der ersten UI-Zeile: Zustands- und Aktions-Tabelle der UX-Spec durchgehen;
   vorhandene Muster (`confirm-dialog`, `page-header`, Design-Tokens) übernehmen statt
   nachbauen.
2. Jede „kann X“-Anforderung bis zum manuellen Testpfad denken: Einstieg in der UI,
   erreichbarer Stack, testbarer Datenzustand.
3. Drag & Drop, Sortierung, Inline-Bearbeitung: erst der lokale Zwischenstand im Datenzug,
   dann die Speicherung; der Test prüft den Zustand **vor** der Antwort.
4. `compose.yml` (Ports, Healthchecks, Startreihenfolge) wie Code behandeln und im PR
   begründen.
5. Architektur-Doku (arc42, ADR, Design-DNA) im selben Schritt wie der Code.
6. Layout visuell verifizieren; Auslieferung im ausgelieferten Bundle nachweisen.
7. Vertragsabweichungen sofort als Abweichung benennen.
8. Beide Gates laufen lassen, bevor committet wird; Korrekturen delegieren, keine
   Selbsturteile.

---

## 7. Wohin die Regeln geflossen sind

Ein Erkenntnis-Dokument ohne überführte Regel ändert an der nächsten Runde
nichts. Jede Erkenntnis aus diesem Issue ist deshalb jetzt an der Stelle
verankert, an der sie beim Bauen gebraucht wird:

| Erkenntnis | Regel jetzt in |
|---|---|
| 1.1 Overlay am falschen Element | `ux-design` §3a „Muster zuerst suchen" (Overlay auf den Wrapper, Backdrop und Panel in derselben Gitterzelle) |
| 1.1 / 1.2 Layout im Test unsichtbar | `ux-design` §3b „Sichtprüfung"; `feature-testing` „Layout ist kein Testfall"; `code-review-gate` Kategorie 8 |
| 1.2 Aktion erst nach Auswahl sichtbar | `ux-design` §3 „Sichtbarkeit"; `anforderung-fachlich` §4; `anforderung-gate` Runde 2 |
| 1.2 Vorschlagsliste überlagerte Nachbarinhalt | `ux-design` §3 „Anordnung"; `code-review-gate` Proben (`position: absolute`) |
| 1.3 CDK verschiebt kein Modell | `frontend-feature-architecture` „Zwischenstände gehören in die Datenschicht" |
| 1.3 Rückfall beim Ziehen | `feature-testing` „Zwischenstand vor der Antwort prüfen" (Anti-Pattern: erst nach dem Reload prüfen) |
| 1.4 Endpunkt vorhanden, UI fehlte | `anforderung-fachlich` §4 („Einstieg und Voraussetzung"); `anforderung-technisch` §5a; `anforderung-gate` Runde 2 |
| 1.5 Stack startete nicht | `anforderung-technisch` §7/§9 (compose.yml, Healthcheck, Port-Overrides im Verify-Block); `code-review-gate` Kategorie 8 |
| 1.6 Migration ohne Backfill | `backend-ports-adapters` „Fallen"; `anforderung-technisch` §9 (Datenbank-Zustand) |
| 1.7 Vertragslücke | `issue-formulierung` § „Abweichungen" (neu, optional) — Abweichungen gehören ins Issue |
| 2 arc42 nachgetragen (MAJOR) | `anforderung-technisch` §7 („im selben Commit wie der Code"); `anforderung-gate` Runde 3 |
| 2 Labels > 60 Zeichen still verworfen | `backend-ports-adapters` Regel 9 („Kein stilles Verwerfen"); `feature-testing` „Stilles Verwerfen ist ein Fehlerfall"; `anforderung-fachlich` §7 |
| 2 Start-Route-Guard ohne Test | `feature-testing` Testorte (Navigation/Guards) — Issue #3 |
| 2 `core`-Dienst wird pro Komponente erzeugt (`UserSearch`) | `frontend-feature-architecture` „`core` heißt: genau eine Instanz" — erledigt in Issue #4: `UserSearch` ist jetzt `providedIn: 'root'` (eine Instanz), der Zustand ist geteilt und die Reset-Regel steht im Skill und an der Datei |
| 3 Angular-22-Testfallen | `feature-testing` „Die Angular-22-Fallen" (Tabelle) |
| 4 „Muster nicht nachbauen" | `frontend-feature-architecture` „Fallen"; `ux-design` §3a |
| 5 Erkenntnisse festhalten | `AGENTS.md` Definition of Done Punkt 6 „Nachbereitung"; `anforderung-loop` „Erkenntnisse werden zu Regeln, nicht nur zu Text" |

**Zusätzlich korrigiert:** `anforderung-technisch` beschrieb das Frontend noch
mit Bootstrap 5, FontAwesome, Jasmine, `MockMvcBuilders.standaloneSetup` und
den Pfaden `domain/<feature>/features/<name>/` sowie `endpoint/model/…Dto.java` —
das Projekt ist aber Angular 22 (zoneless, Signals, Signal Forms, Vitest) mit
`features/<feature>/` und Records in `adapters/inbound/rest/dto/`. Solche
falschen Annahmen in den Skills sind die Ursache dafür, dass ein Model die
Architektur des Projekts verfehlt — sie sind jetzt korrigiert, ebenso die
veralteten Pfadkonventionen in `anforderung-gate`.

