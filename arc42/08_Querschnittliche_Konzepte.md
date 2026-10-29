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

> **Offen — wesentliche Lücke:** Es gibt **keine Authentifizierung und keine
> Autorisierung**. `spring-boot-starter-security` ist nicht eingebunden, die
> API ist ungeschützt. Vor einem Produktivbetrieb ist zu entscheiden:
> Session, JWT (OAuth2 Resource Server) oder zentraler Identity Provider.
> Der ARC-Agent markiert dies als Risiko (siehe 11).

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
