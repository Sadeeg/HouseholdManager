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
- **Nachbesserung:** Die Testumgebung bekommt die Geometrie zentral über das
  Vitest-Setup (`test.options.setupFiles` in `angular.json`,
  `shared/testing/vitest-setup.ts`) — ursprünglich ein Helfer, den jede Fokus-
  Datei selbst aufrief; das ließ dieselbe Komponente in zwei Umgebungen laufen
  (mit und ohne Sichtbarkeit). Das Setup ist eine Einrichtung der Testumgebung,
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
| Testumgebungen einrichten, wo sie blind sind | `feature-testing` — jsdom kennt keine Geometrie; die Umgebung wird **einmal** über das Vitest-Setup eingerichtet (`test.options.setupFiles` in `angular.json`, `shared/testing/vitest-setup.ts`), nicht pro Datei — sonst ist dieselbe Komponente in zwei Umgebungen testbar |

## 3. Geprüfte Gates

- Architektur-Review: siehe Kommentar am Issue #9.
- Abnahme: siehe Kommentar am Issue #9.
- Belege: Frontend 28 Dateien / 129 Tests grün (inkl. des ergänzten
  Esc-Falls am Aufgabenformular), Lint und Sheriff ohne Beanstandung,
  Production-Build erfolgreich.

### Manualer Tastaturpfad (Browser, Chrome)

Der manuelle Pfad wurde am laufenden Dev-Server dieses Branches gespielt
(Playwright, `channel: chrome`). Die UI ist der reale Stand von
`feature/9-fokus-overlays`; die API-Antworten sind auf HTTP-Ebene gestubt,
weil das Backend ein signiertes OIDC-Token verlangt, das hier nicht geprägt
werden kann — die Fokusbehandlung selbst läuft ungefiltert im Browser.

Für **beide** Overlays, jeweils: über Tab zum Auslöser, `Enter` zum Öffnen:

| Schritt | Bestätigungsdialog | Aufgabenformular |
|---|---|---|
| Initialer Fokus | im Panel (`cdkFocusInitial`) ✓ | im Panel (`cdkFocusInitial`) ✓ |
| Tab und Shift+Tab | bleiben im Dialog ✓ | bleiben im Formular ✓ |
| `Esc` | schließt, Fokus zurück auf dem Auslöser („LÖSCHEN“) ✓ | schließt, Fokus zurück auf dem Auslöser („Aufgabe anlegen“) ✓ |

Belege: `docs/images/issue-9-focus-confirm.png`,
`docs/images/issue-9-focus-taskform.png` (Fokusoutline im Bild sichtbar).
Ergebnis steht als Kommentar am Issue #9.

Nebenbefund beim Einrichten des Pfads: die OIDC-Library prüft das Token gegen
`Date.now()` in **Millisekunden** — ein `expires_at` in Sekunden lässt den Guard
zur Anmeldung umleiten, obwohl ein Token in der Storage steht. Wer den Guard im
Browser ohne Identity Provider passieren will, muss `expires_at` in Millisekunden
setzen.
