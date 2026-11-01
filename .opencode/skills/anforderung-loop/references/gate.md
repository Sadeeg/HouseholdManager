# Gate — Vollständigkeitscheckliste je Runde

Prüft das **GitHub-Issue**. Eingabe: `issue_read` `get` (Body mit Marker, z. B.
`<!-- anforderung: runde=2 status=IN_PROGRESS issue=129 -->`) und
`get_comments` (Array direkt, kein `comments`). Marker fehlt → `FORMAT` → 1.
`runde`: 1/2/3/final. `final` prüft den **redigierten** Body (Runde 4).

## Gate Runde 4 — Redaktion (Ziel 4)

- [ ] Body ist **kürzer** als vor der Redaktion · Ziel: unter 40 % der Zeilen
- [ ] **Kein Verlust:** kein AC, kein Fehlerfall, keine Zustandsangabe, kein
      Token-Name, kein API-Pfad, kein Dateipfad, keine V-Nummer fehlt
- [ ] `## Acceptance Criteria` unverändert vollständig · `## Tests` je AC
      eine Zeile · `## Verify` mit Kommandos und manuellem Satz
- [ ] `## UX` trägt Elemente, Zustände, Labels, Token-Namen, Reihenfolge
- [ ] `## Files` nur neu/geändert, je Pfad eine Zeile
- [ ] `## Implementation` nur Stellen mit Alternativen
- [ ] Lesehinweis vorhanden: „Body allein; Kommentare nur, wenn hier etwas
      nicht ermittelbar ist"
- [ ] Keine Prosa ohne Handlungsfolge · keine Wiederholung aus Goal/Scope

## Ausgabeformat

```text
GATE: runde=<n> status=<PASS|GAPS>
LÜCKEN:
- [<KATEGORIE>] <konkrete Lücke> → runde=<zielrunde>
KORREKTUREN:
- [<Abschnitt>] <was zu korrigieren ist> → runde=<zielrunde>
```

- `PASS`: keine Lücke; `KORREKTUREN` still mitgenommen. `GAPS`: ≥1 Lücke mit
  Zielrunde; `final` → 1/2/3.
- Lücke = fehlende Information; Korrektur = Formfehler (Titel, Reihenfolge,
  Formulierung, Sammelkriterium) und blockiert nie. Zielrunde: AC 2, Tests 3,
  Goal/Offene Punkte 1.
- Kategorien: `SCOPE` 1, `FACH` 2, `TECH` 3, `FORMAT` 1, `VERSTÄNDNIS` 1/2/3.
  `FORMAT` nicht für Geschmack; `VERSTÄNDNIS` nur bei Beleg (offene Frage,
  unbeantwortete Entscheidung, fehlendes Protokoll).

## Runde 1 — Erfassen (Ziel 1)

- [ ] Ziel in 1–2 Sätzen, Nutzersicht, bestätigt (Protokoll) · Kontext:
      Seite/Service/Klasse
- [ ] Scope (Verb-Stichpunkte) + Nicht-Ziele · Kern/Verzicht: was es *nicht* ist
- [ ] Epic-Entscheidung; bei Epic Phasen `N.M` + Abhängigkeiten
- [ ] Statusblock `runde`/`status`/`issue` · Protokoll-Kommentar mit F/A
- [ ] Keine offene User-Entscheidung in `## Offene Punkte` · Annahmen
      `_Annahme: …` markiert und gelistet

## Runde 2 — Fachlich (Ziel 2)

- [ ] Rolle + Kontext · Nutzerfluss: geordnete Schritte
- [ ] Regeln: Berechnung, Zustände, Fristen, Rechte, Grenzen
- [ ] AC fachlich prüfbar (keine Datei-/API-/Klassennamen) · jedes AC technisch
      übersetzbar
- [ ] Fehlerfälle: leer, ungültig, abgelehnt, Abbruch, doppelt · Out of Scope +
      fachliche Abhängigkeiten
- [ ] Keine offene Fachfrage (sonst mit Optionen) · Protokoll R2 mit F/A,
      Antworten eingearbeitet
- [ ] Markierungen: `AC<n>` → AC-ID, `Abweichung` → `## Referenz`,
      `Regel …` → Regeln
- [ ] AC-IDs fortlaufend, ohne Lücke/Doppelung · Eingabe: Pflegeschritt + Ort
- [ ] UX umsetzbar (UI): Screens/Rolle/Zweck, Ablauf + Rückwege, Zustände,
      leer/laden/Fehler, Labels, **Token-Namen**
- [ ] Platzhalter mit Rückmeldung · „kann …" mit Einstieg + Voraussetzung
- [ ] Aktionen im Default sichtbar (auch `disabled`) · Grenzen abgelehnt **und
      benannt**
- [ ] `## Referenz`: je Vorbild-Eigenschaft AC oder Abweichung

## Runde 3 — Technisch (Ziel 3)

- [ ] Zielort: Layer, Klassen, Namen · 3–5 Konventionen aus `AGENTS.md`
- [ ] Datenmodell: Tabelle, Spalte, Index, Constraint, V-Nummer — oder „keine
      Schema-Änderung"
- [ ] API: Methode, Pfad, Request, Response, Fehler + Statuscode · Frontend:
      Route, Komponenten, State, Validierung
- [ ] Dateiliste mit verifizierten Pfaden · Teststrategie: Art, Fälle, Ort
- [ ] Verify kopierfertig (`issue-formulierung`): Stack-Start, DB-Zustand,
      Browser-Pfad
- [ ] Doku in der Dateiliste: `arc42/` + `docs/adr/`, `design-dna.md`,
      `compose.yml` — „im selben Commit wie der Code"
- [ ] Risiken + Gotchas · jedes AC technisch prüfbar
- [ ] arc42 aktualisiert; Offenes als `> **Offen:** …` · **Jedes AC hat
      Testzuordnung**
- [ ] `## Begriffe`: DB-Feld, UI-Label, Literal · Verträge passen zum Zielort
- [ ] Keine offene Technikfrage · Protokoll R3 mit F/A

## Gate final (Ziel je Abschnitt)

- [ ] Hausstruktur vollständig, in Reihenfolge · keine Platzhalter (`TODO`,
      `TBD`, `...`, leere Code-Blöcke, „etc.")
- [ ] Jeder Pfad existiert (`find`/`grep`) · neue Pfade nach
      `references/technik-checkliste.md`, „Neu" markiert, Anpassung genannt
- [ ] Endpunkte existieren oder „neu" · Labels/Titel nach `issue-formulierung`
- [ ] `depends_on` verweist auf existierende Phasen/Issues · `## Offene Punkte`
      nur Annahmen
- [ ] Protokoll für **jede** Runde · zu jedem `F…` eine `A…`
- [ ] Stop-Kriterien erfüllt (Ziel, keine offene Frage, entschieden, Durchschau,
      Kern/Verzicht, keine Beobachtung ohne Folge)
- [ ] **Kein Kriterium ohne Test** · **keine Markierung ohne Gegenstück**
- [ ] **Release gedeckt** (Bump, Tag, Image in `AGENTS.md`) → sonst `FORMAT` → 1
- [ ] Body enthält **kein** Protokoll · Simulation: Dateien, API und Fertigsein
      ohne Rückfrage
- [ ] **Redaktion** (Runde 4): Body ist kürzer als vor der Redaktion, kein AC
      / Fehlerfall / Zustand / Token / API-Pfad / Dateipfad verloren · Lesehinweis
      („Body allein, Kommentare nur bei nicht Ermittelbarem") vorhanden →
      sonst `FORMAT` → 4

## Arbeitsweise

1. Body + Kommentare lesen, Marker prüfen.
2. Checkliste durchgehen; offene Zeile = Lücke, reine Formfrage = Korrektur.
3. Getrennt bewerten; Formfrage macht nicht `GAPS`.
4. Lücke als Aussage: `AC fehlen für: Fehlerfall "Betrag größer als 0"`.
5. Ausgabe exakt im Format, ohne Zusatztext.

Pfade prüfen (`rg` fehlt — `grep`/`find`): `find frontend/src/app -name
'*ausgaben*'`, `grep -rn 'class Ausgaben' frontend/src/app`, `ls
backend/src/main/resources/db/migration/ | sort -V | tail -3`. GitHub nur über
GitHub-Tools, nicht `gh`.
