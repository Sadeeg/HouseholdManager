---
name: Anforderung-Loop
description: Use when a rough feature idea must become a GitHub issue that an agent can implement without asking questions. Drives the 3-round refinement loop (Erfassen → fachliches Refinement → technisches Refinement), grills the requirement with one question at a time until the understanding is secured, asks at every phase boundary whether to continue, gates every round and loops back to the responsible round on gaps. The GitHub issue itself is the working state. Load this when the user asks for ein Issue, eine Anforderung, eine Epic-Planung, or wants an existing issue refined further.
---

# Anforderungs-Loop

Wandelt eine grobe Idee in ein Issue um, das ein Agent ohne Rückfragen
bearbeiten kann. Drei Runden, jede mit Grilling-Phase, festem Ergebnis und
Gate.

Der Kern des Loops ist **nicht** Lückensammlung, sondern **Verständnissicherung**:
in jeder Runde wird die Anforderung kritisch hinterfragt — eine Frage pro
Zug — bis Agent und Auftraggeber dasselbe verstehen. Findet das Gate
Lücken, läuft die Schleife in die zuständige Runde zurück.

Die Methode steht in `references/grilling.md` — Angriffskategorien,
Frageprotokoll, Stop-Kriterien. Vor dem ersten Grilling lesen.

## Arbeitsstand: das GitHub-Issue

**Es gibt keine lokale Draft-Datei.** Das Issue ist der Arbeitsstand:

| Element | Ort |
|---------|-----|
| aktueller Issue-Text | Issue-Body, ersetzt nach jeder Runde |
| Zustand (`runde`, `status`) | HTML-Kommentar ganz oben im Body |
| Grilling-Protokoll | ein Kommentar je Runde |
| „in Arbeit"-Signal | `[WIP]`-Präfix im Titel |

Repo: `Sadeeg/HouseholdManager`.

Damit ist der Loop von jeder Session und jedem Rechner fortsetzbar, und die
Runden sind unterwegs sichtbar statt erst am Ende.

Die Skill `issue-github` kapselt das Schreiben: Teil A (anlegen, Ende
Runde 1), Teil B (Runde aktualisieren, Ende Runde 2/3), Teil C (finalisieren,
nach dem finalen Gate).

## Runden

| # | Runde | Skill/Agent | Grilling | Ergebnis |
|---|-------|-------------|----------|----------|
| 1 | Erfassen | `anforderung-intake` | Verständnis sichern (Ziel, Kern, Verzicht) | Issue angelegt: bestätigtes Ziel, Kontext, Scope, Epic-/Phasen-Schnitt |
| 2 | Fachliches Refinement | `anforderung-fachlich` | Fachliche Regeln attackieren (Randfall, Rolle, Umkehr) | Body ersetzt: Rollen, Nutzerfluss, Regeln, Akzeptanzkriterien, Fehlerfälle |
| 2a | **UX-Entwurf (Querrunde)** | Agent `ux-designer` | — (keine Fragen an den Nutzer) | Body ergänzt: `## UX` — Screens, Abläufe, Zustände, Inhalte, Design-DNA |
| 2b | **Architektur (Querrunde)** | Agent `arc-agent` | — (keine Fragen an den Nutzer) | arc42 gepflegt, Vorgaben für Runde 3, offene Architekturfragen |
| 3 | Technisches Refinement | `anforderung-technisch` | Technische Entscheidungen attackieren (Widerspruch, Folge, Zeit) | Body ersetzt: Architektur, Datenmodell, API, UI, Dateien, Tests, Verify |

Die Querrunden liegen **zwischen** den Fachrunden: Wenn die Fachlogik steht,
aber die Umsetzung noch offen ist, entwirft der UX-Agent die Oberfläche
(`## UX` im Issue) und der ARC-Agent die Architektur (arc42, Vorgaben für
Runde 3). Beide entscheiden nichts — offene Fragen gehen als Rückfrage an
den Auftraggeber.

Querschnitt in jeder Runde: `issue-formulierung` (Hausstil, Title-Format,
Labels, Section-Reihenfolge). Prüfung nach jeder Runde: `anforderung-gate`.

## Ablauf

Der Auftraggeber steuert das Tempo: **am Ende jeder Runde wird gefragt, ob
die nächste folgt.** Ohne Bestätigung geht es nicht weiter — die Runde
endet sauber, das Issue bleibt `[WIP]` und kann später fortgesetzt werden.

1. **Runde 1 — Erfassen.** Skill `anforderung-intake` laden und ausführen.
   Grilling durchführen, bis das Verständnis gesichert ist. Danach
   `issue-github` **Teil A**: Issue anlegen, Protokoll der Runde 1 als
   Kommentar anhängen. Dann `anforderung-gate` mit `runde: 1`.
   **→ Phasen-Übergang fragen.**
2. **Runde 2 — Fachliches Refinement.** Skill `anforderung-fachlich` laden
   und ausführen, Grilling durchführen, `issue-github` **Teil B** (Body
   ersetzen, Protokoll der Runde 2 anhängen), Gate mit `runde: 2`.
   **→ Phasen-Übergang fragen.**
3. **Runde 2a — UX-Entwurf (Querrunde).** Nach der Bestätigung für Runde 3
   den Subagenten `ux-designer` starten (eigene Session, frischer Kontext):

   ```text
   Entwirfe die Oberfläche für das Feature aus Issue <Nr> (fachliche Runde
   steht) nach der Design-DNA. Schreibe den Entwurf als Abschnitt „## UX"
   in den Issue-Body. Rückfragen formulieren, nichts erfinden.
   ```

   Ergebnis: `## UX` im Issue. **Offene UX-Fragen** werden dem Auftraggeber
   gestellt — ist eine davon blockierend, wird sie **vor** Runde 3 geklärt.
4. **Runde 2b — Architektur (Querrunde).** Danach den Subagenten `arc-agent`
   starten:

   ```text
   Prüfe die Architektur für das Feature aus Issue <Nr> (fachliche Runde
   steht, UX-Abschnitt ist geschrieben). Pflege die arc42-Dokumentation
   unter arc42/ und liefere die Vorgaben für die technische Runde. Offene
   Architekturfragen als Rückfrage formulieren, nichts selbst entscheiden.
   ```

   Ergebnis: arc42 aktualisiert, Vorgaben für Runde 3. **Offene
   Architekturfragen** werden dem Auftraggeber gestellt — ist eine davon
   blockierend, wird sie **vor** Runde 3 geklärt.
5. **Runde 3 — Technisches Refinement.** Skill `anforderung-technisch` laden
   und ausführen (UX-Abschnitt und ARC-Vorgaben sind die Eingabe), Grilling
   durchführen, `issue-github` **Teil B**, Gate mit `runde: 3`.
   **→ Phasen-Übergang fragen.**
6. **Gate final.** `anforderung-gate` mit `runde: final`. Bei `PASS`:
   `issue-github` **Teil C** — Body finalisieren, `[WIP]` aus dem Titel,
   Labels und Typ setzen, Abschlusskommentar anhängen. Status im Marker
   wird `READY_FOR_AGENT`.
7. **Ausgabe** — der fertige Issue-Text im Chat, mit Nummer, URL und Status.

### Phasen-Übergang

Steht am Ende jeder Runde und fragt **eine** Frage:

```text
Runde 1 abgeschlossen — das Issue #129 steht mit bestätigtem Ziel, Kontext
und Scope.

Soll ich mit Runde 2 weitermachen?

A — Ja, weiter mit dem fachlichen Refinement (Rollen, Regeln,
    Akzeptanzkriterien). Das sind die Fragen, die du selbst beantworten musst.
B — Nein, hier pausieren. Das Issue bleibt [WIP], und du setzt später mit
    `/anforderung 129` fort.
C — <freie Antwort: eigene Richtung>
```

Regeln:

- Die Frage wird **nach** dem Gate der Runde gestellt, nie davor. Ein
  `GAPS` wird zuerst behoben (Rückkehr in die Runde), erst ein `PASS`
  erlaubt den Übergang.
- Die Antwort steuert den Ablauf: **A** → nächste Runde, **B** → die Runde
  endet hier sauber, das Issue bleibt `[WIP]` mit dem Marker der gerade
  abgeschlossenen Runde, **C** → die freie Antwort gilt als Anweisung und
  wird befolgt.
- Nach **B** fasst der Agent zusammen, was erreicht ist und was als Nächstes
  kommt — damit der Wiedereinstieg nach Tagen klar ist.
- Bei **B** wird **nichts** finalisiert: kein `[WIP]`-Entfernen, keine
  Labels, kein Typ. Das passiert erst nach dem finalen Gate.
- Der Übergang ersetzt keine Grilling-Frage, er steht davor.

### Fortsetzen

Der Einstieg entscheidet, was passiert:

| Argument | Bedeutung |
|----------|-----------|
| Eine Idee (Text) | Neues Issue. Der Loop beginnt bei Runde 1. |
| Eine Issue-Nummer | Vorhandenes Issue. Der Marker im Body verrät den Stand: `runde=1` → Runde 2 folgt, `runde=2` → Runde 3, `runde=3` → finales Gate. |

Ohne erkennbares Argument und ohne auffindbares `[WIP]`-Issue wird nach
gefragt, welches Issue gemeint ist.

## Grilling-Phase

Nach dem inhaltlichen Teil jeder Runde folgt das Grilling:

1. Eine Frage nach der anderen stellen. Nie zwei auf einmal, nie gebündelt.
2. Nach jeder Antwort: die Antwort in den **Protokoll-Abschnitt der Runde**
   schreiben, den Body bei Bedarf anpassen, dann die nächste Frage.
3. Nach jedem dritten Frage die Angriffskategorie wechseln.
4. Das Grilling endet, wenn die sechs Stop-Kriterien aus
   `references/grilling.md` erfüllt sind. **Vorher wird nicht weitergearbeitet.**
   Jede Antwort trägt ihre Folge mit: `→ **AC<n>**`, `→ **Abweichung**` oder
   `→ **Regel …**` — ohne diese Markierung geht die Information beim
   Übergang in die nächste Runde verloren.

Zwischen den Fragen wird zusammengefasst, was verstanden wurde — der
Auftraggeber soll den Fortschritt sehen, nicht nur einen Fragebogen.

## Gate-Vertrag

`anforderung-gate` liefert immer exakt dieses Format:

```text
GATE: runde=<n> status=<PASS|GAPS>
LÜCKEN:
- [<KATEGORIE>] <konkrete Lücke> → runde=<zielrunde>
KORREKTUREN:
- [<Abschnitt>] <was zu korrigieren ist> → runde=<zielrunde>
```

- `status=PASS` → `LÜCKEN:` ist leer. `KORREKTUREN:` darf gefüllt sein und
  wird still in der Zielrunde mitgenommen.
- `status=GAPS` → mindestens eine Lücke mit Zielrunde.
- `runde=final` → Zielrunde ist immer `1`, `2` oder `3`.
- `[VERSTÄNDNIS]` bedeutet: das Grilling dieser Runde hat die Stop-Kriterien
  noch nicht erfüllt → zurück in die Runde, dort weiterfragen.

Keine Prosa drumherum, keine Zusammenfassung. Der Orchestrator wertet nur
`status` und die Zielrunde aus. `KORREKTUREN` lösen keinen weiteren
Durchlauf aus.

## Gate-Eingabe

Das Gate liest **das Issue**, nicht eine Datei:

- `issue_read` mit `method: "get"` → der aktuelle Body (= Issue-Text plus
  Zustandsmarker)
- `issue_read` mit `method: "get_comments"` → die Protokolle der bisherigen
  Runden. **Form:** Array direkt, kein Objekt mit `comments`-Schlüssel.

Der Zustand steht im Marker ganz oben im Body. Fehlt ein Protokoll zu einer
abgeschlossenen Runde, ist das eine Lücke der Art `VERSTÄNDNIS`.

## Epic + Phasen-Issues

Runde 1 entscheidet, ob das Thema ein Epic ist:

- **Epic** (unabhängig lieferbare Teile, je allein nutzbar): Runde 1 erzeugt
  die Phasenliste. Das Epic-Issue wird zuerst angelegt, jede Phase als
  **Sub-Issue** mit `parent_issue_number`.
- **Einzelissue** (vertikaler Schnitt — ein Issue in einem Branch): ein
  Issue, keine Phase.

Phasen-Nummerierung ist fortlaufend über das Epic (`3.1`, `3.2`, …). Das
Epic-Nummer-Präfix `[Epic N]` und der Phasen-Title `[N.M]` sind fix, siehe
`issue-formulierung`.

## Regeln

- In keiner Runde wird Code geschrieben. Ziel ist ausschließlich das Issue.
- Der Body des Issues wird nach jeder Runde **vollständig ersetzt**, nicht
  ergänzt. Er enthält immer den kompletten aktuellen Issue-Text.
- Das Protokoll steht in **Kommentaren**, nie im Body.
- **Genau eine Frage pro Zug.** Auch wenn drei Lücken offen sind, wird eine
  gestellt. Das Grilling ist der wichtigste Teil des Loops.
- Kein Verhör: zwischen den Fragen wird zusammengefasst, was verstanden
  wurde, und die Fragen enden, sobald die Stop-Kriterien erfüllt sind.
- Fakten, die nicht belegt sind, werden als Annahme markiert
  (`_Annahme: ...`) und landen in `Offene Punkte` unter **Annahmen**.
  Nach dem Grilling enthält `## Offene Punkte` **nur noch Annahmen** — die
  User-Entscheidungen sind entschieden und stehen als Regeln, Kriterien und
  Festlegungen im Issue-Text.
- Erkenntnisse aus dem Repo (bestehende Klassen, Routen, Konventionen) kommen
  aus der Bestandsprüfung in `anforderung-intake` bzw. aus
  `anforderung-technisch` — nicht aus dem Gedächtnis.
- Am Ende steht immer eine klare Aussage: `READY_FOR_AGENT` oder
  `BLOCKED` mit den offenen Punkten.
- Benachbarte Fehler, die beim Refinement auffallen, werden **nicht** in das
  laufende Issue eingemischt. Sie landen als kurzer Hinweis unter
  „Folge-Issues" am Ende des Bodies, damit sie später als eigenes Issue
  entstehen.
- **Prozesswissen gehört nach `AGENTS.md`, nicht ins Issue.** Braucht das
  Feature einen Release-, Deploy- oder Betriebsschritt, wird der Ablauf in
  `AGENTS.md` beschrieben — das Issue beschreibt das Feature. Das finale
  Gate prüft, ob dieser Schritt dort abgedeckt ist; fehlt er, ist das eine
  Lücke der Art `FORMAT` → runde=1.
- **Erkenntnisse werden zu Regeln, nicht nur zu Text.** Nach den Gates
  (Umsetzung, Review, Abnahme) schreibt die Nachbereitung die Erfahrungen
  nach `docs/erkenntnisse-issue-<n>.md` — und **überführt** die
  wiederkehrenden Fehler in Regeln: Prozesswissen nach `AGENTS.md`,
  Umsetzungswissen in den zuständigen Skill (z. B. Layout-Fehler →
  `ux-design`, Testfallen → `feature-testing`, Grenzwerte ohne Meldung →
  `backend-ports-adapters`). Ein Erkenntnis-Dokument ohne überführte Regel
  ändert an der nächsten Runde nichts.

## Skill-Ladereihenfolge

```text
anforderung-loop
  → references/grilling.md
  → anforderung-intake | anforderung-fachlich
  → ux-designer (nur zwischen Runde 2 und 3, bei UI-Anteil)
  → arc-agent (nur zwischen Runde 2 und 3)
  → anforderung-technisch
  → issue-github
  → issue-formulierung
  → anforderung-gate
```

Die Reihenfolge ist bindend: erst Inhalt, dann Grilling, dann schreiben,
dann formatieren, dann prüfen.
