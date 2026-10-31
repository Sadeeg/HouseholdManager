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
    Zeile (`order: 3`, `flex-basis: 100%`), der Wähler bleibt oben neben der
    Marke.
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
- **AC-k4 (Fokusring):** der Wähler behält seinen Fokusring (`outline: solid
  2px`, Akzentfarbe); es wurde kein `outline` entfernt.
- **Belege als Bild:** `docs/images/issue-14-header-375px.png` (375 px, Kopf
  umgebrochen) und `docs/images/issue-14-header-1280px.png` (1280 px, Kopf wie
  vorher) — aufgenommen mit Chrome gegen den Dev-Server des Branches, die API
  gemockt (ein Haushalt mit langem Namen).
- **Lehre:** Geometrie ist im Test nicht prüfbar — der Beleg ist eine
  Messung am laufenden Stack, mit dem Wert **vorher** als Vergleich. Ohne den
  Ausgangswert sagt „375 px sind 375 px" nichts.

## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| Eine Zeile braucht eine Umbruchstrategie | `frontend-feature-architecture` (Fallen) — der Kopf bricht unter 768 px um, datenabhängige Anzeigetexten werden gekürzt; die Messung (375 px / 1280 px) gehört zur Abnahme |
| Layout ist nicht testbar | `frontend-feature-architecture` — Geometrie braucht die Sichtprüfung, keinen Unit-Test |
