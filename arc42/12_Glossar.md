# 12. Glossar

Fachliche und architektonische Begriffe. Neue Begriffe aus einem Feature
trägt der ARC-Agent hier ein.

## Fachlich

| Begriff | Bedeutung |
|---------|-----------|
| **Haushalt** | Die zentrale Einheit: ein Wohnumfeld mit Mitgliedern, Aufgaben, Ausgaben und Terminen. Alle Daten sind einem Haushalt zugeordnet. |
| **Haushaltsleitung** | Die Person, die den Haushalt erstellt hat; sie ernennt und entfernt Mitglieder und ist selbst immer Mitglied. |
| **Haushaltsmitglied** | Person aus dem Nutzerverzeichnis, die von der Haushaltsleitung zum Haushalt ernannt wurde; die ernannte Zugehörigkeit ist Zugangsvoraussetzung zu den Aufgaben des Haushalts. |
| **Aufgabe** | Eintrag eines Haushalts mit Titel, optionalem Markdown-Inhalt, Status (TODO/DOING/DONE), optionaler Zuständiger, Frist und Labels. |
| **Label** | Frei benannter Text an einer Aufgabe; Identität ist groß-/kleinschreibungsunabhängig, angezeigt wird die zuerst gespeicherte Schreibweise; gleiche Namen erscheinen in gleicher Farbe. |
| **Überfällig** | Aufgabe mit Frist vor dem heutigen (lokalen) Tag und Status ungleich DONE. |
| **Workspace (der App)** | Der auf App-Ebene gewählte Haushalt; alle Ansichten zeigen ausschließlich dessen Daten. |
| **Ausgabe** | Geldbetrag in einem Haushalt, optional mit Kategorie und Beleg (späterer Context). |
| **Termin** | Zeitpunkt mit Beschreibung, ggf. wiederkehrend (späterer Context). |
| **Müllkalender** | Die hinterlegte ICS-Quelle eines Haushalts samt der daraus abgeleiteten Abholtermine; je Haushalt gilt höchstens eine Quelle, sie ist optional. |
| **Quelle** | Die Adresse des ICS-Feeds, aus der die Termine eines Haushalts gelesen werden (`household.waste_calendar_url`). |
| **Mülltermin** | Ein ganztägiger Abholtermin aus der Quelle: Tonnenart (`SUMMARY`) und Datum (`DTSTART`) — ohne Uhrzeit, unverändert übernommen. |
| **Sechs-Wochen-Fenster** | Die 42 Tage ab dem Tag des Laufs, für die Termine gespeichert werden (R5). |
| **Stand** | Der Zeitpunkt des letzten **erfolgreichen** Synchronisationslaufs eines Haushalts (`waste_sync_state.last_synced_at`); ohne solchen Lauf: „Noch keine Synchronisation". |
| **Veraltet** | Der Zustand, in dem seit dem letzten erfolgreichen Lauf ein Lauf fehlgeschlagen ist: die gespeicherten Termine bleiben sichtbar, der Stand wird als veraltet ausgewiesen (R13). |
| **Invariante** | Fachregel, die immer gilt (z. B. Name nicht leer, höchstens 120 Zeichen). |
| **Telegram-Chat** | Ein über Telegram verbundener Chat — entweder eine **Gruppe** oder ein **Einzelchat** mit dem Bot. Beide heißen gemeinsam „Telegram-Chat". |
| **Freischalt-Code** | Die dauerhaft gültige Kennung eines Haushalts, mit der ein Telegram-Chat diesem Haushalt zugeordnet wird. Kein Ablaufdatum, mehrfach verwendbar, für alle Mitglieder sichtbar. Er trägt die **Person, die ihn angefordert hat** — sie ist die freischaltende Person der daraus entstehenden Verbindungen. |
| **Freischaltende Person** | Das Mitglied, das den Freischalt-Code angefordert hat und den Chat freigeschaltet hat (`TelegramChat.activatedBy`). Nur sie darf die Verbindung trennen; alle Mitglieder sehen sie. Der Bot selbst kennt keine Person. |
| **Zustellvermerk** | Das fachliche Protokoll eines Benachrichtigungsversands: Ziel, Inhalt, Versuchszählung, Fehler, Status. Er ist persistent und damit Grundlage der Wiederholungsregel. |
| **Zustellfehler** | Telegram war nicht erreichbar (Netzwerk, Zeitüberschreitung, Serverfehler). Der Versuch wird gezählt und beim nächsten stündlichen Lauf wiederholt (`TelegramDeliveryException`). |
| **Protokollfehler** | Telegram hat die Anfrage fachlich abgelehnt (HTTP 200 mit `ok: false` — unbekannter Chat, ungültige Kennung). Er wird protokolliert, aber **nicht** wiederholt (`TelegramProtocolException`). |
| **Erinnerung** | Die Nachricht am Abend vor einem Mülltermin mit dessen Abfalltyp und Datum; fasst alle Abfalltypen des Folgetags in **einer** Nachricht zusammen. |
| **Wochenübersicht** | Die Nachricht mit allen Müllterminen der kommenden Woche (Montag–Sonntag), aufsteigend nach Datum. |

## Architektur

| Begriff | Bedeutung |
|---------|-----------|
| **Bounded Context** | Abgeschlossener fachlicher Bereich mit eigenem Modell (hier: Haushalt, Aufgabe, Ausgabe, Termin). |
| **Port** | Schnittstelle, die die Anwendung anbietet (Inbound) oder braucht (Outbound). Nur die Schnittstelle, keine Technik. |
| **Adapter** | Implementierung eines Ports an der Grenze: REST-Controller (ein-/ausgehend), JPA-Repository (ausgehend). |
| **Use Case** | Eine fachliche Fähigkeit der Anwendung; orchestriert, prüft Invarianten, ist die Transaktionsgrenze. |
| **Fachmodell** | Die fachliche Form der Daten (ohne Framework). |
| **Persistenzmodell** | Die technische Form der Daten (JPA-Entity, Tabelle). |
| **Composition Root** | Der Ort, an dem Beans verdrahtet werden (`config/`). |
| **Feature** | Im Frontend ein bounded context als Ordner mit `routes.ts` und den Schichten `model`, `data`, `ui`. |
| **Smart Component** | Component mit Datenzugriff (Feature-Schicht). |
| **Dumb Component** | Component ohne Datenzugriff, nur Inputs/Outputs (UI-Schicht). |
| **Route-scoped Service** | Service, der nur für die Dauer einer Route lebt (Feature-Zustand). |
| **Sheriff** | ESLint-Plugin, das Modulgrenzen der Feature-Architektur prüft. |
| **Fach-Exception** | Fehler des Fachkerns (framework-frei, ohne HTTP-Typen) — wird am eingehenden Adapter in einen Statuscode übersetzt. |
| **ProblemDetail** | Standard-Fehlerformat der REST-API (RFC 9457) mit `status`, `title` und `detail`. |
| **Signal** | Reaktiver Zustandscontainer im Frontend (Angular 22, zoneless). |
| **httpResource** | Reaktive Angular-Ressource für Lesezugriff; liefert `value()`, `error()`, `isLoading()` und `reload()`. |
| **Testcontainer** | Test mit echter Datenbankinstanz (PostgreSQL 17) statt Mock — für den ausgehenden Adapter. |
| **Dev Services** | Boot-4-Funktion: Spring Boot startet die Compose-Datei selbst — im Projekt nur im Profil `local` aktiv. |
| **Vertikaler Schnitt** | Feature als durchgehender Schnitt von der Oberfläche bis zur Persistenz. |
| **Scheduler (eingehender Adapter)** | Zeitgesteuerter Auslöser in `adapters/inbound/scheduler/` — er ruft genau einen Use Case auf und enthält keine Fachlogik (ADR 0005). |
| **Bot-Eingang (eingehender Adapter)** | Adapter, der Nachrichten eines Chat-Dienstes entgegennimmt und daraus einen Use Case auslöst — die Entsprechung des Controllers für einen externen Kanal (ADR 0007). |
| **Long-Poll** | Abfrageverfahren, bei dem der Client den Dienst aktiv nach neuen Nachrichten fragt und die Verbindung offen hält. Der Bot-Eingang nutzt es: die Anwendung braucht **keinen** öffentlich erreichbaren Endpunkt, nur Egress (ADR 0007, O1). |
| **Chat-Art** | Die fachliche Form eines Telegram-Chats: `GROUP` (Gruppe) oder `PRIVATE` (Einzelchat). Das Modell kennt die Telegram-Protokollwerte nicht — die Übersetzung liegt am Adapter. |
| **Zustellvermerk** (Architektur) | Persistenter Zustand je Nachricht im Fachkern; trägt die Versuchszählung, damit „höchstens drei Versuche" über einen Neustart hinweg gilt (ADR 0007). |
| **Fremdprotokoll** | Das Format oder die API eines externen Dienstes (ICS, Bot-API) — bleibt vollständig im ausgehenden Adapter, der Port bleibt protokollfrei (ADR 0006, ADR 0007). |
| **ICS** | Das Austauschformat der Abholtermine (iCalendar): `VCALENDAR` mit `VEVENT`-Ereignissen; ausgewertet wird es mit `ical4j` **im ausgehenden Adapter** — die Anwendung kennt das Format nicht (ADR 0006). |

## Prozess

| Begriff | Bedeutung |
|---------|-----------|
| **AC** | Akzeptanzkriterium (`AC1`, `AC2`, …) — prüfbare Aussage im Issue. |
| **ADR** | Architecture Decision Record unter `docs/adr/`. |
| **arc42** | Architekturdokumentation dieses Projekts (dieser Ordner). |
| **Gate** | Prüfschritt, ohne den ein Feature nicht fertig ist: Architektur-Review (`OK`) und Abnahme (`PASS`). |
| **Grilling** | Interaktives Hinterfragen der Anforderung, eine Frage pro Zug. |
| **Korrekturauftrag** | Ausführbare Liste von Korrekturen, die der Reviewer liefert und der Implementierer umsetzt. |
