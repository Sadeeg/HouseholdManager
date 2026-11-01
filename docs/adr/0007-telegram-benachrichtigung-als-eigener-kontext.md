# ADR 0007 — Telegram-Benachrichtigung als eigener Fach-Kontext

- **Status:** angenommen (Issue #35, umgesetzt am 2026-11-01)
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

> **Aufgelöst in Runde final (Issue #35).** Die Entscheidungen zu O1–O4 stehen
> im Abschnitt „Entscheidungen zu O1–O4" weiter unten; die Tabelle bleibt als
> Nachweis der offenen Punkte im Anforderungs-Lauf erhalten.

Diese Punkte hat das Issue **nicht** entschieden; der ARC-Agent entscheidet sie
nicht selbst.

Diese Punkte hat das Issue **nicht** entschieden; der ARC-Agent entscheidet sie
nicht selbst.

| Nr | Frage | Warum sie architektonisch ist |
|----|-------|-------------------------------|
| O1 | **Wie kommt eine Chat-Nachricht ins Backend** — HTTP-Webhook (öffentlicher Endpunkt, zusätzlicher Ingress) oder **Long-Poll** (der Bot fragt Telegram aktiv ab)? | Webhook = ein öffentlich erreichbarer Endpunkt (Terrorangriff auf `0.0.0.0/0` in der Deployment-Sicht), Reverse-Proxy/TLS nötig, zusätzliche Authentizierung des Telegram-Aufrufers. Long-Poll = nur **Egress** aus der Anwendung, passt zur bestehenden Betriebsannahme (ADR 0003, 0005) und braucht keinen Ingress — kostet aber eine dauerhaft laufende Abfrage-Schleife im Backend. |
| O2 | **Wie werden die Wiederholungen ausgelöst** — ein eigener stündlicher Scheduler-Tick, ein `@Scheduled(fixedDelay)`-Wiederholer oder ein gespeicherter Versuchszeitpunkt? | Der Versuchszähler braucht persistenten Zustand; die Wahl bestimmt, ob ein **zweiter** Zeitgeber in `ApplicationConfig` nötig wird (Zyklusrisiko) und ob „nicht nachholen" (Out of Scope) eingehalten bleibt. |
| O3 | **Liegt der Bot-Token im Backend** (Egress) — ja, sonst kann die Anwendung nicht senden. | Bestätigt nur, dass `householdmanager.telegram.bot-token` ein **Secret** in `.env` ist und nie ins Repository kommt; kein Client-Secret, kein OIDC-Binding. |
| O4 | **Wie wird der Bot-Token im Frontend behandelt**? | Der Code wird **im Chat an den Bot** gesendet, nicht über die Oberfläche. Das Backend muss den Code also **aus Telegram heraus** entgegennehmen — der Code darf den Oberflächenweg nicht verlassen. |

## Entscheidungen zu O1–O4 (Issue #35, Runde final)

Das Issue hat die vier offenen Punkte entschieden; die Umsetzung folgt ihnen.

| Nr | Entscheidung | Umsetzung |
|----|--------------|-----------|
| O1 | **Long-Poll.** Die Anwendung fragt Telegram aktiv ab — nur Egress, kein öffentlicher Endpunkt, kein Zertifikat, kein Ingress. | `adapters/inbound/telegram/TelegramBotInputAdapter` — ein Daemon-Thread mit `getUpdates`; der Thread endet beim Herunterfahren (`ContextClosedEvent`), sonst blockiert er den Shutdown. |
| O2 | **Eigener stündlicher Scheduler-Tick.** Der Versuchszähler liegt persistent in `notification_delivery`; der Wiederholungslauf läuft stündlich über `ApplicationConfig` (zweiter CronTask neben dem 18:00-Lauf). | `adapters/inbound/scheduler/NotificationRetryJob` → `NotificationDispatchUseCase.retryFailed()`; die Vermerke mit `attempt_count < 3` und fehlgeschlagenem letzten Versuch werden erneut versucht — frühestens **eine Stunde** nach dem letzten Versuch (`NotificationDelivery.isDueAt`). |
| O3 | **Der Bot-Token liegt im Backend.** `householdmanager.telegram.bot-token` ist ein Secret aus der `.env` — nie im Repository, nie in einer Logausgabe, nie in der Oberfläche. | `config/TelegramConfig.TelegramToken` (eigener Typ statt eines nackten `String`-Beans); der Adapter meldet nur „gesetzt"/„nicht gesetzt". |
| O4 | **Der Code verlässt die Oberfläche nicht.** Er wird **im Chat an den Bot** gesendet; das Backend nimmt ihn aus Telegram heraus entgegen. Die Oberfläche zeigt und kopiert ihn nur. | `TelegramChatUseCase.handleMessage(chatId, text)` — der Use Case erkennt den Code; `GET /api/households/{id}/telegram/code` liefert nur den Anzeigewert. |

### Die freischaltende Person

Der Bot kennt **keine Person** — er sieht nur die Chat-Kennung und die
Nachricht. Wer den Chat freigeschaltet hat, muss trotzdem fachlich feststehen:
nur diese Person darf die Verbindung trennen (AC13, AC14).

Die Festlegung: der **Code trägt die anfordernde Person**. Er entsteht beim
ersten Anfordern durch ein Mitglied und wird zusammen mit dessen Kennung
gespeichert (`telegram_activation_code.activated_by`). Eine Freischaltung über
diesen Code gehört dann der Person, die den Code angefordert hat — je Chat
genau eine freischaltende Person.

Der Code selbst bleibt haushaltsweit und für jedes Mitglied sichtbar; wer ihn
zuerst anfordert, ist der Eigentümer der darüber entstehenden Verbindungen.
Das ist eine bewusste Vereinfachung: sie braucht keine Identität im
Telegram-Protokoll und hält die Trennberechtigung eindeutig.

### Der Code bestimmt den Haushalt

Der Bot kennt keine Person und keine Zuordnung von Chats zu Haushalten —
er sieht nur die Chat-Kennung und die Nachricht. Deshalb bestimmt **der
Code** den Haushalt: der Code gehört zu genau einem Haushalt, und eine
Freischaltung über diesen Code gehört diesem Haushalt.

Daraus folgt die Antwortregel des Bots: ein **unbekannter** Code und ein
Code, der zu einem **anderen** Haushalt als dem des Chats gehört, bekommen
dieselbe Antwort — einen Hinweis, wie ein gültiger Code beschafft wird
(AC9, AC10). Die Antwort verrät damit nicht, ob der Code existiert, und
nennt nie den Haushaltsnamen (AC8).

Ein Chat gehört zu höchstens einem Haushalt: die Chat-Kennung ist der
Zugriffsweg, und dieselbe Kennung in zwei Haushalten wäre eine doppelte
Zustellung. Der Adapter sichert das zusätzlich über einen Unique-Index
über (Haushalt, Chat-ID).

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
