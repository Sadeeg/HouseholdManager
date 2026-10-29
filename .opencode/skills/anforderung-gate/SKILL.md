---
name: Anforderung-Gate
description: Use after each refinement round of the requirements loop to check the draft issue for completeness. Produces a fixed PASS/GAPS verdict with the target round for every gap. Load this when a draft must be validated against the completeness checklists of round 1, 2, 3 or the final check.
---

# Anforderung-Gate

Prüft das GitHub-Issue gegen die Vollständigkeitscheckliste der jeweiligen
Runde und liefert ein maschinenlesbares Urteil. Kein Text, kein Ratgeber —
nur der Befund.

## Eingabe

Das Gate liest **das Issue**, nicht eine Datei:

- `issue_read` mit `method: "get"` → der aktuelle Body: der Zustandsmarker
  (oben) plus der Issue-Text
- `issue_read` mit `method: "get_comments"` → die Protokolle der
  bisherigen Runden. **Form:** das Tool liefert ein **Array direkt**
  zurück, kein Objekt mit `comments`-Schlüssel. Ein Zugriff auf `.comments`
  ergibt `undefined` und die Protokolle wären unsichtbar.

Der Zustand steht im Marker ganz oben im Body:
`<!-- anforderung-loop: runde=2 status=IN_PROGRESS issue=129 -->`

- `runde`: `1`, `2`, `3` oder `final`
- `status`: `IN_PROGRESS`, `READY_FOR_AGENT` oder `BLOCKED`

Fehlt der Marker, ist das eine Lücke der Art `FORMAT` → runde=1.

## Ausgabeformat

```text
GATE: runde=<n> status=<PASS|GAPS>
LÜCKEN:
- [<KATEGORIE>] <konkrete Lücke> → runde=<zielrunde>
KORREKTUREN:
- [<Abschnitt>] <was zu korrigieren ist> → runde=<zielrunde>
```

- `status=PASS` → `LÜCKEN:`-Block entfällt. `KORREKTUREN:` bleibt
  bestehen, sie blockieren nicht.
- `status=GAPS` → mindestens eine Lücke, jede mit Zielrunde.
- `runde=final` → Zielrunde ist immer `1`, `2` oder `3`.
- **Jede Korrektur trägt ihre Zielrunde** — die Runde, die den Abschnitt
  besitzt. Eine Korrektur an `## Acceptance Criteria` gehört zu Runde 2,
  eine an `## Teststrategie` zu Runde 3, eine an `## Goal` oder
  `## Offene Punkte` zu Runde 1.

**Lücke vs. Korrektur.** Eine *Lücke* ist eine Information, die der
umsetzende Agent braucht und die nicht vorhanden ist — fehlende Regel,
fehlendes Kriterium, fehlender Vertrag. Eine *Korrektur* ist ein
Formfehler: unsauberer Titel, abweichende Section-Reihenfolge, unklare
Formulierung, Sammelkriterium mit zwei Aussagen. Korrekturen blockieren den
Loop nie, sie werden in der Zielrunde mitschweigend mitgenommen. Wenn das
Gate nur Korrekturen findet, ist das Ergebnis `PASS` — mit einem nichtleeren
`KORREKTUREN:`-Block.

Kategorien:

| Kategorie | Bedeutung | Zielrunde |
|-----------|-----------|-----------|
| `SCOPE` | Ziel, Scope oder Abgrenzung unklar | 1 |
| `FACH` | fachlich unvollständig (Regel, Ablauf, Kriterium) | 2 |
| `TECH` | technisch unvollständig (Architektur, API, Daten, Tests) | 3 |
| `FORMAT` | Interpretation gefährdet (fehlender `depends_on`, fehlende Epic-Entscheidung, unlesbare Struktur) | 1 |
| `VERSTÄNDNIS` | Das Grilling dieser Runde hat die Stop-Kriterien noch nicht erfüllt | 1, 2 oder 3 |

`FORMAT` ist **nicht** für Geschmacksfragen. „Title hätte einen Doppelpunkt"
ist eine Korrektur, keine Lücke.

`VERSTÄNDNIS` wird nur gemeldet, wenn der Draft **konkret belegt**, dass
noch nicht gefragt wurde: offene Frage im Entwurf, unbeantwortete
User-Entscheidung, oder fehlendes Entscheidungsprotokoll. Ein Verdacht auf
unzureichendes Verständnis ist kein Grund für eine Lücke — dafür ist das
Grilling da, nicht das Gate.

## Checkliste Runde 1 — Erfassen

- [ ] Ziel in ein bis zwei Sätzen, aus Sicht des Nutzers formuliert (nicht "Feature X bauen", sondern "der Nutzer kann …")
- [ ] Ziel ist bestätigt — es steht im Entscheidungsprotokoll als bestätigtes Ziel, oder als Frage mit Antwort
- [ ] Kontext benennt: welche heutige Seite / welcher Service / welche Klasse ist betroffen
- [ ] Scope als Stichpunkte, jeder Punkt beginnt mit einem Verb
- [ ] Nicht-Ziele grob benannt (mindestens "was ist ausdrücklich nicht dabei")
- [ ] Kern und Verzicht benannt: ein Satz, was das Feature *nicht* ist
- [ ] Epic-Entscheidung getroffen: Epic oder Einzelissue — steht im Status-Block
- [ ] Bei Epic: Phasenliste existiert, Phasen sind durchnummeriert (`N.M`), Abhängigkeiten benannt
- [ ] Status-Block vollständig im Zustandsmarker: `runde`, `status`, `issue`
- [ ] Entscheidungsprotokoll existiert als **Kommentar** dieser Runde und enthält die gestellten Fragen mit ihren Antworten
- [ ] Keine unbeantwortete User-Entscheidung in `## Offene Punkte`
- [ ] Annahmen als `_Annahme: …` markiert und in `Offene Punkte` gelistet

## Checkliste Runde 2 — Fachliches Refinement

- [ ] Nutzerrolle benannt: wer löst das aus, in welchem Kontext
- [ ] Nutzerfluss als geordnete Schritte beschrieben (nicht nur Stichpunkte zu Funktionen)
- [ ] Fachliche Regeln explizit: Berechnungen, Zustände, Fristen, Berechtigungen, Grenzwerte
- [ ] Akzeptanzkriterien fachlich formuliert und prüfbar — ohne Dateinamen, Endpunkte oder Klassen
- [ ] Fehler- und Randfälle: leer, ungültig, abgelehnt, Abbruch, Mehrfachauslösung
- [ ] Out of Scope als explizite Liste
- [ ] Abhängigkeiten fachlich benannt (was muss fachlich vorher fertig sein)
- [ ] Keine offenen fachlichen Fragen mehr übrig — oder sie stehen konkret in `Offene Punkte` mit Optionen
- [ ] Jedes fachliche Kriterium lässt sich später in ein technisches übersetzen
- [ ] Entscheidungsprotokoll der Runde 2 liegt als Kommentar vor, mit den F/A-Paaren
- [ ] Die Antworten aus dem Grilling sind in Regeln, Akzeptanzkriterien oder Fehlerfälle eingearbeitet — nicht nur im Protokoll stehen geblieben
- [ ] **Jede im Protokoll markierte Eigenschaft ist angekommen.** Jede Markierung `→ **AC<n>**` aus den Kommentaren hat ein Akzeptanzkriterium mit genau dieser ID im Body; jede `→ **Abweichung**` steht in `## Referenz`; jede `→ **Regel …**` steht in den fachlichen Regeln. Fehlt eine Markierung ohne Gegenstück, ist das eine Lücke der Art `FACH` → runde=2 — mit Angabe der fehlenden Markierung
- [ ] **Akzeptanzkriterien tragen IDs** (`AC1`, `AC2`, …), fortlaufend, ohne Lücken und ohne doppelte Nummern
- [ ] **Jede sichtbare Eingabe hat einen Pflegeschritt im Nutzerfluss und einen benannten Ort.** Keine Eingabe, die nur im Ergebnis erscheint und für die der Nutzer keinen Ort kennt
- [ ] **Der UX-Abschnitt ist vorhanden und umsetzbar** (bei Features mit UI-Anteil): Screens mit Rolle und Zweck, Ablauf inkl. Rückwegen, Elemente mit Zuständen, Leer-/Lade-/Fehlerzustand, Formulare mit Label und Fehlerzuordnung. **Gestaltungsentscheidungen nennen Token-Namen aus der Design-DNA**, keine Beschreibung wie „wirkt modern". Fehlt der Abschnitt bei einem Feature mit UI-Anteil, ist das eine Lücke der Art `FACH` → runde=2.
- [ ] **Jeder Platzhalter hat eine Rückmeldung.** Überall dort, wo ein leerer Wert als Platzhalter, Auslassung oder leere Liste erscheint, steht, wie der Nutzer es erfährt (Hinweisliste, Hinweisfeld, Fehlermeldung)
- [ ] **Eigenschaften eines Vorbilds sind entschieden.** Gibt es ein Referenzdokument, einen Screenshot oder eine Vorgängerfunktion, trägt `## Referenz` jede beobachtbare Eigenschaft mit „übernommen als AC<n>" oder „Abweichung". Eine Eigenschaft ohne beide ist eine Lücke der Art `FACH` → runde=2

## Checkliste Runde 3 — Technisches Refinement

- [ ] Zielort im Code benannt: welche Layer, neue Klassen oder Erweiterungen, mit Namensvorschlag
- [ ] Datenmodell und Migration: neue Tabelle / Spalte / Index / Constraint, mit der nächsten freien V-Nummer — oder explizit "keine Schema-Änderung"
- [ ] API: Endpunkte mit Methode, Pfad, Request und Response, Fehlerfälle mit Statuscode
- [ ] Frontend: Route, Komponenten, State (Signale), Validierung
- [ ] Relevante Konventionen aus `AGENTS.md` benannt (nur die 3–5 relevanten, nicht die ganze Liste)
- [ ] Konkrete Dateiliste mit echten Pfaden — jeder Pfad ist im Repo verifiziert
- [ ] Teststrategie: Testart, Testfälle, Ort der Tests
- [ ] Verify-Block mit kopierfertigen Kommandos (siehe `issue-formulierung`)
- [ ] Risiken und bekannte Gotchas benannt
- [ ] Fachliche Akzeptanzkriterien sind technisch prüfbar (jedes Kriterium hat einen Testfall oder ein Kommando)
- [ ] **Die arc42-Dokumentation ist aktualisiert.** Die vom ARC-Agenten berührten Dokumente unter `arc42/` beschreiben das Feature (Bausteine, Abläufe, Daten, Entscheidungen); offene Punkte sind als `> **Offen:** …` markiert, nicht geraten.
- [ ] **Jedes Akzeptanzkriterium hat eine Testzuordnung.** Die Teststrategie führt jede `AC<n>` in einer Zeile mit Testart und Ort. Ein AC ohne Zeile ist eine Lücke der Art `TECH` → runde=3
- [ ] **Begriffsabgleich vorhanden.** `## Begriffe` führt für jeden im Dokument sichtbaren Fachbegriff DB-Feld, UI-Label und das Literal im Ergebnis. Wo die vier auseinanderlaufen, ist das benannt
- [ ] **Die Verträge passen zum Zielort.** Kein Vertrag widerspricht dem Zielort aus Schritt 1 — insbesondere kein Vertrag, der eine Datenbankabfrage verbietet, während der Zielort Daten aus der Datenbank lädt
- [ ] Keine offenen technischen Fragen mehr übrig
- [ ] Entscheidungsprotokoll der Runde 3 liegt als Kommentar vor, mit den F/A-Paaren

## Checkliste Gate final

- [ ] Alle Abschnitte der Hausstruktur vorhanden und in Reihenfolge
- [ ] Keine Platzhalter: kein `TODO`, kein `TBD`, kein `...`, keine leeren Code-Blöcke, kein "etc."
- [ ] Jeder referenzierte Pfad existiert im Repo (mit `find`/`grep` prüfen, nicht raten)
- [ ] Jeder Pfad, der neu ist, folgt exakt der Namenskonvention des Repos (`endpoint/model/<Entity>Dto.java`, `domain/<feature>/features/<name>/<name>.component.ts`, `service/<Entity>Service.java`) — und ist als „Neu" markiert, mit dem Hinweis, dass der Pfad beim Bauen anzupassen ist, falls die Konvention nicht greift
- [ ] Verwendete Endpunkte existieren oder sind als neu markiert
- [ ] Labels und Title folgen dem Format aus `issue-formulierung`
- [ ] `depends_on` verweist auf existierende Phasen bzw. Issues
- [ ] `## Offene Punkte` enthält nach dem Grilling **nur Annahmen** — keine offene User-Entscheidung mehr
- [ ] Das Issue hat für **jede** abgeschlossene Runde einen Protokoll-Kommentar mit den F/A-Paaren
- [ ] Zu jedem `F…` im Protokoll existiert eine `A…`-Antwort — keine Frage ohne Antwort
- [ ] Die Stop-Kriterien aus `references/grilling.md` sind erfüllt: Ziel bestätigt, keine offene Frage mit Tragweite, Entscheidungen entschieden, Durchschau-Test bestanden, Kern und Verzicht benannt, keine Beobachtung ohne Folge
- [ ] **Kein Kriterium ohne Test.** Jede `AC<n>` aus `## Acceptance Criteria` hat eine Zeile in `## Tests` oder einen benannten manuellen Prüfschritt in `## Verify`
- [ ] **Keine Markierung ohne Gegenstück.** Über alle Protokoll-Kommentare: jede `→ **AC<n>**` ist im Body, jede `→ **Abweichung**` in `## Referenz`, jede `→ **Regel …**` in den Regeln
- [ ] **Release-Schritt gedeckt.** Wenn das Feature ausgeliefert wird, steht der Deploy-/Release-Ablauf in `AGENTS.md` (Versions-Bump, Tag, Image-Publish). Fehlt er dort, ist das eine Lücke der Art `FORMAT` → runde=1
- [ ] Der Body enthält **kein** Entscheidungsprotokoll — es steht ausschließlich in den Kommentaren
- [ ] Ein Agent kann das Issue ohne weitere Rückfrage bearbeiten — Simulation: Würde ein Agent wissen, welche Dateien er anfasst, welche API er baut und woran er erkennt, dass es fertig ist?

## Arbeitsweise

1. Issue-Body und Kommentare lesen, Zustandsmarker prüfen.
2. Checkliste der Runde durchgehen. Jede offene Zeile wird eine Lücke —
   außer sie ist eine reine Formfrage, dann ist sie eine Korrektur.
3. Lücken und Korrekturen getrennt bewerten. Eine Formfrage macht den
   Status nicht `GAPS`.
4. Lücken formulieren als konkrete Aussage, nicht als Frage:
   `Akzeptanzkriterien fehlen für: Fehlerfall "Betrag größer als 0"`.
5. Ausgabe exakt in der oben definierten Form, ohne Zusatztext.

## Verifikation von Pfaden

Pfade prüfen, nicht annehmen. `rg` ist auf dem Host nicht installiert —
`grep`/`find` sind der Weg:

```bash
find frontend/src/app -name '*ausgaben*'
grep -rn 'class Ausgaben' frontend/src/app
ls backend/src/main/resources/db/migration/ | sort -V | tail -3
```

Wenn `rg` verfügbar ist, ist es schneller:

```bash
rg --files frontend/src/app | rg 'ausgaben'
```

Jeder bestehende Pfad muss im Repo existieren (mit `find`/`grep` prüfen, nicht
raten). Neue Pfade müssen der Namenskonvention aus `AGENTS.md` folgen und als
„Neu" markiert sein.

GitHub wird **nicht** über die Kommandozeile (`gh`) abgefragt, sondern über
die GitHub-Tools (`list_issues`, `search_issues`, `get_label`). `gh` ist auf
dem Host nicht installiert.
