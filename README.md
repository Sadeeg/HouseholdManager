# HouseholdManager

![CI](https://github.com/Sadeeg/HouseholdManager/actions/workflows/ci.yml/badge.svg)

Haushaltsverwaltung: Mitglieder, Aufgaben, Ausgaben und Termine in einem
Haushalt.

| Teil | Stack | Pfad |
|------|-------|------|
| Backend | Spring Boot 4.1 (Spring Framework 7), Java 25, JPA, Flyway, PostgreSQL 17 | `backend/` |
| Frontend | Angular 22 (zoneless, Signals, Vitest) | `frontend/` |
| Infrastruktur | Docker Compose, nginx, Dockerfiles | `compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` |

## Voraussetzungen

- **Backend:** Java 25 + Maven 3.9 (oder Docker). Kein lokales JDK nötig,
  wenn mit Docker gebaut wird.
- **Frontend:** Node ≥ 22.22.3 (Angular CLI 22 verlangt Node 22.22.3+ /
  24.15+ / 26+).
- **Datenbank:** Docker (Postgres 17) oder eine eigene PostgreSQL-Instanz.

## Schnellstart

```bash
# Alles in Docker: Postgres + Backend + Frontend
docker compose -f compose.yml up -d --build
#   Frontend  http://localhost:4200
#   Backend   http://localhost:8080  (Health: /actuator/health)

# Nur die Datenbank (für die lokale Entwicklung)
docker compose -f compose.yml up -d postgres
```

Lokal statt Docker:

```bash
# Backend (http://localhost:8080, Health: /actuator/health)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Frontend (http://localhost:4200, /api wird auf das Backend geproxyt)
cd frontend
npm install
npm start
```

Im Profil `local` startet Spring Boot die Compose-Datei aus dem Repo-Root
selbst (Dev Services) — der Datenbank-Schritt entfällt dann.

## Architektur

| Bereich | Muster | Entscheid |
|---------|--------|-----------|
| Backend | **Ports & Adapters** (hexagonal): `domain` → `application` (Ports, Use Cases) → `adapters` (REST, JPA) | [ADR 0001](docs/adr/0001-backend-ports-and-adapters.md) |
| Frontend | **Feature-basiert** (Manfred Steyer): Feature-Ordner mit `routes.ts`, smart Component, `model/`, `data/`, `ui/`; Grenzen per Sheriff erzwungen | [ADR 0002](docs/adr/0002-frontend-feature-architecture.md) |

## Authentifizierung

Nextcloud ist Identity Provider und Nutzerverzeichnis (wie in Constructly):

- **Backend**: OAuth2 Resource Server, opake Tokens + Introspection
  (`/apps/oidc/introspect`), confidential Client
- **Frontend**: `angular-oauth2-oidc`, Code-Flow, public Client
- **Konfiguration**: `.env` (Properties-Format) — **enthält Secrets, wird
  nie committet**. Vorlage: `.env.example`

Einrichtung (Nextcloud-Host `docker-extern`):

```bash
# Clients anlegen (Details in .env.example)
docker exec -u www-data nextcloud php occ oidc:create "HouseholdManagerBackend" \
  "http://localhost:4200" --type=confidential --client_id="…" --client_secret="…" \
  --resource_url="http://localhost:8080/api"

# Service-User für das Nutzerverzeichnis
docker exec -u www-data nextcloud php occ user:add --password-from-env \
  --display-name "HouseholdManager Service" householdmanager-svc -g admin
docker exec -u www-data nextcloud php occ user:auth-tokens:add \
  householdmanager-svc --name householdmanager-local
```

Ohne konfigurierte Credentials startet die API **ohne** Authentifizierung
(lauter Warnhinweis im Log) — nur für die lokale Entwicklung.

## Struktur

```text
backend/                       Spring-Boot-Service (Ports & Adapters)
  src/main/java/de/householdmanager/
    domain/model/              Fachkern, ohne Framework-Imports
    application/
      port/in|out/             Inbound-/Outbound-Ports
      usecase/                 Use Cases
    adapters/
      inbound/rest/            Controller, DTOs, Fehlerübersetzung
      outbound/persistence/    JPA-Entity, Repository, Mapping
    config/                    Composition Root
  src/main/resources/
    db/migration/              Flyway-Migrationen (V<n>__…)
  src/test/java/               Tests je Schicht (JUnit 5, Mockito, Testcontainers)

frontend/                      Angular-22-App (Feature-Architektur)
  src/app/
    core/                      App-weit (Konfiguration, HTTP-Basis)
    shared/                    Von mehreren Features benutzt, fachneutral
    features/<feature>/
      routes.ts                Feature-Routen (lazy)
      <feature>-page.ts        Smart Component
      model/ data/ ui/         Modell, Datenzugriff, dumb Components
  sheriff.config.ts            Modulgrenzen (Steyer)
  eslint.config.js             Lint-Regeln inkl. Sheriff-Plugin

docs/adr/                      Architekturentscheidungen (ADRs)
docs/design/design-dna.md      Gestaltungssprache: Dark Mode, Tokens, Muster
arc42/                         Architekturdokumentation (12 Kapitel),
                               gepflegt vom ARC-Agenten
.opencode/                     Skills und Commands für OpenCode
  skills/                      Anforderungs-Loop, Architektur-, UX-, Angular- und Spring-Skills
  commands/                    /anforderung, /review, /abnahme, /issue-check, /issue-github
  agents/                      ux-designer, arc-agent, architektur-reviewer, kritischer-tester

AGENTS.md                      Projektkonventionen (Quelle der Wahrheit)
```

## Prüfen

```bash
# Backend: Kompilieren + Tests (Adapter-Test braucht Docker für Testcontainers)
cd backend && ./mvnw -B test

# Frontend: Type-Check, Tests, Lint, Modulgrenzen, Production-Build
cd frontend
npx tsc --noEmit -p tsconfig.app.json
npx tsc --noEmit -p tsconfig.spec.json
npm test
npm run lint
npm run sheriff
npm run build
```

## Anforderungen

Issues sind der Arbeitsstand für Anforderungen. Der Loop aus
`.opencode/skills/anforderung-loop` führt von der Idee in drei Runden zu
einem Issue, das ohne Rückfragen umsetzbar ist:

```text
/anforderung <Rohidee|Issue-Nr>
```

Details: `.opencode/README.md`.
