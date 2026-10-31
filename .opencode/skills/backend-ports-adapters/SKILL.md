---
name: Backend-Ports-Adapters
description: Use for every change in the Spring Boot backend of HouseholdManager. Encodes the project's Ports-and-Adapters (hexagonal) architecture after Manfred Steyer - package layout, the dependency rule (domain knows nobody, application knows only ports, adapters know the ports), where DTOs and mapping live, where transactions belong, and how each layer is tested. Load this when adding or changing a use case, a REST endpoint, a repository, an entity or a migration.
---

# Backend — Ports & Adapters

Das Backend von HouseholdManager ist eine **Ports-and-Adapters-Architektur**
(hexagonale Architektur, Alistair Cockburn). Der Fachkern ist die Mitte,
alles andere ist austauschbar.

Verbindliche Quelle: `AGENTS.md`. Allgemeine Methodik:
`hexagonal-architecture` (vendort).

## Die drei Schichten

```text
de.householdmanager
├── domain/                      # FACHKERN — regelreich, keine Frameworks
│   └── model/                   #   Entities, Value Objects, Invarianten
│
├── application/                 # ANWENDUNG — Use Cases,_ports
│   ├── port/
│   │   ├── in/                  #   Inbound Ports: was kann die Anwendung?
│   │   └── out/                 #   Outbound Ports: was braucht die Anwendung?
│   └── usecase/                 #   Use-Case-Implementierungen
│
├── adapters/                    # ADAPTER — Technik, austauschbar
│   ├── inbound/rest/            #   HTTP (Controller, DTOs, Fehlerübersetzung)
│   └── outbound/persistence/    #   JPA (Entity, Spring-Data-Repo, Mapping)
│
└── config/                      # Composition Root: Verdrahtung
```

## Die Abhängigkeitsregel

```text
adapters ──► application ──► domain
    └──────────────────────────►   (Adapter kennen die Ports, nicht umgekehrt)
```

- **domain** importiert **nichts** aus Spring, JPA, Jackson oder Hibernate.
  Keine Annotation, kein Framework-Import. Ausnahme: Lombok (reines
  Code-Generierungs-Werkzeug, kein Framework).
- **application** importiert nur `domain` und die eigenen `port`-Interfaces.
  Erlaubt sind Spring-**Annotationen** (`@Service`, `@Transactional`) in
  `application/usecase` — Verdrahtung und Transaktion, keine Technologie-
  bindung. Verboten bleiben JPA-, HTTP- und Jackson-Typen.
- **adapters** importieren `application.port.*` und `domain`, niemals
  `application.usecase.*` direkt (außer der eigene Adapter).
- **config** ist der einzige Ort, an dem Beans verdrahtet werden.

## Wo liegt was

| Thema | Ort | Nicht dort |
|-------|-----|-----------|
| Fachregel, Invariante | `domain/model` | Controller, Entity |
| Use Case (Orchestrierung) | `application/usecase` | Controller |
| Was die Anwendung kann | `application/port/in` | — |
| Was die Anwendung braucht | `application/port/out` | `adapters` |
| HTTP-Pfad, Statuscodes, JSON | `adapters/inbound/rest` | `application`, `domain` |
| Request-/Response-DTOs | `adapters/inbound/rest/dto/` (Records) | verschachtelt im Controller |
| Fehlerübersetzung in HTTP | `adapters/inbound/rest/GlobalExceptionHandler` | `domain` |
| Tabellen, Spalten, JPA-Mapping | `adapters/outbound/persistence` | `domain` |
| Übersetzung Persistenz ↔ Fachmodell | `adapters/outbound/persistence/*Adapter` | — |
| Transaktionen | Use Case / Service (`@Transactional`) | Controller, Adapter |
| Konfiguration, Beans | `config` | überall sonst |

## Regeln

1. **Ein eingehender Adapter pro Protokoll.** REST ist einer; ein
   Kafka-Listener oder CLI käme als weiterer dazu und spräche denselben
   Inbound Port an.
2. **Jede externe Abhängigkeit ist ein Port.** Datenbank (`HouseholdRepository`),
   Uhr (`Clock`), später: Dateisystem, Messaging, HTTP-Calls.
3. **Keine Framework-Typen in der Anwendung.** Kein `ProblemDetail`, kein
   `HttpStatus`, kein `JpaRepository` in `application` oder `domain`.
4. **Übersetzung liegt am Rand.** Persistenzmodell ↔ Fachmodell und
   HTTP-DTO ↔ Fachmodell werden in den Adaptern übersetzt. Das Fachmodell hat
   genau eine Form.
5. **Validierung an der Grenze** (eingehender Adapter: `@Valid` auf dem
   Request) **und** in der Invariante (Use Case bzw. Fachmodell). Der
   eingehende Adapter schützt die API, das Modell schützt die Fachregel.
6. **Transaktionen an der Use-Case-Grenze**, nicht im Adapter:
   `@Transactional(readOnly = true)` fürs Lesen.
7. **Der Composition Root bleibt sichtbar.** Alle Verdrahtung steht in
   `config/`; ein Adapter wird nie per `@Autowired` in einen anderen Adapter
   injiziert.
8. **Keine Änderung bestehender Flyway-Migrationen.** Neue Migration,
   neue V-Nummer. Bei `ddl-auto: validate` muss die Entity zur Migration
   passen, sonst startet die App nicht.
9. **Kein stilles Verwerfen.** Fällt ein Wert außerhalb einer Grenze weg,
   muss der Nutzer das erfahren: die Grenze wird **validiert** (mit
   Meldung), nicht stillschweigend angewendet. „Label länger als 60 Zeichen
   wird ignoriert" ist ein Fehlerfall, keine Normalisierung — der Wert
   verschwindet sonst ohne jede Spur.
   **Ebenso: ein technischer Ausfall wird nicht in ein fachlich leeres
   Ergebnis umgewandelt.** Eine leere Liste bedeutet „es gibt keine
   Treffer" — sie darf nicht auch „ich konnte niemanden fragen" bedeuten.
   Der ausgehende Adapter meldet den Ausfall als Fachfehler (mit dem
   konkreten Grund in der Meldung, ohne interne URLs), der eingehende
   übersetzt ihn in einen eigenen Status (503 bei Nextcloud, nicht 200 mit
   leerer Liste).
10. **DTOs sind Records in `adapters/inbound/rest/dto/`.** Kein DTO als
    verschachteltes Record im Controller, kein DTO im Controller-Ordner
    ohne `dto/`.
11. **Ausgehende Abrufe prüfen ihr Ziel.** Eine ausgehende HTTP-Quelle prüft
    vor dem Abruf die **aufgelöste** Adresse: interne Ziele (Loopback,
    private Netze, Link-Local, unique local) werden nicht abgerufen. Eine
    Prüfung des eingetippten Textes genügt nicht — ein Hostname kann auf
    eine interne Adresse zeigen. Die fachliche Grenze einer Adresse (was
    darf gespeichert werden) liegt im Fachkern, die Auflösung (was wird
    erreicht) im Adapter. Jede Technik, die der Adapter für eine
    Umgebungsabhängigkeit braucht (z. B. die Namensauflösung), kommt hinter
    eine kleine Schnittstelle — sonst wird der Test unzuverlässig.

## Beispiel: ein Feature durch die Schichten

```text
domain/model/Household            Fachmodell, keine Annotationen
application/port/out/HouseholdRepository     „ich kann Haushalte speichern“
application/port/in/HouseholdUseCase        „ich kann Haushalte listen/anlegen“
application/usecase/HouseholdUseCaseImpl    Orchestrierung + Invariante
adapters/inbound/rest/HouseholdController   HTTP → Use Case → HTTP
adapters/outbound/persistence/HouseholdRepositoryAdapter   Port → JPA
```

Ein neues Feld durchläuft exakt diese Kette: `Household` (domain) →
`HouseholdJpaEntity` + Migration (adapter) → `HouseholdRequest`/
`HouseholdResponse` (REST) → Mapping im Adapter. Fehlt eine Stelle, ist die
Architektur verletzt.

## Tests — je Schicht ihr Werkzeug

| Schicht | Test | Werkzeug |
|---------|------|----------|
| `domain` | reine Regeltests | JUnit, keine Mocks |
| `application` | Use Case gegen gemockten Port | JUnit + Mockito, **ohne Spring** |
| `adapters/inbound` | Protokoll-Mapping | `@WebMvcTest` + `@MockitoBean` (Port mocken) |
| `adapters/outbound` | Persistenz & Migration | `@DataJpaTest` + Testcontainers-Postgres |
| Ende zu Ende | Use Journey | `@SpringBootTest` ( sparsam einsetzen ) |

```text
Package: org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
Package: org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
```

Der Testcontainers-Test braucht Docker — der Maven-Build im Container
braucht deshalb zusätzlich den Socket:
`-v /var/run/docker.sock:/var/run/docker.sock`.

## Fallen

- **`@MockBean` ist tot** — in Boot 4 `@MockitoBean` verwenden.
- **`@DataJpaTest` braucht eine echte Datenbank**: Ohne H2 auf dem Classpath
  schlägt der Test fehl; hier kommt Postgres per Testcontainers.
- **Lombok im `domain`**: `@Getter`/`@Builder` sind unkritisch, aber **kein**
  `@EqualsAndHashCode`/`@Data` auf einer JPA-Entity (Proxy-Problem) — das
  Persistenzmodell hat eigene Annotationen.
- **Transaktionen**: `save()` im Adapter läuft ohne eigene Transaktion — die
  Use Case ist die Transaktionsgrenze.
- **`open-in-view: false`** — die Entity darf den Adapter nicht verlassen;
  der Adapter liefert das Fachmodell zurück.
- **Eine Migration mit `NOT NULL` ohne Backfill setzt einen frischen
  Datenbestand voraus.** Das ist keine Fußnote, sondern eine
  Startvoraussetzung: ohne neues DB-Volume startet die App nicht. Der
  Neuaufbau steht im Issue (Verify-Block) und in `README.md`
  (Abschnitt „Schnellstart") — die Entscheidung „Bestand wird neu
  aufgebaut" steht dort.
- **Datenbank-Zustand:** `ddl-auto: validate` kennt keine
  nachgelagerte Migration — jede Entity-Änderung braucht ihre `V`-Datei
  im selben Commit.
