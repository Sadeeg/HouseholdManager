# ADR 0006 — ICS-Auswertung mit iCalendar-Bibliothek (ical4j)

- **Status:** angenommen
- **Datum:** 2026-10-31
- **Entscheidung:** Der ICS-Feed wird mit einer **etablierten iCalendar-Bibliothek** ausgewertet: `org.mnode.ical4j:ical4j` in der Version **4.4.0**. Die Bibliothek bleibt **vollständig im ausgehenden Adapter** (`adapters/outbound/ics/`); Domäne und Anwendung kennen kein iCalendar.

## Kontext

Der Feed des Entsorgers ist iCalendar: `VCALENDAR`/`VEVENT` mit
`DTSTART;VALUE=DATE` (Ganztagsdatum ohne Uhrzeit), `SUMMARY` (Tonnenart) und
`UID`. Die fachliche Anforderung (R9, R12, AC8, AC19) lautet: Tonnenart und
Datum **unverändert** übernehmen; das Werkzeug kennt keine feste Liste von
Tonnenarten (Abweichungen-Tabelle im Issue).

Die Auswertung des Formats — Zeilenfaltung, Maskierung, Zeitzonen,
Wiederholungen — ist Fachwissen, das nicht im Projekt gepflegt werden soll.
Der ARC-Agent hatte zunächst eine **eigene, kleine Auswertung** vorgeschlagen;
der Auftraggeber hat entschieden: **Bibliothek statt eigener Auswertung**.

## Entscheidung

- **Koordinaten und Version:** `org.mnode.ical4j:ical4j:4.4.0` (Maven Central,
  veröffentlicht 2026-10-02). **Nicht** `com.github.ical4j:ical4j` (auf Maven
  Central nicht vorhanden) und **nicht** die 3.x-Reihe `net.fortuna.ical4j:ical4j`
  (auf Maven Central nicht vorhanden; sie zog `javax.mail` — Konflikt mit Jakarta).
- **Ort:** ausschließlich `adapters/outbound/ics/`.
  `application/port/out/WasteCalendarFeed.java` bleibt **bibliotheksfrei** — Domäne
  und Anwendung kennen weder `Calendar` noch `VEvent` noch `org.threeten.bp`.
  Der Adapter übersetzt Bibliothekstypen in das Fachmodell (`WasteEvent`:
  Tonnenart, Datum, `UID`).
- **Die Bibliothek ergänzt die Anwendung, sie ersetzt sie nicht:** `application`
  und `domain` bleiben ohne Spring, JPA und Bibliothekstypen (ADR 0001).
- **Die fachlichen Grenzen bleiben beim Adapter:** `LOCATION`, `DTSTAMP` und
  `END` werden nicht übernommen (Abweichungen-Tabelle im Issue); die Tonnenart
  wird **nicht** normalisiert. Der Adapter ist **tolerant** gegenüber
  Formatvarianten: ein Ereignis **mit Uhrzeit** wird auf seinen Kalendertag in
  **Europe/Berlin** abgebildet, eine **Wiederholung** (`RRULE`) auf das
  42-Tage-Fenster expandiert, ein Ereignis **ohne verwendbares Startdatum**
  ignoriert. Das Fachmodell bleibt ganztägig (R12) — die Abbildung macht der
  Adapter.
- **Formatfehler ist nicht „leere Quelle":** Ein Dokument, das die Bibliothek
  nicht lesen kann, ist ein **Fehler** (Quelle nicht lesbar → Bestand bleibt,
  R7/AC6); ein gültiges `VCALENDAR` ohne Termine im Fenster ist ein **Leerfall**
  (Bestand wird geleert, R8/AC13). Diese Unterscheidung entscheidet der
  **Adapter** an der Auswertung, nicht die Anwendung.

## Prüfung der Tauglichkeit (Stand 2026-10-31)

| Prüfpunkt | Ergebnis |
|-----------|----------|
| Verfügbarkeit | Maven Central, `org/mnode/ical4j/ical4j/4.4.0`; `lastUpdated` 2026-10-02; Jar und POM geprüft |
| Java | Class-File-Major **55** (Java 11) → läuft auf **Java 25**; keine Blockade |
| Spring Boot 4.1.2 | **Kein** Mail-Konflikt: 4.x zieht **weder `javax.mail` noch `jakarta.mail`** (das ist der Fallstrick der 3.x-Reihe) |
| Transitive Abhängigkeiten | `slf4j-api` 2.0.12, `commons-codec` 1.17.0, `commons-lang3` 3.18.0, `org.threeten:threeten-extra` **1.8.0**; optional und damit **nicht** transitiv: caffeine, jparsec, groovy |
| Versionskonflikte | Boot 4.1.2 verwaltet `slf4j` 2.0.20, `commons-codec` 1.21.0, `commons-lang3` 3.20.0 — jeweils **neuer** als von ical4j gefordert; das Boot-BOM gewinnt, **keine** Downgrades. `threeten-extra` verwaltet Boot **nicht** → es gilt 1.8.0 aus ical4j |
| `threeten-extra` | `org.threeten.bp.*` ist eine **Ergänzung** zu `java.time`, kein Konflikt; Hibernate 7 und Spring Boot 4 ziehen es nicht. ical4j liefert Datumswerte als `org.threeten.bp.LocalDate` — der Adapter wandelt sie in `java.time.LocalDate` |
| Lizenz | **BSD 3-Clause** (`LICENSE.txt` im ical4j-Repository; das POM referenziert es als „iCal4j - License") — permissiv, keine Copyleft-Pflichten |
| Größe | ical4j-Jar rund 1,7 MB; mit commons-codec, commons-lang3 und threeten-extra rund 3 MB zusätzlich im Klassenpfad |
| API | 4.x ist eine neue Hauptversion mit **Package-Verschiebungen** gegenüber 3.x: `CalendarBuilder` liegt in `net.fortuna.ical4j.data` (nicht `…model`), `VEvent` in `net.fortuna.ical4j.model.component`, Datumswerte in `net.fortuna.ical4j.model.Date` |
| Toleranz | ical4j kennt Wiederholungsregeln (`Recur`, `net.fortuna.ical4j.transform.recurrence.*`) und zonenbehaftete Datumswerte — die Auflösung und die Abbildung auf den Kalendertag in Europe/Berlin macht der **Adapter**. **Achtung:** die 3.x-Klasse `CalendarExpander` gibt es in 4.x **nicht**; die Auflösung läuft über die 4.x-Recur-/Transform-Klassen |

> **Offen:** (1) Die Lizenz des **Release-Tags** 4.4.0 — das POM verweist auf `master/LICENSE.txt`;
> (2) die Festlegung, ob `threeten-extra` auf **1.8.0** (von ical4j getestet) bleibt oder auf
> **1.11.0** (auf Central seit 2026-10-15) gehoben wird. Beides in Runde 3 entscheiden.

## Konsequenzen

**Positiv**

- Keine eigene Formatlogik im Projekt: Zeilenfaltung, Maskierung, BOM, CRLF und
  Zeitzonen übernimmt die Bibliothek.
- Die Anwendung bleibt frei von Fremdformaten — die Bibliothek endet an der
  Adaptergrenze.
- Das Verhalten ist gegen eine echte Bibliotheksversion getestet statt gegen eine
  eigene Annahme.

**Negativ / Kosten**

- Eine neue Abhängigkeit im Klassenpfad (rund 3 MB mit Transitiven).
- Die Bindung an eine Bibliothek: 4.x hat die Package-Struktur gegenüber 3.x
  geändert; ein Wechsel auf eine künftige Hauptversion ist ein Codeeingriff im
  Adapter (und sonst nirgends).
- Die Bibliothek bringt ein eigenes Objektmodell und `threeten-extra` mit; die
  Übersetzung in das Fachmodell ist Sache des Adapters und muss getestet werden.

**Neutral**

- Der HTTP-Abruf (Timeout, Fehlerübersetzung) bleibt im selben Adapter
  (`IcsWasteCalendarFeed`); die Bibliothek kümmert sich nur um das Format.
