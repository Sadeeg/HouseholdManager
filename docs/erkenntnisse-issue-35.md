# Erkenntnisse — Issue #35 „Telegram-Bot für Mülltermine mit Freischaltung je Haushalt“

Aufgeschrieben am 01.11.2026, nach Abschluss von Issue #35 (Branch
`feature/35-telegram-bot-muelltermine`). Zweck: die Befunde der unabhängigen
Abnahme festhalten und die Lehren als Regeln überführen.

---

## 1. Behobene Probleme

### 1.1 Die Anwendung startete nicht — zwei `RestClient`-Beans

- **Symptom:** Der Start der Anwendung brach ab: *„expected single matching
  bean but found 2: icsRestClient,telegramRestClient“*. Die Anwendung hat zwei
  HTTP-Clients — einen für die ICS-Quelle, einen für die Telegram-Bot-API —
  und beide sind vom Typ `RestClient`.
- **Ursache:** `IcsWasteCalendarFeed` nahm seinen Client **per Typ** über den
  Konstruktor. Bei zwei gleich typisierten Beans findet Spring keinen
  eindeutigen Kandidaten.
- **Nachbesserung:** Beide Clients tragen jetzt einen Qualifizier, dessen
  Konstante zugleich ihr Bean-Name ist (`IcsClientConfig.ICS_CLIENT`,
  `TelegramConfig.TELEGRAM_CLIENT`). Der Qualifizier steht **an beiden
  Stellen**: an der `@Bean`-Methode und an der Injektionsstelle. Nur an der
  `@Bean`-Methode genügt es nicht — Spring löst die Abhängigkeit am
  Injektionspunkt auf.
- **Lehre:** Eine technische Verdrahtung mit **mehreren Kandidaten eines Typs**
  braucht den Qualifizier an beiden Enden. Der Typ allein ist kein Kandidat.

### 1.2 Die Anwendung startete nicht — zwei Konstruktoren an einem Use Case

- **Symptom:** *„No default constructor found“* beim Start.
- **Ursache:** `NotificationDispatchUseCaseImpl` hatte Lombok über
  `@RequiredArgsConstructor` **und** einen eigenen Konstruktor (mit der
  Zonen-Zeichenkette aus der Konfiguration). Spring wählt einen Konstruktor
  nur, wenn genau einer existiert oder einer mit `@Autowired` markiert ist.
- **Nachbesserung:** Der Use Case hat **einen** Konstruktor; die Zone wird
  darin aus der Zeichenkette in eine `ZoneId` übersetzt. Die Klassenfelder
  werden direkt zugewiesen — Lombok ist hier nicht mehr im Spiel.
- **Lehre:** `@RequiredArgsConstructor` und ein eigener Konstruktor dürfen
  nicht nebeneinander stehen. Wer einen Konfigurationswert braucht, schreibt
  den Konstruktor selbst.

### 1.3 Kein Test bootete je einen Spring-Kontext

- **Ursache:** Beide Startfehler blieben unbemerkt, weil kein Test den
  Kontext startet — die Tests prüfen Use Cases, Adapter und Controller
  einzeln.
- **Nachbesserung:** `ApplicationContextTest` startet die Anwendung
  vollständig (gegen eine echte PostgreSQL-Instanz aus Testcontainers) und
  prüft, dass die beiden HTTP-Clients eindeutig sind und die Use Cases ohne
  Hilfe des Testers baubar sind.
- **Lehre:** Ein Feature, das den **Start** verändert (Verdrahtung, neue
  Beans), braucht einen Kontext-Test. Sonst fällt der Fehler erst im
  Betrieb auf.

### 1.4 AC15 verletzt: der Wiederholungslauf schrieb an getrennte Chats

- **Symptom:** Ein getrennter Chat bekam über den **stündlichen
  Wiederholungslauf** doch noch eine Nachricht. Der 18:00-Uhr-Lauf war
  korrekt abgesichert (dort wird die Chat-Liste neu gelesen), der
  Wiederholungslauf versandte jedoch an die Chat-ID des Vermerks, ohne die
  Bindung zu prüfen.
- **Ursache:** Die Zustellvermerke (`notification_delivery`) leben unabhängig
  von den Chat-Zeilen weiter. Das Trennen löscht nur die Chatzeile.
- **Nachbesserung:** Der Wiederholungslauf prüft vor jedem Versuch die
  Chat-Bindung des Vermerks (`findByHouseholdAndChatId`). Ist der Chat
  getrennt, wird der Vermerk **abgebrochen** (`CANCELLED`) statt versendet.
- **Lehre:** Jeder Weg, über den ein Vermerk zu einem Chat führen kann, muss
  die Bindung prüfen. „Ein Weg ist abgesichert" ist keine Entlastung für die
  anderen Wege.

### 1.5 Protokollfehler wurden wiederholt

- **Symptom:** Ein Protokollfehler (Telegram lehnt die Nachricht fachlich ab)
  wurde wie ein Zustellfehler gespeichert — der Vermerk kam beim nächsten
  stündlichen Lauf wieder auf die Liste.
- **Ursache:** `failed(...)` kennt nur einen Fehlerzustand; der Adapter
  nimmt alle Vermerke im Zustand `FAILED` wieder auf.
- **Nachbesserung:** Das Fachmodell kennt jetzt **fünf** Zustände:
  `PENDING`, `DELIVERED`, `FAILED`, `REJECTED` (fachlich abgelehnt — kein
  weiterer Versuch) und `CANCELLED` (Chat getrennt — kein weiterer Versuch).
  Wiederholbar ist **nur** `FAILED`.
- **Lehre:** „Wird nicht wiederholt" ist eine **fachliche** Eigenschaft. Sie
  gehört in das Fachmodell, nicht in einen Kommentar neben der Stelle, an der
  gespeichert wird.

### 1.6 Kleinbefunde

| Befund | Nachbesserung |
|---|---|
| Code wird auch ohne Bot-Token erzeugt | `code(...)` liefert `Optional.empty()` ohne konfigurierten Bot; der Code entsteht dann nicht. Die Oberfläche zeigt nur den Hinweis — keine wirkungslose Aktion. |
| Keine Kollisionsbehandlung bei der Code-Erzeugung | `ActivationCodeGenerator.nextUnused(...)` zieht einen neuen Code, wenn der gezogene bereits vergeben ist. Ein belegter Code ist kein Serverfehler. |
| Kommentar zu `PENDING` falsch | Der Kommentar beschreibt jetzt den wirklichen Zustand: der Versuch läuft oder das Ergebnis steht aus. Ein `PENDING`-Vermerk wird **nicht** wiederholt — ein erneuter Versand wäre eine doppelte Zustellung. |
| „Freigeschaltet von“ nennt den Anforderer, nicht den Absender | Der Bot kennt keine Person: die **anfordernde** Person trägt den Code und damit die Verbindung (ADR 0007, Risiko S14). Das ist eine bewusste Entscheidung, kein Versehen — der Anzeigetext bleibt, die Herkunft steht im Glossar. |

## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| Mehrere Kandidaten eines Typs brauchen den Qualifizier an `@Bean` **und** an der Injektionsstelle | `backend-ports-adapters` (Verdrahtung: technische Beans eindeutig qualifizieren) |
| `@RequiredArgsConstructor` und ein eigener Konstruktor dürfen nicht nebeneinander stehen | `backend-ports-adapters` (Use Cases: genau ein Konstruktor) |
| Ein Feature, das den Start verändert, braucht einen Kontext-Test | `feature-testing` (Testebene „Start": Kontext-Test für Verdrahtung) |
| Jeder Weg zu einem Ziel (Chat) prüft dessen Bindung | `feature-testing` (Randbedingungen je Weg prüfen, nicht je Funktion) |
| „Wird nicht wiederholt" ist eine fachliche Eigenschaft und gehört ins Fachmodell | `backend-ports-adapters` (Fachmodell: Zustände statt Kommentaren) |

## 3. Offen (Follow-up-Issues)

- Der Freischalt-Code ist sechsstellig und hat **kein Rate-Limit**: über den
  Bot ließen sich Codes erraten. Das ist bewusst offengelassen, weil eine
  Sperre eine eigene fachliche Regel mit Zählern je Chat wäre — sie gehört
  in ein eigenes Issue, nicht in die Reparatur. → **Issue #36**
  (`fix(telegram): Freischalt-Code gegen Durchprobieren härten`).
- Die drei nicht blockierenden Befunde der Review-Runde 3 (M1 Namensauflösung
  je Chat, M2 Controller-Javadoc, N1 totes Feld `activatedBy`) sind bereits
  in `3fbb171` umgesetzt und brauchen kein eigenes Issue.

## 4. Lehren aus dem Gate-Lauf

Das Feature hat fünf Review- und drei Abnahme-Runden gebraucht, mit 29 vollen
`mvn test`-, 20 `npm test`-Läufen und sechs Kontext-Kompaktierungen. Zwei
Ursachen:

1. **Der Befundstand lebte nur im Chat.** Nach einer Kompaktierung war die
   Liste der offenen Befunde weg, der Reviewer begann wieder bei null und
   fand neue NITs statt die alten zu schließen. Deshalb steht der Stand jetzt
   als Marker `<!-- review-findings: runde=<n> … -->` am Issue und ist die
   einzige Quelle der Folgerunde.
2. **Runden ohne Grenze und ohne Delta.** Jede Runde prüfte wieder alles.

Überführte Regeln (AGENTS.md DoD 4, `.opencode/README.md`,
`/review`, `/abnahme`, beide Gate-Agenten, Commit `69055d7`):

| Lehre | Regel |
|---|---|
| Ein Befundstand im Chat überlebt keine Kompaktierung | Der Stand steht am Issue, nicht im Verlauf |
| Eine Vollerhebung findet beliebig viele NITs | Runde 1 erhebt geschlossen, Runden 2–3 prüfen nur offene `K`-Befunde und Regressionen im Korrektur-Diff |
| Ein Gate ohne Rundengrenze ist ein Kostenrisiko | Höchstens drei Review-Runden insgesamt, gezählt am Issue-Marker; die Abnahme wird höchstens einmal wiederholt |
| Urteile gehören an das Issue, nicht in den Chat | `review:`- und `abnahme:`-Marker als Kommentar, mit Rundennummer und geprüftem Commit |

Zusätzlich: MINOR/NIT blockieren nie und werden zu Follow-up-Issues — sonst
wird eine Korrekturrunde zur NIT-Jagd, während die eigentliche Freigabe
wartet.
