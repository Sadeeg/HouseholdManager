# Erkenntnisse — Issue #21 „Ausfall des Nutzerverzeichnisses als Fehler melden statt als leere Liste“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #21 (Branch
`feature/21-verzeichnis-ausfall`). Zweck: den MINOR-Befund aus der Abnahme zu
Issue #18 festhalten und die Lehre als Regel überführen.

---

## 1. Behobenes Problem

### 1.1 Ein Ausfall des Verzeichnisses sah aus wie „niemand gefunden“

- **Symptom:** Ist Nextcloud nicht erreichbar, antwortet `api/users/search`
  mit HTTP 200 und einer leeren Liste. Das Suchfeld meldet „Keine Person
  gefunden." — der Nutzer sucht weiter, obwohl niemand gefragt werden konnte.
- **Ursache:** `NextcloudUserDirectory.search` fing `RuntimeException` und
  lieferte `List.of()` — derselbe Rückgabewert wie bei einer Suche ohne
  Treffer. Der Unterschied zwischen „keine Treffer" und „keine Antwort" ging
  im Adapter verloren.
- **Nachbesserung:** Der Adapter wirft jetzt `UserDirectoryUnavailableException`
  (Fachfehler im Modell, nach dem Muster von `WasteFeedUnavailableException`),
  der REST-Adapter übersetzt sie in **503** mit dem Titel „Nutzerverzeichnis
  nicht erreichbar". Die Meldung nennt den konkreten Grund („Nextcloud
  antwortete mit HTTP 503." bzw. „Die Verbindung ist fehlgeschlagen.") — ohne
  interne Adresse und ohne Stacktrace.
- **Lehre:** Ein Adapter darf einen technischen Ausfall nicht in ein
  fachlich gültiges, aber **falsches** Ergebnis umwandeln. „Leere Liste" ist
  eine Aussage über die Welt („es gibt niemanden") — sie darf nicht auch
  „ich konnte niemanden fragen" bedeuten.

### 1.2 `findById` bleibt bewusst tolerant

- **Befund:** `findById` fängt Fehler ebenfalls und liefert `Optional.empty()`.
  Das ist an anderer Stelle gewollt: eine nicht ladbare Person führt zur
  Meldung „Die Zuständige ist im Nutzerverzeichnis unbekannt." (400), nicht
  zu einem Verzeichnisfehler.
- **Entscheidung:** Für Issue #21 bleibt `findById` unverändert; geändert
  wurde nur der Suchpfad. Der Unterschied ist im Code kommentiert.
## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| Technischer Ausfall ≠ fachlich leeres Ergebnis | `backend-ports-adapters` Regel 9 (ergänzt um den Ausfall einer externen Quelle: Fachfehler im ausgehenden Adapter, eigener Status im eingehenden) |

## 3. Abhängigkeit zu Issue #18

Die sichtbare Meldung „Nutzerverzeichnis nicht erreichbar." im Suchfeld
setzt Issue #18 voraus: dort wird der Fehlerzustand der Suche abgefangen,
damit die Komponente nicht während des Renderns abbricht. #21 liefert den
Fehlerstatus, #18 macht ihn sichtbar — beide zusammen ergeben das gewünschte
Bild.
