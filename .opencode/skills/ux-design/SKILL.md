---
name: UX-Design
description: Use when a feature needs a UI. Defines the design method (flow, screens, states), the '## UX' issue section and the design DNA.
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
- **Sichtbarkeit:** Jede Aktion ist im **Default-Zustand vorhanden** — nicht
  erst nach einer Auswahl. Ein `disabled`-Zustand bedeutet „sichtbar, aber
  gesperrt". Fehlt eine Aktion im Default-Zustand, ist das ein Entwurfsfehler.
- **Anordnung:** Was steht nebeneinander, was ist rechtsbündig, was
  verdeckt nichts. Eine Trefferliste gehört in den Dokumentfluss, nicht als
  Overlay über den Nachbarinhalt — ein Overlay (Dialog, Menü) verdeckt dann
  bewusst und nur im eigenen Zustand.

### 3a. Muster zuerst suchen

Vor dem ersten eigenen UI-Code wird das Repo nach einem vorhandenen Muster
durchsucht:

```bash
grep -rl "position: fixed" frontend/src/app --include=*.scss   # Overlays
find frontend/src/app/shared/components -maxdepth 1 -type d     # wiederverwendbar
```

- **Ein vorhandenes Muster wird kopiert, nicht erfunden.** Braucht das
  Feature einen Dialog, Bestätigungsdialog oder Seitenkopf, wird das Muster
  benutzt — auch wenn das eigene „einfacher" wirkt.
- **Ein Overlay ist ein Muster mit drei Teilen:** Backdrop, Panel
  (`role="alertdialog"`, `aria-modal="true"`, `Esc` schließt) und Fokus auf
  dem Panel. Der Fokus ist umgesetzt (Issue #9): die **Fokusfalle** liegt auf
  dem Wrapper (`cdkTrapFocus` mit `cdkTrapFocusAutoCapture`), das Panel trägt
  `cdkFocusInitial` und `tabindex="-1"` — dadurch bekommt es den initialen
  Fokus, der Tab bleibt im Dialog, und beim Schließen kehrt der Fokus auf das
  auslösende Element zurück. Das Overlay gehört auf den **Wrapper** (nicht auf
  den Backdrop-Knopf), Backdrop und Panel liegen in derselben Gitterzelle
  (\`grid-area: 1 / 1\`), das Panel bekommt \`max-height: 100%; overflow-y:
  auto\`. Der Backdrop-Knopf steht **nicht** in der Tab-Reihenfolge
  (\`tabindex="-1"\`) — \`Esc\` schließt ohnehin.
- **Ein neues Muster wird in die Design-DNA aufgenommen** (Schritt 7), sonst
  fehlt es beim nächsten Feature wieder.

### 3b. Sichtprüfung — Layout ist kein Testfall

jsdom kennt keine Geometrie: Position, Überlappung, Verdeckung und
Ausrichtung sind in Unit-Tests **nicht** sichtbar. Solche Fehler sind der
teuersten Fund: Die Tests sind grün, das Feature ist unbedienbar.

Zu jedem Feature mit UI-Anteil gehört deshalb **eine Sichtprüfung** vor der
Abnahme:

1. Echtes SCSS der Komponente kompilieren
   (`frontend/node_modules/.bin/sass --load-path=frontend/src/styles`).
2. Markup aus dem echten Template in einen kleinen Harness übernehmen.
3. `google-chrome --headless=new --screenshot=…` rendern lassen.
4. Vorher/Nachher vergleichen und die Sichtprüfung im PR-Protokoll benennen.

Typische Befunde, die **nur** die Sichtprüfung findet: Overlay am falschen
Element, Aktion absolut positioniert über dem Text daneben, Vorschlagsliste
als Overlay über dem Nachbarinhalt, Element außerhalb des Viewports.

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
- Anordnung: <was steht nebeneinander, was ist rechtsbündig, was verdeckt nichts>
```

Der Entwurf endet mit dem Satz **„Sichtprüfung: <was wird angesehen und was
dabei erwartet wird>"** — das ist der Schritt, den kein Unit-Test ersetzen
kann.

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
