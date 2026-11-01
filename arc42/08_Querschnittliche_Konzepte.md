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
| Versand fehlgeschlagen (Zustellfehler) | der Fehler wird protokolliert, der Zustellvermerk zählt den Versuch; ein weiterer Versuch folgt beim nächsten **stündlichen** Lauf |
| Versand fachlich abgelehnt (Protokollfehler) | der Fehler wird protokolliert, der Vermerk zählt den Versuch — aber **ohne** weiteren Versuch |
| Ziel-Chat inzwischen getrennt | der Vermerk wird abgebrochen; **kein** Versuch an den getrennten Chat |
| **dritter** Versuch fehlgeschlagen | kein weiterer Versuch; die **Verbindung des Chats bleibt bestehen** (Trennen bleibt allein der freischaltenden Person vorbehalten) |
| ein Chat schlägt fehl | die übrigen Chats desselben Haushalts werden **trotzdem** zugestellt |
| ein Haushalt schlägt fehl | die übrigen Haushalte werden **trotzdem** zugestellt |

Der Zustellvermerk kennt fünf Zustände — die Wiederholung hängt allein am
Zustand `FAILED`:

| Zustand | Bedeutung | wiederholbar |
|---------|-----------|--------------|
| `PENDING` | der Versuch läuft oder das Ergebnis steht aus | nein |
| `DELIVERED` | zugestellt | nein |
| `FAILED` | der letzte Versuch schlug fehl (Zustellfehler) | ja — bis zum dritten Versuch |
| `REJECTED` | Telegram hat die Nachricht fachlich abgelehnt | nein |
| `CANCELLED` | der Ziel-Chat wurde getrennt | nein |

Ein getrennter Chat wird auch über den **Wiederholungslauf** erkannt: der
Wiederholungslauf prüft die Chat-Bindung des Vermerks und bricht den Vermerk
ab, statt an einen getrennten Chat zu senden (AC15).

**Zustellfehler ≠ Protokollfehler.** Die Unterscheidung ist fachlich und
entscheidet über die Wiederholung:

| Fehler | Auslöser | Fach-Exception | Wiederholung |
|--------|----------|----------------|--------------|
| **Zustellfehler** | Netzwerk, Zeitüberschreitung, Telegram-Serverfehler, nicht erreichbarer Chat | `TelegramDeliveryException` | ja — der stündliche Lauf versucht es erneut |
| **Protokollfehler** | HTTP 200 mit `ok: false` — unbekannter Chat, ungültige Kennung, abgelehnter Text | `TelegramProtocolException` | nein — ein weiterer Versuch würde beim selben Fehler scheitern |

Die Abgrenzung liegt im ausgehenden Adapter (`TelegramBotAdapter`), die
Entscheidung über die Wiederholung im Use Case. Der Adapter wirft die
fachlichen Exceptions; die Anwendung entscheidet, was wiederholt wird.

Die Versuchszählung ist **persistenter Zustand** im Fachkern
(`NotificationDelivery`), nicht ein Zähler im Adapter: nur so überlebt sie
einen Neustart und nur so ist „höchstens drei Versuche je Nachricht"
überprüfbar. Der Adapter kennt nur „gesendet" oder „nicht gesendet".

Zwischen zwei Versuchen derselben Nachricht liegt mindestens **eine Stunde**
(`NotificationDelivery.RETRY_INTERVAL`). Die Grenze ist fachlich: ohne sie
erschöpften sich die drei Versuche in Sekunden statt in zwei Stunden. Der
Adapter liefert die **Kandidaten** (`findRetryCandidates()`), die Fälligkeit
entscheidet die Anwendung (`NotificationDelivery.isDueAt(now)`).

Der Zustellvermerk entsteht **vor** dem Versand — der Zustand des Versuchs ist
so jederzeit nachvollziehbar. `PENDING` ist dabei ein **flüchtiger
Zwischenzustand innerhalb der Transaktion** des Laufs: der Versand und die
Fortschreibung des Vermerks (`DELIVERED`, `FAILED`, `REJECTED`) passieren in
derselben Transaktion (arc42/06). Bricht der Vorgang dazwischen ab, wird die
Transaktion verworfen — ein dauerhafter `PENDING`-Datensatz entsteht nicht.

Dauerhaft wiederholt wird ausschließlich ein Vermerk im Zustand `FAILED` — dort
steht fest, dass der Versand nachweislich fehlgeschlagen ist (AC26). Ein
`PENDING` wird **nicht** wiederholt: Telegram könnte die Nachricht schon
angenommen haben, ein zweiter Versand liefe auf eine doppelte Zustellung
hinaus.

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
| Auslöser | `@EnableScheduling` in `config/ApplicationConfig`; die Jobs `adapters/inbound/scheduler/WasteSyncJob`, `…/NotificationDispatchJob` und `…/NotificationRetryJob` sind **eingehende Adapter** und enthalten keine Fachlogik |
| Taktung | einmal täglich, Uhrzeit fest im Server (Property `householdmanager.waste.sync-cron`, Vorgabe `0 0 3 * * *`) — **nicht** durch die Oberfläche einstellbar (ADR 0005). Der Benachrichtigungslauf läuft **18:00 Uhr** (Property `householdmanager.telegram.dispatch-cron`), sonntags mit zusätzlicher Wochenübersicht; der Wiederholungslauf läuft **stündlich** (Property `householdmanager.telegram.retry-cron`) |
| Uhr | die Anwendung rechnet ausschließlich mit der `Clock`-Bean; kein `Instant.now()` im Fachkern. Die Fenstergrenze (42 Tage) wird aus `LocalDate.now(clock.withZone(ZoneId.of("Europe/Berlin")))` abgeleitet — der Tag des Laufs im Betriebsgebiet, nicht in UTC. Dasselbe gilt für „morgen" und „kommende Woche" im Benachrichtigungs-Kontext |
| Testbarkeit | der Job ruft genau einen Use Case auf; die Regeln stehen im Use Case (JUnit + Mockito, `Clock` fest) — kein Test wartet auf die Tagesuhr |
| Fehlerisolierung | ein fehlgeschlagener Lauf beendet den Job nicht; der Fehler wird je Haushalt protokolliert |
| Sofort-Sync nach dem Speichern | **zweiter Use-Case-Aufruf im Controller** nach dem Commit der Speicherung — eigene Transaktion, eigener HTTP-Timeout (5 s); kein Ereignis, kein Listener, kein `@Async`. Die Antwort **wartet** auf den Lauf; ein Lauf-Fehler ist isoliert, das Speichern bleibt erfolgreich (ADR 0005) |
| Sperren | keine — der Lauf **ersetzt** den Bestand je Haushalt; ein doppelter Lauf (mehrere Instanzen) ist unkritisch |

### Registrierung mehrerer Zeitgeber — Zyklusrisiko

`config/ApplicationConfig` **liefert keine Beans**; die Klasse trägt nur die
Planung. Würde ein Use Case oder ein Job eine Bean von ihr beziehen, entstünde
ein Zyklus (`ApplicationConfig → Job → Use Case → ApplicationConfig`).

Mit dem Benachrichtigungskontext kommen **zwei** weitere Zeitgeber dazu — der
18:00-Uhr-Lauf und der stündliche Wiederholungslauf. Die Regeln daraus:

- Die technische Verdrahtung (Clients, Bot-Token) gehört in **eigene**
  `config/`-Klassen (`ClockConfig`, `IcsClientConfig`, `TelegramConfig`),
  **nicht** in `ApplicationConfig`.
- `ApplicationConfig` registriert **nur** Cron-Tasks; sie bekommt die Jobs
  als Konstruktor-Parameter und gibt **keine** Bean zurück.
- Die Zeitregeln selbst liegen **nicht** im Job, sondern im Use Case — der Job
  kennt keinen Wochentag und kein Datum (ADR 0007).

Der Bot-Token ist kein nackter `String`-Bean, sondern ein eigener Typ
(`config/TelegramConfig.TelegramToken`): ein `String`-Bean würde mit jeder
anderen String-Bean im Kontext kollidieren, und der eigene Record gibt den
Wert in keiner Logausgabe preis — `toString()` meldet nur „gesetzt" bzw.
„nicht gesetzt".

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
| Rechte | alle Mitglieder sind gleichberechtigt; **nur wer selbst freigeschaltet hat**, darf die Verbindung trennen (`TelegramChat.activatedBy`, geprüft im Use Case → 403 `NotChatOwnerException`). Es gibt keine Admin-Rolle im Haushalt |
| Freischaltende Person | der Bot kennt **keine Person**; der **Code** trägt sie. Der Code entsteht beim ersten Anfordern im Namen eines Mitglieds (`telegram_activation_code.activated_by`) — die Freischaltung über diesen Code gehört der Person, die ihn angefordert hat. Der Bot-Token erreicht ausschließlich den Bot-Dienst, nie die Oberfläche |

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
