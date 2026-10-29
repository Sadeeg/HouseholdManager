# 1. Einführung und Ziele

## Dokumentation

| | |
|---|---|
| Zweck | Fachlicher und technischer Überblick über HouseholdManager |
| Pflege | Der ARC-Agent (`@arc-agent`) bei jedem Feature; Entscheidungen als ADR unter `docs/adr/` |
| Stand | Initiale Version zum Projektstart (Sprint 0), fortgeschrieben bei Wartungsdurchläufen |

## Aufgabenstellung

HouseholdManager ist eine Webanwendung zur Verwaltung eines Haushalts:
Mitglieder, Aufgaben, Ausgaben und Termine werden gemeinsam organisiert.
Die Anwendung ist mandantenfähig gedacht — jeder Nutzer sieht nur seinen
Haushalt.

## Qualitätsziele

| Priorität | Ziel | Begründung |
|-----------|------|------------|
| 1 | **Wartbarkeit**: Fachlogik ist ohne Framework testbar, Änderungen bleiben lokal | Kleines Team, wechselnde Anforderungen |
| 2 | **Nachvollziehbarkeit**: Jede Anforderung ist ein Issue, jede Architekturentscheidung ein ADR | Anforderungen kommen aus Gesprächen, nicht aus dem Code |
| 3 | **Erweiterbarkeit**: Neue Features sind eigenständige, lazy ladbare Einheiten | Haushaltsverwaltung wächst um Aufgaben, Ausgaben, Termine |
| 4 | **Testbarkeit**: Verhalten ist ohne laufende Infrastruktur prüfbar | Schnelles Feedback, kleine Modelle im Einsatz |
| 5 | **Bedienbarkeit**: Ein Feature ist in einem Durchgang erlernbar | Endnutzer ohne technische Vorbildung |
| 6 | **Performance**: Listen und Detailansichten reagieren lokal ohne spürbare Wartezeit | Later als Anforderung zu konkretisieren |

> **Offen:** Prioritäten mit dem Auftraggeber bestätigen; Messwerte für
> Performance ergänzen.

## Stakeholder

| Rolle | Erwartung |
|-------|-----------|
| Auftraggeber | Anforderungen priorisiert, Entscheidungen zu Architekturfragen |
| Haushaltsleitung | Verwaltung von Aufgaben, Ausgaben, Terminen |
| Mitglieder | Eigene Aufgaben und Termine, ggf. geteilte Ausgaben |
| Entwicklung (Agenten) | Nachvollziehbare Issues, klare Gates, kurzer Kontext |
