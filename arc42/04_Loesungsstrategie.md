# 4. Lösungsstrategie

## Kurzfassung

Das System ist als **Webanwendung** aufgebaut: ein Spring-Boot-Service
liefert eine REST-Schnittstelle, eine Angular-SPA bedient sie. Beide Seiten
sind in einem Repository enthalten (Modulith mit getrennten Builds).

## Entscheidungen im Überblick

| Thema | Entscheidung | Begründung | ADR |
|-------|--------------|------------|-----|
| Backend-Architektur | **Ports & Adapters** (hexagonal) | Fachlogik ohne Framework testbar, Persistenz austauschbar | [ADR 0001](../docs/adr/0001-backend-ports-and-adapters.md) |
| Frontend-Architektur | **Feature-basiert** (Manfred Steyer) mit Architektur-Matrix | Feature = bounded context, Grenzen per Sheriff prüfbar | [ADR 0002](../docs/adr/0002-frontend-feature-architecture.md) |
| Datenbank | PostgreSQL 17, Migrationen über Flyway | Schema-Historie im Repo, `ddl-auto: validate` erzwingt Passgenauigkeit | — |
| API-Stil | REST/JSON, `ProblemDetail` für Fehler | Standard, keine zusätzliche Verhandlung | — |
| Zustand im Frontend | Signals, `httpResource` für Serverdaten | Angular-22-Standard, zoneless-tauglich | — |
| Modulgrenzen | Sheriff als ESLint-Plugin | Grenzen maschinell prüfen, nicht nur im Review | — |
| Teststrategie | Tests je Schicht, Testcontainers für die DB | Fehler auf der tiefsten Ebene finden | — |
| Tageslauf externer Quellen | **Serverseitiger Scheduler** in derselben Anwendung (`@Scheduled`), kein Worker, keine Queue | Eine Deploymenteinheit (ADR 0003); geplanter und sofortiger Lauf teilen einen Use Case | [ADR 0005](../docs/adr/0005-taeglicher-serverlauf-fuer-externe-quellen.md) |
| ICS-Auswertung | **iCalendar-Bibliothek** `org.mnode.ical4j:ical4j` 4.4.0, ausschließlich im ausgehenden Adapter | Formatwissen (Zeilenfaltung, Maskierung, Zeitzonen) wird nicht im Projekt gepflegt; die Anwendung bleibt frei von Fremdformaten | [ADR 0006](../docs/adr/0006-ical4j-fuer-die-ics-auswertung.md) |
| Haushaltsbenachrichtigung | **Eigener Fach-Kontext** „Benachrichtigung"; Telegram-Transport nur als Adapter, Müllkalender bleibt read-only | Zustellzustand und Chat-Verwaltung sind eigenes Fachmodell; Kontextgrenzen laufen über In-Ports | [ADR 0007](../docs/adr/0007-telegram-benachrichtigung-als-eigener-kontext.md) |

## Muster

| Muster | Ort |
|--------|-----|
| Ports & Adapters | Backend: `domain` / `application` / `adapters` |
| Ports & Adapters | Frontend: `features/<f>/{model,data,ui}` |
| Composition Root | `config/ApplicationConfig`, Spring-Konfiguration |
| Feature-Slices / vertical slices | Ein Issue = ein Branch = ein Feature |
| Gate-Pattern | Subagenten `architektur-reviewer` und `kritischer-tester` |
| Scheduler als eingehender Adapter | `adapters/inbound/scheduler/` — löst einen Use Case aus, enthält keine Fachlogik |
| Fremdprotokoll im Adapter | ICS-Format in `adapters/outbound/ics/` (ADR 0006), Telegram-Bot-API in `adapters/outbound/telegram/` (ADR 0007) — die Ports bleiben protokollfrei |
| Zustellvermerk | Versuchszählung und Fehlerzustand liegen **persistent** in der Anwendung, nicht im Adapter — der Adapter kennt nur „gesendet / nicht gesendet" |
| Fachfehler als eigener Typ | `TelegramDeliveryException` (Zustellfehler, wird wiederholt) und `TelegramProtocolException` (Protokollfehler, wird nicht wiederholt) — die Unterscheidung entscheidet über die Wiederholung |
| Geheimnis als eigener Typ | `config/TelegramConfig.TelegramToken` statt eines nackten `String`-Bean — verhindert Kollision und Preisgabe über `toString()` |

## Technologieentscheidungen im Detail

### Warum Ports & Adapters statt Schichtenarchitektur?

Eine Schichtenarchitektur (Controller → Service → Repository) koppelt die
Fachlogik an Spring und JPA: Jede Regel braucht dann eine Datenbank zum
Testen, und ein Persistenzwechsel reißt die Fachlogik mit. Die hexagonale
Variante legt die Ports in die Anwendung und die Adapter an den Rand —
der Preis ist eine höhere Zahl an Klassen (Fachmodell und Persistenzmodell
sind getrennt).

### Warum Feature-Ordner statt eines flachen `shared/`-Baums?

Ein flacher Baum wächst unkontrolliert, und es bleibt unklar, welcher Code
zu welchem Feature gehört. Feature-Ordner machen die Zugehörigkeit am
Pfad ablesbar; die Matrix-Subordner (`model`, `data`, `ui`) verhindern,
dass ein dumb Component den Datenzugriff anfasst.

> **Entschieden (ADR 0003):** Fach-Contexte sind Module derselben
> Anwendung, kein eigener Service je Context.
> **Offen:** Mandantenfähigkeit (Context pro Haushalt statt pro Anwendung).

### Warum eine iCalendar-Bibliothek statt einer eigenen Auswertung?

Der Feed des Entsorgers ist iCalendar; die Auswertung des Formats ist
Fachwissen, das nicht im Projekt gepflegt werden soll. Statt eigener
Formatlogik kommt `org.mnode.ical4j:ical4j` **4.4.0** zum Einsatz — die
4.x-Reihe ist die einzige auf Maven Central verfügbare und zieht **weder
`javax.mail` noch `jakarta.mail`** (der Fallstrick der 3.x-Reihe). Sie läuft
auf Java 25 (Class-File-Major 55) und kollidiert nicht mit dem
Dependency-Management von Spring Boot 4.1.2 (Details in ADR 0006).

Die Bibliothek bleibt **vollständig im ausgehenden Adapter**
(`adapters/outbound/ics/`): `application/port/out/WasteCalendarFeed.java` ist
**bibliotheksfrei**, Domäne und Anwendung kennen kein iCalendar. Der Adapter
übersetzt die Bibliothekstypen in das Fachmodell (`WasteEvent`) und hält die
fachliche Grenze: nur Ganztagsdaten sind Termine, ein unlesbares Dokument ist
ein **Fehler** (Bestand bleibt) und kein Leerfall (Bestand wird geleert).

### Warum die Benachrichtigung ein eigener Kontext ist?

Der Müllkalender ist read-only und endet an seiner Grenze. Die Benachrichtigung
braucht ein eigenes Fachmodell: **Chat** (Kennung, Anzeigename, Art,
Haushalt, freischaltende Person), **Freischalt-Code** und **Zustellvermerk**
(Ziel, Inhalt, Versuchszählung, Fehler). Keines dieser Begriffe gehört zum
Mülltermin — sie gehören zum **Zustellvorgang**.

Der Kontext liest die Termine deshalb über den **In-Port des
Müllkalender-Kontexts** und fasst dessen Tabellen nicht an (ADR 0003, 0007).
Damit bleibt der Müllkalender unangetastet: ein späterer Kanal (E-Mail) oder
ein späteres Ereignis (Aufgaben) ergänzt den Benachrichtigungs-Kontext, ohne
den Müllkalender zu berühren.

Die Telegram-Kenntnis (Chat-IDs, Bot-Token, `sendMessage`, Polling) bleibt im
Adapter — dasselbe Muster wie das ICS-Format in ADR 0006. Der Port bleibt
protokollfrei: die Anwendung weiß, dass sie „einen Chat benachrichtigt", nicht
wie Telegram das tut.

Der **Empfang** läuft über Long-Poll, nicht über einen Webhook: die
Anwendung fragt Telegram aktiv ab. Das hält die Betriebsannahme ein — nur
Egress, kein öffentlich erreichbarer Endpunkt, kein Zertifikat, kein
zusätzlicher Ingress (ADR 0007, O1).

Der Bot kennt **keine Person**. Damit die Trennberechtigung trotzdem
eindeutig bleibt, trägt der **Freischalt-Code** die anfordernde Person: der
Code entsteht beim ersten Anfordern im Namen eines Mitglieds, und die
Freischaltung über diesen Code gehört dieser Person. Der Bot sieht nur den
Code (ADR 0007).

### Warum der Tageslauf in derselben Anwendung?

Ein externer Scheduler oder ein Worker wäre eine zweite Deploymenteinheit und
damit ein Bruch mit ADR 0003. Der geplante und der sofortige Lauf (nach dem
Speichern einer Quelle) rufen denselben Use Case auf — die Regeln „Lauf
ersetzt den Bestand" und „Quelle nicht lesbar → Bestand bleibt" gelten damit
genau einmal (ADR 0005). Der Sofort-Sync ist dabei ein **zweiter Use-Case-Aufruf
im Controller** (eigene Transaktion, 5-s-Grenze) — ohne Ereignis, Listener oder
`@Async`; die Antwort wartet auf das Ergebnis des Laufs.
