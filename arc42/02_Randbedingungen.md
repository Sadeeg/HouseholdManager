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
| Architekturprüfung | ESLint + **Sheriff** (Modulgrenzen) | `frontend/sheriff.config.ts` |
| Laufzeitumgebung | Docker / Docker Compose | `compose.yml` |
| Node-Version | ≥ 22.22.3 (24.15+ / 26+) — Angular-CLI-Anforderung | `frontend/README.md` |

## Organisatorische Randbedingungen

- **Issue-first**: Anforderungen entstehen als GitHub-Issue im Repo
  `Sadeeg/HouseholdManager`, nicht als Chat-Durchsage.
- **Gates**: Ein Feature gilt erst als fertig, wenn Architektur-Review
  (`OK`) **und** Abnahme (`PASS`) vorliegen — siehe `AGENTS.md`.
- **Sprache**: Issues und Dokumente deutsch, Identifier englisch.
- **Kontext-Disziplin**: Die Entwicklung läuft mit einem kleinen lokalen
  Modell und 256k Kontext. Kurze, präzise Arbeitsschritte sind Pflicht
  (`AGENTS.md` → Kontext-Disziplin).
- **Kein Direct-Push auf `main`**, ein Issue = ein Branch.

## Technische Randbedingungen (bewusste Einschränkungen)

| Einschränkung | Folge |
|---------------|-------|
| Noch kein Spring Security | Keine Authentifizierung/Autorisierung — vor Produktivbetrieb zu klären (siehe 11) |
| Noch kein CI | Gates laufen lokal (`/review`, `/abnahme`) |
| Noch kein E2E-Setup (Playwright) | Journeys werden über Backend-Integrationstests abgedeckt |
| `ddl-auto: validate` | Schema-Änderungen brauchen zwingend eine Flyway-Migration |

> **Offen:** CI-Pipeline, Authentifizierungskonzept, E2E-Strategie.
