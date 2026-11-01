# ADR 0007 — Telegram-Benachrichtigung als eigener Fach-Kontext

- **Status:** vorgeschlagen (Runde 2b, Issue #35) — Entscheidung durch den Auftraggeber offen
- **Datum:** 2026-11-01
- **Entscheidung:** Die Telegram-Anbindung (Bot, Freischaltung, Zustellung von
  Mülltermin-Benachrichtigungen) wird als **eigener Fach-Kontext
  „Benachrichtigung"** neben dem Müllkalender geführt. Der Kontext liest die
  Termine **über den In-Port des Müllkalender-Kontexts** und greift nicht auf
  dessen Tabellen zu.

## Kontext

Issue #35 (Telegram-Bot für Mülltermine) bringt eine vierte Art von
Bausteinen in die Anwendung:

- einen **eingehenden Adapter**, der Nachrichten aus Telegram entgegennimmt
  (Freischaltung per Code, Bot-Antwort);
- einen **ausgehenden Adapter**, der Nachrichten an Telegram sendet;
- einen **zweiten zeitgesteuerten Lauf** (18:00 Uhr) neben dem Tageslauf des
  Müllkalenders (ADR 0005);
- **Zustellzustand** (Versuchszählung) — Zustand, den es im Bestand noch
  nicht gibt.

Der Müllkalender-Kontext ist heute ausdrücklich **read-only** und endet an
seiner Grenze: „Aus ihnen entsteht nichts weiter — keine Benachrichtigung"
(`domain/model/WasteCollection.java`). Das Fachmodell des Mülltermins ist
damit **kein** Benachrichtigungsmodell — die Benachrichtigung braucht
zusätzlich Versandtext, Empfänger, Versuchszählung und Fehlerzustand. Es
entsteht ein **eigener Satz Fachmodell, Tabellen und Ports**.

Zwei bestehende Festlegungen begrenzen die Optionen:

- **ADR 0003:** Fach-Kontexte sind Module *derselben* Anwendung — kein eigener
  Service, kein Worker, keine Queue. Das gilt auch hier.
- **ADR 0001:** Kontextgrenzen laufen über In-Ports; ein Kontext fasst die
  Tabellen eines anderen nicht an.

Das Issue begrenzt den Scope bewusst auf Mülltermine und schließt „ein
automatischer Ausbau zu einer Benachrichtigungszentrale für alle Ereignisse
des Haushalts" aus. Das ist eine **Scope**-Grenze, keine **Architektur**-Grenze:
der Kontext ist so geschnitten, dass ein späterer Kanal (E-Mail, Web-Push) und
ein späteres Ereignis (Aufgaben, Termine) **dieselben** Bausteine nutzen
können, ohne den Müllkalender anzufassen.

## Entscheidung

- **Eigener Fach-Kontext „Benachrichtigung"** in der Anwendung
  (`de.householdmanager`), parallel zum Müllkalender. Er umfasst:
  - das Fachmodell eines **Telegram-Chats** (Chat-ID, Anzeigename, Chat-Art,
    Haushalt, freischaltende Person),
  - den **Zustellvermerk** einer Nachricht (Ziel, Inhalt, Versuchszählung,
    Fehler, Status),
  - die **Ports** für den Versand und für die Persistenz.
- **Der Müllkalender bleibt unangetastet.** Die Benachrichtigung liest die
  Termine **über den In-Port des Müllkalender-Kontexts**; die Richtung ist
  **einseitig** (Benachrichtigung → Müllkalender), wie bei der Mitgliedschaft
  (ADR 0003). Der Kontext „Müllkalender" weiß nichts von Telegram.
- **Der Telegram-Transport ist ein Adapter, kein Fachmodell.** Die fachlichen
  Begriffe (Freischalt-Code, Chat-Art, Zustellzustand) stehen im
  `domain`; Telegram-Begriffe (Chat-ID, Bot-Token, `sendMessage`) enden an der
  Adaptergrenze. Das entspricht ADR 0006 für das ICS-Format: Fremdwissen
  (hier: die Bot-API) bleibt im Adapter, der Port bleibt bibliotheks- und
  protokollfrei.
- **Zeitregeln liegen in der Anwendung, nicht im Adapter.** Die Fenster
  („morgen", „kommende Woche"), die Sendezeitpunkte und die Versuchszählung
  sind Domänenregeln und stehen im Use Case mit der `Clock`-Bean; die
  Telegram-API kennt sie nicht.
- **Kein eigener Betriebsbaustein.** Der 18:00-Uhr-Lauf ist ein zweiter
  eingehender Adapter in `adapters/inbound/scheduler/` und wird in
  `config/ApplicationConfig` **neben** dem Tageslauf eingetragen (ADR 0005,
  Muster „eingehender Adapter löst genau einen Use Case aus").
- **Fehlerisolierung je Haushalt** nach dem Vorbild von ADR 0005: ein
  fehlgeschlagener Haushalt stoppt den Lauf nicht; ein fehlgeschlagener Chat
  stoppt die übrigen Chats desselben Haushalts nicht.

## Offene Entscheidungen (Blocker für Runde 3)

Diese Punkte hat das Issue **nicht** entschieden; der ARC-Agent entscheidet sie
nicht selbst.

| Nr | Frage | Warum sie architektonisch ist |
|----|-------|-------------------------------|
| O1 | **Wie kommt eine Chat-Nachricht ins Backend** — HTTP-Webhook (öffentlicher Endpunkt, zusätzlicher Ingress) oder **Long-Poll** (der Bot fragt Telegram aktiv ab)? | Webhook = ein öffentlich erreichbarer Endpunkt (Terrorangriff auf `0.0.0.0/0` in der Deployment-Sicht), Reverse-Proxy/TLS nötig, zusätzliche Authentizierung des Telegram-Aufrufers. Long-Poll = nur **Egress** aus der Anwendung, passt zur bestehenden Betriebsannahme (ADR 0003, 0005) und braucht keinen Ingress — kostet aber eine dauerhaft laufende Abfrage-Schleife im Backend. |
| O2 | **Wie werden die Wiederholungen ausgelöst** — ein eigener stündlicher Scheduler-Tick, ein `@Scheduled(fixedDelay)`-Wiederholer oder ein gespeicherter Versuchszeitpunkt? | Der Versuchszähler braucht persistenten Zustand; die Wahl bestimmt, ob ein **zweiter** Zeitgeber in `ApplicationConfig` nötig wird (Zyklusrisiko) und ob „nicht nachholen" (Out of Scope) eingehalten bleibt. |
| O3 | **Liegt der Bot-Token im Backend** (Egress) — ja, sonst kann die Anwendung nicht senden. | Bestätigt nur, dass `householdmanager.telegram.bot-token` ein **Secret** in `.env` ist und nie ins Repository kommt; kein Client-Secret, kein OIDC-Binding. |
| O4 | **Wie wird der Bot-Token im Frontend behandelt**? | Der Code wird **im Chat an den Bot** gesendet, nicht über die Oberfläche. Das Backend muss den Code also **aus Telegram heraus** entgegennehmen — der Code darf den Oberflächenweg nicht verlassen. |

## Konsequenzen

**Positiv**

- Der Müllkalender bleibt read-only und unangetastet; die Notification wächst
  unabhängig weiter.
- Die Telegram-Kenntnis bleibt im Adapter (wie ICS in ADR 0006) — die
  Anwendung bleibt frei von Fremdprotokollen.
- Die Regeln „morgen", „kommende Woche" und „drei Versuche" existieren genau
  einmal, im Use Case, und sind ohne Spring und ohne Datenbank testbar
  (`Clock` fest).
- Ein späterer Kanal oder ein späteres Ereignis braucht **keine** Änderung am
  Müllkalender.

**Negativ / Kosten**

- Ein zusätzlicher Satz Fachmodell, Tabellen und Ports (Klassen über ADR 0001).
- Ein **zweiter** Zeitgeber in `config/ApplicationConfig` — die Klasse
  dokumentiert ausdrücklich, dass sie **keine Beans** liefert; der
  Notification-Job darf sie deshalb nicht zurückreferenzieren (Zyklusrisiko).
- Der Code-Transport über den Bot ist **umständlicher** als ein Code in der
  Oberfläche: der Freischalt-Code muss vom Backend aus Telegram heraus
  entgegengenommen werden (O4).

**Neutral**

- Der Context ist fachlich auf Mülltermine beschnitten; die Architektur trägt
  mehr Ereignisse und Kanäle bereits.
- Es gibt **kein** Admin-Rolle-Modell; alle Mitglieder sind gleichberechtigt,
  nur wer selbst freigeschaltet hat, kann trennen.
