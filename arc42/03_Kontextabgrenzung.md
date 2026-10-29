# 3. Kontextabgrenzung

## Fachlicher Kontext

HouseholdManager verwaltet **einen Haushalt** mit seinen Mitgliedern.
Die zentrale Einheit ist der **Haushalt**; alles Weitere (Aufgaben,
Ausgaben, Termine) gehört zu genau einem Haushalt.

## Bounded Context

| Context | Inhalt | Reife |
|---------|--------|-------|
| **Haushalt** | Anlegen und Anzeigen von Haushalten; Ernennen und Entfernen von Mitgliedern durch die Haushaltsleitung (Issue #1) | in Arbeit (Issue #1) |
| Aufgabe | Aufgaben-Board TODO/DOING/DONE mit Labels, Fristen, Zuweisungen je Haushalt (Issue #1) | entworfen (Issue #1), nicht implementiert |
| Ausgabe | Ausgaben, Kategorien, Belege | geplant |
| Termin | Termine, Wiederholungen | geplant |

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
