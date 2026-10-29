# 3. Kontextabgrenzung

## Fachlicher Kontext

HouseholdManager verwaltet **einen Haushalt** mit seinen Mitgliedern.
Die zentrale Einheit ist der **Haushalt**; alles Weitere (Aufgaben,
Ausgaben, Termine) gehört zu genau einem Haushalt.

## Bounded Context

| Context | Inhalt | Reife |
|---------|--------|-------|
| **Haushalt** | Anlegen und Anzeigen von Haushalten, Mitgliedschaft | in Arbeit (Sprint 0) |
| Aufgabe | Aufgaben innerhalb eines Haushalts | geplant |
| Ausgabe | Ausgaben, Kategorien, Belege | geplant |
| Termin | Termine, Wiederholungen | geplant |

Die Contexts sind über den **Haushalt** verbunden. Spätere Contexts
(Finanzen, Einkauf) greifen nicht direkt auf die Daten anderer Contexts zu,
sondern über die dokumentierte öffentliche Schnittstelle des jeweiligen
Context — die Festlegung erfolgt mit dem ersten Epic des jeweiligen Contexts.

## Nachbarsysteme

| System | Richtung | Zweck | Stand |
|--------|----------|-------|-------|
| Browser / SPA | ← | Bedienoberfläche | vorhanden |
| PostgreSQL 17 | ↔ | Persistenz | vorhanden |
| Docker-Host | ← | Betrieb der Container | vorhanden |

> **Offen:** Schnittstellen zu Bank, Lieferanten oder Kalenderdiensten —
> erst mit dem passenden Epic zu spezifizieren.

## Scope des aktuellen Sprints

| In Scope | Out of Scope |
|----------|--------------|
| Haushalt anlegen (Name, Zeitstempel) | Mitgliederverwaltung |
| Haushalte auflisten | Aufgaben, Ausgaben, Termine |
| Fehlerbehandlung und Validierung | Authentifizierung, Mandantenfähigkeit |
| Vertikaler Schnitt Backend ↔ Frontend | Export, Benachrichtigungen, Berichte |

> **Stand:** Anlegen ist über die API (`POST /api/households`) umgesetzt; das
> UI-Formular dafür steht noch aus (siehe 11, S1).
