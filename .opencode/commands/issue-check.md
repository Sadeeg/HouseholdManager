---
description: Bestehendes GitHub-Issue gegen den Hausstil und die Vollständigkeit prüfen
---

Prüfe das GitHub-Issue $ARGUMENTS.

Vorgehen:

1. Lade das Issue über die GitHub-Tools: `issue_read` mit `method: "get"`
   (Titel, Body, Labels, Typ) und mit `method: "get_comments"` (die
   Protokolle der Runden). Achtung: `get_comments` liefert ein Array
   direkt zurück, kein Objekt mit `comments`-Schlüssel.
2. Lade `issue-formulierung` und `references/hausstil.md`.
3. Prüfe in dieser Reihenfolge:
   - Title-Format (Epic, Phase, Einzelissue) — `[WIP]` ist zulässig, solange
     das Issue in Arbeit ist
   - Labels gegen das Vokabular
   - Section-Reihenfolge und Pflichtabschnitte
   - `## Acceptance Criteria` prüfbar, ohne Sammelkriterien
   - `## Verify` mit lauffertigen Kommandos
   - `## Depends on` verweist auf existierende Issues
   - referenzierte Pfade im Repo existieren (`find`/`grep`, nicht raten)
   - der Body enthält **kein** Entscheidungsprotokoll — es gehört in die
     Kommentare
4. Lade `anforderung-gate` und führe ihn mit `runde: final` gegen den
   Issue-Body aus. Fehlt ein Protokoll zu einer abgeschlossenen Runde, ist
   das eine Lücke der Art `VERSTÄNDNIS` → Zielrunde 1.
5. Lade `anforderung-technisch` und prüfe, ob die technischen Angaben
   (Datenmodell, API, Dateiliste, Tests) für eine Umsetzung reichen. Fehlt
   etwas, ist das eine `TECH`-Lücke → Zielrunde 3.
6. Lade `anforderung-fachlich` und prüfe, ob die fachlichen Angaben
   (Rollen, Ablauf, Regeln, Fehlerfälle) vollständig sind. Fehlt etwas,
   ist das eine `FACH`-Lücke → Zielrunde 2.
7. Lade `references/grilling.md` und prüfe, ob die Stop-Kriterien erfüllt
   sind: Ziel bestätigt, keine offene Frage mit Tragweite, Entscheidungen
   entschieden, Durchschau-Test bestanden, Kern und Verzicht benannt.
   Offene Punkte sind eine `VERSTÄNDNIS`-Lücke → in die betroffene Runde.
8. Gib aus: Gate-Befund, Liste der konkreten Befunde mit Abschnitt und
   Zielrunde, danach ein korrigierter Issue-Text als Vorschlag — ohne das
   Issue zu veröffentlichen oder zu verändern.
