# 8. Querschnittliche Konzepte

## Fehlerbehandlung

| Ebene | Regel |
|-------|-------|
| Fachkern | Wirft **Fach-Exceptions** (`InvalidHouseholdNameException`), framework-frei |
| Use Case | Übersetzt fachliche Regeln in Fach-Exceptions; keine HTTP-Typen |
| Eingehender Adapter | Übersetzt Fach-Exception in `ProblemDetail` + Statuscode |
| Statuscode-Regel | Nur **Fachfehler** → 4xx. Allgemeine `IllegalArgumentException`, `NullPointerException` → 5xx (Programmierfehler) |
| Frontend | Fehler über `httpResource.error()` → Hinweis in der Oberfläche; Validierung zusätzlich im Formular |

## Validierung

| Ort | Was |
|-----|-----|
| Grenze (REST-DTO) | `@NotBlank` — Anwesenheit, nicht Format |
| Invariante (Use Case) | Länge, Trim, fachliche Regeln — am **getrimmten** Wert |
| Datenbank | `NOT NULL`, `VARCHAR(120)` — letzte Instanz (Fremdschlüssel erst ab dem zweiten Context) |

Zwei Prüfungen an derselben Sache sind zu vermeiden (Doppelmessung mit
unterschiedlichen Werten). Die Längengrenze hat genau eine Quelle:
`Household.MAX_NAME_LENGTH` (Fachmodell), referenziert von Use Case und
JPA-Entity.

## Transaktionen

- Transaktionsgrenze ist die **Use Case** (`@Transactional`), nicht der
  Adapter und nicht der Controller.
- Lesezugriffe: `@Transactional(readOnly = true)`.
- `open-in-view: false` — die Entity verlässt den Adapter nicht; nach außen
  geht nur das Fachmodell.

## Security

**Stand: umgesetzt.** Nextcloud ist Identity Provider und Nutzerverzeichnis
(wie in Constructly).

| Thema | Entscheidung |
|-------|--------------|
| Protokoll | OAuth2/OIDC, **JWT-Access-Tokens** (RFC 9068) — Signaturprüfung über den JWKS des Issuers |
| Backend | Spring Boot als Resource Server, **kein Client-Secret** nötig (JWT statt opaker Tokens) |
| Frontend | `angular-oauth2-oidc`, Code-Flow, public Client, Token im Speicher, Bearer-Header per Interceptor |
| Nutzerverzeichnis | Nextcloud OCS-API mit Service-User (`householdmanager-svc`) + App-Password |
| Konfiguration | `.env` (Properties, gitignored), Properties `nextcloud.oidc.issuer-uri` und `nextcloud.oidc.client-id` |
| Grenzen | `CurrentUserPort` (ein-) und `UserDirectory` (aus-) — die Anwendung kennt keine Security-Klassen |

**Ohne konfigurierten Issuer läuft die API ohne Authentifizierung** (lauter
Warnhinweis im Log). Das ist der Zustand eines frischen, noch nicht
eingerichteten Setups — **kein** Produktivbetrieb.

Mandantentrennung und Rollen — Zielbild aus Issue #1: die Tabelle
`household_member` macht ernannte Zugehörigkeit sichtbar; jeder Task-Use-Case
prüft vor der Fachaktion die Mitgliedschaft der angemeldeten Person
(`CurrentUserPort` + `HouseholdMemberRepository`, sonst 403). Haushaltsleitung
= Ersteller/in des Haushalts; diese Information fehlt in der Schema-Entwurf
von V1 und wird mit Issue #1 ergänzt (siehe 11).

## Logging und Observability

| Thema | Stand |
|-------|-------|
| Actuator | `health`, `info` freigeschaltet (`/actuator/health`) |
| Logging | Spring-Defaults, im Profil `local` auf DEBUG für `de.householdmanager` |
| Tracing | **Offen** — Boot 4 bietet `spring-boot-starter-opentelemetry` |
| Strukturierte Logs | **Offen** — Boot 4 kann ECS/Logstash direkt, ohne eigenen Encoder |

## Migrationen

- Flyway, Dateiname `V<n>__<beschreibung>.sql`, Pfad
  `backend/src/main/resources/db/migration`.
- Bestehende `V`-Dateien werden **nie** editiert.
- `ddl-auto: validate` — Entity und Migration müssen übereinstimmen,
  sonst startet die App nicht.

## Internationalisierung

> **Offen:** bisher rein deutsche Texte, keine i18n-Struktur. Festlegen,
> sobald eine zweite Sprache gefordert ist.

## Agenten-Arbeit (Prozess)

| Konzept | Regel |
|---------|-------|
| Issue als Arbeitsstand | Anforderungen, Entscheidungen und Protokolle liegen am Issue |
| Gates | Architektur-Review (`OK`) und Abnahme (`PASS`) vor „fertig" |
| Korrektur-Delegation | Reviewer urteilt, Implementierer korrigiert, Reviewer prüft erneut (max. 3 Runden) |
| Kontext-Disziplin | Kleines Modell, 256k Kontext → grep vor read, Ausgaben kürzen, Logdateien statt Bildschirm |
