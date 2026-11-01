# Skill-Optimierung — Tokenverbrauch senken ohne Qualitätsverlust

Stand: 2026-11-01 (Analyse vom 2026-10-31, P0/P1 umgesetzt — siehe §9) ·
Auftrag: Skills analysieren, Reduktions- und Entfernungsvorschläge,
Erkenntnisse dokumentieren.
Messergebnisse sind mit `wc -c` / `find` erhoben, nicht geschätzt.
Umrechnung: **~3,8 Byte = 1 Token** (deutsche Prosa mit Code/Tabellen).

---

## 1. Messbild

### 1.1 Was in **jedem** Turn im Kontext liegt

| Quelle | Größe | ≈ Token | Anmerkung |
|--------|-------|---------|-----------|
| `AGENTS.md` (Repo) | 22.621 B | ~6.000 | wird pro Turn geladen |
| `/home/sascha/dev/AGENTS.md` (Eltern, **TranscriptFlow**) | 3.031 B | ~800 | fremdes Projekt, wird mitgeladen |
| Skill-Katalog (nur Frontmatter-`description`) | 7.738 Zeichen | ~2.000 | 25 Skills, im System-Prompt |
| System-/Harness-Prompt | — | ~1.500 | nicht beeinflussbar |
| **Summe statisch** | **~33 KB** | **~8.800** | × jeder Turn × jeder Subagent |

Das ist der Posten, der **pro Turn** bezahlt wird — unabhängig davon, ob ein
Skill benutzt wird.

### 1.2 Was on demand dazukommt

| Paket | Größe | ≈ Token |
|-------|-------|---------|
| Alle 25 `SKILL.md` (Bodies) | 242.770 B | ~64.000 |
| Alle 79 `references/`-Dateien | 374.626 B | ~99.000 |
| davon vendored (fremde Skills) | 119.688 B | ~31.500 |
| `angular-developer/references/` allein (43 Dateien) | ~240 KB | ~63.000 |
| Agenten-Prompts (`agents/*.md`, 4 Stück) | 20.080 B | ~5.300 |
| Commands (`.opencode/commands/*.md`) | 14.868 B | ~3.900 |
| `.opencode/README.md` | 10.747 B | ~2.800 |

### 1.3 Was ein Feature **tatsächlich** lädt

**Anforderungs-Loop** (laut bindender „Skill-Ladereihenfolge" in
`anforderung-loop/SKILL.md`):

```
anforderung-loop + grilling.md + intake + fachlich + technisch
+ technik-checkliste + gate + issue-formulierung + hausstil
+ issue-github (+ ux-design, wenn UI)   = 112.740 B ≈ 29.700 Token
```

**Umsetzung + Review:**

```
backend-ports-adapters + frontend-feature-architecture
+ feature-testing + code-review-gate       = 36.405 B ≈ 9.600 Token
```

Ein Feature mit Gates kommt so auf **~45–55k Token reine Anleitung**, bevor
eine Zeile Code gelesen wurde. Dazu vier Subagenten (arc, ux, reviewer,
tester) mit je eigenem frischem Kontext und je eigener Skill-Ladung.

### 1.4 Die 290,6M einordnen

Kosten pro Session ≈ Σ(Kontextgröße pro Turn) = **Turns × durchschnittlicher
Kontext**. Bei 290,6M Token und realistischen 90–150k Kontext pro Turn
entspricht das **~2.000–3.200 Turns** an einem Tag.

Damit gilt ehrlich:

| Ursache | Anteil an 290,6M | Hebel |
|---------|------------------|-------|
| Turn-Anzahl × auflaufende Historie | **dominierend** (80–90 %) | Session-Hygiene, Pruning, Subagenten statt Hauptkontext |
| Statischer Sockel (AGENTS + Katalog) | ~6 % (8,8k × 2.000) | AGENTS.md kürzen, Katalog straffen |
| On-demand-Skill-Ladungen | ~1–2 % | Loop-Kette zusammenfassen |
| Tool-Ausgaben (Dateien, Build-Logs, Issues) | ebenfalls groß | Regeln in AGENTS.md §Kontext-Disziplin |

**Fazit:** Skills allein erklären die 290M nicht. Aber die Skills sind
der Teil, der die *schlechte Angewohnheit architektonisch erzwingt*: Die
Ladereihenfolge verlangt bis zu 10 Dateien pro Runde, und `AGENTS.md` legt
~6k Token in jeden Turn. Beides ist direkt und verlustfrei reparierbar.

---

## 2. Befunde (nach Schwere)

### B1 — Fremde Anweisungen im Kontext (P0, Qualitätsrisiko)
`/home/sascha/dev/AGENTS.md` beschreibt **TranscriptFlow** (Angular 17,
Spring Boot 3.2, Karma, Python-Worker) und wird in jedem Turn mitgeladen.
Es widerspricht dem Projekt (Angular 22, Boot 4, Vitest) — ein Agent, der
beide Dateien sieht, bekommt zwei Wahrheiten.

### B2 — `AGENTS.md` ist ein Handbuch statt einer Regeldatei (P0)
22,6 KB, davon sind weite Teile Duplikat:

| Abschnitt | Größe | Gehört hin |
|-----------|-------|------------|
| Kontext-Disziplin | 3.317 B | Harness-Prompt / `state.md`-Regel — nicht pro Turn |
| Branch/Commit/Push/PR | 3.022 B | `commands/pr.md` bzw. `code-review-gate` |
| Konventionen (Auth/Backend/Frontend/Boot-4) | 5.689 B | `backend-ports-adapters`, `frontend-feature-architecture`, `spring-boot` |
| Gotchas | 1.886 B | dieselben Skills (stehen dort schon) |
| Release | 990 B | `docs/release.md` |
| Skills-Tabelle | 1.662 B | **redundant** — der Katalog liefert dieselbe Info automatisch |
| Struktur | 1.778 B | `backend-ports-adapters` / `frontend-feature-architecture` |

Ziel: **≤ 8 KB**, nur Entscheidungen + Verweise.

### B3 — Der Skill-Katalog ist zu geschwätzig (P0)
25 Beschreibungen, 7.738 Zeichen, im Schnitt 310 Zeichen. Sie erkären
*warum* und *wie* — der Katalog braucht nur *wann laden*. Beispiele:

- `anforderung-loop`: 581 Zeichen (7 Zeilen Prosa im System-Prompt)
- `frontend-feature-architecture`: 478 · `angular-developer`: 472 ·
  `backend-ports-adapters`: 460

**Regel:** `description` ≤ 120 Zeichen, Form „Wann laden, was liefert es".
Ersparnis: ~4.200 Zeichen ≈ **1.100 Token pro Turn**.

### B4 — Der Anforderungs-Loop ist sieben Skills für einen Ablauf (P1)
`anforderung-loop`, `-intake`, `-fachlich`, `-technisch`, `-gate`,
`issue-formulierung`, `issue-github` = 71 KB + 25 KB Referenzen.
Die Rundenbeschreibung steht **dreifach** (Loop, Rundenskill, Gate-Checkliste),
das Gate wiederholt die Rundenchecklisten, und die „Ladereihenfolge" zwingt
den Agenten, alles zu öffnen.

`anforderung-gate` ist 14,2 KB für eine Checkliste. `anforderung-technisch`
ist 20,9 KB, davon ein großer Teil kopierte Konventionen aus `AGENTS.md` und
`technik-checkliste.md`.

**Ziel:** ein Skill `anforderung` (~10 KB) + `references/grilling.md` +
`references/gate.md`, `issue-formulierung` als kurze Stilregel (2 KB) mit
`hausstil.md` nur bei Bedarf. **112,7 KB → ~30 KB (−21k Token pro Issue).**

### B5 — Befehle und Fakten sind vielfach kopiert (P1, Driftrisiko)
`npm run sheriff` steht in 12 Dateien, `./mvnw -B test` in 11, Boot-4-Fakten
in 16. Jede Kopie ist eine Stelle, die veralten kann, und jede wird bezahlt,
wenn sie geladen wird. **Einmal in `AGENTS.md` (Verify), überall sonst
`siehe AGENTS.md §Verify`.**

### B6 — Vendored Skill-Ballast (P2)
119,7 KB fremde, generische Skills, die das Projekt nicht braucht:

| Skill | Größe | Bewertung |
|-------|-------|-----------|
| `angular-new-app` | 4,3 KB | Projekt existiert — **entfernen** |
| `spring-versions` | 6,2 KB (+Ref) | Versionen stehen in `pom.xml`/`package.json` — **entfernen** |
| `hexagonal-architecture` | 11,5 KB | Methodik steckt in ADR 0001 + `backend-ports-adapters` — **entfernen** |
| `spring-boot`, `spring-web`, `spring-data-jpa`, `spring-security`, `spring-framework`, `spring-flyway`, `spring-testing` | ~97 KB | generische API-Referenz; Projektregeln sind projektspezifisch — **auslagern** (nicht im Skill-Pfad) |
| `angular-developer` | 256 KB inkl. 43 Referenzen | 11,5 KB Body beschreibt überwiegend `ng new`/Projektaufbau (irrelevant); nur ~6 Referenzen sind relevant — **auf diese kürzen** |
| `vitest` | 3,7 KB + 19 Refs | klein und nützlich — **behalten**, Referenzen nur on demand |

### B7 — `angular-developer` ist der größte Einzelposten (P2)
43 Referenzdateien (~240 KB ≈ 63k Token), davon `signal-forms.md` 27,8 KB und
`angular-aria.md` 18,6 KB. Real gebraucht für dieses Projekt:
`signals-overview`, `signal-forms`, `http-client`, `resource`,
`component-harnesses`, `angular-aria`. Der Rest ist Erklärstoff.

---

## 3. Zielbild

### 3.1 Skill-Landkarte nachher

| Skill | vorher | nachher | Änderung |
|-------|--------|---------|----------|
| `anforderung` (neu, ersetzt 5) | 66,6 KB | ~10 KB | Merge Loop+Intake+Fachlich+Technisch+Gate-Teil |
| `anforderung`-Referenzen | 26,1 KB | ~14 KB | `grilling.md` straffen, `gate.md` kompakt, Checkliste in Loop |
| `issue-formulierung` | 3,2 + 8,6 KB | ~2 + 8,6 KB | Body straffen, Hausstil nur on demand |
| `issue-github` | 7,9 KB | ~3 KB | nur die GitHub-Schritte |
| `backend-ports-adapters` | 8,1 KB | ~5 KB | Boot-4/Gotchas aufnehmen, Beispiele raus |
| `frontend-feature-architecture` | 10,2 KB | ~5 KB | dito |
| `feature-testing` | 10,8 KB | ~6 KB | Testbeispiele raus, Ebenen-Tabelle behalten |
| `code-review-gate` | 7,3 KB | ~4 KB | Review-Kategorien + Urteilsformat |
| `ux-design` | 9,8 KB | ~5 KB | DNA steht in `docs/design/design-dna.md` |
| `angular-developer` | 11,5 + 240 KB | ~2 + 40 KB | nur relevante Referenzen |
| `vitest` | 3,7 + 152 KB | unverändert | — |
| 7 × `spring-*`, `hexagonal`, `angular-new-app` | 119,7 KB | 0 | ausgelagert/entfernt |

### 3.2 Skill-Vertrag (verbindlich, in `.opencode/skills/README.md`)

1. `description` ≤ 120 Zeichen, beginnt mit „Wann laden".
2. `SKILL.md` ≤ 4 KB. Was länger ist, gehört in `references/`.
3. Kein Skill wiederholt `AGENTS.md`-Inhalt — er verweist darauf.
4. Befehle stehen **einmal** (AGENTS.md §Verify), nie kopiert.
5. Referenzen werden nur geladen, wenn die Frage genau sie betrifft
   (max. eine pro Aufgabe).
6. Kein Skill enthält Beispielcode, der nicht projekttypisch ist.

---

## 4. Maßnahmen

### P0 — sofort, jede Session, jeder Turn

| # | Maßnahme | Ersparnis |
|---|----------|-----------|
| P0.1 | `/home/sascha/dev/AGENTS.md` nach `~/dev/TranscriptFlow/AGENTS.md` verschieben (Projekt-Scope statt Eltern-Scope) | ~800 Token/Turn + Qualität |
| P0.2 | `AGENTS.md` auf ≤ 8 KB schneiden (Tab. B2), Details in die zuständigen Skills | ~3.900 Token/Turn |
| P0.3 | Skills-Tabelle aus `AGENTS.md` entfernen (Katalog liefert sie) | ~400 Token/Turn |
| P0.4 | Alle 25 `description` auf ≤ 120 Zeichen | ~1.100 Token/Turn |
| P0.5 | Session-Hygiene als Regel: **1 Feature = 1 Session**, nach jeder Runde `state.md` (≤ 40 Zeilen) + neue Session | 50–70 % der Gesamtkosten |

**Wirkung P0:** statischer Sockel 8.800 → ~2.800 Token/Turn (**−68 %**).
Bei 2.000 Turns: ~12M Token pro Tag, allein hier.

### P1 — pro Feature

| # | Maßnahme | Ersparnis |
|---|----------|-----------|
| P1.1 | Loop-Skills zu `anforderung` mergen, Ladereihenfolge auf 3 Dateien reduzieren | ~21.000 Token/Issue |
| P1.2 | `anforderung-technisch` von kopierten Konventionen befreien (nur „was schreibe ich ins Issue") | ~8.000 Token/Issue |
| P1.3 | `anforderung-gate` auf eine Checkliste (~4 KB) | ~6.000 Token/Issue |
| P1.4 | Verify-Befehle zentralisieren; Kopien durch Verweis ersetzen | ~2.000 Token/Issue + Driftfreiheit |
| P1.5 | Subagenten bekommen nur ihren Skill + Issue-Abschnitt, nicht die ganze Kette | ~10.000 Token/Subagent |

### P2 — Ballast

| # | Maßnahme | Ersparnis |
|---|----------|-----------|
| P2.1 | `angular-new-app`, `spring-versions`, `hexagonal-architecture` löschen | Katalogplatz + 3 Ladungen |
| P2.2 | 7 × `spring-*` nach `.opencode/vendor-skills/` (nicht registriert), bei Bedarf zurückholen | ~97 KB Decke |
| P2.3 | `angular-developer` auf 6 Referenzen kürzen, Body auf Verweis | ~200 KB Decke |
| P2.4 | `vitest`-Referenzen auf `core-*` + `features-mocking` kürzen | ~100 KB Decke |

---

## 5. Was **nicht** gekürzt werden darf

- **Akzeptanzkriterien-Disziplin** (`feature-testing`): jedes AC braucht einen
  Test — das ist der Qualitätskern.
- **Gate-Urteile und Korrekturauftrag** (`code-review-gate`): Format und
  Severity bleiben vollständig.
- **Design-DNA-Verweise** und die Regel „keine Hex-Literale".
- **Boot-4-/Angular-22-Fallen**: sie verhindern teure Fehlversuche — sie
  gehören aber in *einen* Ort, nicht in sechs.
- **`state.md`-Übergabe**: sie ist die Voraussetzung für kurze Sessions.

Kürzen heißt hier: **Duplikate entfernen, nicht Wissen**. Jede gestrichene
Zeile muss an genau einer anderen Stelle stehen oder nachweislich
selbstverständlich sein.

---

## 6. Messverfahren (damit die Zahl belegbar bleibt)

1. **Vorher/Nachher messen**, nicht schätzen:
   `find .opencode/skills -name SKILL.md -printf '%s\n' | awk '{s+=$1} END{print s}'`
   und derselbe Lauf für `references/`.
2. **Sockel pro Turn** prüfen: `wc -c AGENTS.md` + Summe der
   `description`-Zeichen + Eltern-AGENTS.
3. **Kosten pro Feature** als Zielwert notieren (heute ~50k Token Anleitung;
   Ziel ≤ 20k) und im PR gegen den Vorwert stellen.
4. **Nach 5 Features** Review: Sind Fehlversuche/„Skill nachgeladen, aber
   nicht gebraucht"-Fälle gestiegen, wird die Kürzung zurückgenommen.

---

## 7. Umsetzungsplan

1. P0.1 (Eltern-AGENTS) — 1 Minute, größter Qualitätsgewinn.
2. P0.4 (Beschreibungen) — mechanisch, danach `wc` prüfen.
3. P0.2/P0.3 (`AGENTS.md` schneiden) — Inhalt in Skills verschieben, dann
   kürzen; `AGENTS.md` bleibt die Quelle für Entscheidungen.
4. P1.1–P1.3 (Loop-Merge) — braucht eine Sichtprüfung des Ablaufs, weil die
   Runden die Arbeitsgrundlage sind.
5. P2 (Aussortieren) — erst nach 2–3 Features mit den neuen Skills, damit
   sichtbar wird, was wirklich fehlt.

Reihenfolge ist bewusst: erst der Sockel (wirkt in jedem Turn), dann die
Ladungen (wirken pro Feature), zuletzt der Ballast (wirkt selten).

---

## 8. Kurzfazit

Die Skills sind nicht *zu viele*, sondern **zu gesprächig und zu oft
dupliziert**. Der größte Einzelhebel ist nicht das Löschen von Skills, sondern
`AGENTS.md` (6.000 Token in jedem Turn) und die siebenfache Loop-Kette
(30.000 Token pro Issue). Beides lässt sich ohne Wissensverlust auf ein
Drittel bringen; die Messgröße dafür steht in §6.

---

## 9. Umsetzungsprotokoll (2026-11-01)

Umgesetzt: P0 vollständig, P1.1–P1.3, P2.1–P2.2 sowie der Konzept-Merge für
Hexagonal. Ist-Werte mit `wc -c` / `find` nachgemessen.

### 9.1 Vorher / Nachher

| Posten | vorher | nachher | Δ |
|--------|--------|---------|---|
| `AGENTS.md` (jeder Turn) | 22.621 B ≈ 6.000 T | 12.932 B ≈ 3.400 T | **−43 %** |
| Eltern-`AGENTS.md` (TranscriptFlow) | 3.031 B ≈ 800 T | entfallen | **−100 %** |
| Skill-Katalog (Beschreibungen) | 7.738 Zeichen ≈ 2.000 T | 1.018 Zeichen ≈ 260 T | **−87 %** |
| **Sockel pro Turn** | **~33,4 KB ≈ 8.800 T** | **~14,0 KB ≈ 3.650 T** | **−58 %** |
| Registrierte Skills | 14 (+10 vendored) | 10 | −4 sichtbar |
| `SKILL.md`-Bodies (registriert) | 123.082 B | 81.471 B | −34 % |
| `references/` (registriert) | 66 Dateien / 308 KB | 66 Dateien / 308 KB | unverändert |
| Anforderungs-Loop-Kern | 83.197 B ≈ 21.900 T | 32.788 B ≈ 8.600 T | **−61 %** |
| Loop-Kette gesamt (inkl. Formulierung, GitHub, UX) | 112.740 B ≈ 29.700 T | 62.331 B ≈ 16.400 T | **−45 %** |
| Ausgelagert (nicht mehr im Katalog) | — | 10 Skills, 119.688 B | Decke −31.500 T |

**Wirkung:** ~5.150 Token weniger pro Turn. Bei 2.000 Turns/Tag sind das
**~10,3M Token pro Tag**, ohne eine einzige Regel zu verlieren.

### 9.2 Was konkret gemacht wurde

1. **Eltern-`AGENTS.md` entfernt** — es beschrieb TranscriptFlow und galt
   zugleich für dieses Repo.
2. **`AGENTS.md` geschnitten**: keine Skill-Tabelle mehr (der Katalog liefert
   sie), Release nach `docs/release.md`, Branch/Commit/PR auf die
   Entscheidungen reduziert (Ablauf im Command `/pr`), Struktur-Baum auf
   fünf Zeilen, Boot-4-Fallen gestrafft. Jede Entscheidung ist erhalten.
3. **Katalog-Beschreibungen auf ≤ 112 Zeichen** (vorher bis 581), Form
   „Wann laden, was liefert es".
4. **Anforderungs-Loop: 5 Skills → 1** (`anforderung`) +
   `references/{grilling,gate,technik-checkliste}.md`. Gestrichen wurden nur
   die dreifache Rundenbeschreibung, die Ladereihenfolge-Tabelle und nach
   `AGENTS.md` kopierte Konventionen/Befehle.
5. **10 generische Skills nach `.opencode/vendor-skills/`** verschoben
   (`hexagonal-architecture`, `spring-versions`, 7 × `spring-*`,
   `angular-new-app`) — nicht registriert, aber im Repo auffindbar. Begründung
   je Skill: `.opencode/vendor-skills/README.md`; Rückholweg dokumentiert.
6. **Hexagonal-Wissen gesichert, bevor gelöscht wurde**: Der Projekt-Skill
   `backend-ports-adapters` hat jetzt einen Abschnitt „Das Konzept in
   Kurzform" (Port-Definition, inbound/outbound, Dependency Inversion,
   Adapter, Composition Root, Begründung, Abgrenzung zum Frontend). Damit
   kennt auch ein kleines Modell die Begriffe **und** die Abbildung auf
   `de.householdmanager`.
7. **Verweise repariert**: `AGENTS.md`, `.opencode/README.md`,
   `.opencode/skills/README.md`, `docs/adr/0001`, `feature-testing`,
   Commands und Agents zeigen auf die neuen Orte; `git grep` auf die alten
   Skill-IDs ist leer (außer historischen `docs/erkenntnisse-*`).
8. **Skill-Vertrag** in `.opencode/skills/README.md` festgeschrieben
   (Beschreibung ≤ 130 Zeichen, Body ≤ 6 KB, keine Wiederholung von
   `AGENTS.md`, Befehle nur an einer Stelle, neue generische Skills nur nach
   `vendor-skills/`).
9. **Keine Skill-Liste mehr in `AGENTS.md`** — die Doppelpflege (Tabelle +
   Katalog) ist weg; das war eine der Driftquellen.

### 9.3 Offen (bewusst nicht in diesem Schritt)

| # | Punkt | Grund |
|---|-------|-------|
| O1 | `angular-developer`: 43 Referenzen (~240 KB), Body 11,5 KB überwiegend `ng new` | P2.3; braucht inhaltliche Auswahl, keine Mechanik |
| O2 | Body-Größen über dem 6-KB-Ziel (`feature-testing` 10,8 · `frontend-feature-architecture` 10,2 · `ux-design` 9,8 · `anforderung` 10,0) | weitere Runde; jede Kürzung braucht eine Sichtprüfung der Regeln |
| O3 | Verify-Befehle stehen noch in mehreren Skills | P1.4; jetzt durch `AGENTS.md §Verify` als Single Source markiert |
| O4 | `references/technik-checkliste.md` ist von 6,8 auf 10,1 KB gewachsen | hat Runde-3-Details aufgenommen, die vorher im SKILL.md standen — netto trotzdem −61 % |
| O5 | `vitest`-Referenzen (19 Dateien) unangetastet | klein und selten geladen; kein Hebel |

### 9.4 Prüfregel für die nächste Session

Nach den ersten 3–5 Features gegenprüfen: Steigen Fehlversuche, Rückfragen
oder „falscher Skill geladen"-Fälle, wird die Kürzung an der Stelle
zurückgenommen (Skill aus `vendor-skills/` holen bzw. Abschnitt
wiederherstellen). Die Zahlen dafür stehen in §6.

---

## 10. Weitere Hebel: Konfiguration (`.opencode/opencode.jsonc`)

| Key | Wert | Wirkung |
|-----|------|---------|
| `tool_output.max_lines` | 400 (Default 2000) | Tool-Ergebnisse gehen in **jedem** Folge-Turn mit — die größte Einzelquelle nach dem Sockel |
| `tool_output.max_bytes` | 20.000 (Default 51.200) | dto. |
| `compaction.keep.tokens` | 10.000 | so viel frischer Kontext bleibt nach der Kompaktierung |
| `compaction.buffer` | 30 % | Kompaktierung startet bei 70 % des Fensterinhalts |
| `experimental.subagent_depth` | 1 | Subagenten starten keine Subagenten (kein Fan-out auf einem Slot) |
| `experimental.background_compaction` | true | Zusammenfassung im Hintergrund — keine Pause im Turn |

**Nicht** aktivieren: `warming` (periodische Leerlauf-Anfragen = Tokens ohne
Arbeit).

**Prompt-Cache beachten:** Jede Änderung an `AGENTS.md` oder an einer
Skill-`description` invalidiert den Prefix-Cache des Providers. Nach einer
Optimierung die Session neu starten, sonst zahlt der erste Turn die
Wiedereingabe doppelt.

---

## 11. Getrennte Front-/Backend-Subagenten — der Rechenweg

| Variante | Hauptkontext | Einmalig im Subagent | Bewertung |
|----------|--------------|-----------------------|-----------|
| Alles im Hauptagenten | wächst auf ~40.000 Tokens (60 Dateien) und **wird in jedem Turn mitgeschickt** | — | teuer über viele Turns |
| `backend-dev` / `frontend-dev` | +300 Tokens (Zusammenfassung) | Basis (~3.700) + eigener Lesevorgang | **billiger**, sobald der Stoff groß ist |

**Regel:** delegieren ab ~10 Dateien oder ~20.000 Tokens Material. Kleine
Fixes bleiben im Hauptagenten — der Subagent würde die Basis (≈ 3.700 Token)
mehr kosten als die Aufgabe.

**Weitere Gewinne durch die Trennung**

- Jeder Subagent darf **nur** seinen Stack anfassen (`permissions`:
  `edit` auf den anderen Stack = `deny`) — kein Cross-Stack-Drift.
- Jeder Subagent darf **nur** passende Skills laden (`skill`-Allowlist) —
  kein versehentliches Laden der 43 Angular-Referenzen im Backend-Agent.
- Der Bericht ist eine **Zusammenfassung** (≤ 300 Wörter, Pfade als Beleg),
  kein Dateidump — das ist der eigentliche Hebel.

**Kosten:** beide Subagenten laufen **nacheinander** (ein Slot), die
Wandzeit verdoppelt sich. Dafür ist der Hauptkontext zwischenzeitlich klein
und das Ergebnis passt in 300 Wörter.

---

## 12. Sequentialität bei Ollama (ein Slot) — was möglich ist

**Antwort vorweg:** OpenCode hat **keinen** Schalter für eine
Parallelitätsgrenze. Die vollständige V2-Konfigurationsliste wurde geprüft:
es gibt `experimental.subagent_depth` (Verschachtelungstiefe) und
`permissions` mit `action: subagent` (welche Agenten startbar sind) — aber
kein `max_concurrency`, `max_parallel` oder `queue`.

Sequenzielle Ausführung entsteht deshalb auf drei Ebenen:

1. **Anweisung (der wirksame Hebel)** — `AGENTS.md §Arbeitsweise`:
   „Nie zwei Subagenten in einer Nachricht starten — Ergebnis abwarten, dann
   den nächsten." Das Modell entscheidet, wann es startet; die Anweisung
   greift genau dort.
2. **Server** — auf der Ollama-Seite:
   ```bash
   OLLAMA_NUM_PARALLEL=1     # nur ein Slot: Anfragen werden serialisiert
   OLLAMA_MAX_QUEUE=2        # Queue statt Ablehnung
   OLLAMA_KEEP_ALIVE=30m     # Modell zwischen Calls nicht entladen
   ```
   `OLLAMA_KEEP_ALIVE` spart zusätzlich Prefill-Zeit: nur so bleibt der
   KV-Cache für den gemeinsamen Prefix erhalten.
3. **Verschachtelungstiefe** — `experimental.subagent_depth: 1` verhindert,
   dass ein Subagent selbst Subagenten startet (kein Fan-out auf einem Slot).

**Warum Parallelität bei lokalen Modellen teuer ist:** Jeder Call promptet
den **kompletten** Kontext erneut (Prefill). Zwei parallele Calls auf einem
Slot bedeuten doppelten Prefill, während der zweite auf den KV-Cache wartet.
Sequenziell kann der Provider den Prefix wiederverwenden — das ist bei
lokalen Modellen der eigentliche Grund für die Regel.

**Hinweis:** Der Ollama-/llama-router-Server muss diese Variablen selbst
setzen; OpenCode kann sie nicht setzen. Der Router
(`192.168.178.83:8080/v1`) muss die Anfragen von OpenCode an den
Ollama-Server durchreichen, ohne selbst zu parallelisieren.

---

## 13. Der Architektur-Reviewer — Diagnose und AbSpeckung

### 13.1 Warum er so lange lief

Drei Ursachen, jede einzeln teuer:

| Ursache | Wirkung |
|---------|---------|
| **Er führte die Gates selbst aus** (`./mvnw -B test`, `npm test`, `npm run lint`, `npm run sheriff`) — obwohl der Implementierer das schon getan hatte und das Abnahme-Gate es nochmal tut | **ein kompletter Build-Zyklus je Review-Runde**, bei bis zu 3 Runden = 3 Builds |
| **Er lud 3 Skills** (`code-review-gate` 7,3 + `backend-ports-adapters` 9,7 + `frontend-feature-architecture` 10,2 KB ≈ 27 KB ≈ 7.100 Tokens) | große Instruktionsbasis vor der eigentlichen Prüfung |
| **Keine Schrittbegrenzung, keine Scope-Grenze** | der Agent konnte endlos suchen, lesen und prüfen |

Der Reviewer war damit **nicht zu gründlich, sondern zu breit**: Er machte
Arbeit, die schon erledigt war (Builds), und suchte selbst, was ihm als
Brief hätte geliefert werden können.

### 13.2 Was geändert wurde

| vorher | nachher |
|--------|---------|
| führt Builds und Tests aus | **statisch**; Gate-Ergebnisse kommen aus dem Brief des Orchestrators |
| lädt 3 Skills | **1 Skill** (`code-review-gate`) + **1 Referenz** (`references/pruefgrundlage.md`, 4,5 KB) — die kompakte Fassung der beiden Architektur-Skills |
| sucht selbst (Issue, Diff, Repo) | bekommt einen **Brief**: Issue-Nr, ACs, `git diff --stat`, Gate-Ergebnisse |
| prüft jedes Mal alles | **Runde 2/3 prüft nur** die Befunde der Vor-Runde und die seither geänderten Dateien |
| ohne Limit | `steps: 30` — am letzten Schritt werden die Tools entfernt, das Modell muss zusammenfassen |
| Reviewer-Body 4.577 B | 4.333 B, Beschreibung 357 → 134 Zeichen (Katalog) |

**Ergebnis je Review-Lauf:** Instruktionsbasis 27,2 KB → 12,0 KB
(**−4.000 Tokens**), zusätzlich **ein Build-Zyklus weniger** (das ist der
eigentliche Zeitgewinn) und Runde 2/3 um einen Bruchteil so teuer.

Die geprüften Regeln sind **nicht** verloren: `references/pruefgrundlage.md`
enthält sie kompakt (Abhängigkeitsrichtung, Transaktion, Flyway,
Feature-Grenzen, Testebenen, Token-Regeln, Security) mit denselben Greps.
Quelle der Wahrheit bleibt `AGENTS.md`.

### 13.3 Ist der Reviewer überhaupt nötig?

**Das Gate ja, die bisherige Ausführung nicht.** Ein Review ohne
Implementierer-Blick erfindet Befunde; der Reviewer ist das einzige Gate,
das Architekturverstöße überhaupt sieht (Der Tester prüft Verhalten, nicht
Schichten). Aber:

- **Kein Test-Gate-Ersatz.** Beide Gates prüfen anderes: der Reviewer die
  Struktur, der Tester die Beweise.
- **Bei Kleinigkeiten entfällt der Aufwand** — siehe O3 unten.
- Der **Tester** wurde ebenso entschärft — siehe §13.5.
- **ARC-Agent:** `steps: 30`, darf nur `arc42/**` editieren.
- **UX-Designer:** `steps: 20`, nur `ux-design` + `anforderung`.

### 13.5 Der kritische Tester — Diagnose und AbSpeckung

#### 13.5.1 Warum er teuer war

Drei Ursachen, jede einzeln teuer:

| Ursache | Wirkung |
|---------|---------|
| **Allowlist zu weit** (`feature-testing` + `code-review-gate` + `anforderung`) | `anforderung` (10,0 KB) war freigegeben, obwohl der Tester den Anforderungs-Loop nie braucht — er prüft ein fertiges Feature. `code-review-gate` (7,6 KB) plus `references/pruefgrundlage.md` (4,5 KB) enthält die **Architektur**-Regeln — die prüft der Reviewer, nicht der Tester. |
| **Ein Skill zu groß** (`feature-testing` 10,6 KB) | über dem 6-KB-Vertrag; enthielt Codebeispiele, die Angular-Fallen-Tabelle und eine Implementierer-Checkliste, die der Tester gar nicht anfasst. |
| **Kein Schrittlimit, keine Trennung von Implementierer-Wissen** | der Tester konnte in den Lernmodus des Implementierers rutschen (Checkliste „Vor dem Abschluss" doppelt im Skill **und** im Agent-Body). |

#### 13.5.2 Was geändert wurde

| vorher | nachher |
|--------|---------|
| 3 Skills erlaubt | **1 Skill** (`feature-testing`), Rest per `permissions` auf `deny` |
| Tester lädt `code-review-gate` | lädt nur die **eine** Datei `references/severitaet.md` (1,1 KB) für die Befund-Stufen — eine Quelle für beide Gates, keine Doppelung |
| `feature-testing` 10,6 KB | **6,3 KB**; Codebeispiele, Angular-Fallen und Abschluss-Checkliste in `references/` (nur on demand) |
| `steps: 60` | **`steps: 40`** — am letzten Schritt muss der Bericht stehen |
| Tester liest Issue selbst | bekommt einen **Brief** vom `/abnahme`-Command (ACs, `## Tests`, `## Verify`, `## Out of Scope`) — spart eine GitHub-Runde und Kontext |
| `git commit` / `git push` erlaubt | per `permissions` **deny** — der Tester meldet, er committet nicht |

#### 13.5.3 Messung (Vorher/Nachher, `wc -c`)

Instruction-Basis je Testlauf (Agent-Body + geladene Dateien):

| Position | vorher | nachher |
|----------|--------|---------|
| Agent-Body | 5.330 | 5.807 |
| `feature-testing` (Skill) | 10.602 | 6.261 |
| `code-review-gate` (Skill) | 7.566 | — (entfällt) |
| `pruefgrundlage.md` (Referenz) | 4.463 | — (entfällt) |
| `anforderung` (erlaubt, ungenutzt) | 9.989 | — (entfällt) |
| `severitaet.md` (Referenz) | — | 1.102 |
| `testmuster.md` + `angular-fallen.md` (on demand) | — | 3.033 |
| **Summe (voll beladen)** | **37.950 B ≈ 9.990 T** | **16.203 B ≈ 4.264 T** |
| **Summe (Realfall)** | **37.950 B ≈ 9.990 T** | **13.170 B ≈ 3.466 T** |

**Ergebnis:** −65 % Instruction-Basis im Realfall, **≈ 6.500 Token pro
Testlauf** weniger. Bei 2 Gates und mehreren Features pro Tag ist das die
größte verbleibende Einzelposition.

#### 13.5.4 Was bewusst erhalten blieb

- **Die Abdeckungsmatrix** (AC → Test → Beweis) — der Kern des Gates.
- **Die sechs adversarialen Fragen** inkl. Tautologie-Check.
- **Die Anti-Patterns** und die **drei Praxisregeln** (Zwischenstand, Layout,
  stilles Verwerfen) — sie stehen jetzt in `feature-testing` §Drei Regeln
  statt nur in der Anti-Pattern-Tabelle, weil `docs/erkenntnisse-issue-1.md`
  genau diese Begriffe zitiert.
- **Das Urteilsformat** (`ABNAHME: PASS | FAIL` + Befundstufen).
- **Die Trennung vom Reviewer:** Der Tester prüft weiterhin *Verhalten*,
  der Reviewer *Struktur*. Das wird nicht zusammengelegt, nur der jeweils
  überflüssige Skill weggelassen.

### 13.6 Weitere Optimierungen (offen, nach Wirkung sortiert)

| # | Maßnahme | Nutzen | Risiko |
|---|----------|--------|--------|
| O1 | `angular-developer` auf 6 relevante Referenzen kürzen, Body auf ~2 KB | ~200 KB Decke | mittel (Auswahl) |
| O2 | Skills mit Body > 6 KB auf ≤ 6 KB kürzen (`frontend-feature-architecture` 9,8 · `ux-design` 9,8 · `anforderung` 10,0) | ~8.000 Tokens pro Voll-Ladung | mittel (Sichtprüfung) |
| O3 | **Light Path** für kleine Änderungen (≤ 3 Dateien, keine neue Fachregel): nur 1 Gate statt ARC + UX + Review + Abnahme | 2–3 Subagenten-Läufe pro Feature | **hoch** — DoD-Regel, braucht deine Entscheidung |
| O4 | Runde 2/3 des Reviews durch **denselben** Kontext (Session fortsetzen) statt neuer Subagent | 2 × Basis (≈ 7.400 Tokens) | mittel — frischer Kontext war bisher Absicht (unbefangene Augen) |
| O5 | Modell-Routing: Reviewer/Tester auf ein kleineres Modell | **Zeit/Kosten, nicht Tokens** | — |
| O6 | Issue-Bodies im GitHub-Tool nur abschnittsweise lesen | ~1.000 Tokens/Runde | niedrig |

**Zu O5 ausdrücklich:** Ein kleineres Modell senkt **Kosten und Laufzeit**,
nicht die Tokenzahl — die wird vom Prompt bestimmt. Token spart man nur über
kürzere Prompts und weniger Kontext.

---

## 14. Backend-Architekturregeln als Tests (statt Agentenurteil)

### 14.1 Die Lücke

| Stack | Regelprüfung | Wer prüft |
|-------|--------------|------------|
| Frontend | `npm run sheriff` | **Test in CI** — 0 Agent-Tokens |
| Backend | **keine** — alle Regeln standen nur in Prosa | Agent (ARC-Runde, Review, Tester) |

Genau diese Lücke machte den ARC-Agenten teuer: Er las dieselben Regeln
bei jedem Feature erneut, interpretierte sie neu, und konnte sie übersehen.
Eine Regel, die ein Test erzwingt, wird **immer** geprüft — kostenlos.

### 14.2 Warum ArchUnit und nicht ArcUnit

Geprüft am 2026-11-01:

| Kandidat | Befund |
|----------|--------|
| `de.arc42:arc-unit*` | auf Maven Central **nicht vorhanden** (Suche 0 Treffer, Metadaten 404). Nur über JitPack — verlangt dort ein **Zugriffstoken** („No access token"). Zielt zudem auf JUnit 5 |
| `com.tngtech.archunit:archunit-junit6` | **verfügbar**, Version 1.5.1 (HTTP 200). JUnit-6-Modul, passend zu Boot 4.1.2 (JUnit Jupiter **6.0.3**) |

Boot 4.1.2 verwaltet die ArchUnit-Version nicht (`spring-boot-dependencies`
kennt kein `archunit`), deshalb steht sie fest im `pom.xml`.

### 14.3 Was implementiert wurde

- **`backend/pom.xml`**: `com.tngtech.archunit:archunit-junit6:1.5.1`
  (test scope).
- **`backend/src/test/java/de/householdmanager/architecture/ArchitectureRulesTest.java`** —
  9 Regeln mit `because`-Text (der Text ist Teil der Regel):

  | Regel | ArchUnit-Ausdruck |
  |-------|-------------------|
  | Fachkern kennt nur Java + Lombok | `dependOnClassesThat().resideOutsideOfPackages("java..","javax..","lombok..","..domain..")` |
  | Fachkern kennt die Anwendung nicht | `..domain..` → `..application..` |
  | Anwendung kennt keine Adapter, kein JPA/HTTP | `resideInAnyPackage("..adapters..", "jakarta.persistence..", …)` |
  | `@Transactional` nur in `application/usecase` | Annotation-Ort |
  | `@Entity` nur in `adapters/outbound/persistence` | Annotation-Ort |
  | `@RestController` nur in `adapters/inbound/rest` | Annotation-Ort |
  | DTOs sind Records | `beRecords()` |
  | Ports sind Interfaces | `beInterfaces()` (Top-Level) |
  | keine Feld-Injektion | `noFields().should().beAnnotatedWith(Autowired.class)` |

- **`pruefgrundlage.md`**: verweist auf den Test, listet die Regel→Test-Tabelle
  und definiert die **Driftprüfung** (Regel geändert, Test nicht → MAJOR).
- **`architektur-reviewer`**: prüft nicht mehr, was der Test erzwingt, sondern
  nur noch, was ein Test nicht ausdrücken kann (fachliche Passung,
  Entity↔Migration, AC-Abdeckung, Drift).
- **`code-review-gate`**: verweist auf die ausführbaren Regeln.
- **`AGENTS.md §Backend`**: „Architekturregeln sind Tests — wer eine Regel
  ändert, ändert den Test mit."

### 14.4 Was das bringt

| Größe | vorher | nachher |
|-------|---------|---------|
| Reviewer-Instruktionen | 27,2 KB (3 Skills) | 12,0 KB (1 Skill + 1 Referenz) |
| davon mechanisch prüfbar | Agent liest + greppt | **ArchUnit in CI** — 0 Tokens, immer ausgeführt |
| ARC-Runde je Feature | 1 Subagent (~8.000–15.000 Tokens) | **noch offen — Schritt 4 (braucht Freigabe)** |
| Reviewer-Prüfpunkte | 7 Grep-Kategorien | 4 semantische + Testresultat |

**Verifiziert (2026-11-01, Docker-Run mit Maven 3.9 / Temurin 25):**

| Prüfung | Ergebnis |
|---------|----------|
| `mvn test -Dtest=ArchitectureRulesTest` | 9 Tests, 0 Fehler, BUILD SUCCESS |
| **Gegenprobe** (Sondenklasse mit `@Service` im `domain`-Paket) | **1 Failure**, Meldung nennt die Regel **und** den `because`-Text → der Test greift tatsächlich |
| komplette Suite `mvn test` | **199 Tests, 0 Fehler, BUILD SUCCESS** |
| ArchUnit + JUnit 6 unter Boot 4.1.2 | funktioniert (`archunit-junit6:1.5.1`, Jupiter 6.0.3) |

### 14.5 Offen

| # | Punkt | Wer entscheidet |
|---|-------|----------------|
| A1 | ARC-Runde aus dem Anforderungs-Loop nehmen, arc42-Pflege in Runde 3 integrieren, Agent auf Drift-Wächter reduzieren | **du** (DoD/Prozess) |
| A2 | CI-Job, der prüft: Regel in `AGENTS.md`/`pruefgrundlage.md` geändert → Test mitgeändert | umsetzbar, sobald A1 steht |
| A3 | `TempProbeTest.java` (untracked, kompiliert nicht) aufräumen — sie brach den Maven-Build | du (vorbestehende Datei, nicht von mir) |

---

## 15. Zweite Runde (2026-11-01) — Dubletten und Katalog

§9 beschreibt eine Optimierung, die im Repo **nicht nachgehalten** war:
Die 10 Fremd-Skills lagen byte-gleich in `.opencode/skills/` **und**
`.opencode/vendor-skills/` (also doppelt im Katalog), und die 5
`anforderung-*`-Skills waren wieder aufgespalten, obwohl §9.2.4 einen Merge
auf **eine** Skill beschreibt. Diese Runde hat beides nachgezogen.

### 15.1 Befund

| Befund | Messwert | Beleg |
|--------|-----------|-------|
| **Fremd-Skills doppelt registriert** | 10 Skills, 119.688 B zweimal im Repo | `diff -rq .opencode/skills/<s> .opencode/vendor-skills/<s>` → 0 Unterschiede bei allen 10 |
| **Loop wieder aufgespalten** | 6 Skills (`anforderung`, `-loop`, `-intake`, `-fachlich`, `-technisch`, `-gate`) | die 5 tragen Inhalt, den `anforderung` + 3 Referenzen bereits vollständig abdecken |
| **Katalog zu geschwätzig** | 8.177 Zeichen in 25 `description` | längste: `anforderung-loop` 598 Zeichen |

### 15.2 Was geändert wurde

1. **10 Fremd-Skills entfernt** (registriert *und* vendort):
   `angular-new-app`, `hexagonal-architecture`, `spring-boot`,
   `spring-data-jpa`, `spring-flyway`, `spring-framework`,
   `spring-security`, `spring-testing`, `spring-versions`, `spring-web`.
   **Begründung je Skill** in `.opencode/skills/README.md`. Vor dem Löschen
   geprüft: `backend-ports-adapters` enthält das hexagonale Konzept
   vollständig (Schichten, Abhängigkeitsregel, Ports, Adapter, Composition
   Root, Tests je Ebene) — der Fremd-Skill brachte keine projektspezifische
   Regel. `spring-versions` nannte Boot 4.0.5, das Projekt läuft 4.1.2.
2. **Loop konsolidiert:** die 5 `anforderung-*`-Skills sind in
   `anforderung-loop` aufgegangen (Body der gemergten `anforderung`-Skill +
   `references/{gate,grilling,technik-checkliste}.md`). Der gemergte Body
   wurde gegen die 5 Quell-Skills geprüft: jede Regel, die dort stand, ist
   entweder im Body oder in einer Referenz enthalten.
3. **Alle 10 `description` auf ≤ 130 Zeichen** gekürzt, Form „Wann laden,
   was liefert es". Katalog: 8.177 → 1.195 Zeichen.
4. **Verweise repariert:** `AGENTS.md`, `.opencode/README.md`,
   `.opencode/skills/README.md`, ADR 0001, `backend-ports-adapters`,
   `feature-testing`, `commands/anforderung.md`, `commands/issue-check.md`,
   `agents/backend-dev.md` (Allowlist).

### 15.3 Messung

| Posten | vorher | nachher | Δ |
|--------|--------|---------|---|
| Registrierte Skills | 25 | 10 | −15 sichtbar |
| **Katalog (Beschreibungen)** | 8.177 Z ≈ 2.150 T | 1.195 Z ≈ 315 T | **−85 %** |
| `SKILL.md`-Bodies | 256.519 B | 83.760 B | −67 % |
| `references/` | 408.086 B | 319.008 B | −22 % |
| Skill-Ladung Anforderungs-Loop | ~112.700 B | ~46.000 B | **−59 %** |
| `AGENTS.md` | 13.832 B | 13.842 B | ±0 |

**Sockel pro Turn:** der Katalog schrumpft um ~1.835 Token — bei jedem Turn
und in **jedem** Subagenten (der Katalog steht auch dort im System-Prompt).

### 15.4 Fehler, die dabei passiert sind

| Fehler | Ursache | Fix |
|--------|---------|-----|
| YAML-Fehler „mapping values are not allowed here" | `description:` mit unquotiertem `:` im Text | YAML-Description quoten, wenn sie `:` enthält |
| `---` am Zeilenende der Description | `edit` ohne abschließenden Zeilenumbruch | Frontmatter mit Skript neu schreiben, nicht mit Einzel-Edit |
| `feature-testing` ohne Description | Beschreibungstext und `---` waren in einem Edit verschmolzen | Frontmatter neu aufgebaut, YAML validiert |

**Regel:** Frontmatter-Änderungen immer per Skript, danach mit `yaml.safe_load`
gegenprüfen — nicht per `edit` am Zeilenende.

### 15.5 Was bewusst erhalten blieb

- **Grilling, Gate-Checklisten, Stop-Kriterien** — der Kern des Loops, jetzt in
  `anforderung-loop/references/`.
- **Akzeptanzkriterien-Disziplin** (`feature-testing`): jedes AC braucht einen
  Test.
- **Gate-Urteile und Korrekturauftrag** (`code-review-gate`).
- **Design-DNA-Verweise** und „keine Hex-Literale".
- **Boot-4-/Angular-22-Fallen** in `AGENTS.md §Boot-4-Fallen` und
  `anforderung-loop/references/technik-checkliste.md`.

### 15.6 Offen

| # | Punkt | Grund |
|---|-------|-------|
| O1 | `angular-developer`: 43 Referenzen (~240 KB), Body überwiegend `ng new` | inhaltliche Auswahl nötig, kein Automatismus |
| O2 | `vitest`-Referenzen (19 Dateien) | klein und selten geladen, kein Hebel |
| O3 | Bodies über dem 6-KB-Ziel (`feature-testing`, `frontend-feature-architecture`, `ux-design`, `anforderung-loop`) | jede Kürzung braucht eine Sichtprüfung der Regeln |
| O4 | `Skill-Optimierung.md` selbst (~35 KB) | gehört aus dem Repo-Root — nach `docs/` oder weg |

### 15.7 Prüfregel

Nach den ersten 3–5 Features gegenprüfen: Steigen Fehlversuche, Rückfragen
oder „falscher Skill geladen"-Fälle, wird die Kürzung an der Stelle
zurückgenommen. Die Zahlen dafür stehen in §6.

---

## 16. Dritte Runde (2026-11-01) — angular-Skill und arc42-Schwelle

### 16.1 `angular-developer` — Body kürzen, Referenzen behalten

Der Body war eine reine Inhaltsverzeichnis-Seite über 43 Referenzen
(11.516 B) und enthielt eine vollständige **`ng new`-Anleitung** — gegenstandslos,
das Projekt existiert. Die 43 Referenzdateien (170 KB) bleiben **unangetastet**:
sie werden nur geladen, wenn genau die Frage sie betrifft, und kosten dann
nichts.

**Neuer Body (4.459 B):** Projekt-Kontext (Angular 22, zoneless, Signal
Forms, route-scoped Services, Tokens, Vitest), eine Referenztabelle mit den
31 tatsächlich genutzten Dateien, und ein Hinweis auf die 12 Dateien, die
für dieses Projekt nicht relevant sind (Tailwind, SSR/Hydration, Animationen,
Migrationen, E2E, MCP).

**Gegenprobe:** jede referenzierte Datei existiert; die genannten Themen
(Signal Forms, `httpResource`) kommen im Projekt tatsächlich vor
(`frontend/src/app/features/tasks/data/tasks-data.ts`).

### 16.2 arc42 — die Schwelle „nur Architektur"

**Befund:** arc42 wuchs **mit der Zahl der Features**, nicht mit der Zahl der
Entscheidungen. Kapitel 05 hatte zwei „Zielbild"-Abschnitte (Issue #1, #10) mit
je ~30 Zeilen Feature-Entwurf; Kapitel 06 hatte 8 Einzel-Szenarien, davon 5
mit Issue-Bezug; 03, 08, 10, 11, 12 mischten Feature-Status mit stabiler
Architektur.

**Die Schwelle:**

> **arc42 beschreibt, _wie_ das System gebaut ist und _warum_.**
> Das **Issue** beschreibt, _was_ ein Feature macht.

| In arc42 | Nicht in arc42 |
|----------|----------------|
| Bausteine, Ports, Adapter, Abhängigkeitsrichtungen | Feature-Abläufe, einzelne Nutzerflüsse |
| bounded contexts und ihre Grenzen | Akzeptanzkriterien, Testlisten |
| Laufzeitmuster, Jobs, externe Abrufe | „Zielbild"-Abschnitte pro Feature |
| Querschnittskonzepte | Issue-Status, Reifegrade, Sprint-Scope |
| Entscheidungen (ADR, in 09 verlinkt) | API-Verträge im Detail |
| Risiken, technische Schulden | Wiederholungen aus 05, 08, 09 |
| Fachbegriffe | |

**Umsetzung:**

1. **Agent `arc-agent`**: neuer Abschnitt „Die Schwelle: was in arc42 gehört"
   mit der Trennlinie, der Normalfall-Erregel („nutzt ein Feature nur eine
   bestehende Struktur, bleibt arc42 unberührt") und einem neuen
   Ergebnis-Feld `Architektur-Betroffenheit: strukturell | nur Nutzung`.
2. **Skill `anforderung-loop`**: Runde 2b als Schwellenprüfung beschrieben.
3. **`.opencode/README.md`**: dieselbe Trennlinie als Tabelle.
4. **Bestandsaufräumung** in den Kapiteln selbst:
   - 05: zwei „Zielbild"-Abschnitte entfernt → dafür „Muster über alle
     Contexts" (Use Case je Vorgang, Fach-Exception je Regelverletzung,
     Repository-Port + Adapter, In-Port für Context-Zugriff, Scheduler-Adapter
     …) und eine „Datenzuordnung"-Tabelle
   - 06: 8 Szenarien → **ein** Request-Muster, die drei Arten eingehender
     Adapter, die ausgehenden Adapter, der Context-Zugriff, der route-scoped
     Lebenszyklus
   - 03: „Scope des aktuellen Sprints" entfernt, Issue-Bezüge aus der
     Context-Tabelle raus
   - 08, 10, 11, 12: Issue-Bezüge raus, Q11–Q15 generalisiert

**Wissens-Gegenprobe:** nach dem Aufräumen wurde geprüft, ob die
Schlüsselbegriffe der entfernten Abschnitte noch auffindbar sind. Zwei
Verluste wurden entdeckt und archtektonisch wieder verankert: `WorkspaceUseCase`
(→ 05 „Muster über alle Contexts") und `SYNC_WINDOW_DAYS` / `MAX_URL_LENGTH`
(→ 05 „Fachliche Konstanten im `domain`").

### 16.3 Messung

| Posten | vorher | nachher | Δ |
|--------|--------|---------|---|
| `angular-developer/SKILL.md` | 11.516 B | 4.459 B | **−61 %** |
| arc42 gesamt | 73.694 B | 59.258 B | **−20 %** |
| arc42/05 Bausteinsicht | 18.775 B | 9.288 B | −51 % |
| arc42/06 Laufzeitsicht | 8.050 B | 4.946 B | −39 % |
| arc42/03 Kontextabgrenzung | 3.837 B | 3.306 B | −14 % |
| `arc-agent`-Beschreibung (Katalog) | 428 Z | 262 Z | −39 % |

**Dauerhafter Effekt:** arc42 wächst nicht mehr je Feature. Bei 20 Features
gespart wären das ~50 KB Wachstum — pro Feature ~2,5 KB, die bei jedem
Arc42-Lesevorgang mitbezahlt werden.

