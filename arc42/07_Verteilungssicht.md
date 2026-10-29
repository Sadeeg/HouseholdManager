# 7. Verteilungssicht

## Container und Ports

```text
┌──────────────────────── Docker-Host ─────────────────────────┐
│                                                              │
│  ┌────────────────────┐        ┌─────────────────────────┐  │
│  │ Frontend           │        │ Backend                 │  │
│  │ nginx + SPA        │──/api──▶│ Spring Boot (JRE 25)   │  │
│  │ Port 80            │        │ Port 8080               │  │
│  └────────────────────┘        └───────────┬─────────────┘  │
│                                                │ JDBC         │
│                                    ┌───────────▼─────────────┐  │
│                                    │ PostgreSQL 17           │  │
│                                    │ Port 5432               │  │
│                                    └─────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

| Einheit | Technik | Port | Zuständig für |
|---------|---------|------|---------------|
| `frontend` | nginx + gebaute SPA | 80 | Auslieferung der Dateien, Proxy `/api` → Backend |
| `backend` | Spring Boot, JRE 25 | 8080 | REST-API, Fachlogik, Persistenzzugriff |
| `postgres` | postgres:17-alpine | 5432 | Daten, Flyway-Migrationen |

> **Stand:** Das Bild ist das Zielbild des Betriebs. `compose.yml` definiert
> heute **nur** den Dienst `postgres`; `frontend` und `backend` existieren als
> Image-Definition (`frontend/Dockerfile`, `backend/Dockerfile`) und werden
> manuell gestartet. `frontend/nginx.conf` erwartet für den Proxy bereits
> einen Compose-Dienst `backend` (Port 8080) — siehe 11, S6.

## Development

| Teil | Start | Adresse |
|------|-------|---------|
| Datenbank | `docker compose -f compose.yml up -d postgres` | `localhost:5432` |
| Backend | `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local` | `localhost:8080` |
| Frontend | `cd frontend && npm start` | `localhost:4200` |

- Im Profil `local` startet Spring Boot die Compose-Datei **selbst**
  (Dev Services) — `spring.docker.compose.file: ../compose.yml`.
- Das Frontend leitet `/api` per `proxy.conf.json` an `localhost:8080`.
- Der Host-Port von Postgres ist überschreibbar
  (`POSTGRES_PORT=15432`), falls 5432 belegt ist.

## Konfiguration

| Variable | Default | Zweck |
|----------|---------|-------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/householdmanager` | Datenbankverbindung |
| `DB_USER` / `DB_PASSWORD` | `household` / `household` | Zugangsdaten (Dev) |
| `SPRING_PROFILES_ACTIVE` | — | `local` schaltet Dev Services frei |

## Betrieb

> **Offen:** Release-Ablauf (Versions-Bump, Tag, Image-Publish, Rollback)
> ist noch nicht festgelegt — siehe `AGENTS.md` → Release. Health-Check über
> `/actuator/health` ist vorhanden.
