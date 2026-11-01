---
description: Rohidee in ein umsetzungsreifes Issue überführen (3 Runden, Grilling, Phasen-Übergänge)
---

Führe den Anforderungs-Loop aus:

$ARGUMENTS

Der Arbeitsstand ist das **GitHub-Issue** — es wird nach Runde 1 angelegt
und nach jeder Runde aktualisiert. Es gibt keine lokale Draft-Datei.

**Einstieg:**

- Ist das Argument eine **Idee** → neues Issue, der Loop beginnt bei Runde 1.
- Ist das Argument eine **Issue-Nummer** → vorhandenes Issue fortsetzen. Der
  Zustandsmarker im Body verrät den Stand: `runde=1` → Runde 2 folgt,
  `runde=2` → Runde 3, `runde=3` → finales Gate.
- Ist nichts erkennbar → nachfragen, welches Issue gemeint ist
  (`list_issues` mit `state: "OPEN"`, dann im Titel auf `[WIP]` filtern —
  das Tool hat keinen Titel-Filter).

**Ablauf je Runde:**

1. Lade die Skill `anforderung-loop` und führe die Runde aus (Runde 1/2/3
   sind darin beschrieben). Lies vorher
   `.opencode/skills/anforderung-loop/references/grilling.md`.
2. **Grilling**: stelle **eine** Frage nach der anderen und warte auf jede
   Antwort. Höre erst auf, wenn die Stop-Kriterien erfüllt sind. Schreibe
   jede Antwort in den Protokoll-Abschnitt der Runde.
3. Lade `issue-formulierung` und formatiere den Issue-Text nach Hausstil.
4. Lade `issue-github` — Teil A nach Runde 1 (Issue anlegen), Teil B nach
   Runde 2 und 3 (Body ersetzen, Protokoll kommentieren).
5. Prüfe mit `anforderung-loop` → `references/gate.md` (Runde `<n>`). Bei
   `GAPS` → Zielrunde bestimmen, Runde erneut ausführen (max. 2 Durchläufe
   je Runde).
6. **Phasen-Übergang:** Frage **eine** Frage: „Soll ich mit Runde <n+1>
   weitermachen?" — A: weiter, B: pausieren (Issue bleibt `[WIP]`), C: eigene
   Richtung. Bei B zusammenfassen, was erreicht ist und was als Nächstes
   kommt, dann enden.
7. **Zwischen Runde 2 und Runde 3: die Querrunden.**
   - **UX-Entwurf (2a):** Subagent `ux-designer` starten — er entwirft die
     Oberfläche nach der Design-DNA und schreibt den Abschnitt `## UX` in den
     Issue-Body.
   - **Architektur (2b):** danach Subagent `arc-agent` starten — er pflegt
     `arc42/` und liefert die Vorgaben für Runde 3.
   - Offene UX- oder Architekturfragen dem Auftraggeber stellen, bevor Runde 3
     beginnt.
8. Nach Runde 3: `anforderung-loop` → `references/gate.md` mit `runde: final`.
   Bei `PASS` → `issue-github` **Teil C** — finalisieren, `[WIP]` aus dem
   Titel, Labels und Typ setzen, Abschlusskommentar.

Regeln:

- **Genau eine Frage pro Nachricht.** Auch wenn mehrere Lücken offen sind.
- Jede Frage nennt die Kategorie, die Frage selbst, was an ihr hängt und
  die Optionen mit Empfehlung.
- Erfinde keine Fakten. Unbelegtes wird als `_Annahme: …` markiert und in
  `Offene Punkte` gelistet.
- Das Protokoll gehört in die **Kommentare**, nicht in den Issue-Text.
