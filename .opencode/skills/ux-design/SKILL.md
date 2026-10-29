---
name: UX-Design
description: Use when a feature needs a user interface or an interaction design. Defines the design method for HouseholdManager (flow, screens, states, interaction rules), the format of the UX section written into the issue, and the design DNA that every implementation must follow — modern, dark mode, a touch of terminal. Load this before designing screens or before implementing any UI.
---

# UX-Design

Der UX-Entwurf ist eine eigene Runde im Anforderungs-Loop: **Runde 2a**, nach
der fachlichen Runde, vor der ARC-Runde. Ziel ist, dass die Umsetzung die
Oberfläche **nicht erfinden** muss — der Entwurf steht im Issue, die
Design-DNA liefert die verbindliche Sprache.

Verbindliche Quelle für das Design: [`docs/design/design-dna.md`](../../docs/design/design-dna.md)
und die Tokens in `frontend/src/styles/_design-tokens.scss`.

## Die Design-DNA (Kurzform, Details im Dokument)

| Aspekt | Festlegung |
|--------|------------|
| Grundton | **Dark Mode** (`--hm-bg` `#0b0f14`), helle Variante nur als Ausnahme |
| Anmutung | Modern, Terminal-Referenz: Monospace für Daten, `$`-Prefix, feine Linien, scharfe Kanten (Radius 4px) |
| Farbe | Akzent **trägt Bedeutung**: `--hm-accent` (Aktion), `--hm-accent-2` (Info), `--hm-accent-3` (Mitgliedschaft); max. 2 Akzente je Ansicht |
| Typografie | Sans für Text, Mono für IDs/Zeitstempel/Beträge/Tabellenköpfe (12px uppercase) |
| Linien statt Schatten | 1px `--hm-border`; Schatten nur für Overlays (Toast, Dialog) |
| Bewegung | 120–180ms, nur bei Zustandswechsel, nie als Dekoration |
| Verboten | Verläufe, Glassmorphism, große Radien, Emoji-Icons, Farbe als einzige Information |

## Entwurfsprozess

### 1. Verstehen (aus Runde 2, nicht erfragen)

Grundlage sind die fachlichen Ergebnisse: Rollen, Nutzerfluss, Regeln,
Fehlerfälle, Akzeptanzkriterien. Der UX-Entwurf übersetzt sie — er erfindet
keine Funktionen. Fehlt eine fachliche Grundlage (z. B. unklar, was bei
Fehlern passiert), ist das eine **Rückfrage**, kein Entwurf.

### 2. Bildschirme und Abläufe festlegen

Je Nutzerfluss:

```text
Screen: <Name>   Rolle: <wer>   Zweck: <was der Nutzer hier erreicht>
Eingang: <woher kommt der Nutzer>   Ausgang: <wohin geht er weiter>
```

Danach der **Ablauf** als geordnete Schritte (bereits in Runde 2 fachlich
erhoben) — der UX-Entwurf ergänzt **Interaktion** und **Rückwege**:

```text
1. Der Nutzer öffnet „Haushalte".
2. Er klickt „Haushalt anlegen" (Primäraktion, rechte Kopfzeile).
3. Das Formular erscheint inline (kein Dialog) — Grund: ...
4. Absenden → Ladezustand am Button → Erfolg → Hinweiszeile „Angelegt".
5. Abbruch → zurück zur Liste, Eingaben bleiben erhalten.
```

### 3. Zustände je Element (Pflicht)

Für jedes interaktive Element:

| Element | Default | Hover | Focus | Active | Disabled | Loading | Fehler |
|---------|---------|-------|-------|--------|----------|---------|--------|
| Button „Anlegen" | … | … | Ring sichtbar | … | – | Spinner + Text | – |

- **Leerzustand:** Text + eine Aktion (nie eine leere Fläche).
- **Fehlerzustand:** Text + Grund + nächste Handlung; `role="alert"`.
- **Ladezustand:** nie nur Spinner — was passiert, steht daneben.
- **Mehrfachauslösen:** Button während des Requests deaktiviert.

### 4. Inhalt und Sprache

- **Deutsch**, du-Formulierung, konkret („Haushalt anlegen", nicht „Neues
  Objekt erstellen").
- Fehlermeldungen: **was ist passiert + warum + was jetzt**. Kein
  Fehlercode ohne Erklärung.
- Technische Werte (IDs, Zeitstempel, Beträge) in Monospace; Datumsformat
  `dd.MM.yyyy`, Beträge mit Tabellenziffern.
- Zahlen nie in Fließtext, sondern in eigenen Zeilen/Zellen.

### 5. Barrierefreiheit mitentwerfen

- Kontrast: Fließtext ≥ 4.5:1, große Schrift/Linien ≥ 3:1 — gegen die
  Tokens der DNA prüfen.
- Fokus immer sichtbar (`--hm-focus-ring`), niemals `outline: none`.
- Keine Information nur über Farbe (auch nicht im Status).
- Formularfelder mit Label, Pflichtfelder markiert, Fehler am Feld **und**
  als Zusammenfassung.
- Reihenfolge der Tastatur bedienbar, Modal fokussiert und schließt mit
  `Esc`.

### 6. Das Issue bekommt den UX-Abschnitt

Der Entwurf wird als **eigener Issue-Abschnitt** `## UX` geschrieben (siehe
`issue-formulierung/references/hausstil.md`). Format:

```markdown
## UX

**Gestaltung:** Dark Mode, Design-DNA (Akzent für Aktion, Monospace für Daten)

### Screen: <Name>

| | |
|---|---|
| Rolle | <wer> |
| Zweck | <was> |
| Eingang / Ausgang | <woher / wohin> |

**Ablauf**

1. …

**Elemente**

| Element | Typ | Verhalten | Zustände |
|---------|-----|-----------|----------|
| „Haushalt anlegen" | Primäraktion (Button) | öffnet Formular inline | default, hover, focus, disabled |
| Namensfeld | Input (Signal Forms) | Pflicht, max. 120 Zeichen | default, focus, invalid (Text am Feld) |

**Zustände**

- Leer: „Noch kein Haushalt" + Aktion „Haushalt anlegen"
- Lade: „Haushalte werden geladen …"
- Fehler: „Haushalte konnten nicht geladen werden." + Aktion „Erneut versuchen"

**Regeln**

- Formular inline, kein Dialog (Begründung)
- Abbruch behält Eingaben
- Button ist während des Requests deaktiviert
```

### 7. Design-DNA pflegen

Braucht das Feature ein **neues** UI-Muster (eine neue Komponentenart, eine
neue Zustandslogik, ein neuer Akzent-Einsatz), dann:

- Muster in `docs/design/design-dna.md` unter **Komponenten** ergänzen —
  **bestehende Tokens wiederverwenden**; neue Farben oder Maße sind nur
  zulässig, wenn das Muster sie wirklich braucht, und werden dann in
  `_design-tokens.scss` nachgezogen.
- Bestehende Muster **nicht** duplizieren: passt eine vorhandene Komponente,
  wird sie benutzt.

## Regeln

- **Keine Funktionen erfinden.** Der UX-Entwurf macht die Fachlogik
  bedienbar, nicht größer.
- **Kein Moodboard.** Konkrete Werte aus der DNA, keine Beschreibung wie
  „wirkt modern".
- **Keine Hex-Literale im Entwurf**, sondern Token-Namen.
- **Der Entwurf ist umsetzbar:** Wer den Abschnitt liest, kann die Komponente
  bauen, ohne zu raten.
- **Alles, was nicht entschieden ist, wird als Frage markiert** — nicht als
  Vorschlag im Text versteckt.

## Grenze zu den anderen Runden

| Runde | Thema | Überschneidung vermeiden |
|-------|-------|--------------------------|
| 2 Fachlich | Regeln, Rollen, Fehlerfälle | UX legt **Interaktion und Darstellung** fest, nicht die Regel |
| 2a UX | Bildschirme, Zustände, Inhalte, DNA | Keine fachlichen Regeln neu erfinden |
| 2b ARC | Bausteine, Schichten, Daten | UX nennt keine Klassen — das ist die technische Runde |
| 3 Technisch | Komponenten, Dateien, Tests | Setzt den UX-Abschnitt um, ohne ihn zu ändern |
