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
  (`reload()`) — der Fehlerzustand wird dabei zurückgesetzt, die Meldung
  verschwindet, während der Versuch läuft. Der Wähler zeigt im Fehlerfall
  einen Knopf „Erneut versuchen"; er steht an der Meldung, ist während des
  Versuchs gesperrt und löst genau einen Request aus.
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
| `httpResource` feuert bei URL-Änderung | `AGENTS.md` (Angular-Fallen) — für einen erneuten Versuch mit derselben URL `reload()` benutzen, nicht auf ein Signal warten |
