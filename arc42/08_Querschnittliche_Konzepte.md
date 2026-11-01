# 8. Querschnittliche Konzepte

## Fehlerbehandlung

| Ebene | Regel |
|-------|-------|
| Fachkern | Wirft **Fach-Exceptions** (`InvalidHouseholdNameException`), framework-frei |
| Use Case | Übersetzt fachliche Regeln in Fach-Exceptions; keine HTTP-Typen |
| Eingehender Adapter | Übersetzt Fach-Exception in `ProblemDetail` + Statuscode |
| Statuscode-Regel | Nur **Fachfehler** → 4xx. Allgemeine `IllegalArgumentException`, `NullPointerException` → 5xx (Programmierfehler) |
| Frontend | Fehler über `httpResource.error()` → Hinweis in der Oberfläche; Validierung zusätzlich im Formular |

### Fehler beim Abruf einer externen Quelle

Der Abruf einer ICS-Quelle ist **kein** Fehler des Nutzerdialogs, sondern ein
Ereignis im Hintergrund: er endet nie als HTTP-Fehler der Lese- oder
Schreib-Schnittstelle.

| Fall | Bedeutung | Wirkung |
|------|-----------|---------|
| Timeout, HTTP-Fehler, kein lesbares ICS | `WasteFeedUnavailableException` | Bestand bleibt unverändert (R7), Stand gilt als veraltet (R13), der Lauf läuft mit dem nächsten Haushalt weiter |
| Quelle erreichbar, keine Termine im Fenster | leeres Ergebnis | Bestand wird geleert (R8), Stand wird aktualisiert |

Die Oberfläche liest den **veralteten Stand** aus der Antwort des Servers
(R13) — sie leitet ihn nicht aus dem Alter des Zeitstempels ab. Das
Speichern einer Quelle meldet **nur** das Speichern, auch wenn der
anschließende Lauf fehlschlägt; ob die Synchronisation gelang, zeigt der
Stand.

### Toleranz des Feed-Adapters

Der Adapter ist tolerant gegenüber Formatvarianten. Die Regeln liegen im
ausgehenden Adapter, das Fachmodell bleibt **ganztägig**.

| Variante | Behandlung |
|----------|------------|
| Ereignis **mit Uhrzeit** (`DTSTART` mit `TZID` oder Uhrzeit) | wird auf seinen **Kalendertag in Europe/Berlin** abgebildet — das Modell trägt keine Uhrzeit (R12) |
| **Wiederholung** (`RRULE`) | wird auf das **42-Tage-Fenster** expandiert (je ein Termin pro Tag im Fenster) |
| Ereignis **ohne verwendbares Startdatum** | wird ignoriert (kein Termin) |
| Termin außerhalb des Fensters | wird nicht übernommen (R5) |

Die Toleranz betrifft den aktuellen Feed des Entsorgers nicht (dort sind alle
Ereignisse Ganztagsdaten) — sie ist Robustheit für andere Quellen.

## Validierung

| Ort | Was |
|-----|-----|
| Grenze (REST-DTO) | `@NotBlank` — Anwesenheit, nicht Format |
| Invariante (Use Case) | Länge, Trim, fachliche Regeln — am **getrimmten** Wert |
| Datenbank | `NOT NULL`, `VARCHAR(120)` — letzte Instanz (Fremdschlüssel erst ab dem zweiten Context) |

Die Längengrenze einer ICS-Adresse (500 Zeichen) hat ihre Quelle im
Fachmodell des Müllkalender-Kontexts (`WasteCalendar.MAX_URL_LENGTH`); das
Persistenzmodell des Haushalts spiegelt sie als Spaltenlänge (VARCHAR(500)) —
siehe die Spiegeldrift in 11.

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

## Scheduling und Zeit

| Thema | Festlegung |
|-------|-------------|
| Auslöser | `@EnableScheduling` in `config/ApplicationConfig`; der Job `adapters/inbound/scheduler/WasteSyncJob` ist ein **eingehender Adapter** und enthält keine Fachlogik |
| Taktung | einmal täglich, Uhrzeit fest im Server (Property `householdmanager.waste.sync-cron`, Vorgabe `0 0 3 * * *`) — **nicht** durch die Oberfläche einstellbar (ADR 0005) |
| Uhr | die Anwendung rechnet ausschließlich mit der `Clock`-Bean; kein `Instant.now()` im Fachkern. Die Fenstergrenze (42 Tage) wird aus `LocalDate.now(clock.withZone(ZoneId.of("Europe/Berlin")))` abgeleitet — der Tag des Laufs im Betriebsgebiet, nicht in UTC |
| Testbarkeit | der Job ruft genau einen Use Case auf; die Regeln stehen im Use Case (JUnit + Mockito, `Clock` fest) — kein Test wartet auf die Tagesuhr |
| Fehlerisolierung | ein fehlgeschlagener Lauf beendet den Job nicht; der Fehler wird je Haushalt protokolliert |
| Sofort-Sync nach dem Speichern | **zweiter Use-Case-Aufruf im Controller** nach dem Commit der Speicherung — eigene Transaktion, eigener HTTP-Timeout (5 s); kein Ereignis, kein Listener, kein `@Async`. Die Antwort **wartet** auf den Lauf; ein Lauf-Fehler ist isoliert, das Speichern bleibt erfolgreich (ADR 0005) |
| Sperren | keine — der Lauf **ersetzt** den Bestand je Haushalt; ein doppelter Lauf (mehrere Instanzen) ist unkritisch |

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

**Mandantentrennung und Rollen:** die Tabelle `household_member` macht
ernannte Zugehörigkeit sichtbar; jeder Use Case prüft vor der Fachaktion die
Mitgliedschaft der angemeldeten Person (`CurrentUserPort` +
`HouseholdMemberRepository`, sonst 403). Haushaltsleitung = Ersteller/in des
Haushalts — diese Information ist im Schema noch nicht persistiert (siehe 11).

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
