# ADR 0005 — Täglicher Serverlauf für externe Terminquellen

- **Status:** angenommen
- **Datum:** 2026-10-31
- **Entscheidung:** Termine aus einer externen Quelle (ICS-Feed) werden **serverseitig** in einem **geplanten Tageslauf** innerhalb derselben Spring-Boot-Anwendung synchronisiert. Es gibt keinen externen Scheduler, keine Message-Queue und keinen Worker-Prozess.

## Kontext

Issue #10, R4/AC2: „Ein geplanter Lauf synchronisiert täglich alle Haushalte mit
hinterlegter Quelle." Der Bestand hat **keinen** Scheduler und **keinen**
HTTP-Client für öffentliche Feeds (Gotchas im Issue). Nach ADR 0001/0003 ist
HouseholdManager **eine** Deploymenteinheit; ein Worker wäre eine zweite.

Der Tageslauf darf die Anwendung nicht blockieren und muss testbar sein: die
Fenstergrenze (42 Tage, R5) hängt am Tag des Laufs, und ein Test darf nicht auf
die Tagesuhr warten.

## Entscheidung

- **Trigger:** `@EnableScheduling` in `config/ApplicationConfig`; der Auslöser
  ist ein eingehender Adapter `adapters/inbound/scheduler/WasteSyncJob` mit
  `@Scheduled(cron = …)`, der genau einen Use Case aufruft
  (`WasteCollectionUseCase.syncAllHouseholds()`). Der Zeitpunkt liegt fest im
  Server: **03:00 Europe/Berlin** (Annahme 1 im Issue), übersteuerbar per
  Property (`householdmanager.waste.sync-cron`, Vorgabe `0 0 3 * * *`) und
  **nicht** durch die Oberfläche einstellbar.
- **Taktung:** einmal täglich.
- **Zeit ist eine Abhängigkeit:** gerechnet wird ausschließlich mit der
  `Clock`-Bean (`config/ApplicationConfig`). Die Fenstergrenze wird aus
  `LocalDate.now(clock.withZone(ZoneId.of("Europe/Berlin")))` abgeleitet — der Tag
  des Laufs im Betriebsgebiet, nicht in UTC. Der Job kennt keine eigene Uhr.
- **Fehlerisolierung:** ein fehlgeschlagener Lauf **beendet** den Job nicht. Der
  Use Case fängt den Fehler je Haushalt, protokolliert ihn und fährt mit dem
  nächsten Haushalt fort. Der Bestand des betroffenen Haushalts bleibt
  unverändert (R7), sein Stand gilt als veraltet (R13).
- **Ein Codepfad:** die sofortige Synchronisation nach dem Speichern einer
  Quelle (R6/AC11) ruft denselben Use Case auf (`syncHousehold(householdId)`);
  geplanter und sofortiger Lauf teilen damit die Regeln „Lauf ersetzt den
  Bestand" (R5/R8) und „Quelle nicht lesbar → Bestand bleibt" (R7).
- **Sofort-Sync (R6/AC11) als zweiter Use-Case-Aufruf:** Der Controller ruft
  **zwei Use Cases nacheinander** auf: erst den Use Case zum Speichern der
  Quelle (Transaktion 1, endet mit dem Methodenaufruf), danach den Use Case zur
  Synchronisation des Haushalts (Transaktion 2). Der Abruf der Quelle hat ein
  **eigenes Zeitlimit von 5 s**. Es gibt **kein** Ereignis, **keinen** Listener,
  kein `@Async`/`@EnableAsync` — der Mechanismus bleibt in der Anwendung. Die
  HTTP-Antwort **wartet** auf das Ergebnis der Synchronisation: der Nutzer sieht
  die Termine direkt nach dem Speichern. Der Tageslauf wird nicht abgewartet.

## Konsequenzen

**Positiv**

- Die Regeln des Laufs existieren genau einmal — im Use Case, ohne Spring.
- Testbar ohne Wartezeit: der Job ist ein dünner Adapter, die Logik liegt im
  Use Case (JUnit + Mockito, `Clock` fest).
- Kein zweiter Betriebsbaustein (kein Worker, keine Queue) — ADR 0003 bleibt
  gewahrt; `compose.yml` und die Dockerfiles bleiben unverändert.
- Keine zusätzlichen Klassen und keine Zyklen zwischen Use Case und Adapter: der
  Sofort-Sync ist ein zweiter Methodenaufruf. Der Nutzer sieht die Termine
  direkt nach dem Speichern.

**Negativ / Kosten**

- Die Anwendung braucht ausgehenden Netzzugriff (Egress zum Entsorger) — siehe 07.
- Der Lauf hängt an der Verfügbarkeit des Backends; ein ausgefallenes Backend
  verschiebt den Lauf (kein Nachholen, keine Historie).
- Die HTTP-Antwort wartet bis zu **5 s** auf den Abruf der Quelle; die
  Oberfläche zeigt währenddessen „Wird gespeichert …". Bei nicht erreichbarer
  Quelle wartet der Nutzer die vollen 5 s — das **Speichern selbst bleibt
  erfolgreich**, der Lauf-Fehler wird isoliert und der Stand gilt als veraltet.

**Neutral**

- Ein manueller Synchronisations-Button ist bewusst nicht vorgesehen (Out of Scope
  im Issue).
- Ein mehrere Instanzen laufendes Backend führt den Lauf je Instanz aus — das ist
  unkritisch, weil der Lauf den Bestand je Haushalt **ersetzt** (R5), nicht
  anhängt.
