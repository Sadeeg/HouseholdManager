# Erkenntnisse — Issue #10 „Nächste Mülltermine anzeigen und täglich synchronisieren“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #10 (Branch
`feature/10-muelltermine`, PR offen). Zweck: die Reibung dieses Features festhalten und
die wiederkehrenden Fehler als Regeln überführen.

Belegte Gates: Backend 157 Tests grün · Frontend 16 Dateien / 90 Tests grün · `tsc`,
`lint`, `sheriff`, Build clean · Architektur-Review **OK** (Round 2, nach Korrektur K1) ·
Abnahme **PASS**, 23/23 ACs belegt. **Nicht durchgeführt:** Sichtprüfung und manueller
Browserpfad — die UI-Anteile sind nur durch Tests belegt, jsdom kennt keine Geometrie.

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

---

## 2. Werkzeug und Umgebung

- **Node:** Angular 22 verlangt Node ≥ 22.22.3; lokal ist 22.22.1. Frontend-Tests laufen
  über `docker run … node:24 sh -c "npm test -- --no-progress"`.
- **Backend:** lokales JDK fehlt → `maven:3.9-eclipse-temurin-25`. Der Maven-Cache
  `-Dmaven.repo.local` muss dem ausführenden Nutzer gehören; ein vom Container angelegter
  Cache führt zu `AccessDeniedException`.
- **Signal-Forms-Introspection:** `CronTrigger` liefert keine Zone ablesbar; die Zone ist
  nur über die berechnete Ausführung belegbar.

---

## 3. Offene Kleinbefunde (Follow-up Issue #11)

Vier Punkte aus der Abnahme, alle MINOR, alle Testgenauigkeit — das Verhalten ist richtig,
der Test beweist weniger als er behauptet:

1. **AC3 (Fensterfilter)** wird nur auf Adapterebene geprüft; der Use Case filtert nicht
   selbst, sein Test prüft nur das übergebene Fenster.
2. **AC4 (Reihenfolge)** ist über den Render-Pfad nicht bewiesen: der Test prüft die
   Durchgabeordnung, nicht `waste-data.terms()`.
3. **AC7 (kein Bezug zu Aufgaben/Ausgaben)** ist ein Strukturtest über Feldtypen — er kann
   eine zur Laufzeit erzeugte Aufgabe nicht widerlegen.
4. **AC23 (Entfernen)** beweist nur die Wiederherstellung des Feldes, nicht dass kein
   Schreibpfad ausgelöst wurde.

Dazu: die AC-Labels in `features/waste/waste-page.spec.ts` sind gegenüber dem Issue
verschoben (AC13/AC14/AC15) — Inhalt korrekt, Bezeichnungen falsch.
