---
description: GitHub-Issue des Anforderungs-Loops anlegen, aktualisieren oder finalisieren
---

Verwalte das GitHub-Issue des Anforderungs-Loops.

Issue:
$ARGUMENTS

Vorgehen:

1. Lade die Skill `issue-github` und folge ihr exakt.
2. Wähle den passenden Teil:
   - **Teil A** — Issue anlegen (Ende Runde 1): Duplikatsprüfung, Labels
     prüfen, Typ bestimmen, `[WIP]`-Titel, Body mit Zustandsmarker,
     Protokoll der Runde als Kommentar.
   - **Teil B** — Runde aktualisieren (Ende Runde 2/3): Body ersetzen,
     Protokoll der Runde als Kommentar anhängen, Body zurücklesen.
   - **Teil C** — finalisieren (nach dem finalen Gate): Body finalisieren,
     `[WIP]` aus dem Titel, Labels und Typ setzen, Abschlusskommentar.
3. Ohne Issue-Nummer im Argument: finde das Issue über `list_issues` mit
   `state: "OPEN"` und filtere selbst im Titel auf `[WIP]` (das Tool hat
   keinen Titel-Filter). Bei mehreren Kandidaten nachfragen.

Regeln:

- Kein Issue ohne vorherige Duplikatsprüfung anlegen.
- Keine Labels erfinden.
- Das Protokoll gehört als Kommentar an das Issue, nicht in den Body.
- Der Body wird immer vollständig ersetzt, nie ergänzt.
- Bestehende Issues weder schließen noch editiert.
