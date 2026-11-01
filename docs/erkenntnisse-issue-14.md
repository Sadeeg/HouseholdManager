# Erkenntnisse — Issue #14 „App-Kopf läuft bei schmalen Viewports seitlich aus“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #14 (Branch
`feature/14-kopf-umbruch`). Zweck: die Ursache der Überbreite festhalten und
die Lehre als Regel überführen.

---

## 1. Behobenes Problem

### 1.1 Der Kopf war eine Zeile ohne Umbruchstrategie

- **Symptom:** Auf einem 375 px breiten Viewport war das Dokument
  **603 px** breit → waagerechtes Scrollen auf **jeder** Seite der
  Anwendung. Der Kopf allein (`.app-nav` bis x=480, `app-workspace-switcher`
  bis x=603) war der Verursacher; die Feature-Seiten hielten sich an die
  Vorgabe.
- **Ursache:** `.app-header` war eine Flex-Zeile ohne Umbruch. Die
  Navigationslinks brechen nicht um (`white-space: nowrap` auf den Links), und
  der Wähler bekam seine Breite vom Inhalt — der Name des Haushalts war
  unbegrenzt lang.
- **Nachbesserung:**
  - Unter 768 px bricht der Kopf um: die Navigation kommt in eine eigene
    Zeile (`order: 3`, `flex-basis: 100%`); der Wähler steht **über** der
    Navigation in einer eigenen Zeile (bei 375 px: Marke y=16–36, Wähler
    y=44–76, Navigation y=84–111).
  - Der Wähler wird auf die verfügbare Breite begrenzt; der Name wird mit
    Ellipse gekürzt statt die Zeile zu sprengen.
- **Lehre:** `flex-wrap` allein reicht nicht. Eine Zeile braucht eine
  **Umbruchstrategie** — und wer Inhalt anzeigt, dessen Länge von Daten
  abhängt (ein Name!), muss dessen Breite begrenzen.

### 1.2 Gemessen wird am laufenden Stack

- **Vorgehen:** Der Kopf wurde mit Playwright (Chrome) am laufenden
  Dev-Server gemessen, nicht geraten: Dokumentbreite, Rechte der
  Navigationslinks, des Wählers und die Sichtbarkeit der Links.
- **Vorher / Nachher bei 375 px:**

  | | vorher | nachher |
  |---|---|---|
  | Dokumentbreite | 603 px | **375 px** |
  | Rechte der Navigationslinks | 479 px | 351 px |
  | Rechter Rand des Wählers | 603 px | 351 px |
  | vollständig sichtbare Navigationspunkte | 1 von 3 | **3 von 3** |

  (Das Issue nennt 608 px — die eigene Messung am Dev-Server ergibt 603 px;
  die Navigation endet in beiden Fällen bei x=480.)

- **AC-k3 (Desktop unverändert):** bei 1280 px sind Kopf, Marke, Navigation
  und Wähler **pixelgleich** zur Position vor dem Umbau (Titel x=24,
  Navigation x=225, Wähler x=922, Kopfhöhe 65 px) — die Umbruchregel greift
  nur unter 768 px.
- **Breakpoint-Reihe (Abnahme):** die Breiten um den Bruchpunkt herum wurden
  einzeln gemessen (320, 360, 374, 375, 376, 400, 600, 767, 768, 769, 800,
  1024, 1280 px) — Dokumentbreite immer gleich dem Viewport, alle drei
  Navigationspunkte sichtbar.

  | Viewport | vorher (Dokumentbreite) | nachher |
  |---|---|---|
  | 320 px | überbreit | 320 px |
  | 375 px | 603 px | **375 px** |
  | 767 px | überbreit | 767 px |
  | **768 px** | 768 px (kein Fehler) | 768 px |
  | 769 px / 800 px | **838 px — Regression** | 769 px / 800 px |

  Die Regression bei 768–800 px kam **nicht** aus der Umbruchregel, sondern aus
  dem Wähler: `app-workspace-switcher` ist Flex-Item und hat als `:host` den
  automatischen Mindestinhalt (`min-width: auto` = min-content des unumbrochenen
  Namens, 293 px). Das überschreibt die `max-width`-Schranke und ließ den Kopf
  ab 768 px überlaufen — obwohl die Umbruchregel dort gar nicht greift. Der Fix
  ist `min-width: 0` am `:host` (dann greift `max-width: 100%`).

- **AC-k4 (Fokusring):** der Wähler behält seinen Fokusring (`outline: solid
  2px`, Akzentfarbe); es wurde kein `outline` entfernt.
- **Belege als Bild:** `docs/images/issue-14-header-375px.png` (375 px, Kopf
  umgebrochen) und `docs/images/issue-14-header-1280px.png` (1280 px, Kopf wie
  vorher) — aufgenommen mit Chrome gegen den Dev-Server des Branches, die API
  gemockt (ein Haushalt mit langem Namen).
- **Lehre:** Geometrie ist im Test nicht prüfbar — der Beleg ist eine
  Messung am laufenden Stack, mit dem Wert **vorher** als Vergleich. Ohne den
  Ausgangswert sagt „375 px sind 375 px" nichts.
- **Lehre (aus der Abnahme):** eine **einzelne** Messung belegt nur diese
  Breite. Die Regression bei 768 px lag genau neben dem Messpunkt — die
  Breakpoint-Reihe (vorher **und** nachher) deckt sie auf. Und: eine
  Breitenbegrenzung (`max-width`) ist bei Flex-Items wirkungslos, solange der
  automatische Mindestinhalt (`min-width: auto`) größer ist.

## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| Eine Zeile braucht eine Umbruchstrategie | `frontend-feature-architecture` (Fallen) — der Kopf bricht unter 768 px um, datenabhängige Anzeigetexten werden gekürzt; die Messung (375 px / 1280 px) gehört zur Abnahme |
| Layout ist nicht testbar | `frontend-feature-architecture` — Geometrie braucht die Sichtprüfung, keinen Unit-Test |
| Eine Messung belegt eine Breite | `frontend-feature-architecture` (Fallen) — die Sichtprüfung misst eine **Reihe** um den Bruchpunkt (320…1280 px) und vergleicht mit dem Stand vor dem Issue; eine Regression, die nur **neben** dem Messpunkt liegt, sieht man sonst nicht |
| `max-width` wirkt bei Flex-Items erst mit `min-width: 0` | `frontend-feature-architecture` (Fallen) — als Flex-Item gilt `min-width: auto` = min-content; ein unumbrochener Text (Name!) sprengt die Zeile trotz `max-width`. Wer Inhalt anzeigt, dessen Länge von Daten abhängt, setzt `min-width: 0` **und** `max-width` |
