---
name: Anforderung-Loop
description: Use when a rough idea must become an implementation-ready GitHub issue. Runs the 3-round loop with grilling and gate.
---
# Anforderungs-Loop — von der Rohidee zum umsetzungsreifen Issue

Wandelt eine grobe Idee in ein Issue, das ein Agent ohne Rückfragen bearbeitet:
drei Runden mit Grilling, Ergebnis und Gate, dazwischen zwei Querrunden (UX,
Architektur). Kern ist **Verständnissicherung**, nicht Lückensammlung — eine
Frage pro Zug, bis beide dasselbe verstehen. Gate-Lücken führen in die
zuständige Runde zurück.

## Das GitHub-Issue als Arbeitsstand

**Keine lokale Draft-Datei.** Der Body ist der aktuelle Issue-Text und wird
nach jeder Runde **vollständig ersetzt**. Zustand (`runde`, `status`) im
HTML-Kommentar oben: `<!-- anforderung: runde=2 status=IN_PROGRESS issue=129 -->`.
Das Grilling-Protokoll ist **ein Kommentar je Runde**, nie im Body. `[WIP]` im
Titel bis zum finalen Gate. Repo `Sadeeg/HouseholdManager`. Schreiben:
`issue-github` A/B/C; Form: `issue-formulierung`.

## Die drei Runden

### Runde 1 — Erfassen

Draft v0 mit Ziel, Kontext, Scope, Abgrenzung und Phasen-Schnitt — bewusst
lückenhaft (Input für R2/R3).

- **Ziel in einem Satz** aus Nutzersicht („<Rolle> kann <Fähigkeit>"); geht
  das nicht → Epic.
- **Bestand prüfen, nicht raten:** `explore`-Subagent mit **benannten** Zielen
  (Route, Komponente/Klasse, Controller, Migrationen); Antwort nur Ergebnis
  (ja/nein, exakter Pfad, Zustand). Nichts gefunden ist ein Ergebnis. `rg`
  fehlt — `grep`/`find`.
- **Scope** (Stichpunkte mit Verb, Ergebnis formuliert) und **Out of Scope**
  (mindestens drei Punkte, z. B. Export, Admin-UI, Benachrichtigungen).
- **Epic-Entscheidung** (siehe unten): vertikaler Schnitt = ein Issue, ein
  Branch; Epic nur bei unabhängig lieferbaren, je allein nutzbaren Teilen.
- **Grilling** (Ziel, Kern, Mess, Gegner, Zeit, Verzicht), dann `issue-github`
  **Teil A**; AC in R1 dürfen leer sein (in R2 nicht), `## Tests`/`## Verify`
  „folgt in Runde 3". Gate `runde: 1` → **Phasen-Übergang**.

### Runde 2 — Fachliches Refinement

Wer macht was, in welcher Reihenfolge, unter welchen Regeln. **Keine Technik**
— keine Dateinamen, Endpunkte, Klassennamen.

- **Fachvokabular:** wachsend; jeder neue Begriff wird beim ersten Vorkommen
  definiert, nichts geraten (`_Annahme: …`). **Rollen:** je Rolle
  sehen/dürfen/verweigert. **Nutzerfluss:** geordnete Schritte je Rolle.
  **Regeln:** Berechnung, Zustände/Übergänge, Grenzwerte, Berechtigungen,
  Default-Sortierung/Gruppierung.
- **Akzeptanzkriterien:** prüfbar ohne Code-Begriffe, **je Zeile genau eine**
  (kein „und"), IDs `AC<n>` fortlaufend und stabil; jede Regel mindestens
  einmal abgedeckt; jedes „kann …" nennt **Einstieg und Voraussetzung**;
  `disabled`-Aktionen sind im Default sichtbar; Grenzwerte werden abgelehnt
  **und benannt**.
- **Eingaben:** je Angabe Pflegeschritt im Fluss, benannter Ort (Seite/Dialog,
  nicht „geeignete Stelle") und Leerfall-Regel **mit Rückmeldung**; „Leer → —"
  allein ist keine fertige Regel.
- **Referenz:** jede beobachtbare Eigenschaft eines Vorbilds wird
  Akzeptanzkriterium oder notierte Abweichung in `## Referenz`.
  **Fehler-/Randfälle:** leer, ungültig, außerhalb der Grenze (mit Meldung),
  Berechtigung fehlt, doppelt ausgelöst, Abbruch — die zutreffenden.  **Abhängigkeiten:** `depends_on` und `## Depends on` mit Phase/Issue.
- **Grilling** (Randfall, Rolle, Umkehr, Widerspruch, Folge, Mess, Referenz);
  jede Markierung einarbeiten. `issue-github` **Teil B**, Body zurücklesen,
  Gate `runde: 2` → **Phasen-Übergang**.

### Querrunden 2a/2b — UX und Architektur

Nach der Fachrunde, vor Runde 3: `ux-designer` entwirft die Oberfläche nach
Design-DNA und schreibt `## UX` in den Body; `arc-agent` pflegt `arc42/` und
liefert die Vorgaben für R3. Beide entscheiden nichts; offene Fragen gehen an
den Auftraggeber und werden, wenn blockierend, **vor** R3 geklärt.

### Runde 3 — Technisches Refinement

Ort im Code, Datenmodell, API, UI, Dateiliste, Tests, Verify. Alle Details:
`references/technik-checkliste.md`; verbindlich bleibt `AGENTS.md`. Was dort
nicht steht, entscheidet R3 und wandert danach nach `AGENTS.md` bzw. in die
Checkliste.

- **Vorgaben:** ARC-Vorgaben aus 2b sind **Vorgabe** (Abweichung nur mit
  Grund und Rückfrage; fehlende ARC-Runde nachholen). `## UX` wird umgesetzt,
  nicht neu entworfen; fehlt er bei UI-Anteil → `TECH`. Widersprüche
  Issue/arc42/Code hier auflösen.
- **Design-DNA bindend:** nur Tokens (`var(--hm-…)`), keine Hex-Literale, Dark
  Mode Standard, `shared/components/`, Zustände inkl. Fokus
  (`--hm-focus-ring`); neue UI-Muster gehören in die UX-Runde.
- **Zielort:** je Teil Schicht/Klasse mit konkretem Namen; bestehende Pfade
  zitieren (geprüft), neue nach Konvention. **Begriffsabgleich** →
  `## Begriffe` (Fachbegriff, DB-Feld, UI-Label, Literal), Abweichungen
  benennen.
- **Daten, API, Frontend:** Schema mit nächster V-Nummer oder „Keine
  Schema-Änderung — <Grund>"; API-Vertrag mit Fehlerfällen, DTO-Records und
  zentraler Fehlerbehandlung, passend zum Zielort; Frontend-Route, State und
  Signal Forms nach Konvention (Details: Checkliste).
- **Einstieg:** jedes „kann …"-AC nennt Einstieg in der Oberfläche und
  Datenvoraussetzung; ein Endpunkt allein genügt nicht → `TECH`.
- **Konventionen:** nur die 3–5 aus `AGENTS.md`, die greifen. **Dateiliste:**
  verifizierte Pfade, neue als **Neu** plus „Pfad nach Namenskonvention, beim
  Bauen anpassen"; Muster in der Checkliste. Berührte Doku mit aufnehmen:
  `compose.yml`, `proxy.conf.js`, `arc42/` und `docs/adr/` **im selben
  Commit**, `design-dna.md`.
- **Tests/Verify:** je AC Ebene/Ort/Fälle, kein AC ohne Test oder manuellen
  Schritt; nur relevante Kommandos (`issue-formulierung`,
  `AGENTS.md §Verify`) plus manueller Satz, mit UI Stack-Start, DB-Zustand und
  Browser-Pfad.
- **Risiken:** `AGENTS.md §Gotchas` plus die erprobten
  Fehlerquellen (stille Verwerfung, Migration ohne Backfill, Overlay statt
  Muster, Nur-Endpunkt, nicht startender Stack).
- **Grilling** (Widerspruch, Folge, Zeit, Umkehr, Kern, Referenz), dann Teil B,
  Gate `runde: 3`, danach Gate `runde: final`. Bei `PASS`: Teil C — `[WIP]`
  weg, Labels und Typ, Abschlusskommentar, Status `READY_FOR_AGENT`.

## Grilling-Phase

Nach dem Inhalt jeder Runde: **genau eine Frage pro Zug**, nie gebündelt.
Antwort ins Protokoll, Body bei Bedarf anpassen, nächste Frage; nach drei
Fragen derselben Kategorie wechseln, zwischendurch zusammenfassen. Ende erst,
wenn die sechs Stop-Kriterien erfüllt sind — vorher nicht weiterarbeiten.
Jede Antwort trägt ihre Folge (`→ **AC<n>**`, `→ **Abweichung**`,
`→ **Regel …**`), sonst geht sie beim Übergang verloren. Methode, Kategorien,
Frageprotokoll, nicht-interaktiver Lauf und Protokollformat:
`references/grilling.md`.

## Gate

Das Gate prüft nach jeder Runde **das Issue** (Body plus Kommentare) gegen die
Checkliste (`runde: 1`, `2`, `3`, `final`); fehlender Marker → `FORMAT` →
runde=1. `status=PASS` = keine Lücke (Korrekturen werden still mitgenommen),
`status=GAPS` = mindestens eine Lücke mit Zielrunde `1`/`2`/`3`.
`VERSTÄNDNIS` = Stop-Kriterien nicht erfüllt → zurück in die Runde. Keine
Prosa, der Orchestrator wertet `status` und Zielrunde. Ausgabeformat,
Kategorien, Checklisten, Arbeitsweise: `references/gate.md`.

## Epic und Phasen

**Epic** = unabhängig lieferbare, je allein nutzbare Teile; R1 erzeugt die
Phasenliste, das Epic-Issue zuerst, jede Phase als **Sub-Issue** mit
`parent_issue_number`. **Einzelissue** = vertikaler Schnitt (auch Migration +
Entity + Service + Controller + Frontend), ein Issue, ein Branch. Tie-Breaker:
Kann Teil 1 allein ausgeliefert werden und ist nutzbar? Nein → Einzelissue.
Phasen laufen fort (`3.1`, `3.2`, …); nächste Epic-Nummer = höchste `[Epic N]`
+ 1 (kein `epic`-Label); Titel `[Epic N]`, Phase `[N.M]`
(`issue-formulierung`).

## Regeln

- **Phasen-Übergang:** Eine Frage am Ende jeder Runde — **nach** dem Gate, nie
  davor; `GAPS` zuerst beheben. A: weiter, B: pausieren (Issue bleibt `[WIP]`,
  Fortsetzung über `/anforderung <Nr>`; zusammenfassen, **nichts**
  finalisieren), C: freie Antwort ist Anweisung.
- **Fortsetzen:** Idee → neues Issue ab R1; Issue-Nummer → Marker verrät den
  Stand (`runde=1` → R2, `2` → R3, `3` → finales Gate); sonst nachfragen.
- Kein Code in keiner Runde; Protokoll nur in Kommentaren, Body immer komplett.
  Eine Frage pro Zug, kein Verhör, nicht aufblähen.
- Unbelegtes → `_Annahme: …` in `## Offene Punkte`; nach dem Grilling stehen
  dort **nur Annahmen**. Repo-Erkenntnisse aus der Bestandsprüfung, nicht aus
  dem Gedächtnis.
- Scope nicht stillschweigend erweitern → `SCOPE` → R1; Widersprüche zwischen
  Annahme und Realität benennen, nicht auflösen — der User entscheidet.
  Benachbarte Fehler unter „Folge-Issues" am Body-Ende notieren.
- **Prozesswissen nach `AGENTS.md`.** Fehlt ein nötiger Release-/Deploy-Schritt
  dort → `FORMAT` → runde=1. Erkenntnisse werden Regeln:
  `docs/erkenntnisse-issue-<n>.md` plus Überführung nach `AGENTS.md` oder in
  den zuständigen Skill.
- Ende: `READY_FOR_AGENT` oder `BLOCKED` mit offenen Punkten. Der Issue-Text
  bleibt fachlich lesbar und ist die Quelle für die Umsetzung.
- **Runde laden, dann `references/gate.md` prüfen** — erst Inhalt, dann
  Grilling, dann schreiben, dann formatieren, dann prüfen.

## Ergebnisformat

```text
Runde <n> abgeschlossen: <Titel>
Issue: #<N> <URL>
Grilling: <n> Fragen, <n> offen; Offene Punkte: <n>, Annahmen: <n>
Runde 1: Epic <ja/nein>, Phase <N.M>, User-Entscheidungen <n>
Runde 2: Kriterien <n> (AC1–AC<n>), Eingaben ohne Pflegeort <n>,
         Referenz <n>, Abweichungen <n>
Runde 3: AC <n>, technisch geprüft <n>, AC ohne Test <n>, Begriffe <n>
GATE: runde=<n> status=<PASS|GAPS>
LÜCKEN:
- …
KORREKTUREN:
- …
```
