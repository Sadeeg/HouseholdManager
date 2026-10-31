# 11. Risiken und technische Schulden

## Risiken

| Nr | Risiko | Auswirkung | Gegenmaßnahme | Status |
|----|--------|------------|---------------|--------|
| R1 | ~~Keine Authentifizierung~~ — **gemindert**: Nextcloud/OIDC ist eingerichtet (JWT-Access-Tokens, JWKS-Validierung), die API ist ohne Token 401 | Kein Zugriffsschutz für Endnutzer, wenn der Client umgangen wird | `SecurityConfig` (Ports & Adapters), CORS auf den Frontend-Origin, `.env` außerhalb des Repos | **aktiv** |
| R2 | Mandanten-Cut (ein Nutzer sah alle Haushalte) | Datenleck bei Mehrhaushalten | entworfen (Issue #1): `household_member` + Prüfung je Use Case (siehe 08) | in Arbeit |
| R3 | Kleines lokales Modell (27B, 256k) | Fehler in der Umsetzung, vergessene Regeln | Gates (Review + Abnahme), Kontext-Disziplin, Handover-Datei | aktiv |
| R4 | ~~Keine CI~~ — **gemindert**: CI prüft bei jedem PR Build, Tests, Lint, Modulgrenzen und den Compose-Stack | Ein Feature kann ohne grünes CI nicht gemergt werden; der menschliche Merge bleibt nötig | GitHub Actions (`.github/workflows/ci.yml`), Branch-Schutz auf `main` | **aktiv** |
| R5 | Kein E2E-Test (Playwright) | Schnittstellenfehler zwischen SPA und API werden spät entdeckt | Integrationstest im Backend ergänzen; E2E mit dem ersten UI-Feature | offen |
| R6 | `ddl-auto: validate` + manuelle Migrationen | Änderung ohne Migration → Startabbruch (laut, aber unbequem) | Flyway-Regel in AGENTS.md, Reviewer prüft Migrationen | aktiv |
| R7 | Nur ein Entwickler (mit Agenten) | Wissensverlust bei Context-Verlust | arc42, ADRs, Issues, Skills als Wissensstand | aktiv |
| R8 | Spring Boot 4 / Angular 22 sind sehr jung | Schnelle API-Änderungen, seltene Upstream-Themen | Boot-4-Fallen in `AGENTS.md` dokumentiert; Skills vendort | aktiv |
| R9 | Die iCalendar-Bibliothek bringt ein eigenes Objektmodell und `threeten-extra` mit; 4.x ist eine neue Hauptversion | Ein Bibliotheksupdate kann das Verhalten ändern (Package-Verschiebungen, andere Normalisierung) | Die Bibliothek ist auf den Adapter gekapselt — die Übersetzung ins Fachmodell ist getestet, die Version im `pom.xml` festgeschrieben; ein Wechsel betrifft nur `adapters/outbound/ics/` (ADR 0006) | aktiv |
| R10 | Der Tageslauf hängt an der Verfügbarkeit des Backends | Termine veralten, ohne dass es jemand merkt | Die Anzeige weist den veralteten Stand aus (R13); ein Ausfall wird nicht nachgeholt, sondern beim nächsten Lauf geflickt | aktiv |
| R11 | Die Sofort-Synchronisation bindet den HTTP-Request | Der Nutzer wartet bis zu 5 s auf den Abruf der Quelle; bei nicht erreichbarer Quelle wartet er die volle Grenze | ADR 0005 (angenommen): eigener HTTP-Timeout von 5 s, Fehlerisolierung — das Speichern bleibt erfolgreich, der Stand zeigt den veralteten Zustand | **aktiv** |
| R12 | Spätere Entsorger liefern andere Feed-Semantiken | Die Toleranzregeln (Uhrzeit → Kalendertag, `RRULE`-Expansion) decken die heute bekannten Varianten ab, nicht aber jede denkbare Semantik | Bewusste Grenze: das Feature kennt genau eine Quelle je Haushalt; eine andere Semantik ist eine eigene Entscheidung (ADR 0006) | offen |

## Technische Schulden

| Nr | Schuld | Auswirkung | Auflösung |
|----|--------|------------|-----------|
| S1 | `HouseholdsData.create()` wird nicht aufgerufen — kein Anlegen im UI | Halber vertikaler Schnitt | Nächstes Feature: Formular (Signal Forms) |
| S2 | `Household.withId()` wird nur im Test benutzt | API-Rest | Beim ersten echten Update-Feature prüfen |
| S3 | Kein `equals`/`hashCode` am Fachmodell | Vergleiche nur über Identität | Nur relevant, wenn Domänenwerte verglichen werden |
| S4 | ~~Release-Ablauf nicht definiert~~ — **gemindert**: Versions-Bump, Tag, Build, Deploy und Rollback sind festgelegt | Ohne Registry-Publish wird das Image auf dem Zielsystem gebaut | `AGENTS.md` → Release |
| S5 | `MAX_NAME_LENGTH` wird auch von der Migration gespiegelt (VARCHAR(120) als Literal) | Drift möglich, wenn der Wert sich ändert | Bei einer Änderung: neue Migration statt Editieren |
| S6 | `compose.yml` definiert nur `postgres` — Backend und Frontend laufen manuell über ihre Dockerfiles | Der Ziel-Stack aus 07 ist nicht als Compose-Stack reproduzierbar | Compose-Dienste `backend` und `frontend` ergänzen (`frontend/nginx.conf` erwartet den Dienst `backend`) |
| S7 | Die Längengrenze der ICS-Adresse (500 Zeichen) steht an zwei Stellen: im Fachmodell des Müllkalenders und als Spaltenlänge `VARCHAR(500)` am Haushalt | Drift möglich, wenn der Wert sich ändert | Wie S5: bei einer Änderung eine neue Migration statt Editieren; die Grenze wird im Use Case und im Spaltenformat genannt |
| S8 | Die Daten des Müllkalenders liegen auf zwei Tabellen: die Adresse am Haushalt, der Stand in `waste_sync_state` | Ein Umzug des Kontexts erfordert eine Migration | Bewusste Grenze: der Haushalt-Kontext trägt das Attribut, das die Haushaltsverwaltung zeigt; der Müllkalender-Kontext trägt seinen Laufzustand |
| S9 | Die Formatkenntnis steckt in einer Fremdbibliothek (`org.mnode.ical4j:ical4j` 4.4.0) | Eine neue Quelle oder ein Wechsel auf eine künftige Hauptversion ist ein Codeeingriff im Adapter | Bewusste Grenze (ADR 0006); das Verhalten des Adapters ist durch Tests abgesichert, die Version im `pom.xml` festgeschrieben |
| S10 | `threeten-extra` wird vom Boot-BOM **nicht** verwaltet; die Version kommt transitiv aus ical4j (1.8.0), auf Central existiert bereits 1.11.0 | Ein Bibliotheks-Update verschiebt die transitiv gezogene Version | Version im `pom.xml` festlegen (1.8.0 wie von ical4j getestet — oder 1.11.0); Entscheidung in Runde 3 (ADR 0006) |

> **Geprüft und nicht mehr offen:** die zentralisierte Längengrenze
> (`Household.MAX_NAME_LENGTH`, referenziert von Use Case und JPA-Entity), das
> unveränderte `createdAt` beim Update (`HouseholdRepositoryAdapter.save`,
> abgesichert durch `HouseholdRepositoryAdapterTest`) und die Fach-Exception
> `InvalidHouseholdNameException` statt einer pauschalen
> `IllegalArgumentException` — umgesetzt und in 08 beschrieben.

> **Architekturpunkte aus Issue #1 — Stand 2026-10-29:**
> 1. Ersteller nicht persistiert — **entschieden (A14):** `created_by`
>    NOT NULL, frische Dev-DB, kein Backfill (siehe 05, Migration V2).
> 2. Kartenreihenfolge je Spalte — **entschieden (A15):** dauerhaftes
>    `position`-Feld im Task-Datenmodell (siehe 05, 06).
> 3. **Offen:** Name der Zugewiesenen: Live-Auflösung über `UserDirectory` je
>    Request (Verzeichnisabhängigkeit) oder Namens-Snapshot bei Zuweisung —
>    Entscheidung in Runde 3 (siehe 05).
> 4. **Offen:** Markdown-Renderer `shared` vs. `features/tasks/ui` —
>    Entscheidung in Runde 3 (siehe 05).

> **Offen:** Risiken priorisieren und mit dem Auftraggeber durchgehen.
