# 11. Risiken und technische Schulden

## Risiken

| Nr | Risiko | Auswirkung | Gegenmaßnahme | Status |
|----|--------|------------|---------------|--------|
| R1 | ~~Keine Authentifizierung~~ — **gemindert**: Nextcloud/OIDC ist eingerichtet (JWT-Access-Tokens, JWKS-Validierung), die API ist ohne Token 401 | Kein Zugriffsschutz für Endnutzer, wenn der Client umgangen wird | `SecurityConfig` (Ports & Adapters), CORS auf den Frontend-Origin, `.env` außerhalb des Repos | **aktiv** |
| R2 | Mandanten-Cut (ein Nutzer sah alle Haushalte) | Datenleck bei Mehrhaushalten | entworfen: `household_member` + Prüfung je Use Case (siehe 08) | in Arbeit |
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
| R13 | Der 18:00-Uhr-Lauf hängt an der Verfälligkeit des Backends — ein Ausfall um 18:00 Uhr verschiebt die Benachrichtigung auf den nächsten Lauf | Mülltermine werden nicht oder zu spät erinnert; bei einem Ausfall am Sonntag fehlt die Wochenübersicht | Muster aus ADR 0005: Ausfall wird **nicht** nachgeholt, sondern beim nächsten Lauf geflickt; der Fehler wird protokolliert. Ein Nachholen verpasster Benachrichtigungen ist bewusst nicht Teil des Scopes | **aktiv** |
| R14 | Die Registrierung von **drei** Zeitgebern in `config/ApplicationConfig` (Tageslauf, 18:00-Uhr-Lauf, stündlicher Wiederholungslauf) erzeugt einen **Bean-Zyklus**: die Klasse liefert ausdrücklich keine Beans, und ein Job, der sie zurückreferenziert, schließt den Kreis `ApplicationConfig → Job → Use Case → ApplicationConfig` | Startabbruch der Anwendung — die App läuft dann gar nicht, auch nicht ohne Telegram | Technische Verdrahtung (Bot-Client, Token) in **eigene** `config/`-Klassen (`TelegramConfig`, analog `ClockConfig`/`IcsClientConfig`); `ApplicationConfig` registriert **nur** Cron-Tasks; die Zeitregeln liegen im Use Case, nicht im Job. Der Reviewer prüft das im Architektur-Gate | **aktiv** |
| R15 | Der Bot-Token liegt als Secret in der Umgebung; die Anwendung muss ihn für **jeden** Versand mitführen | Leck über Logs, Fehlermeldungen oder Konfigurationsausgaben → Vollzugriff auf den Bot | Token nur aus der Property, nie loggen, nie im `ProblemDetail`; eigener Typ `TelegramConfig.TelegramToken` statt eines nackten `String`-Bean (verhindert Kollision und Preisgabe über `toString()`); `.env` außerhalb des Repos (AGENTS.md); die Oberfläche bekommt den Token nie | **aktiv** |
| R16 | ~~Ein eingehender Telegram-Webhook ist ein öffentlicher Endpunkt~~ — **vermieden**: umgesetzt ist der **Long-Poll** (nur Egress, kein Ingress, kein Zertifikat) | Angriffsfläche ohne Nutzer-Authentifizierung; ungeprüfte Aufrufe | Umgesetzt in `adapters/inbound/telegram/TelegramBotInputAdapter` (ADR 0007, O1). Bei einem späteren Wechsel auf einen Webhook: Prüfung des Telegram-Secrets, Rate-Limit, eigener Endpunkt außerhalb der Nutzer-API | **aktiv** |
| R17 | Der Long-Poll ist ein **dauerhaft laufender Thread** im Backend-Container | Ein hängender Thread oder eine blockierte Abfrage hält den Shutdown auf; Telegram ist nicht erreichbar | Daemon-Thread, Abbruch beim `ContextClosedEvent`; der Abfragefehler beendet den Lauf nicht, er wartet und versucht es erneut; HTTP-Timeout (10 s) und Abfrageintervall als Puffer | **aktiv** |
| R18 | Der Wiederholungslauf ist **stündlich**, nicht punktgenau nach dem Fehlschlag | Eine Benachrichtigung kann bis zu knapp einer Stunde später zugestellt werden als vorgesehen | Bewusste Wahl (ADR 0007, O2): ein punktgenauer Versuchszeitpunkt bräuchte einen eigenen Wiederholer im Adapter. Der Versuchszähler bleibt persistent, ein Neustart verliert ihn nicht | **aktiv** |

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
| S11 | Der Kontext „Benachrichtigung" ist fachlich auf **Mülltermine** beschnitten, obwohl Kanal (Telegram) und Ereignis (Mülltermin) fachlich unabhängig sind | Ein späteres Ereignis (Aufgaben, Termine) braucht neue Use Cases und neue Regeln — die Struktur des Kontexts trägt es | Bewusste Grenze (Scope des Issues); die Grenzen liegen an den Use Cases, nicht an den Tabellen — eine Erweiterung ergänzt Use Cases, nicht das Schema (ADR 0007) |
| S12 | Die Chat-Identität (Telegram-Chat-ID) ist eine **fremde** Kennung im eigenen Schema | Eine Änderung der Telegram-Kennung (Chat-Umbenennung, Migration) ist im Haus nicht abgebildet | Die Chat-ID ist der **Zugriffsweg** auf den Chat und wird dauerhaft gespeichert; Anzeigename und Chat-Art werden als Snapshot gehalten, weil Telegram sie jederzeit neu liefert | **aktiv** |
| S13 | Der Freischalt-Code ist **dauerhaft gültig** und mehrfach verwendbar; er ist über die Oberfläche für alle Mitglieder sichtbar | Wer den Code sieht, kann Chats freischalten — auch für fremde Chats | Bewusste Entscheidung des Auftraggebers; die Freischaltung ist auf Mitglieder des Haushalts begrenzt (der Code selbst wird im Chat geprüft, der Chat-Bezug entsteht erst durch die Zuordnung) | **aktiv** |
| S14 | Die **freischaltende Person** hängt am Code, nicht am Chat: der erste anfordernde Mitglied wird zum Eigentümer aller daraus entstehenden Verbindungen | Wer den Code zuerst anfordert, kann eine andere Person nicht mehr an eigenen Verbindungen beteiligen; ein Code-Wechsel ist nicht vorgesehen | Bewusste Vereinfachung (ADR 0007): sie braucht keine Identität im Telegram-Protokoll und hält die Trennberechtigung eindeutig. Ein späterer Kanal (E-Mail) braucht eine eigene Personenbindung | **aktiv** |
| S15 | Der Benachrichtigungskontext besitzt **keinen** E2E-Test mit laufendem Telegram-Dienst | Verhalten gegen die echte Bot-API (Rate-Limit, Gruppenrechte, Bot-Rechte im Chat) bleibt ungeprüft | Bewusste Grenze: die Bot-API wird nur in `adapters/outbound/telegram/` angesprochen; ein Test gegen den echten Dienst ist ein eigener, späterer Baustein | offen |

> **Geprüft und nicht mehr offen:** die zentralisierte Längengrenze
> (`Household.MAX_NAME_LENGTH`, referenziert von Use Case und JPA-Entity), das
> unveränderte `createdAt` beim Update (`HouseholdRepositoryAdapter.save`,
> abgesichert durch `HouseholdRepositoryAdapterTest`) und die Fach-Exception
> `InvalidHouseholdNameException` statt einer pauschalen
> `IllegalArgumentException` — umgesetzt und in 08 beschrieben.

> **Offene Architekturpunkte:**
> 1. Ersteller nicht persistiert — **entschieden:** `created_by` NOT NULL,
>    frische Dev-DB, kein Backfill (siehe 05, Migration V2).
> 2. Kartenreihenfolge je Spalte — **entschieden:** dauerhaftes `position`-Feld
>    im Task-Datenmodell (siehe 05, 06).
> 3. **Offen:** Name der Zugewiesenen: Live-Auflösung über `UserDirectory` je
>    Request (Verzeichnisabhängigkeit) oder Namens-Snapshot bei Zuweisung.
> 4. **Offen:** Markdown-Renderer `shared` vs. `features/tasks/ui`.

> **Offen:** Risiken priorisieren und mit dem Auftraggeber durchgehen.
