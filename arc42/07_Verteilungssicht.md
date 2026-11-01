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
| `HOUSEHOLDMANAGER_WASTE_SYNC_CRON` | `0 0 3 * * *` | Tageslauf des Müllkalenders — ADR 0005 |
| `HOUSEHOLDMANAGER_WASTE_SYNC_ZONE` | `Europe/Berlin` | Zeitzone des Tageslaufs (`householdmanager.waste.sync-zone`) — ADR 0005 |
| `HOUSEHOLDMANAGER_WASTE_TIMEOUT` | 5 s | Zeitlimit für den Abruf einer ICS-Quelle (`householdmanager.waste.timeout`, auch im Sofort-Sync, ADR 0005) |
| `HOUSEHOLDMANAGER_TELEGRAM_BOT_TOKEN` | — | **Secret** des Telegram-Bots (`householdmanager.telegram.bot-token`); ohne Wert ist der Benachrichtigungs-Kontext inaktiv (ADR 0007) |
| `HOUSEHOLDMANAGER_NOTIFICATION_CRON` | `0 0 18 * * *` | 18:00-Uhr-Lauf der Benachrichtigung (`householdmanager.notification.cron`) — ADR 0007 |
| `HOUSEHOLDMANAGER_NOTIFICATION_ZONE` | `Europe/Berlin` | Zeitzone der Benachrichtigung (`householdmanager.notification.zone`) |

## Ausgehende Verbindungen (Egress)

Der Backend-Container braucht ausgehenden Netzzugriff: er ruft die
ICS-Quelle des Entsorgers ab (`GET`, HTTPS). Es kommen **keine** neuen
Container, **keine** neuen Ports und **keine** neuen Abhängigkeiten dazu —
der Abruf läuft über den vorhandenen `RestClient` (ADR 0005, ADR 0006).

Ein Container-Image muss das Zertifikat des Entsorgers vertrauen; in der
Entwicklungsumgebung genügt das System-Truststore des Basis-Images.

### Telegram

Für den Versand von Benachrichtigungen braucht der Backend-Container
zusätzlich **ausgehenden** Netzzugriff zur Telegram-Bot-API (`HTTPS`).

Der Empfang läuft über **Long-Poll**: die Anwendung fragt Telegram aktiv
nach Nachrichten ab. Damit braucht der Container **keinen eingehenden**
Port, kein TLS-Zertifikat und keinen öffentlich erreichbaren Endpunkt
(ADR 0007, O1).

Es kommen **keine** neuen Container, **keine** neuen Ports und **keine**
neuen Abhängigkeiten dazu — die Benachrichtigung läuft in derselben
Anwendung (ADR 0003, ADR 0007).

## Betrieb

Der **Release-Ablauf ist festgelegt** — siehe `AGENTS.md` → Release:
Versions-Bump in `backend/pom.xml` und `frontend/package.json`, Tag
`v<version>`, Bau der Images `householdmanager-backend` und
`householdmanager-frontend` über `docker compose -f compose.yml build`, Deploy
mit `docker compose up -d --build`, Rollback über den vorherigen Tag. Ein
**Publish in eine Container-Registry ist nicht eingerichtet** — die Images
werden auf dem Zielsystem gebaut. Health-Check über `/actuator/health` ist
vorhanden.
