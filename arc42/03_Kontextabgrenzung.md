# 3. Kontextabgrenzung

## Fachlicher Kontext

HouseholdManager verwaltet **einen Haushalt** mit seinen Mitgliedern.
Die zentrale Einheit ist der **Haushalt**; alles Weitere (Aufgaben,
Ausgaben, Termine) gehört zu genau einem Haushalt.

## Bounded Context

| Context | Inhalt | Reife |
|---------|--------|-------|
| **Haushalt** | Anlegen und Anzeigen von Haushalten; Ernennen und Entfernen von Mitgliedern durch die Haushaltsleitung (Issue #1) | in Arbeit (Issue #1) |
| Aufgabe | Aufgaben-Board TODO/DOING/DONE mit Labels, Friften, Zuweisungen je Haushalt (Issue #1) | entworfen (Issue #1), nicht implementiert |
| **Müllkalender** | Read-only-Projektion der Abholtermine eines Haushalts aus einer externen ICS-Quelle: Quelle je Haushalt, täglicher Abruf, Sechs-Wochen-Fenster, Stand (Issue #10) | in Arbeit (Issue #10) |
| Ausgabe | Ausgaben, Kategorien, Belege | geplant |
| Termin | Termine, Wiederholungen | geplant |

Der **Müllkalender** ist **kein** Termin-Context: er bildet keinen
Abfallkalender im Haus ab — keine manuell gepflegten Termine, keine
Sonderleerungen, keine Aufgaben, keine Ausgaben (Out of Scope in Issue #10).
Der geplante Context *Termin* bleibt davon getrennt.

Die Contexts sind über den **Haushalt** verbunden. Spätere Contexts
(Finanzen, Einkauf) greifen nicht direkt auf die Daten anderer Contexts zu,
sondern über die dokumentierte öffentliche Schnittstelle des jeweiligen
Context — die Festlegung erfolgt mit dem ersten Epic des jeweiligen Contexts.
Issue #1 konkretisiert das: der Aufgaben-Kontext fragt die Mitgliedschaft
über den In-Port des Haushalt-Kontexts ab und fasst dessen Tabellen nicht an.

## Nachbarsysteme

| System | Richtung | Zweck | Stand |
|--------|----------|-------|-------|
| Browser / SPA | ← | Bedienoberfläche | vorhanden |
| PostgreSQL 17 | ↔ | Persistenz | vorhanden |
| Docker-Host | ← | Betrieb der Container | vorhanden |
| Nextcloud | ↔ | Identity Provider (OIDC) **und** Nutzerverzeichnis (OCS) — Identitätsquelle für Mitglieder-Ernennung und Aufgaben-Zuweisung | vorhanden (`NextcloudUserDirectory`) |
| Entsorger (ICS-Feed) | ausgehend (Abruf) | Abholtermine des Haushalts als ICS (`VCALENDAR`/`VEVENT`); nur gelesen, keine Antwort des Systems | vorgesehen (`IcsWasteCalendarFeed`, Issue #10) |

Die Abhängigkeit zum Entsorger ist **einseitig**: das System ruft ab, der
Entsorger weiß nichts vom HouseholdManager. Die Quelle gehört zum einzelnen
Haushalt; die Termine zweier Haushalte werden nie zusammengeführt (Annahme 3
in Issue #10).

> **Offen:** Schnittstellen zu Bank, Lieferanten oder Kalenderdiensten —
> erst mit dem passenden Epic zu spezifizieren.

## Technische Schnittstellen

| Endpunkt | Richtung | Zweck | Stand |
|----------|----------|-------|-------|
| `GET api/users/search?q=` | ← (SPA) | Personenvorschläge aus dem Nextcloud-Nutzerverzeichnis (`id` + Anzeigename, keine E-Mail) | vorhanden (`UserSearchController`) |

> **Abweichung vom API-Vertrag von Issue #1:** dieser Endpunkt ist dort nicht
> aufgeführt und ist eine bewusste Erweiterung. Ohne ihn kann die Oberfläche
> keine Person sicher auswählen: die Personenkennung kommt aus dem
> Nextcloud-Verzeichnis und ist weder der Tafel (Zuständige zuweisen, AC8)
> noch der Haushaltsverwaltung (Mitglied ernennen, AC11) bekannt. Bausteine:
> siehe 5.

## Scope des aktuellen Sprints

| In Scope | Out of Scope |
|----------|--------------|
| Haushalt anlegen (Name, Zeitstempel) | Mitgliederverwaltung |
| Haushalte auflisten | Aufgaben, Ausgaben, Termine |
| Fehlerbehandlung und Validierung | Authentifizierung, Mandantenfähigkeit |
| Vertikaler Schnitt Backend ↔ Frontend | Export, Benachrichtigungen, Berichte |

> **Stand:** Anlegen ist über die API (`POST /api/households`) umgesetzt; das
> UI-Formular dafür steht noch aus (siehe 11, S1).
