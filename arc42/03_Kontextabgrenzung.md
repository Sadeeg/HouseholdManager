# 3. Kontextabgrenzung

## Fachlicher Kontext

HouseholdManager verwaltet **einen Haushalt** mit seinen Mitgliedern.
Die zentrale Einheit ist der **Haushalt**; alles Weitere (Aufgaben,
Ausgaben, Termine) gehört zu genau einem Haushalt.

## Bounded Context

| Context | Inhalt | Reife |
|---------|--------|-------|
| **Haushalt** | Anlegen und Anzeigen von Haushalten; Ernennen und Entfernen von Mitgliedern durch die Haushaltsleitung | in Umsetzung |
| Aufgabe | Aufgaben-Board TODO/DOING/DONE mit Labels, Fristen, Zuweisungen je Haushalt | entworfen, nicht implementiert |
| **Müllkalender** | Read-only-Projektion der Abholtermine eines Haushalts aus einer externen ICS-Quelle: Quelle je Haushalt, täglicher Abruf, Sechs-Wochen-Fenster, Stand | in Umsetzung |
| **Benachrichtigung** | Zustellung von Haushaltsmeldungen an verbundene Telegram-Chats: Freischaltung per Code, Anzeige und Trennen der Verbindungen, Benachrichtigung über Mülltermine | entworfen, nicht implementiert |
| Ausgabe | Ausgaben, Kategorien, Belege | geplant |
| Termin | Termine, Wiederholungen | geplant |

Der **Müllkalender** ist **kein** Termin-Context: er bildet keinen
Abfallkalender im Haus ab — keine manuell gepflegten Termine, keine
Sonderleerungen, keine Aufgaben, keine Ausgaben. Der geplante Context
*Termin* bleibt davon getrennt.

Der **Müllkalender** ist außerdem **kein** Benachrichtigungs-Context: er ist
read-only und endet an seiner Grenze („aus den Terminen entsteht nichts
weiter — keine Benachrichtigung"). Die **Benachrichtigung** ist ein eigener
Kontext, der die Termine **über den In-Port des Müllkalender-Kontexts** liest
(ADR 0007). Die Richtung ist einseitig: Benachrichtigung → Müllkalender,
nie umgekehrt — der Müllkalender kennt weder Telegram noch Chats noch
Zustellung.

Die Contexts sind über den **Haushalt** verbunden. Spätere Contexts
(Finanzen, Einkauf) greifen nicht direkt auf die Daten anderer Contexts zu,
sondern über die dokumentierte öffentliche Schnittstelle des jeweiligen
Context — die Festlegung erfolgt mit dem ersten Epic des jeweiligen Contexts.
Der Aufgaben-Kontext fragt die Mitgliedschaft über den In-Port des
Haushalt-Kontexts ab und fasst dessen Tabellen nicht an (ADR 0003). Dasselbe
Muster trennt den Benachrichtigungs-Kontext vom Müllkalender: er fragt die
Termine über den In-Port des Müllkalender-Kontexts ab (ADR 0007).

> **Hinweis:** Der Stand einzelner Features und der aktuelle Arbeitsumfang
> stehen im jeweiligen GitHub-Issue. arc42 hält die Context-Grenzen — nicht
> den Feature-Verlauf.

## Nachbarsysteme

| System | Richtung | Zweck | Stand |
|--------|----------|-------|-------|
| Browser / SPA | ← | Bedienoberfläche | vorhanden |
| PostgreSQL 17 | ↔ | Persistenz | vorhanden |
| Docker-Host | ← | Betrieb der Container | vorhanden |
| Nextcloud | ↔ | Identity Provider (OIDC) **und** Nutzerverzeichnis (OCS) — Identitätsquelle für Mitglieder-Ernennung und Aufgaben-Zuweisung | vorhanden (`NextcloudUserDirectory`) |
| Entsorger (ICS-Feed) | ausgehend (Abruf) | Abholtermine des Haushalts als ICS (`VCALENDAR`/`VEVENT`); nur gelesen, keine Antwort des Systems | vorgesehen (`IcsWasteCalendarFeed`) |
| Telegram (Bot-API) | ↔ | Empfang von Chat-Nachrichten (Freischalt-Code) und Versand von Benachrichtigungen; Identität des Bots über ein Token aus der Umgebung | vorgesehen (`TelegramBot`, ADR 0007) |

Die Abhängigkeit zum Entsorger ist **einseitig**: das System ruft ab, der
Entsorger weiß nichts vom HouseholdManager. Die Quelle gehört zum einzelnen
Haushalt; die Termine zweier Haushalte werden nie zusammengeführt.

Die Abhängigkeit zu Telegram ist **zweiseitig**: das System sendet
Benachrichtigungen und nimmt Chat-Nachrichten entgegen. Der Bot-Token ist ein
**Secret** (`.env`, nie im Repository), kein Client-Secret im OIDC-Sinne —
die Anwendung ist ein Client des Bot-Dienstes, kein Identity Provider.

> **Offen:** Wie eine Chat-Nachricht das Backend erreicht — HTTP-Webhook
> (öffentlicher Endpunkt) oder Long-Poll (Egress). Beides ändert die
> Betriebsannahme in 07; die Entscheidung liegt beim Auftraggeber
> (ADR 0007, O1).

> **Offen:** Schnittstellen zu Bank, Lieferanten oder Kalenderdiensten —
> erst mit dem passenden Epic zu spezifizieren.

## Technische Schnittstellen

| Endpunkt | Richtung | Zweck | Stand |
|----------|----------|-------|-------|
| `GET api/users/search?q=` | ← (SPA) | Personenvorschläge aus dem Nextcloud-Nutzerverzeichnis (`id` + Anzeigename, keine E-Mail) | vorhanden (`UserSearchController`) |

Der Endpunkt ist eine bewusste Erweiterung des ursprünglichen
API-Vertrags. Ohne ihn kann die Oberfläche keine Person sicher auswählen:
die Personenkennung kommt aus dem Nextcloud-Verzeichnis und ist weder der
Tafel (Zuständige auswählen) noch der Haushaltsverwaltung (Mitglied
ernennen) bekannt. Bausteine: siehe 5.

