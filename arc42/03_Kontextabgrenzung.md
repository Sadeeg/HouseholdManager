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
| Ausgabe | Ausgaben, Kategorien, Belege | geplant |
| Termin | Termine, Wiederholungen | geplant |

Der **Müllkalender** ist **kein** Termin-Context: er bildet keinen
Abfallkalender im Haus ab — keine manuell gepflegten Termine, keine
Sonderleerungen, keine Aufgaben, keine Ausgaben. Der geplante Context
*Termin* bleibt davon getrennt.

Die Contexts sind über den **Haushalt** verbunden. Spätere Contexts
(Finanzen, Einkauf) greifen nicht direkt auf die Daten anderer Contexts zu,
sondern über die dokumentierte öffentliche Schnittstelle des jeweiligen
Context — die Festlegung erfolgt mit dem ersten Epic des jeweiligen Contexts.
Der Aufgaben-Kontext fragt die Mitgliedschaft über den In-Port des
Haushalt-Kontexts ab und fasst dessen Tabellen nicht an (ADR 0003).

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

Die Abhängigkeit zum Entsorger ist **einseitig**: das System ruft ab, der
Entsorger weiß nichts vom HouseholdManager. Die Quelle gehört zum einzelnen
Haushalt; die Termine zweier Haushalte werden nie zusammengeführt.

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

