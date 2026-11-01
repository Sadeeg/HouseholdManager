# Erkenntnisse — Issue #15 „Spec-Dateien je Produktionsklasse bündeln“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #15 (Branch
`feature/15-spec-bündeln`). Zweck: die verteilten Testdateien des
Waste-Features zusammenführen, ohne einen Fall zu verlieren.

---

## 1. Behobenes Problem

### 1.1 Sechs Dateien für eine Seite, vier für einen Abschnitt

- **Symptom:** Für die Seite „Mülltermine" lagen sechs Spec-Dateien nebeneinander
  (`waste-page.spec.ts`, `-no-source`, `-stale`, `-order`, `-reload`,
  `-reload-production`), für den Abschnitt „Müllkalender" drei weitere. Jede
  Datei richtete ihr TestBed selbst ein, der `tick()`-Helfer war mehrfach
  kopiert.
- **Ursache:** Jede Datei entstand bei einem Befund aus der Abnahme zu
  Issue #10 — die Fälle sind nicht überflüssig, sie waren nur an der
  falschen Stelle verteilt.
- **Nachbesserung:** Je Produktionsklasse genau eine Spec-Datei
  (`waste-page.spec.ts`, `waste-calendar-section.spec.ts`) mit **einer**
  TestBed-Einrichtung und **einem** Helfer-Set. Die Fälle behalten ihre Namen
  und ihre Aussage; die Zahl der Tests bleibt gleich (121 im Frontend).
- **Lehre:** Ein Befund aus einer Abnahme ist ein Testfall — aber nicht
  zwangsläufig eine Datei. Die Datei entsteht aus dem **Testgegenstand**, nicht
  aus dem Befund.

### 1.2 Ein Test prüfte eine Konfiguration, die es im Betrieb nicht gibt

- **Symptom:** `waste-page-reload.spec.ts` prüfte den erneuten Lesezugriff
  mit einem Router **ohne** `withAutoCleanupInjectors()` — eine Konfiguration,
  die die Anwendung nicht verwendet (`app.config.ts` setzt sie). Der Test
  hielt einen Zustand fest, den es im Betrieb nicht gibt.
- **Nachbesserung:** Der Fall ist als benannter Fall in der Sammeldatei
  erhalten — jetzt aber mit der Konfiguration der Anwendung. Der
  Fehlerfall, den der alte Test beschrieb (ohne Aufräumregel überlebt die
  Resource den Wegwechsel), ist als Begründung im Kommentar festgehalten.
- **Lehre:** Ein Test, der eine Konfiguration prüft, die es nicht gibt, ist
  eine Locke: Er grünt, solange die Konfiguration falsch ist, und wird rot,
  wenn die Anwendung richtig konfiguriert ist. Tests prüfen den Betrieb, nicht
  eine Vermutung über ihn.

### 1.3 Was beim Zusammenführen auffällt

- Die Zahl der Tests bleibt gleich — **kein** Fall geht verloren (AC-t4).
- Zwei Tests der alten Dateien prüften denselben Sachverhalt an
  verschiedenen Stellen (der Reload in der Produktions- und in der
  Nicht-Produktionsvariante). Beide bleiben erhalten, aber mit derselben
  Router-Konfiguration: einer zählt die GETs, der andere prüft, dass die
  Termine nach dem erneuten Betreten wieder dastehen.

## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| Eine Spec-Datei je Produktionsklasse | `feature-testing` — die Testdatei gehört zur Produktionsklasse; neue Fälle kommen in dieselbe Datei, nicht in eine neue daneben |
| Tests prüfen den Betrieb | `feature-testing` — ein Test prüft die Konfiguration der Anwendung, nicht eine Variante davon |
