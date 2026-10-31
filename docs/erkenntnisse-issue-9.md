# Erkenntnisse — Issue #9 „Fokusverhalten der Overlays (initialer Fokus + Fokusfalle)“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #9 (Branch
`feature/9-fokus-overlays`). Zweck: die Lücke zwischen Muster und Umsetzung
schließen und die Lehre überführen.

---

## 1. Behobenes Problem

### 1.1 Der Fokus wanderte aus dem Dialog

- **Symptom:** Beide Overlays (`confirm-dialog`, `task-form`) waren `position:
  fixed` am Ende des Seiteninhalts — ohne Fokusbehandlung wanderte der
  Tastaturfokus beim Öffnen im Dokument weiter, auf Elemente, die der Nutzer
  gar nicht sieht. `Esc` schloss, die Tastaturbedienung war aber unbrauchbar.
- **Ursache:** Das Muster verlangte Fokus auf dem Panel und eine Fokusfalle —
  umgesetzt war nichts (`grep` nach `autofocus|cdkTrapFocus|cdkFocusInitial|
  .focus(|tabindex` ergab null Treffer).
- **Nachbesserung (beide Overlays gleich):**
  - Die **Fokusfalle** liegt auf dem Wrapper (`cdkTrapFocus` mit
    `cdkTrapFocusAutoCapture="true"`): Tab und Shift+Tab bleiben im Dialog.
  - Das **Panel** trägt `cdkFocusInitial` und `tabindex="-1"` → es bekommt
    den initialen Fokus (nicht die destruktive Aktion!).
  - Beim **Schließen** kehrt der Fokus auf das auslösende Element zurück (der
    CDK holt ihn beim Abbau der Falle zurück).
  - `Esc` schließt weiterhin — die Falle blockiert es nicht.
  - Der Backdrop-Knopf steht nicht mehr in der Tab-Reihenfolge
    (`tabindex="-1"`): er ist eine Klick-Aktion, keine Tastaturaktion.
- **Lehre:** „Der Dialog schließt mit `Esc`" ist nicht „der Dialog ist
  bedienbar". Ein Overlay ohne Fokusbehandlung sieht bedienbar aus und ist es
  nicht.

### 1.2 Die Falle liegt auf dem Wrapper, nicht auf dem Panel

- **Beobachtung:** Der CDK sucht das Element mit `cdkFocusInitial` **innerhalb**
  der gefangenen Region. Liegen Falle und Panel auf demselben Element, findet
  die Falle ihr eigenes Ziel nicht — der Fokus landet auf dem ersten
  tabbbaren Kind (bei `confirm-dialog` auf „Endgültig löschen").
- **Nachbesserung:** Falle auf den Wrapper, `cdkFocusInitial` auf das Panel.
- **Lehre:** Die Falle definiert die Region, das Panel ist nur ihr Inhalt.

### 1.3 jsdom hat keine Geometrie — und der CDK hält unsichtbare Elemente für unfokussierbar

- **Problem:** Der CDK prüft vor dem Fokussieren, ob das Element sichtbar ist
  (`offsetWidth`/`getClientRects`). In jsdom ist jedes Element 0 px groß →
  „unsichtbar" → die Falle fokussiert nichts, der Test schlägt fehl, obwohl
  der Browser es richtig macht.
- **Nachbesserung:** `shared/testing/jsdom-focus-geometry.ts` gibt den
  Elementen im Test eine Größe. Das ist eine Einrichtung der Testumgebung,
  kein Produktivverhalten.
- **Lehre:** Wenn eine Bibliothek im Test grundsätzlich nichts tut, ist die
  erste Frage nicht „ist der Code falsch?", sondern „was prüft die Bibliothek
  und was kann die Testumgebung nicht?" — und dann: die Umgebung so
  einrichten, dass die Aussage des Tests der des Browsers entspricht.

## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| Ein Overlay führt den Fokus | `ux-design` §3a — das Muster ist jetzt vollständig beschrieben: Falle auf dem Wrapper, `cdkFocusInitial` + `tabindex="-1"` auf dem Panel, Fokus zurück beim Schließen, Backdrop nicht in der Tab-Reihenfolge |
| Ein Muster muss erfüllt sein, nicht behauptet | `ux-design` — die frühere Abweichung („noch nicht umgesetzt") ist behoben; die Design-DNA beschreibt den Zustand |
| Testumgebungen einrichten, wo sie blind sind | `feature-testing` — jsdom kennt keine Geometrie; ein Test, der die Fokusfalle belegen soll, braucht die Geometrie (siehe `shared/testing/jsdom-focus-geometry.ts`) |

## 3. Geprüfte Gates

- Architektur-Review: siehe Kommentar am Issue #9.
- Abnahme: siehe Kommentar am Issue #9.
- Belege: Frontend 28 Dateien / 128 Tests grün, Lint und Sheriff ohne
  Beanstandung, Production-Build erfolgreich.
