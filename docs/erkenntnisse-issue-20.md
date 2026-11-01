# Erkenntnisse — Issue #20 „Suche nach einem Fehler mit demselben Begriff erneut auslösen“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #20 (Branch
`feature/20-retry-personensuche`, gestapelt auf #18). Zweck: die Grenze von
`httpResource` festhalten und die Lehre überführen.

---

## 1. Behobenes Problem

### 1.1 Der naheliegendste Griff tat nichts

- **Symptom:** Nach einem Fehler im Suchfeld blieb „Nutzerverzeichnis nicht
  erreichbar." stehen — auch wenn der Nutzer denselben Begriff noch einmal
  tippt. Der Fehlerzustand blieb, die Meldung blieb, der Request kam nicht.
- **Ursache:** `httpResource` feuert nur bei **URL-Änderung**. Das Signal des
  Suchbegriffs änderte sich nicht (gleicher Begriff), also gab es keinen
  neuen Request. Der Effekt war im Issue #18 als Charakterisierungstest
  festgehalten („keeps the error state when the same query is set again").
- **Nachbesserung:** `UserSearch.retry()` holt dieselbe URL erneut
  (`reload()`). Der Fehlerzustand wird dabei **nicht** zurückgesetzt — bei
  gleicher URL behält die Resource ihren bisherigen Stream, `error()` liefert
  weiter den alten Fehler. Die Meldung verschwindet, weil `failed()` einen
  laufenden Versuch ausschließt (`!isLoading()`), nicht weil der Fehler
  gelöscht wäre. Der Wähler zeigt im Fehlerfall einen Knopf „Erneut
  versuchen"; er steht an der Meldung, löst genau einen Request aus und ist
  nach einem weiteren Fehlschlag wieder da (der dritte Versuch geht).
- **Lehre:** Ein Zustand, der nur durch eine **Änderung** wieder wegkommt,
  braucht eine zweite Handlung. „Noch einmal tippen" ist eine Handlung —
  aber keine, die der Nutzer wissen kann. Ein sichtbarer Knopf ist
  eindeutig; ein stilles Verhalten nicht.

### 1.2 `reload()` statt eines Zähler-Signals

- **Vorschlag im Issue:** ein Zähler-Signal im Request-Ausdruck, damit die
  URL gleich bleibt, der Request aber neu feuert.
- **Umgesetzt:** `reload()` — die Bibliothek bringt den Vorgang mit. Der
  Zähler wäre eine zweite Wahrheit („wann wird neu geladen?") neben der
  Resource.
- **Lehre:** Bevor man Zustand baut, um eine Bibliothek zu überlisten, prüft
  man, ob sie die Bewegung schon kann.

## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| Ein Zustand braucht einen Ausweg | `frontend-feature-architecture` — ein Fehlerzustand braucht eine **sichtbare** Handlung, ihn zu verlassen (Knopf „Erneut versuchen"), nicht nur eine andere Eingabe |
| `httpResource` feuert bei URL-Änderung | `frontend-feature-architecture` (Fallen) — für einen erneuten Versuch mit derselben URL `reload()` benutzen, nicht auf ein Signal warten; `reload()` setzt den Fehlerzustand nicht zurück, die Meldung blendet man über einen laufenden Versuch aus |

## 3. Geprüfte Gates

- Architektur-Review: siehe Kommentar am Issue #20.
- Abnahme: siehe Kommentar am Issue #20.
- Belege: Frontend 31 Dateien / 152 Tests grün (inkl. der Dienst- und
  Komponentenfälle zum Zwischenzustand, zur Request-Anzahl und zum dritten
  Versuch), Type-Check, Lint und Sheriff ohne Beanstandung, Production-Build
  erfolgreich.

### Manueller Pfad (Browser, Chrome)

Am Dev-Server dieses Branches gespielt (Playwright, `channel: chrome`); die
API antwortet auf HTTP-Ebene gestubt (erster Suchversuch 500, der zweite
200) — die UI ist der reale Stand des Branches.

| Schritt | Beobachtung |
|---|---|
| Suche „car" | Fehlermeldung „Nutzerverzeichnis nicht erreichbar." sichtbar ✓ |
| Knopf | „Erneut versuchen" steht an der Meldung ✓ |
| Klick auf den Knopf | Meldung verschwindet, „Personen werden gesucht …" steht ✓ |
| Antwort | Treffer „Carl Vogel" sichtbar ✓ |
| Requests | genau 2 (der erste Versuch und der Wiederholungsversuch) ✓ |

Beleg: `docs/images/issue-20-retry.png`.

Nebenbefund für den nächsten: die OIDC-Library prüft das Token gegen
`Date.now()` in **Millisekunden** — ein `expires_at` in Sekunden lässt den
Route-Wächter zur Anmeldung umleiten, obwohl ein Token in der Storage steht.
