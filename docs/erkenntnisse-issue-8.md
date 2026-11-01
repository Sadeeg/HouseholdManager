# Erkenntnisse — Issue #8 „Labels über 60 Zeichen werden still verworfen“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #8 (Branch
`feature/8-label-grenze-meldung`). Zweck: die stille Verwerfung festhalten,
die Regel benennen und die Lehre überführen.

---

## 1. Behobenes Problem

### 1.1 Ein zu langes Label verschwand ohne Spur

- **Symptom:** Ein Label mit mehr als 60 Zeichen (die Grenze der Spalte
  `label.name`) wurde beim Speichern einer Aufgabe **stillschweigend**
  verworfen: das Label tauchte nirgends auf, das Formular meldete nichts.
- **Ursache:** `TaskUseCaseImpl.resolveLabels()` übersprang jedes Label, das
  nicht in die Grenze passte — die Grenze wurde angewendet, nicht validiert.
  Der Titel hingegen wurde korrekt geprüft (`Task.MAX_TITLE_LENGTH`,
  `validatedTitle()`), das Label nicht.
- **Nachbesserung:**
  - Backend: `InvalidLabelNameException` (Fachfehler im Modell, Muster wie
    `InvalidTaskTitleException`); die Use Case wirft sie, wenn ein Label zu
    lang ist → HTTP 400 mit dem Titel „Ungültiger Labelname" und dem Grund
    („Ein Label darf höchstens 60 Zeichen lang sein."). Leere Namen und
    Dubletten bleiben — wie bisher — still übersprungen: sie sind keine
    Fehler, sondern doppelte Eingaben.
  - Frontend: `MAX_LABEL_NAME_LENGTH = 60` im Modell, ein `labelError` als
    abgeleiteter Zustand im Formular; `addLabel` blockiert einen zu langen
    Namen und die Meldung steht an der Eingabe.
- **Lehre:** Eine Grenze, die angewendet statt validiert wird, ist ein
  Datenverlust ohne Meldung. „DAS LABEL IST ZU LANG" ist keine
  Normalisierung — der Wert verschwindet sonst ohne jede Spur.

### 1.2 Die Grenze wird nach der Normalisierung gemessen

- **Beobachtung:** Backend (`Label.normalize(raw).length`) und Frontend
  (`normalizeLabel(raw).length`) messen **nach** dem Trimmen und Kleinschreiben
  — beide dieselbe Größe. Ein Name mit 60 Zeichen und Leerzeichen drumherum
  ist also zulässig, weil er gespeichert auch 60 Zeichen hat.
- **Lehre:** Zwei Implementierungen derselben Grenze (hier: die Dublette
  `60` im Frontend) messen dieselbe Größe — sonst meldet das Formular einen
  Fehler, den das Backend nicht sieht (oder umgekehrt).

### 1.3 Kein `maxLength` am Label-Feld

- **Bewusst so:** Am Label-Feld steht **kein** natives `maxlength`. Angular
  spiegelt `maxLength()` als natives Attribut ins Feld — der Browser würde
  einen zu langen Namen beim Paste still abschneiden, die Grenze bliebe
  unsichtbar. Der eigene `validate`-Regler misst die getrimmte Länge und
  meldet sichtbar (AGENTS.md: „Signal Forms").
- **Lehre:** Eine Grenze, die man erklären will, braucht eine eigene
  Regel — keine, die der Browser im Hintergrund anwendet.

## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| Stilles Verwerfen ist ein Fehlerfall | `backend-ports-adapters` Regel 9 („Kein stilles Verwerfen") — eine Grenze wird validiert, nicht angewendet |
| Stilles Verwerfen ist ein Fehlerfall | `feature-testing` — jede Grenze braucht einen Test auf **beiden** Seiten der Grenze (gültig / zu lang) |
| Kein natives `maxlength` bei eigener Grenze | `AGENTS.md` („Signal Forms") — sonst schneidet der Browser still ab |

## 3. Geprüfte Gates

- Architektur-Review: **OK** (keine korrekturpflichtigen Befunde; die
  Dokumentations-Lücke wurde in diesem Schritt nachgezogen).
- Abnahme: siehe Kommentar am Issue #8.
- Belege (Stand nach dem Rebase auf `main`): Backend **209 Tests** grün
  (BUILD SUCCESS), Frontend **25 Dateien / 174 Tests** grün, Type-Check
  (`tsc` app + spec) grün, Lint und Sheriff ohne Beanstandung,
  Production-Build erfolgreich, Images über `docker compose build` gebaut.

### Manueller Pfad (Browser, Chrome)

Am laufenden Stack gespielt (Playwright, `channel: chrome`; die API auf
HTTP-Ebene gestubt — das Backend antwortet auf das Speichern mit 400 und
dem `ProblemDetail`):

| Schritt | Beobachtung |
|---|---|
| 61 Zeichen ins Label-Feld | Meldung „Ein Label darf höchstens 60 Zeichen lang sein." steht an der Label-Eingabe ✓ |
| `aria-describedby` der Eingabe | verweist auf `task-label-error` ✓ |
| Speichern | Ablehnung bleibt sichtbar, die Aufgabe wird nicht angelegt ✓ |
| Titel „Einkaufen gehen" | bleibt in der Eingabe stehen ✓ |
| Label-Eingabe | bleibt mit 61 Zeichen stehen (nicht geleert) ✓ |

Beleg: `docs/images/issue-8-label-limit.png`.
