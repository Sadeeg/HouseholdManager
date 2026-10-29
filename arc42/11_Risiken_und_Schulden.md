# 11. Risiken und technische Schulden

## Risiken

| Nr | Risiko | Auswirkung | Gegenmaßnahme | Status |
|----|--------|------------|---------------|--------|
| R1 | **Keine Authentifizierung/Autorisierung** — die API ist ungeschützt | Kritisch für den Produktivbetrieb | Security-Konzept entscheiden (ADR), `spring-boot-starter-security` ergänzen | **offen — vorrangig** |
| R2 | Kein Mandanten-Cut (ein Nutzer sieht alle Haushalte) | Datenleck bei Mehrhaushalten | Context-Schnitt + Autorisierung je Haushalt | offen |
| R3 | Kleines lokales Modell (27B, 256k) | Fehler in der Umsetzung, vergessene Regeln | Gates (Review + Abnahme), Kontext-Disziplin, Handover-Datei | aktiv |
| R4 | ~~Keine CI~~ — **gemindert**: CI prüft bei jedem PR Build, Tests, Lint, Modulgrenzen und den Compose-Stack | Ein Feature kann ohne grünes CI nicht gemergt werden; der menschliche Merge bleibt nötig | GitHub Actions (`.github/workflows/ci.yml`), Branch-Schutz auf `main` | **aktiv** |
| R5 | Kein E2E-Test (Playwright) | Schnittstellenfehler zwischen SPA und API werden spät entdeckt | Integrationstest im Backend ergänzen; E2E mit dem ersten UI-Feature | offen |
| R6 | `ddl-auto: validate` + manuelle Migrationen | Änderung ohne Migration → Startabbruch (laut, aber unbequem) | Flyway-Regel in AGENTS.md, Reviewer prüft Migrationen | aktiv |
| R7 | Nur ein Entwickler (mit Agenten) | Wissensverlust bei Context-Verlust | arc42, ADRs, Issues, Skills als Wissensstand | aktiv |
| R8 | Spring Boot 4 / Angular 22 sind sehr jung | Schnelle API-Änderungen, seltene Upstream-Themen | Boot-4-Fallen in `AGENTS.md` dokumentiert; Skills vendort | aktiv |

## Technische Schulden

| Nr | Schuld | Auswirkung | Auflösung |
|----|--------|------------|-----------|
| S1 | `HouseholdsData.create()` wird nicht aufgerufen — kein Anlegen im UI | Halber vertikaler Schnitt | Nächstes Feature: Formular (Signal Forms) |
| S2 | `Household.withId()` wird nur im Test benutzt | API-Rest | Beim ersten echten Update-Feature prüfen |
| S3 | Kein `equals`/`hashCode` am Fachmodell | Vergleiche nur über Identität | Nur relevant, wenn Domänenwerte verglichen werden |
| S4 | Release-Ablauf nicht definiert | Kein reproduzierbares Deployment | `AGENTS.md` → Release ergänzen |
| S5 | `MAX_NAME_LENGTH` wird auch von der Migration gespiegelt (VARCHAR(120) als Literal) | Drift möglich, wenn der Wert sich ändert | Bei einer Änderung: neue Migration statt Editieren |
| S6 | `compose.yml` definiert nur `postgres` — Backend und Frontend laufen manuell über ihre Dockerfiles | Der Ziel-Stack aus 07 ist nicht als Compose-Stack reproduzierbar | Compose-Dienste `backend` und `frontend` ergänzen (`frontend/nginx.conf` erwartet den Dienst `backend`) |

> **Geprüft und nicht mehr offen:** die zentralisierte Längengrenze
> (`Household.MAX_NAME_LENGTH`, referenziert von Use Case und JPA-Entity), das
> unveränderte `createdAt` beim Update (`HouseholdRepositoryAdapter.save`,
> abgesichert durch `HouseholdRepositoryAdapterTest`) und die Fach-Exception
> `InvalidHouseholdNameException` statt einer pauschalen
> `IllegalArgumentException` — umgesetzt und in 08 beschrieben.

> **Offen:** Risiken priorisieren und mit dem Auftraggeber durchgehen.
