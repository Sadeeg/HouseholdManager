# ADR 0003 — Fach-Contexte als Module einer Anwendung

- **Status:** angenommen
- **Datum:** 2026-10-29
- **Entscheidung:** Die Fach-Contexte (Haushalt, Aufgabe, Ausgabe, Termin) werden als **Module innerhalb derselben Anwendung** gebaut — Paketgruppen in der Schichtstruktur von ADR 0001, kein eigener Service je Context.

## Kontext

arc42/04 und 09 ließen offen, ob Aufgaben, Ausgaben und Termine eigene
Services werden oder Module im selben Service. Issue #1 (Aufgaben-Board) ist
ein vertikaler Schnitt, der den bestehenden Haushalt-Kontext braucht
(Mitgliedschaft, Nutzerverzeichnis) und mit ihm Transaktionen,
Fehlerbehandlung und Deployment teilt. Team Größe (eine Person mit Agenten),
ein Deployment-Stack (compose.yml) und der Entwicklungsmaßstab sprechen
gegen Service-Granularität.

## Entscheidung

- Eine Deploymenteinheit: `de.householdmanager` bleibt die einzige Anwendung.
- Contexte trennen über **Namen und Pakete** innerhalb der Schichten aus
  ADR 0001 (`domain/…`, `application/port/…`, `adapters/…`) — die
  Schichtregel bleibt die erste Ordnung, der Contextname kennzeichnet die
  Zugehörigkeit (z. B. `Task*`, `HouseholdMember*`).
- **Contextgrenze:** ein Context fasst das Fachmodell, die Tabellen und die
  Ports eines anderen Contexts **nicht** an. Der Aufgaben-Kontext fragt die
  Mitgliedschaft über den In-Port des Haushalt-Kontexts (`MembershipUseCase`)
  ab.
- Ein neuer Context ist ein neuer Satz Pakete in derselben Anwendung,
  dasselbe Repository, dieselbe CI, dieselbe Migrationsreihe.
- Service-Aufteilung nur bei skalierungs- oder organisationsbedingtem
  Anlass — dann als neue ADR.

## Konsequenzen

**Positiv**

- Vertikale Schnitte bleiben durchgehend (Backend-Schichten, Migrationen,
  Tests unverändert); Use Cases bleiben ohne Spring testbar.
- Keine verteilten Transaktionen, keine Dienst-zu-Dienst-Verträge, kein
  zusätzlicher Deployments-Aufwand.
- Die Contextgrenze ist als Regel testbar (Reviewer, Sheriff- analog:
  Paket-Review).

**Negativ / Kosten**

- Keine laufzeitmäßige Isolation: ein fehlerhafter Context betrifft die
  ganze Anwendung.
- Contextgrenzen sind maschinell schwächer prüfbar als Dienstgrenzen —
  sie leben vom Review.
- Die Migrationsreihe teilt sich über alle Contexte (V2, V3, …).

**Neutral**

- Mandantenfähigkeit im Sinne „Context pro Haushalt/Mieter" bleibt als
  eigene Frage offen (siehe 09, 11).
