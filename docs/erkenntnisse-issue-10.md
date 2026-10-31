# Erkenntnisse — Issue #10 „Nächste Mülltermine anzeigen und täglich synchronisieren“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #10 (Branch
`feature/10-muelltermine`). Zweck: die Reibung dieses Features festhalten und
die wiederkehrenden Fehler als Regeln überführen.

Belegte Gates (Ende): Backend **177** Tests grün · Frontend **26 Dateien / 115 Tests** grün ·
`tsc` (app + spec), `lint`, `sheriff`, Build clean · Architektur-Review **OK**
(0 BLOCKER, 0 MAJOR, 5 MINOR, 1 NIT) · Abnahme **PASS**, 23/23 ACs belegt ·
Sichtprüfung am laufenden Stack (Nextcloud/OIDC-Anmeldung, 375 px und 1280 px).

Die Abnahme lief fünf Runden; jeder Lauf hat echte Fehler gefunden, die jetzt
behoben und mit einem roten Regressionstest abgesichert sind. Die Abschnitte
1.8 bis 1.12 halten diese Fehler fest — sie sind die Regeln mit dem größten
Wirkungsgrad.

---

## 1. Behobene Probleme

### 1.1 Ganztagsdatum wurde über die Zeitzone gelesen

- **Symptom:** Der Test auf die Terminzeile erhielt für `2026-01-09` ein Datum, das nicht
  mit dem gespeicherten Tag übereinstimmte; ein anderer Test erwartete für einen
  UTC-Zeitstempel das Datum des Vortags.
- **Ursache:** `DatePipe` interpretiert einen Datumsstring ohne Uhrzeit als Moment einer
  Zeitzone und formatiert ihn in der Zeitzone der Ausführung. Für einen **ganztägigen**
  Termin (R12) ist das falsch: in einer Zeitzone hinter UTC erscheint der vorherige Tag.
- **Nachbesserung:** `features/waste/model/waste.model.ts` — `formatDay()` liest das Datum
  aus den Teilen des ISO-Werts, ohne Zeitzone; `waste-collection-list.html` zeigt dieses
  Bild. Für einen echten Zeitpunkt (Stand des letzten Laufs) bleibt `DatePipe` richtig: der
  Leser sieht die Ortszeit, und der Test leitet sein erwartetes Bild aus demselben
  Zeitpunkt ab, statt es fest zu schreiben.
- **Lehre:** Ein Test, der ein formatiertes Datum vergleicht, darf nicht von der Maschine
  abhängen, auf der er läuft. Regel im Skill `feature-testing` (Fallen-Tabelle).

### 1.2 Fehlermeldung im Formular erschien im Test nicht

- **Symptom:** Die Zeichen-Zähler-Assertion war grün, die Meldung „höchstens 500 Zeichen“
  fehlte.
- **Ursache:** Signal Forms zeigt die Validator-Meldung nur, wenn das Feld **touched** ist.
  Der Test hat den Wert gesetzt, aber nie ein `blur` ausgelöst.
- **Nachbesserung:** Im Test wird nach der Eingabe `blur` dispatcht, dann die Assertion
  durchgeführt.
- **Lehre:** Eingabe ist nicht Berührung. Regel im Skill `feature-testing`.

### 1.3 Zoneless: Request vor der Erwartung

- **Symptom:** `httpMock.expectOne('/api/households/2/waste')` fand keinen Request, obwohl
  der Workspace gewechselt wurde (AC13).
- **Ursache:** Effekte laufen erst nach `detectChanges()` — die Regel bestand bereits, der
  neue Test hat sie verletzt.
- **Nachbesserung:** Signale setzen → `detectChanges()` → Request erwarten → flush →
  Makrotask.

### 1.4 Der geplante Tageslauf war ohne Test auf seinem Zeitpunkt

- **Symptom:** Architektur-Review K1 (MAJOR): AC2 war nur zur Hälfte belegt. Der
  Adapter-Test prüft die Ausführung über alle Haushalte; der konfigurierte Zeitpunkt und die
  Zeitzone waren nirgends geprüft.
- **Nachbesserung:** `config/ApplicationConfigTest` ruft `configureTasks`, prüft den
  registrierten Cron-Ausdruck, rechnet die nächste Ausführung aus einem festen Zeitpunkt
  (03:00 `Europe/Berlin` = 02:00 UTC) und belegt, dass der registrierte Lauf den Adapter
  aufruft.
- **Lehre:** Konfiguration ist Verhalten. Wenn eine Anforderung „täglich um X in Zone Y“
  sagt, gehört die Prüfung an den Composition Root — nicht an den Adapter, der nur ausführt.

### 1.5 Das neue Feature war nicht gekennzeichnet

- **Symptom:** `npm run sheriff` war grün, obwohl die Modulgrenzen des neuen Features
  gar nicht geprüft wurden.
- **Ursache:** `frontend/sheriff.config.ts` trägt seine Module einzeln (`autoTagging: false`).
  Ein neues Feature ist erst geschützt, wenn es dort steht — grün ist dann keine Aussage.
- **Nachbesserung:** `domain:waste` mit `feature`/`model`/`data`/`ui` eingetragen; die
  Prüfung bleibt grün.
- **Lehre:** Ein neues Feature gehört in die Sheriff-Konfiguration, sonst ist die
  Architekturprüfung für es wirkungslos.

### 1.6 Der Composition Root schloss einen Bean-Zyklus

- **Symptom:** Der Container startete nicht: „The dependencies of some of the beans in the
  application context form a cycle“ — `ApplicationConfig → WasteSyncJob →
  HouseholdUseCaseImpl → MembershipUseCaseImpl → ApplicationConfig`.
- **Ursache:** `ApplicationConfig` war zugleich Verdrahtung der Planung (hängt am
  Scheduler-Adapter) und Lieferant von Beans, die Use Cases brauchen (`Clock`,
  `icsRestClient`). Der zweite Weg über den ICS-Adapter war derselbe Fehler.
- **Nachbesserung:** die Beans liegen in `config/ClockConfig` und `config/IcsClientConfig`;
  `ApplicationConfig` liefert nichts mehr, sie trägt nur die Cron-Freigabe. Der Start ist
  grün, 158 Backend-Tests grün.
- **Lehre:** Ein Composition Root, der selbst von einem Adapter abhängt, darf keine Bean
  liefern, die eine Use Case braucht. Dieser Fehler ist mit Tests nicht zu finden, solange
  kein Test den vollen Kontext aufbaut — er zeigt sich erst beim Start. Regel im AGENTS.md
  (Gotchas) und Guard-Test in `ApplicationConfigTest`.

### 1.7 `webcal://` konnte nicht gespeichert werden

- **Symptom:** `webcal://www.abfalltermine-forchheim.de/…/ics?...` wurde abgelehnt
  („muss mit http:// oder https:// beginnen“).
- **Ursache:** `webcal://` ist die Konvention für einen ICS-Link — dieselbe Adresse, nur als
  Abonnement bezeichnet. Der Abrufadapter kann ein Schema, das er nicht kennt, nicht lesen,
  und R18 ließ nur http/https zu.
- **Nachbesserung:** `WasteCalendar.validatedSourceUrl` setzt eine `webcal://`-Adresse auf
  `https://` und speichert diese Form; andere Schemata bleiben abgelehnt. Test:
  `HouseholdUseCaseImplTest.setWasteSource_normalizesWebcalToTheRetrievableUrl`. Die
  Oberfläche zeigt nach dem Speichern die abrufbare Adresse.
- **Lehre:** Eine Eingabe, die Nutzer aus einem Kalenderprogramm kopieren, ist Teil der
  Fachregel — nicht nur der Zeichenanzahl. Was gespeichert wird, muss die tatsächlich
  abrufbare Adresse sein. Regel im Issue #10 nachgetragen (R18).

### 1.8 `maxLength()` wurde zum stillen Abschneiden (MAJOR)

- **Symptom:** Eine zu lange ICS-Adresse wurde **gekürzt und gespeichert** — ohne
  Fehlermeldung, ohne roten Zustand. Der Nutzer glaubte, die Adresse sei übernommen.
- **Ursache:** `maxLength()` aus `@angular/forms/signals` spiegelt sich als **natives
  `maxlength`-Attribut** in das Eingabefeld. Der Browser schneidet den Wert beim Einfügen
  (Paste!) still ab — die Oberfläche sieht einen gültigen Wert und der Validator greift nie.
  Der Fehler lag damit **außerhalb** der Anwendung: im Browser.
- **Nachbesserung:** eigener Validator statt `maxLength()`:
  `validate(schema.url, …)` mit `maxLengthError(…)` — er prüft den **getrimmten** Text (so
  messen es auch Zähler und Speichern-Aktion) und erzeugt **kein** `maxlength`-Attribut.
  Tests: `waste-calendar-section-limit.spec.ts` (Grenze sichtbar), `-native-limit.spec.ts`
  (kein natives Attribut), `-trim.spec.ts` (getrimmt messen).
- **Lehre:** Ein Validator, der eine Browser-Fähigkeit spiegelt, ist nicht dasselbe wie eine
  Prüfung. Wird die Grenze im Browser durchgesetzt, ist sie **unsichtbar** — die Anzeige
  (Zähler, Fehlermeldung, gesperrter Knopf) ist dann die eigentliche Prüfung.

### 1.9 Die Seite las beim erneuten Betreten nicht neu (MAJOR)

- **Symptom:** Nach dem Speichern einer Quelle zeigte die Seite „Mülltermine" weiterhin den
  alten Stand. Der Nutzer glaubte, das Speichern habe nicht gewirkt (R6: „die Anzeige zeigt
  den Stand beim nächsten Aufruf“).
- **Ursache:** Zwei Ursachen, die zusammenspielten:
  1. `httpResource` feuert nur, wenn sich die **Request-URL** ändert. Bleibt der Haushalt
     gleich, kommt kein zweiter Request.
  2. Der route-scoped Provider überlebt den Wegwechsel — die Route wird nicht zerstört,
     also bleibt die Instanz mit ihrem alten Ressourcenstand am Leben.
- **Nachbesserung:** `provideRouter(routes, withAutoCleanupInjectors())` im Composition
  Root — der Injector der Route wird beim Verlassen zerstört, das Feature ist beim erneuten
  Betreten eine frische Instanz. Zusätzlich `WasteData.onEnter()` als Sicherung für den
  Fall, dass eine `RouteReuseStrategy` Handles behält. Belegt: genau **ein** GET je Betreten
  (`waste-page-reload-production.spec.ts`, Live-Messung über `performance.getEntriesByType`).
- **Lehre:** „Route-scoped Provider“ heißt nur: gültig, solange die Route aktiv ist. Wer sich
  darauf verlässt, muss die Zerstörung **einrichten** — sonst lebt der Zustand weiter. Und
  ein `httpResource` ist kein Abonnement, sondern ein Request bei URL-Änderung.

### 1.10 „Veraltet“ war nach dem ersten Fehlschlag falsch (MAJOR)

- **Symptom:** Nach einem **ersten** fehlgeschlagenen Lauf meldete die Übersicht
  `stale = false`. Die Anzeigte behauptete einen aktuellen Stand, obwohl es nie einen
  erfolgreichen Lauf gab.
- **Ursache:** `WasteCalendar.isStale()` verlangte einen erfolgreichen Lauf
  (`lastSyncedAt != null`), um überhaupt veraltet sein zu können. „Kein Stand“ war damit
  gleich „nicht veraltet“.
- **Nachbesserung:** veraltet, wenn ein Versuch nach dem letzten erfolgreichen Lauf liegt
  **oder** es (noch) keinen erfolgreichen Lauf gab, aber ein Versuch stattfand. Ohne jeden
  Versuch bleibt der Stand unveraltet — die Anzeige nennt dann „Noch keine Synchronisation“
  (R16). Tests: `WasteCalendarStaleTest` (Regeltest im Fachkern),
  `WasteCollectionFirstRunFailureTest` (Verhalten über die Anwendung), `waste-page-stale.spec.ts`.
- **Lehre:** Ein Zustand, den die Oberfläche in zwei Varianten zeigt („alter Stand“ und „gar
  kein Stand“), braucht **zwei** Felder im Modell — nicht ein Boolean, das beide Fälle
  vermischt. „Fehlgeschlagen“ ist ein eigener Zustand, kein Sonderfall von „erfolgreich“.

### 1.11 Der Leerzustand brauchte die Standzeile (MAJOR)

- **Symptom:** Ohne hinterlegte Quelle fehlte die ganze Anzeige „Quelle und Stand“ — die
  Zeile „Stand: Noch keine Synchronisation“ kam nicht (AC16).
- **Ursache:** Die Seite rendert das Panel nur im Zweig „Quelle vorhanden“. Der Leerfall
  zeigte nur einen Textabsatz.
- **Nachbesserung:** Das Panel wird **immer** gerendert; Quelle und Stand sind eigene
  Zustände („Kein Müllkalender hinterlegt“ / „Noch keine Synchronisation“). Zusätzlich die
  Primäraktion „Zur Haushaltsverwaltung“. Test: `waste-page-no-source.spec.ts`.
- **Lehre:** Die Zustandstabelle der `## UX` nennt **Kombinationen** von Zuständen. Ein
  Aufbau, der die Zustände ineinander verschachtelt (`@if` auf dem einen, dann der andere),
  kann eine geforderte Kombination gar nicht anzeigen. Jede Zustandszeile gehört an eine
  eigene Stelle, unabhängig vom Zustand der anderen.

### 1.12 Responsive Verhalten war nicht implementiert (MAJOR)

- **Symptom:** Bei 375 px Viewport lief die Seite 608 px breit (waagerechter Scrollbalken),
  die Terminzeile blieb einzeilig und die Aktionen standen nebeneinander.
- **Ursache:** In `waste-source-panel.scss`, `waste-collection-list.scss` und
  `waste-calendar-section.scss` stand **keine einzige** `@media`-Regel.
- **Nachbesserung:** Rasterlayouts (`grid-template-columns`) mit einem Breakpoint bei
  767 px: Panel einspaltig mit linksbündigem Wert, Terminzeile gestapelt, Aktionen
  untereinander; auf dem Desktop Wert rechtsbündig. Am laufenden Stack gemessen.
- **Lehre:** Die `## UX` ist eine **Anforderung**, keine Beschreibung. „Unter 768 px
  gestapelt“ ist so verbindlich wie eine Regel — und Geometrie ist in jsdom nicht prüfbar:
  sie gehört ins Bild der Sichtprüfung, nicht in den Unit-Test. Der waagerechte Overflow
  selbst kam aus dem App-Kopf (`core`) und ist als **Issue #14** ausgelagert.

---

## 2. Werkzeug und Umgebung

- **Node:** Angular 22 verlangt Node ≥ 22.22.3; lokal ist 22.22.1. Frontend-Tests laufen
  über `docker run … node:22.22.3 npm test`.
- **Backend:** lokales JDK fehlt → `maven:3.9-eclipse-temurin-25`. Der Maven-Cache
  `-Dmaven.repo.local` muss dem ausführenden Nutzer gehören; ein vom Container angelegter
  Cache führt zu `AccessDeniedException`.
- **Signal-Forms-Introspection:** `CronTrigger` liefert keine Zone ablesbar; die Zone ist
  nur über die berechnete Ausführung belegbar.
- **Sichtprüfung am laufenden Stack:** Die Anwendung meldet sich über Nextcloud/OIDC an; die
  Nextcloud-Consent-Seite („Allow“) überlagert den Button — ein normaler Klick schlägt fehl,
  ein Klick im DOM wirkt. Der Anmeldezustand liegt im `sessionStorage`. Playwright lag lokal
  vor, ist aber **keine** Projektabhängigkeit; die Grundlage dafür ist **Issue #13**.
- **Testdaten am laufenden Stack:** Zwei Testhaushalte eines Testkontos („Leerfall“ ohne
  Quelle, „Kersbach“ mit Quelle und 12 echten Terminen). Jeder Prüflauf, der Quellen setzt
  oder entfernt, muss den Ausgangszustand wiederherstellen — sonst prüft der nächste Lauf
  einen anderen Zustand.

---

## 3. Abnahme und Review — was der Prüfer gefunden hat

Die Abnahme lief fünf Runden, das Architektur-Review zwei. Die Befunde, die zu MAJOR
wurden, sind in Abschnitt 1.8 bis 1.12 festgehalten. Übrig blieben fünf MINOR-Befunde aus
dem Review (keiner blockierend):

1. Der Reload-Mechanismus existiert doppelt (Auto-Cleanup **und** `onEnter()`); der zweite
   trägt nur die isolierte Testkonfiguration. → entschärft durch einen zutreffenden Kommentar.
2. Deutsche Identifier (`besucht`, `beimBetreten`) im Produktionscode → umgestellt auf
   `visited` / `onEnter()` (Identifier im Code sind englisch, `AGENTS.md`).
3. Das Verhalten „Feld während des Speicherns gesperrt“ war implementiert, aber unbelegt →
   `waste-calendar-section-saving.spec.ts`.
4. Die Regel `isStale()` war nur indirekt belegt → `WasteCalendarStaleTest` (Regeltest im
   Fachkern).
5. Die app-weite Router-Option war in `arc42/` nicht erwähnt → `arc42/06` (Lebenszyklus der
   route-scoped Provider) und `arc42/09` (Entscheidungstabelle) nachgetragen.

## 4. Offene Kleinbefunde (Follow-ups)

| Issue | Gegenstand |
|-------|-----------|
| [#11](https://github.com/Sadeeg/HouseholdManager/issues/11) | Testgenauigkeit aus der Abnahme — zwischenzeitlich durch die neuen Tests geschlossen |
| [#13](https://github.com/Sadeeg/HouseholdManager/issues/13) | Playwright-Grundlage für End-to-End-Tests (E2E-Basis) |
| [#14](https://github.com/Sadeeg/HouseholdManager/issues/14) | App-Kopf läuft bei schmalen Viewports seitlich aus (`core`, vorbestehend) |
| [#15](https://github.com/Sadeeg/HouseholdManager/issues/15) | Spec-Dateien des Waste-Features je Produktionsklasse bündeln |
| [#16](https://github.com/Sadeeg/HouseholdManager/issues/16) | Abruf der ICS-Adresse gegen interne Ziele absichern (SSRF) |

Die vier Befunde der ersten Abnahme (AC3 Fensterfilter, AC4 Reihenfolge, AC7 Bezug zu
Aufgaben, AC23 Entfernen) sind inzwischen durch neue Tests geschlossen: der Use Case setzt
das 42-Tage-Fenster selbst (`termsInWindow`), die Reihenfolge wird über `waste-data.terms()`
geprüft, `WasteSyncLeavesTasksUntouchedTest` belegt AC7 als **Verhalten** gegen Postgres, und
das Entfernen läuft nur nach der Bestätigung (eigener Test in `households-page.spec.ts`).
