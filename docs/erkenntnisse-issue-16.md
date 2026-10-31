# Erkenntnisse — Issue #16 „Der Abruf einer ICS-Adresse soll nicht auf interne Ziele zeigen können“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #16 (Branch
`feature/16-ics-interne-ziele`). Zweck: die Sicherheitslücke festhalten, die
behobene Regel begründen und die Lehre in den Skill überführen.

---

## 1. Behobenes Problem

### 1.1 Die Quelle zeigte auf das Netz des Servers

- **Symptom:** Die ICS-Adresse des Entsorgers wurde ungeprüft abgerufen. Wer
  sie setzen durfte (jedes Mitglied, AC23), konnte den Server auf beliebige
  Ziele im Netz des Betreibers ansetzen — bis zur Metadaten-Schnittstelle der
  Cloud (169.254.169.254) oder auf das interne Netz hinter dem Reverse Proxy.
- **Ursache:** Die fachliche Prüfung der Adresse (`WasteCalendar.validatedSourceUrl`)
  prüfte Schema und Länge, aber nicht das Ziel. Der Abruf selbst prüfte nichts:
  die Adresse kam ungeprüft vom Mitglied.
- **Nachbesserung in zwei Schichten:**
  1. **Fachkern (`WasteCalendar.validatedSourceUrl`):** eine Adresse mit einem
     internen Literal (`127.0.0.1`, `10.x`, `172.16–31.x`, `192.168.x`,
     `169.254.x`, `::1`, `fc00::/7`, `fe80::/10`, `::ffff:…`) wird beim
     Speichern abgelehnt — mit der Meldung „Die ICS-Adresse darf nicht auf ein
     internes Ziel zeigen." (400). Das Mitglied erfährt es sofort.
  2. **Ausgehender Adapter (`IcsWasteCalendarFeed`):** vor dem Abruf wird der
     Host **aufgelöst** und die aufgelöste Adresse geprüft. Ein Hostname, der
     auf eine interne Adresse zeigt, wird nicht abgerufen — der Lauf schlägt
     mit „Die ICS-Adresse zeigt auf ein internes Ziel und wird nicht
     abgerufen: …" fehl und der Stand wird veraltet.
- **Lehre:** Eine Prüfung des eingetippten Textes ist keine Prüfung des
  Ziels. Wer eine Adresse prüft, muss sie **auflösen** und die Adresse prüfen,
  die tatsächlich erreicht wird.

### 1.2 Der Test muss nicht ins Netz

- **Problem:** Die Auflösung des Hostnamens im Adapter wäre im Test eine
  echte DNS-Abfrage — der Test wäre langsam und von der Umgebung abhängig.
- **Nachbesserung:** Der Adapter bekommt die Auflösung als kleine
  Schnittstelle (`HostResolver`) übergeben; im Test steht eine bekannte
  öffentliche Adresse statt einer echten Auflösung.
- **Lehre:** Jede Technik, die der Adapter für eine *Umgebungsabhängigkeit*
  braucht, gehört hinter eine kleine Schnittstelle — sonst wird der Test
  unzuverlässig. (Gleiches Muster wie der `RestClient` aus dem Composition
  Root.)

### 1.3 Der Grund landet beim Mitglied

- **Problem:** Die Oberfläche zeigte bei jedem Fehler einen generischen Satz
  („Der Dienst ist nicht erreichbar.") — auch dann, wenn das Backend die
  Adresse fachlich abgelehnt hatte.
- **Nachbesserung:** `HouseholdsPage.sourceErrorMessage` liest das Detail des
  ProblemDetail und zeigt den Grund des Backends.
- **Lehre:** Eine Fehlermeldung, die den Grund nicht nennt, ist eine halbe
  Fehlermeldung. Das Backend hat den Grund — er muss durch.

## 2. Bewusst offene Grenzen

- **Prüfung und Abruf sind nicht atomar:** zwischen der Auflösung (Prüfung)
  und dem Verbindungsaufbau kann eine andere Adresse stehen (DNS-Wechsel,
  „DNS rebinding"). Der Schutz senkt das Risiko, beseitigt es nicht. Wer es
  ganz ausschließen will, muss die Auflösung in den HTTP-Client hängen.
- **Ein nicht konfiguriertes Verzeichnis** (ohne Nextcloud) liefert weiterhin
  eine leere Trefferliste — das ist der Zustand eines frischen Setups, kein
  Fehlerfall (siehe Issue #21).

## 3. Überführte Regeln

| Lehre | Regel |
|---|---|
| Ausgehende Abrufe prüfen ihr Ziel | `backend-ports-adapters` — eine ausgehende HTTP-Quelle prüft vor dem Abruf die **aufgelöste** Adresse; interne Ziele (Loopback, private Netze, Link-Local, unique local) werden nicht abgerufen |
| Adresse = Fachregel im Kern | `backend-ports-adapters` — die fachliche Grenze einer Adresse (was darf rein) liegt im Fachkern; die Auflösung (was wird erreicht) liegt im Adapter |
