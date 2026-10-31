# 2. Randbedingungen

## Technische Randbedingungen

| Bereich | Festlegung | Quelle |
|---------|------------|--------|
| Sprache Backend | Java **25** | `backend/pom.xml` |
| Framework Backend | Spring Boot **4.1.2** (Spring Framework 7, Jakarta EE 11) | `backend/pom.xml` |
| Build Backend | Maven 3.9 (Wrapper im Repo), Java-25-Container für Builds | `backend/mvnw`, `backend/Dockerfile` |
| Persistenz | JPA (Hibernate 7), PostgreSQL **17** | `backend/pom.xml`, `compose.yml` |
| Migrationen | Flyway 12, `V<n>__<beschreibung>.sql` | `backend/src/main/resources/db/migration` |
| Sprache Frontend | TypeScript **6** | `frontend/package.json` |
| Framework Frontend | Angular **22** (standalone, zoneless) | `frontend/package.json` |
| Testwerkzeuge | JUnit 5 + Mockito + Testcontainers (Backend), Vitest (Frontend) | `backend/pom.xml`, `frontend/package.json` |
| Abhängigkeit Backend | `org.mnode.ical4j:ical4j` **4.4.0** — iCalendar-Auswertung, ausschließlich im ausgehenden Adapter | `backend/pom.xml`, [ADR 0006](../docs/adr/0006-ical4j-fuer-die-ics-auswertung.md) |
| Architekturprüfung | ESLint + **Sheriff** (Modulgrenzen) | `frontend/sheriff.config.ts` |
| Laufzeitumgebung | Docker / Docker Compose | `compose.yml` |
| Node-Version | ≥ 22.22.3 (24.15+ / 26+) — Angular-CLI-Anforderung | `frontend/README.md` |

## Organisatorische Randbedingungen

- **Issue-first**: Anforderungen entstehen als GitHub-Issue im Repo
  `Sadeeg/HouseholdManager`, nicht als Chat-Durchsage.
- **Gates**: Ein Feature gilt erst als fertig, wenn Architektur-Review
  (`OK`) **und** Abnahme (`PASS`) vorliegen — siehe `AGENTS.md`.
- **Branch-Workflow**: Ein Feature = ein Branch = ein Issue = ein PR.
  Committen erst nach bestandenen Gates, Push nur bei grünem Stand,
  `main` erhält ausschließlich gemergte PRs.
- **Sprache**: Issues und Dokumente deutsch, Identifier englisch.
- **Kontext-Disziplin**: Die Entwicklung läuft mit einem kleinen lokalen
  Modell und 256k Kontext. Kurze, präzise Arbeitsschritte sind Pflicht
  (`AGENTS.md` → Kontext-Disziplin).
- **Kein Direct-Push auf `main`**, ein Issue = ein Branch.

## Technische Randbedingungen (bewusste Einschränkungen)

| Einschränkung | Folge |
|---------------|-------|
| Noch kein Spring Security für Rollen | Authentifizierung ist da (Nextcloud/OIDC), **Autorisierung** je Haushalt fehlt — vor Produktivbetrieb zu klären (siehe 11) |
| CI vorhanden | Gates laufen in GitHub Actions bei jedem PR (`.github/workflows/ci.yml`) — zusätzlich lokal |
| Noch kein E2E-Setup (Playwright) | Journeys werden über Backend-Integrationstests abgedeckt |
| `ddl-auto: validate` | Schema-Änderungen brauchen zwingend eine Flyway-Migration |

> **Offen:** CI-Pipeline, Authentifizierungskonzept, E2E-Strategie.
