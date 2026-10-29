---
name: Anforderung-Technisch
description: Use for round 3 of the requirements loop to make an issue technically complete and implementable. Fixes the target modules, data model and migration, API contract, frontend design, file list, test strategy and verify commands. Load this when the functional content of an issue is settled and the technical implementation must be pinned down.
---

# Runde 3 — Technisches Refinement

Der Draft wird technisch so festgelegt, dass ein Agent ohne Rückfrage
implementieren kann: Ort im Code, Datenmodell, API-Vertrag, UI, Dateiliste,
Tests und Verify-Kommandos.

Die Kurzfassung der Konventionen steht in `references/technik-checkliste.md`.
Die verbindliche Quelle bleibt `AGENTS.md` — bei Widerspruch gilt `AGENTS.md`.

**Was nicht in `AGENTS.md` steht, entscheidet Runde 3** — nicht das Gedächtnis,
nicht eine andere Skill. Eine so getroffene Entscheidung wandert danach nach
`AGENTS.md` (Konvention) bzw. `technik-checkliste.md` (Details), damit das
nächste Issue sie nicht neu erfindet.

## Schritte

### 1. Zielort im Code

Für jeden Teil der Umsetzung: welche Schicht, welche Klasse, neu oder
erweiterte? Mit konkreten Namen:

```text
Neu:      endpoint/<Ressource>Controller.java
Neu:      service/<Ressource>Service.java
Erweitert: endpoint/model/<Ressource>Dto.java (Feld <feld> ergänzen)
```

Klassenname, Paket und Verantwortlichkeit benennen. Kein „irgendwo im
Service". Bestehende Klassen und Pfade **zitieren** (mit `find`/`grep`
geprüft), neue Klassen nach der Konvention aus `AGENTS.md` benennen.

### 2. Begriffsabgleich

Der fachliche Begriff aus Runde 2 bekommt seine konkrete Entsprechung. Das
verhindert, dass ein Begriff im Dokument und ein anderer im Feld oder im
UI-Label steht — und dass der Nutzer beim Suchen ins Leere greift.

| Fachbegriff (Runde 2) | DB-Feld | UI-Label | Literal im Ergebnis |
|------------------------|---------|----------|---------------------|
| Standort | `household.adresse` | Adresse | „Standort: …" |

Zeilen, in denen die vier Spalten auseinanderlaufen, sind erlaubt — aber sie
werden **hier** benannt. Stillschweigend entstehen zwei Namen für dasselbe
sonst erst, wenn der Nutzer fragt.

Das Ergebnis steht im Issue unter `## Begriffe`.

### 3. Datenmodell und Migration

**Nur wenn Daten betroffen sind.** Das Migrationswerkzeug und die Ablage
stehen noch nicht fest (`AGENTS.md` § Struktur). Deshalb:

- Existiert bereits eine Ablage (Schema, Migrationen), wird sie benutzt und
  die nächste freie Versionsnummer ermittelt:

  ```bash
  ls backend/src/main/resources/db/migration/ | sort -V | tail -3
  ```

- Existiert noch keine, entscheidet das Issue den **Schnitt**: relational
  mit Migrationen oder etwas anderes. Der Dateiname ist dann
  `V<n>__<snake_case_beschreibung>.sql` — diese Konvention wird mit dem
  ersten Backend-Feature in `AGENTS.md` festgeschrieben.

Im Issue festhalten:

- Tabellenname, Spalten mit Typ, Constraints, Indizes
- Ob Spalten nullable sind und warum
- Fremdschlüssel auf den Bezug (z. B. `household_id` mit FK auf `household`) —
  bezugsgebundene Entitäten tragen **`@ManyToOne House household` mit
  `optional=false`**, sofern der Stack das so vorsieht
- Ob `ON DELETE CASCADE` nötig ist
- Kein Eingriff in bestehende V-Dateien — immer neue V-Nummer

Ist kein Schema betroffen, steht im Issue ausdrücklich:
„Keine Schema-Änderung — <Begründung>".

### 4. API-Vertrag

Je Endpunkt:

| Methode | Pfad | Request | Response | Fehler |
|---------|------|---------|----------|--------|
| `GET` | `api/<ressource>` | – | `<Ressource>Dto[]` | 401, 403 |

Regeln:

- Der Pfadpräfix folgt der bestehenden Konvention; wo es keine gibt, wird
  sie mit diesem Issue festgelegt (z. B. `@RestController
  @RequestMapping("api/<resource>")` — **ohne führenden Slash**).
- Pfadvariablen: `@PathVariable("name")`.
- DTOs liegen in `endpoint/model/`, Lombok `@Builder @Data @NoArgsConstructor
  @AllArgsConstructor`. `@JsonProperty` nur, wenn der JSON-Key vom
  Feldnamen abweicht.
- Fehler über eine zentrale Fehlerbehandlung (z. B. `GlobalExceptionHandler`):
  `IllegalArgumentException` → 400, „not found" → 404,
  `MembershipRequiredException` → 403, `ConflictException` → 409.
  Services werfen, keine `ResponseEntity` zurückgeben.
- Bezug zum Kontext: Dienste holen sich den aktuellen Kontext (z. B. den
  aktiven Haushalt) aus dem Request; vor jedem `save()` wird die Zuordnung
  gesetzt (`entity.setHousehold(currentHousehold())`).
- **Der Vertrag passt zum Zielort.** Ein Vertrag, der gegen die Realität des
  Repos läuft, wird hier aufgelöst und nicht erst beim Bauen. Beispiel aus
  der Praxis: „der Renderer fragt die Datenbank nicht ab" ist mit einem
  Renderer unvereinbar, der Bildbytes aus der DB lädt — die Formulierung
  wird auf „fragt keine Fachlogik ab" präzisiert oder der Renderer bekommt
  die Bytes übergeben.

### 5. Frontend-Design

- Route in `app.routes.ts`, lazy via `loadComponent`, Pfad kebab-case.
- Komponente unter `src/app/domain/<feature>/features/<name>/`.
- Daten-Service unter `src/app/domain/<feature>/data/<thing>-data.service.ts`.
- Standalone, `ChangeDetectionStrategy.OnPush`, `inject()` statt
  Konstruktor-DI.
- State als `signal()`, abgeleitet als `computed()`.
- Template: `@if` / `@for` / `@empty`, Bootstrap 5, FontAwesome.
- Das Issue nennt Komponentenname, Route und die Signale, nicht das
  vollständige Template.

### 6. Konventionen

Die 3–5 Konventionen aus `AGENTS.md`, die für dieses Issue tatsächlich
greifen. Steht dort noch nichts, entscheidet das Issue sie und vermerkt
das ausdrücklich. Nicht die ganze Liste abschreiben.

### 7. Dateiliste

Konkrete Pfade, **verifiziert**. Jeder bestehende Pfad muss im Repo
existieren. `rg` ist auf dem Host nicht installiert — `grep`/`find` sind der
Weg:

```bash
find frontend/src/app -name '*ausgaben*'
grep -rn 'class Ausgaben' frontend/src/app
ls backend/src/main/resources/db/migration/ | sort -V | tail -3
```

**Jeder bestehende Pfad muss im Repo existieren** (mit `find`/`grep`
geprüft, nicht geraten). Neue Dateien können nicht existieren — dafür gilt die
Namenskonvention des Repos, und der Pfad wird als **Neu** markiert:

| Artefakt | Pfadmuster |
|----------|-----------|
| Fachmodell (Domain) | `domain/model/<Entity>.java` |
| Outbound-Port | `application/port/out/<Entity>Repository.java` |
| Inbound-Port | `application/port/in/<Entity>UseCase.java` |
| Use Case | `application/usecase/<Entity>UseCaseImpl.java` |
| Backend-DTO | `adapters/inbound/rest/dto/<Entity>Response.java` |
| Backend-Controller | `adapters/inbound/rest/<Entity>Controller.java` |
| Persistenz-Entity | `adapters/outbound/persistence/<Entity>JpaEntity.java` |
| Backend-Repository (JPA) | `adapters/outbound/persistence/<Entity>JpaRepository.java` |
| Port-Implementierung | `adapters/outbound/persistence/<Entity>RepositoryAdapter.java` |
| Flyway-Migration | `db/migration/V<n>__<snake_case>.sql` |
| Frontend-Feature | `features/<feature>/<feature>-page.ts` |
| Frontend-Modell | `features/<feature>/model/<thing>.model.ts` |
| Frontend-Datenschicht | `features/<feature>/data/<thing>-api.ts`, `features/<feature>/data/<feature>-data.ts` |
| Frontend-UI-Schicht | `features/<feature>/ui/<thing>.ts` |
| Frontend-Route | `features/<feature>/routes.ts` (eingehängt in `app.routes.ts`) |

Die Muster gelten, solange `AGENTS.md` nichts anderes festlegt.

Ein als „Neu" markierter Pfad trägt im Issue den Zusatz: „Pfad nach
Namenskonvention, beim Bauen anpassen falls die Konvention nicht greift".
Ein Pfad, der weder existiert noch der Konvention folgt, ist eine Lücke der
Art `TECH` → runde=3, keine erfundene Datei.

Format im Issue: Pfad + Klammer + was dort passiert.

### 8. Teststrategie

| AC | Ebene | Wo | Was |
|----|--------|-----|-----|
| AC1 | Backend | `.../service/XServiceTest.java` | <Fälle> |
| AC2 | Controller | `.../endpoint/XControllerTest.java` | <Fälle> |
| AC3 | Frontend | `.../x.component.spec.ts` | <Fälle> |
| AC4 | E2E | `e2e/x.spec.ts` | <Fälle> |

Konventionen: JUnit 5 + Mockito, Testklasse je Produktionsklasse, gleiches
Package. Kein `@MockBean`.
Controller-Tests: `MockMvcBuilders.standaloneSetup(new XController(service)).build()`.
Frontend: Jasmine + TestBed, `jasmine.createSpyObj`, `provideHttpClient()`,
`provideRouter([])`.

**Jedes fachliche Akzeptanzkriterium hat hier seinen Testfall.** Kein AC
ohne Zeile in dieser Tabelle — und keine Zeile ohne AC. Ein AC, das nur
manuell geprüft wird (ein Export, den man neben ein Musterdokument legt), wird
in der Spalte „AC" mit `manuell` gekennzeichnet und im Verify-Block mit dem
konkreten Prüfschritt benannt.

Diese Zuordnung ist der Test, der Lücken der Umsetzung aufdeckt: sie zeigt,
welches AC beim Bauen unbemerkt übersprungen wurde.

### 9. Verify-Block

Nur die Kommandos, die für dieses Issue relevant sind — siehe
`issue-formulierung/references/hausstil.md`. Plus ein manueller Test-Satz.
Solange `AGENTS.md` keine Verify-Kommandos kennt, werden hier diejenigen
genannt, die für dieses Issue entstehen — sie sind dann mit dem Issue neu zu
validieren.

### 10. Risiken und Gotchas

Bereiche prüfen, die laut `AGENTS.md` § Gotchas historisch Fehler gemacht
haben. Steht dort noch nichts, benennt das Issue die **erste** erwartete
Fehlerquelle, und der Abschnitt in `AGENTS.md` wird nach dem Bauen
nachgezogen.

Tatsächlich einschlägige Gotchas in `## Gotchas` nennen — mit dem
Hinweis, das Issue vor dem Bauen zu lesen. Kein Copy-Paste der ganzen Skill.

### 11. Grilling — technische Entscheidungen attackieren

Jetzt werden die **technischen** Entscheidungen angegriffen. Lade vorher
`.opencode/skills/anforderung-loop/references/grilling.md` und folge dem
Frageprotokoll: **eine Frage pro Zug**, Antwort ins Protokoll, dann die
nächste Frage.

Runde 3 attackiert den Bauplan:

| Reihenfolge | Kategorie | Worum es geht |
|-------------|-----------|----------------|
| F1 | Widerspruchs-Angriff | Gibt es eine bestehende Stelle, die dem Feature widerspricht? Wer ist die Quelle der Wahrheit? |
| F2 | Folge-Angriff | Was kostet das in einem Jahr — Datenmenge, Index, Aufräumen, Migration? |
| F3 | Zeit-Angriff | Was muss vor dem Kern gebaut werden, was kann warten? |
| F4 | Umkehr-Angriff | Was, wenn der Nutzer eine andere Representation will? Ist die Entscheidung zu tief im Code vergraben? |
| F5 | Kern-Angriff | Welcher technische Teil ist Beiwerk und kann raus? |
| F6 | Referenz-Angriff | Welche Eigenschaften des Vorbilds bleiben technisch offen? |

Stop-Kriterien aus `grilling.md` prüfen. Sind sie nicht erfüllt, wird
weiteregefragt.

**Grenze:** Das Grilling fragt Anforderungen, nicht Implementierungsdetails.
"Welche Datenbank?" ist keine gute Frage. "Welche der beiden Datenquellen ist
verbindlich, wenn die alte Erfassung und die neue auseinanderlaufen?" schon.

### 12. Issue aktualisieren und finalisieren

Der Arbeitsstand ist das GitHub-Issue — es gibt keine lokale Datei.

- Issue-Text in der Struktur aus `issue-formulierung` schreiben: Runde 1 +
  Runde 2 + technische Abschnitte. Der Body enthält jetzt `## Begriffe`
  (aus Schritt 2) und die Teststrategie mit AC-Spalte (aus Schritt 8).
- Das Protokoll der Runde 3 (alle F/A-Paare plus Stop-Kriterien-Zeile) wird
  **nicht** in den Body geschrieben, sondern als Kommentar angehängt.

Dann `issue-github` **Teil B** ausführen: Body ersetzen, Protokoll der
Runde 3 anhängen, Body zurücklesen und prüfen. Danach `anforderung-gate`
mit `runde: 3` und anschließend mit `runde: final` ausführen — das Gate
liest Issue-Body und Kommentare.

Bei `PASS` im finalen Gate: `issue-github` **Teil C** — Body finalisieren,
`[WIP]` aus dem Titel, Labels und Typ setzen, Abschlusskommentar anhängen.

## Regeln

- Der Issue-Text bleibt fachlich lesbar. Technische Abschnitte ergänzen,
  sie ersetzen das Goal nicht.
- Kein Code, der nur den Agenten unterhält. Snippets nur dort, wo es
  Alternativen gibt, die eine Entscheidung erzwingen.
- **Jedes AC hat eine Testzuordnung.** Die Tabelle aus Schritt 8 deckt jedes
  Kriterium ab — fehlt eine Zeile, ist das eine Lücke der Art `TECH`.
- **Der Begriffsabgleich steht im Issue.** Wo Fachbegriff, DB-Feld und
  UI-Label auseinanderlaufen, wird das benannt, nicht stillschweigend
  in Kauf genommen.
- Alles, was nicht verifiziert wurde, wird `_Annahme: …` und steht in
  `Offene Punkte`.
- Der Issue-Text ist die Quelle für die Umsetzung.
- **Eine Frage pro Zug**, in jeder Runde.

## Ergebnisformat

```text
Runde 3 abgeschlossen: <Titel>
Issue: #<N> <URL>
Grilling: <n> Fragen, <n> offen
Akzeptanzkriterien: <n>, davon technisch geprüft: <n>
AC ohne Testzuordnung: <n>
Begriffe im Abgleich geprüft: <n>
Offene Punkte: <n>

GATE: runde=3 status=<PASS|GAPS>
LÜCKEN:
- …
KORREKTUREN:
- …

GATE: runde=final status=<PASS|GAPS>
LÜCKEN:
- …
KORREKTUREN:
- …
```
