# ADR 0001 — Backend: Ports-and-Adapters-Architektur

- **Status:** angenommen
- **Datum:** 2026-10-29
- **Entscheidung:** Das Backend von HouseholdManager wird als **Ports-and-Adapters-Architektur** (hexagonale Architektur) aufgebaut.

## Kontext

Das Backend ist ein Spring-Boot-Service, der Fachlogik und Persistenz
bisher in einer Schicht vermischte (`service` + JPA-Entity direkt im
Controller). Mit wachsender Fachlogik wird die Domain schwer testbar
(jeder Test braucht eine Datenbank) und Änderungen an der Persistenz
reißen Fachlogik mit.

Die Architektur soll:

- Fachlogik von Spring, JPA und HTTP trennen,
- den Austausch der Persistenz (Postgres → etwas anderes) ohne Eingriff in
  die Use Cases erlauben,
- Use Cases ohne Spring-Kontext testbar machen.

Referenz für die Methodik: Cockburns Ports & Adapters. Die
projektspezifische Fassung (Konzept, Pakete, Tests je Schicht, Fallen) steht
im Skill `backend-ports-adapters`. Ein früher vendorter allgemeiner Skill
`hexagonal-architecture` wurde entfernt — er enthielt keine projektspezifische
Regel, die nicht dort oder in `AGENTS.md` steht.

## Entscheidung

Drei Ringe, eine Abhängigkeitsrichtung:

```text
de.householdmanager
├── domain/                      # Fachkern, keine Framework-Imports
│   └── model/
├── application/                 # Use Cases + Ports
│   ├── port/in/                 # Inbound Ports
│   ├── port/out/                # Outbound Ports
│   └── usecase/
├── adapters/                    # Technik
│   ├── inbound/rest/            # HTTP
│   └── outbound/persistence/    # JPA
└── config/                      # Composition Root
```

- **domain** importiert nichts aus Spring, JPA, Jackson oder Hibernate.
- **application** importiert nur `domain` und die eigenen Port-Interfaces.
  Ausnahme sind Spring-**Annotationen** (`@Service`, `@Transactional`) in
  `application/usecase` — sie sind Transaktions- und Verdrahtungsdetail,
  keine Technologiebindung. HTTP- und JPA-Typen bleiben verboten.
- **adapters** implementieren die Ports und übersetzen an der Grenze
  (HTTP ↔ Fachmodell, Persistenz ↔ Fachmodell).
- **Transaktionen** liegen an der Use-Case-Grenze, nicht im Adapter.
- **Fehlerübersetzung** (Fachfehler → HTTP) liegt im eingehenden Adapter.
  Nur **Fachfehler** werden zu 4xx; eine allgemeine
  `IllegalArgumentException` bleibt ein Programmierfehler (5xx).

## Konsequenzen

**Positiv**

- Use Cases sind ohne Spring und ohne Datenbank testbar
  (`HouseholdUseCaseImplTest` läuft als reiner JUnit-Test).
- Persistenzdetails sind austauschbar: ein Adapter für ein anderes Backend
  ändert nichts an `application` oder `domain`.
- Fehler und Validierung liegen an genau zwei Stellen: an der Grenze
  (`@Valid`) und in der Invariante (Use Case).

**Negativ / Kosten**

- Mehr Klassen: Fachmodell und Persistenzmodell sind getrennt (Mapping im
  Adapter).
- Ein zusätzlicher Testlauf mit Testcontainers-Postgres für den
  Adapter-Test (benötigt Docker).
- Fachmodell und DTO sind nicht dasselbe — das ist gewollt, aber es muss
  bewusst gepflegt werden.

**Neutral**

- Spring bleibt der Wiring-Mechanismus (Composition Root in `config/`), die
  Architekturentscheidung ist aber die Abhängigkeitsrichtung, nicht das
  Framework.
