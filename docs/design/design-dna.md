# Design-DNA HouseholdManager

Die visuelle Sprache des Produkts. **Quelle der Wahrheit** sind die Tokens in
[`frontend/src/styles/_design-tokens.scss`](../frontend/src/styles/_design-tokens.scss)
— dieses Dokument erklärt sie. Wer eine Komponente baut, benutzt die Tokens
und keine eigenen Werte.

## Charakter in einem Satz

**Modern, Dark Mode, mit Terminal-Anmutung** — wie ein gut gebautes CLI,
nicht wie eine Retro-Hacker-Demo.

## Prinzipien

| # | Prinzip | Konsequenz |
|---|---------|------------|
| 1 | **Dark Mode ist Standard.** Die App ist ein Arbeitswerkzeug, kein Erlebnis-Portal. | `--hm-bg` ist der Standard; helle Variante nur als Ausnahme (`data-theme="light"`) |
| 2 | **Monospace trägt Daten, Sans trägt Text.** | IDs, Zeitstempel, Beträge, Zahlen in Tabellen → `--hm-font-mono`; Fließtext, Labels → `--hm-font-sans` |
| 3 | **Scharfe Kanten statt Rundungen.** | Radius 4px (Standard), 2px für Chips; keine Pill-Buttons, keine 16px-Radien |
| 4 | **Linien statt Schatten.** | 1px `--hm-border` trennt, Schatten nur für echte Ebenen (Overlay, Toast) |
| 5 | **Akzentfarbe trägt Bedeutung.** | Max. 2 Akzente je Ansicht; Akzent = Aktion/Bestätigung, nie Dekoration |
| 6 | **Kein Effekt ohne Aussage.** | Glow nur bei Fokus/Aktiv, Grid nur im Hintergrund, Scanlines nur als Textur mit ≤ 2 % Deckkraft |
| 7 | **Terminal-Referenz statt Zitat.** | Prompt-Marker (`$`), Statuszeilen und Monospace-Labels ja — aber keine ASCII-Art, keine Fake-Terminal-Frames |

## Farben

| Rolle | Token | Hex | Wofür |
|-------|-------|-----|-------|
| Hintergrund | `--hm-bg` | `#0b0f14` | App-Fläche |
| Fläche | `--hm-surface` | `#111820` | Karten, Panels |
| Fläche erhöht | `--hm-surface-2` / `-3` | `#17202b` / `#1e2937` | Inputs, Tabellenkopf, Hover |
| Linie | `--hm-border` | `#22303f` | Trennlinien, Rahmen |
| Text | `--hm-text` | `#e6edf3` | Primärtext |
| Text gedämpft | `--hm-text-muted` | `#8b98a5` | Labels, Sekundärtext |
| Akzent primär | `--hm-accent` | `#00e5a0` | Primäraktion, Bestätigung, Fokus |
| Akzent sekundär | `--hm-accent-2` | `#22d3ee` | Information, Links |
| Akzent tertiär | `--hm-accent-3` | `#e879f9` | Besitz/Mitgliedschaft, Experimentelles |
| Erfolg / Warnung / Fehler | `--hm-success` / `--hm-warning` / `--hm-danger` | `#3fb950` / `#ffb000` / `#ff5c5c` | Status |

**Regeln:** Eine Fehlermeldung ist rot **und** enthält Text — nie Farbe
allein. Der Fokus ist sichtbar (Ring `--hm-focus-ring`), auch im Dark Mode.
Kontraste: Fließtext ≥ 4.5:1, große Schrift und Linien ≥ 3:1.

**Label-Farben (`--hm-label-1` … `--hm-label-8`):** eigenständige Klasse,
**keine Akzente** — sie tragen Identität, nie Aktion, und zählen deshalb
nicht zur Zwei-Akzente-Grenze. Zuordnung deterministisch über
`Hash(normalisierter Name) mod 8`, damit derselbe Name boardweit dieselbe
Farbe erhält. Sie werden nur als Rand und Text von Label-Chips benutzt
(`color-mix(... 45 %, transparent)` als Rand), nie als Fläche einer Karte.

## Typografie

| Rolle | Größe | Font |
|-------|-------|------|
| Seitentitel | `--hm-text-xl` (20px) | Sans, `--hm-line-tight` |
| Abschnittstitel | `--hm-text-2xl` (24px) | Sans |
| Eingabe / Tabellenkopf | `--hm-text-lg` (16px) | Sans |
| Fließtext, Standard | `--hm-text-md` (14px) | Sans |
| Sekundärtext | `--hm-text-sm` (13px) | Sans, `--hm-text-muted` |
| Labels, Kennzahlen | `--hm-text-xs` (12px) | **Mono**, oft `--hm-text-muted`, oft uppercase mit `letter-spacing: 0.06em` |
| IDs, Zeitstempel, Beträge | `--hm-text-sm` | **Mono** (`font-variant-numeric: tabular-nums`) |

## Maße

| Konzept | Wert |
|---------|------|
| Raster | 4px-Basis (`--hm-space-1` … `--hm-space-8`) |
| Radius | 4px Standard, 2px Chips, 6px Overlays |
| Kontrollhöhe | 36px (`--hm-control-height`) |
| Rahmen | 1px |
| Übergang | 120ms (fast) / 180ms (`--hm-duration`) |

## Komponenten

| Komponente | Merkmale |
|------------|----------|
| **Seitentitel** (`page-header`) | Titel 20px Sans, Untertitel 13px `--hm-text-muted`; 1px-Linie unten, kein Schatten |
| **Card** | `--hm-surface`, 1px `--hm-border`, Radius 4px, Innenabstand `--hm-space-4` |
| **Button primär** | Fläche `--hm-accent`, Text `--hm-text-inverse`, Radius 4px, kein Verlauf; Hover hellert auf (nicht größer) |
| **Button sekundär** | transparent, 1px `--hm-border-strong`, Text `--hm-text`; Hover `--hm-surface-2` |
| **Input** | `--hm-surface-2`, 1px `--hm-border`, Radius 4px; Fokus: `--hm-border-strong` + `--hm-focus-ring` |
| **Tabelle** | Kopfzeile `--hm-surface-2`, 12px Mono uppercase; Zellenhöhe 40px; Zahlen rechtsbündig, tabellarisch |
| **Badge/Status** | 2px Radius, 12px Mono uppercase; Fläche `--hm-accent-soft` + Text `--hm-accent` (Aktion/Bestätigung), `--hm-warning-soft` + `--hm-warning` (Statuskennzeichnung an Karten, z. B. „überfällig"), `--hm-accent-soft` + `--hm-accent-3` (Mitgliedschaft/Leitung). Nie Fläche allein: der Text steht im Badge |
| **Workspace-Wähler** (App-Kopf) | Auslöser im App-Kopf: transparent, 1px `--hm-border-strong`, Radius `--hm-radius`, Höhe 32px, Name der aktuellen Auswahl 14px Sans + Marker `▾` in `--hm-text-muted`; Liste: `--hm-surface`, 1px `--hm-border`, Radius `--hm-radius`, `--hm-shadow`, `--hm-z-dropdown`, Zeilenhöhe 40px, aktuelle Zeile `--hm-surface-2` + Mono-Label „AKTUELL" (kein Akzent). Auswahl ohne Neuladen der Seite |
| **Terminal-Block** | Monospace, `--hm-surface`, links 2px `--hm-accent`, Zeilen mit `$`-Präfix für Kommandos — nur für echte Kommandos/Ausgaben |
| **Leerzustand** | Zentriert, Icon in `--hm-text-faint`, eine Zeile Klartext + eine Zeile Aktion |
| **Fehlerzustand** | `--hm-danger-soft` als Fläche, `--hm-danger` als Linie links, Text + optional Aktion |
| **Toast** | `--hm-surface-2`, 1px `--hm-border`, Radius 4px, `--hm-shadow`, 4s Standzeit |
| **Dialog (Overlay)** | Backdrop `--hm-overlay`; Panel `--hm-surface`, 1px `--hm-border`, Radius `--hm-radius-lg`, `--hm-shadow-lg`, Ebene `--hm-z-overlay`; Innenabstand `--hm-space-6`; Titel `--hm-text-xl`, actions rechtsbündig: Primäraktion + „Abbrechen"; Fokusfalle, initialer Fokus auf dem Panel, Fokus zurück auf das auslösende Element, `Esc` schließt |
| **Bestätigungsdialog** | wie Dialog, Text nennt **Zahl und Objekt** („N Aufgaben in „DONE" werden endgültig gelöscht."); destruktive Primäraktion: transparent, 1px `--hm-danger`, Text `--hm-danger`, Hover `--hm-danger-soft`; daneben „Abbrechen" (sekundär) |
| **Board-Spalte** | Bahn ohne eigene Fläche (nutzt `--hm-bg`), Spalten nur durch 1px `--hm-border` getrennt; Kopf: 12px Mono uppercase `--hm-text-muted` (Name) + Anzahl rechts in Mono, 1px-Linie unten; Spalte ist immer sichtbar, Mindestbreite 0 |
| **Kanban-Karte** | `--hm-surface`, 1px `--hm-border`, Radius `--hm-radius`, Innenabstand `--hm-space-3`; Titel 14px Sans; Meta-Zeilen 12–13px Mono/Sans `--hm-text-muted`; Karten **nicht** klickbar (Drag-Fläche), Aktionen als explizite Buttons; Status wird **nicht** über Farbe angezeigt, DONE-Titel in `--hm-text-muted` |
| **Label-Chip** | Radius `--hm-radius-sm`, 12px Mono, **nicht** uppercase (Namen sind Nutzereingaben), Text `var(--hm-label-n)`, Rand 1px `color-mix(… 45 %)`, keine Füllfläche; Entfernen als `×`-Button am Chip |
| **Zeichenlänge (Eingabe)** | 12px Mono (`--hm-text-xs`) rechtsbündig unter dem Eingabefeld, Zählform „128 / 500"; ab 90 % der Grenze in `--hm-warning`, über der Grenze in `--hm-danger`; die Grenze steht zusätzlich in der Fehlermeldung am Feld |
| **Drag-Zustände** | Vorschau: Karte mit 1px `--hm-border-strong` + `--hm-shadow` (echte Ebene), Original Deckkraft 0.35; Einrück-Platzhalter: 1px gestrichelt `--hm-border-strong`, Radius `--hm-radius`; Ziel-Bahn: 1px `--hm-accent-line`; Speichern läuft: Mono-Zeile „Wird gespeichert …" `--hm-text-muted`, Karte nicht erneut greifbar; Übergänge ≤ `--hm-duration` |

## Zustände (Pflicht für jedes interaktive Element)

```text
default → hover → focus-visible → active → disabled
                 ↘ loading (Spinner + Text, Button deaktiviert)
                 ↘ error (Meldung mit Rolle "alert")
```

- **Loading:** nie nur ein Spinner — daneben steht, was passiert („Wird geladen …").
- **Disabled:** `--hm-text-faint`, kein Hover-Effekt.
- **Fehler:** Text + Grund + nächste mögliche Handlung.

## Dark Mode

- Dark Mode ist der Standard; die App startet dunkel, ohne Systemeinstellung
  zu beachten.
- Eine helle Variante existiert als `data-theme="light"` (Tokens in
  `_design-tokens.scss`) — sie wird **nicht** automatisch aktiviert.
- Kein Weiß auf Schwarz invertieren: die hellen Tokens sind bewusst
  gesetzt (Kontraste, Flächen).

## Anti-Patterns

| Nicht so | Sondern |
|----------|---------|
| Verläufe, Glitzer, Glassmorphism | flache Flächen + 1px-Linie |
| Große weiche Schatten | Linien; Schatten nur für Overlays |
| Runde Pill-Buttons | Radius 4px |
| Emoji als Icons | Icon-Set oder Textlabel |
| Farbe als einzige Information | Farbe **plus** Text/Symbol |
| Animation als Dekoration | Bewegung nur bei Zustandswechsel, ≤ 180ms |
| Hero-Sektion im Verwaltungswerkzeug | Seitenkopf + Inhalt, sofort nutzbar |
| Jede Fläche mit Akzentfarbe | Akzent = Bedeutung |
