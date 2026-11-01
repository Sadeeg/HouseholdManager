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

### Fehler bei der Zustellung einer Benachrichtigung

Auch der Versand an einen Kanal ist ein **Hintergrundereignis**: er endet
nie als HTTP-Fehler der Oberfläche. Ein Fehlschlag betrifft **eine
Nachricht**, nicht den Haushalt und nicht die anderen Chats.

| Fall | Wirkung |
|------|---------|
| Versand erfolgreich | der Zustellvermerk gilt als zugestellt; kein weiterer Versuch |
| Versand fehlgeschlagen | der Fehler wird protokolliert, der Zustellvermerk zählt den Versuch; ein weiterer Versuch folgt erst **nach einer Stunde** |
| **dritter** Versuch fehlgeschlagen | kein weiterer Versuch; die **Verbindung des Chats bleibt bestehen** (Trennen bleibt allein der freischaltenden Person vorbehalten) |
| ein Chat schlägt fehl | die übrigen Chats desselben Haushalts werden **trotzdem** zugestellt |
| ein Haushalt schlägt fehl | die übrigen Haushalte werden **trotzdem** zugestellt |

Die Versuchszählung ist **persistenter Zustand** im Fachkern
(`NotificationDelivery`), nicht ein Zähler im Adapter: nur so überlebt sie
einen Neustart und nur so ist „höchstens drei Versuche je Nachricht"
überprüfbar. Der Adapter kennt nur „gesendet" oder „nicht gesendet".

Die Wiederholung ist **zeitversetzt** (1 Stunde) — nicht „sofort nochmal".
Deshalb liegt der Versuchszeitpunkt im Zustellvermerk, nicht im Adapter.

> **Offen:** Wer die Wiederholung auslöst — ein stündlicher Scheduler-Tick,
> ein `fixedDelay`-Wiederholer oder ein gespeicherter Versuchszeitpunkt
> (ADR 0007, O2).

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
| Auslöser | `@EnableScheduling` in `config/ApplicationConfig`; die Jobs `adapters/inbound/scheduler/WasteSyncJob` und `…/NotificationDispatchJob` sind **eingehende Adapter** und enthalten keine Fachlogik |
| Taktung | einmal täglich, Uhrzeit fest im Server (Property `householdmanager.waste.sync-cron`, Vorgabe `0 0 3 * * *`) — **nicht** durch die Oberfläche einstellbar (ADR 0005). Der Benachrichtigungslauf läuft **18:00 Uhr** (Property `householdmanager.notification.cron`), sonntags mit zusätzlicher Wochenübersicht |
| Uhr | die Anwendung rechnet ausschließlich mit der `Clock`-Bean; kein `Instant.now()` im Fachkern. Die Fenstergrenze (42 Tage) wird aus `LocalDate.now(clock.withZone(ZoneId.of("Europe/Berlin")))` abgeleitet — der Tag des Laufs im Betriebsgebiet, nicht in UTC. Dasselbe gilt für „morgen" und „kommende Woche" im Benachrichtigungs-Kontext |
| Testbarkeit | der Job ruft genau einen Use Case auf; die Regeln stehen im Use Case (JUnit + Mockito, `Clock` fest) — kein Test wartet auf die Tagesuhr |
| Fehlerisolierung | ein fehlgeschlagener Lauf beendet den Job nicht; der Fehler wird je Haushalt protokolliert |
| Sofort-Sync nach dem Speichern | **zweiter Use-Case-Aufruf im Controller** nach dem Commit der Speicherung — eigene Transaktion, eigener HTTP-Timeout (5 s); kein Ereignis, kein Listener, kein `@Async`. Die Antwort **wartet** auf den Lauf; ein Lauf-Fehler ist isoliert, das Speichern bleibt erfolgreich (ADR 0005) |
| Sperren | keine — der Lauf **ersetzt** den Bestand je Haushalt; ein doppelter Lauf (mehrere Instanzen) ist unkritisch |

### Registrierung mehrerer Zeitgeber — Zyklusrisiko

`config/ApplicationConfig` **liefert keine Beans**; die Klasse trägt nur die
Planung. Würde ein Use Case oder ein Job eine Bean von ihr beziehen, entstünde
ein Zyklus (`ApplicationConfig → Job → Use Case → ApplicationConfig`).

Mit dem Benachrichtigungslauf kommt ein **zweiter** Zeitgeber dazu. Die
Regeln daraus:

- Die technische Verdrahtung (Clients, Bot-Token) gehört in **eigene**
  `config/`-Klassen (`ClockConfig`, `IcsClientConfig` — analog ein
  `TelegramConfig`), **nicht** in `ApplicationConfig`.
- `ApplicationConfig` registriert **nur** Cron-Tasks; sie bekommt die Jobs
  als Konstruktor-Parameter und gibt **keine** Bean zurück.
- Die Zeitregeln selbst liegen **nicht** im Job, sondern im Use Case — der Job
  kennt keinen Wochentag und kein Datum (ADR 0007).

> **Offen:** Wie die Wiederholung einer fehlgeschlagenen Zustellung
> ausgelöst wird — das entscheidet, ob ein **dritter** Zeitgeber nötig wird
> (ADR 0007, O2).

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

### Zugangsdaten für den Bot

| Thema | Festlegung |
|-------|-----------|
| Bot-Token | **Secret** aus der Umgebung (`householdmanager.telegram.bot-token`, in `.env`, nie im Repository, nie in einem Issue). Es ist ein **Bot-Token**, kein Client-Secret im OIDC-Sinne — die Anwendung ist *Client* des Bot-Dienstes, nicht Identity Provider |
| Ohne Token | der Benachrichtigungs-Kontext ist **inaktiv**: keine Chats, kein Versand, kein Fehler beim Start (analog zur Nutzerverzeichnis-Adapter-Regel) |
| Freischalt-Code | ein Geheimnis **des Haushalts**, kein Geheimnis der Person: jedes Mitglied sieht ihn, er schaltet beliebig viele Chats frei und hat **kein** Ablaufdatum. Er wird **im Chat an den Bot** gesendet, nicht über die Oberfläche |
| Rechte | alle Mitglieder sind gleichberechtigt; **nur wer selbst freigeschaltet hat**, darf die Verbindung trennen. Es gibt keine Admin-Rolle im Haushalt |

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
