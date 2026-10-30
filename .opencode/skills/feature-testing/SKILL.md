---
name: Feature-Testing
description: Use when writing, reviewing or extending the tests of a feature in HouseholdManager. Defines which test belongs on which layer (domain, use case, inbound/outbound adapter, UI), the commands to run them, what "vollständig getestet" means (every acceptance criterion needs a proving test), and the anti-patterns to avoid. Load this before writing tests or when the critical tester reports gaps.
---

# Feature-Testing

Teststrategie für HouseholdManager. Das Ziel ist nicht möglichst viele
Tests, sondern **Beleg**: jedes Akzeptanzkriterium (AC) braucht einen Test,
der das Verhalten zeigt — und zwar auf der **tiefsten** Ebene, die den
Fehler noch findet.

Verwandte Skills: `spring-testing` (Backend-Test-Slices), `vitest`
(Frontend-Testwerkzeug), `angular-developer` (Test-Referenzen),
`backend-ports-adapters` / `frontend-feature-architecture` (Schichtregeln).

## Die Regel

```text
1. Teste das Verhalten, nie den Mock.
2. Jeder Test gehört auf die tiefste Ebene, die den Fehler findet.
3. Ein Test, der aus dem falschen Grund grün ist, ist schlimmer als keiner.
4. Echte Systeme (DB, HTTP) sperren den Merge — Mocks isolieren, sie bestätigen nicht.
5. Coverage ist eine Taschenlampe, kein Ziel.
6. Keine Test-Hooks, Test-Flags oder Test-only-Methoden im Produktionscode.
```

(Quelle: „Testing Boss", Pedro Nauck — https://github.com/pedronauck/skills)

## Welche Ebene für welchen Fehler

| Ebene | Test | Werkzeug | Findet |
|-------|------|----------|--------|
| Fachregel | `domain`/`usecase` | JUnit + Mockito, **ohne Spring** | Logikfehler, Invarianten |
| Protokoll | `adapters/inbound/rest` | `@WebMvcTest` + `@MockitoBean` | Mapping, Statuscodes, Validierung |
| Persistenz | `adapters/outbound/persistence` | `@DataJpaTest` + Testcontainers-Postgres | Queries, Mapping, Migration |
| UI | `features/<f>/ui`, `<f>-page` | Vitest + TestBed + `provideHttpClientTesting()` | Rendering, Reactiveität, Events |
| Navigation | `core/**/*guard.spec.ts` | Vitest + `provideRouter([])` | Weiterleitung, Scoping, Start-Route |
| Journey | Ende zu Ende | `@SpringBootTest` / Playwright (noch nicht eingerichtet) | Schnittstellen-Fehler |

Faustregel: **ein Fehler wird auf der tiefsten Ebene gesucht, die ihn
noch zuverlässig findet.** Ein Test auf der höchsten Ebene ist teuer und
liefert schlechte Fehlermeldungen.

### Konkret: welche Testklasse wofür

```text
backend/src/test/java/de/householdmanager/
  domain/model/*Test.java                       reine Regeltests, keine Mocks
  application/usecase/*UseCaseImplTest.java    Use Case gegen gemockten Port
  adapters/inbound/rest/*ControllerTest.java    @WebMvcTest, Port gemockt
  adapters/outbound/persistence/*AdapterTest.java  @DataJpaTest + Testcontainers

frontend/src/app/
  features/<f>/ui/*.spec.ts                     dumb Component: Inputs → Output
  features/<f>/*.spec.ts                        smart Component: Datenfluss
  core/**/*.guard.spec.ts                       Route-Guards: Weiterleitung
```

Package-Pfade in Boot 4: `@WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure`,
`@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure`.

## Was „vollständig getestet" heißt

Ein Feature ist vollständig getestet, wenn **alle vier** Punkte erfüllt sind:

1. **Abdeckung:** Jedes `AC<n>` aus dem Issue hat mindestens einen Test, der
   es belegt. Die Zuordnung steht in der Abdeckungsmatrix (siehe
   `## Tests` im Issue bzw. im Abnahme-Bericht).
2. **Ebenen:** Regel → Use Case → Adapter → UI. Fehlt eine Ebene, weil der
   Fehler dort nicht auftreten kann, ist das im Bericht zu begründen.
3. **Fehlerpfade:** Validierung, leere Eingabe, Grenzwerte, Abbruch und
   Backend-Fehler sind getestet — nicht nur der Erfolgsfall.
4. **Grün:** Alle Tests laufen grün (`mvn test`, `npm test`) — und die
   Architektur-Checks bleiben grün (`npm run lint`, `npm run sheriff`).

Zusätzlich gilt: **Kein Test darf nur prüfen, dass ein Mock aufgerufen
wurde**, wenn das Verhalten beobachtbar ist. Und: kein Test ohne AC-Bezug,
außer er deckt eine Invariante oder einen Fehlerpfad ab (dann mit
Kommentar, worauf er zielt).

## Kommandos

```bash
# Backend (Adapter-Test braucht Docker für Testcontainers)
cd backend && ./mvnw -B test > /tmp/backend-test.log 2>&1
grep -E "Tests run:|BUILD|ERROR" /tmp/backend-test.log | tail -20

# Nur eine Testklasse
cd backend && ./mvnw -B test -Dtest=HouseholdUseCaseImplTest

# Frontend
cd frontend
npm test                                    # Vitest, alle Tests
npm test -- households-page.spec.ts         # eine Datei
npm run lint && npm run sheriff             # Architektur-Grenzen
```

## Muster

**Use Case (ohne Spring):**

```java
@ExtendWith(MockitoExtension.class)
class HouseholdUseCaseImplTest {
    @Mock private HouseholdRepository householdRepository;
    private HouseholdUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new HouseholdUseCaseImpl(householdRepository, Clock.fixed(FIXED_NOW, UTC));
    }
}
```

- `Clock` als feste Uhr → Zeit ist **deterministisch** und der Test
  beschreibt sie explizit.
- Der Port wird gemockt, **nicht** die Entity.

**Controller-Slice:**

```java
@WebMvcTest(HouseholdController.class)
class HouseholdControllerTest {
    @MockitoBean private HouseholdUseCase householdUseCase;   // der Port, nicht die Impl
}
```

**Adapter gegen echte DB:**

```java
@DataJpaTest
@Import(HouseholdRepositoryAdapter.class)   // @DataJpaTest lädt keine @Repository-Beans
@ImportTestcontainers
class HouseholdRepositoryAdapterTest { @Container @ServiceConnection static PostgreSQLContainer postgres = ...; }
```

**Frontend-Komponente:**

```ts
TestBed.configureTestingModule({
  imports: [HouseholdsPage],
  providers: [provideHttpClient(), provideHttpClientTesting(), HouseholdsData],
});
```

- Zoneless: **`fixture.detectChanges()` vor `httpMock.expectOne(...)`** —
  sonst wird der Request nie erwartbar.
- Route-scoped Services (z. B. `HouseholdsData`) gehören in `providers`.

## Die Angular-22-Fallen (zoneless, Signals, Vitest)

Diese Fallen kosten bei jedem neuen Frontend-Test Zeit. Sie sind hier
gesammelt, damit der Test nicht daran scheitert:

| Falle | Folge | Richtig |
|-------|-------|---------|
| Effekte laufen erst nach `detectChanges()` | `expectOne` findet keinen Request | erst rendern, **dann** erwarten |
| Nach `flush()` folgt ein Reload | `whenStable()` wartet auf den **nicht** geflushzten Request und hängt | Makrotask: `await new Promise(r => setTimeout(r, 0)); fixture.detectChanges()` |
| `httpResource` dedupliziert | der zweite GET geht nie raus | `.reload()` — es gibt **kein** `.refresh()` |
| `error()` liefert `undefined` | `!== null` ist immer wahr | `!!x.error()` prüfen |
| Signal-Forms-Knoten | `required`/`maxlength` im Template greifen nicht | Validatoren `required()`, `maxLength()` im Code |
| `type="submit"` | löst im Test kein Submit aus | Handler über `type="button"` aufrufen |
| `TestBed.configureTestingModule` | zweiter Aufruf wirft | einmal pro Test konfigurieren |

## Drei Regeln, die in diesem Projekt Fehler gefunden haben

1. **Zwischenstand vor der Antwort prüfen.** Bei Interaktionen, die eine
   Serverantwort abwarten (Verschieben, Sortieren), prüft der Test den
   neuen Zustand **vor** dem Flush. Ein Test, der erst nach dem Reload
   zählt, ist blind für genau den Fehler, den er verhindern soll.
2. **Layout ist kein Testfall.** jsdom kennt keine Geometrie — Position,
   Überlappung, Verdeckung, Ausrichtung sind nur per **Sichtprüfung**
   (Screenshot des echten SCSS) zu prüfen. Siehe Skill `ux-design`,
   „Sichtprüfung".
3. **Stilles Verwerfen ist ein Fehlerfall.** Wird ein Wert außerhalb einer
   Grenze ohne Meldung an den Nutzer fallengelassen, ist das ein Defekt:
   Der Grenzwert braucht einen Test auf der Ebene, die die Grenze setzt
   (Fachmodell oder Use Case), und die Oberfläche braucht eine Rückmeldung.

## Anti-Patterns

| Anti-Pattern | Warum schlecht | stattdessen |
|---|---|---|
| Test prüft nur `verify(mock).called()` | beweist nichts über Verhalten | beobachtbares Ergebnis prüfen |
| Snapshot des kompletten HTML | unlesbar, bricht bei jedem Umbau | gezielte Aussagen auf Elemente |
| Test prüft die Implementierung | bricht bei Refactoring | Verhalten/Vertrag prüfen |
| Alles über die HTTP-Schicht | langsam, Fehlerpfad unklar | Regel auf Use-Case-Ebene testen |
| `@Disabled`/`@Ignore` als „später" | verrottet still | Issue anlegen, Test läuft jetzt |
| Test-Hook im Produktionscode (z. B. `if (test) …`) | Produktionscode verändert sich für Tests | Testdaten über den Port steuern |
| Test ohne AC-Bezug | unklar, wofür er da ist | AC nennen oder Zweck im Kommentar |
| Mock, der echte Validierung ersetzt | Fehler bleibt unentdeckt | echte Validierung testen |
| Test prüft erst **nach** dem Reload | blind für Flackern und Rückfall | Zwischenstand vor der Antwort prüfen |
| Wert wird still verworfen (Grenzwert) | Nutzer merkt nichts, Daten fehlen | ablehnen **und** melden, Grenzwert testen |
| Layout per Unit-Test „geprüft" | jsdom sieht keine Geometrie | Sichtprüfung (Screenshot) |

## Grenzen (bewusst)

- **Keine Mutation-Testing- oder Coverage-Schwellen** als Gate: bei 256k
  Kontext und kleinem Modell sind die teuer als der Nutzen. Stattdessen:
  die Abdeckungsmatrix und der kritische Tester.
- **Kein E2E-Setup** (Playwright) — es fehlt noch. Bis dahin wird die
  Journey über `@SpringBootTest` abgedeckt, sobald ein Epic es verlangt.
- **Kein Flaky-Test-Toleranz-Korb**: Ein roter Test ist ein Befund, kein
  Zufall. „Flaky" ist eine Behauptung, die belegt werden muss.

## Vor dem Abschluss

1. Matrix bauen (AC → Test).
2. Lücken mit Tests schließen (nur Testdateien).
3. Alle Tests + Lint + Sheriff laufen lassen.
4. **Bei UI-Anteil: den manuellen Pfad einmal im Browser durchspielen.**
   Gibt es einen Einstieg? Ist der Stack erreichbar? Sind Daten vorhanden?
   Sind alle Aktionen sichtbar? Ein AC ohne Einstieg ist nicht erfüllt,
   nur behauptet.
5. **Bei UI-Anteil: Sichtprüfung** (Screenshot des echten SCSS) für die
   Ansichten, die geändert wurden — Layout ist nicht testbar.
6. **Den kritischen Tester laufen lassen** (`@kritischer-tester` oder
   `/abnahme`) — er urteilt, nicht du.
