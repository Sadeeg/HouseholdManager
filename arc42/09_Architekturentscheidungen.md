# 9. Architekturentscheidungen

Die **ADR** unter [`docs/adr/`](../docs/adr/) sind die Quelle der Wahrheit.
Dieses Kapitel ist das Inhaltsverzeichnis und die Begründung je
Entscheidung — **keine** Dublette.

| ADR | Titel | Status | Datum | Auswirkung |
|-----|-------|--------|-------|------------|
| [0001](../docs/adr/0001-backend-ports-and-adapters.md) | Backend: Ports-and-Adapters-Architektur | angenommen | 2026-10-29 | Fachkern ohne Framework, Adapter an den Rändern, Tests je Schicht |
| [0002](../docs/adr/0002-frontend-feature-architecture.md) | Frontend: Feature-basierte Architektur (Manfred Steyer) | angenommen | 2026-10-29 | Feature = bounded context, Matrix-Schichten, Grenzen per Sheriff |
| [0003](../docs/adr/0003-fach-contexte-als-module-einer-anwendung.md) | Fach-Contexte als Module einer Anwendung | angenommen | 2026-10-29 | Eine Deploymenteinheit; Contextgrenzen nur über In-Ports |
| [0004](../docs/adr/0004-workspace-auswahl-des-haushalts-in-core.md) | Workspace-Auswahl des Haushalts in core/ | angenommen | 2026-10-29 | Auswahl app-weit in `core/workspace`; Features lesen nur den gewählten Haushalt |
| [0005](../docs/adr/0005-taeglicher-serverlauf-fuer-externe-quellen.md) | Täglicher Serverlauf für externe Terminquellen | angenommen | 2026-10-31 | Scheduler als eingehender Adapter in derselben Anwendung (03:00 Europe/Berlin); geplanter und sofortiger Lauf teilen einen Use Case; Fehler je Haushalt isoliert |
| [0006](../docs/adr/0006-ical4j-fuer-die-ics-auswertung.md) | ICS-Auswertung mit iCalendar-Bibliothek (ical4j) | angenommen | 2026-10-31 | `org.mnode.ical4j:ical4j` 4.4.0 ausschließlich im ausgehenden Adapter; der Port bleibt bibliotheksfrei; Formatfehler ≠ Leerfall |
| [0007](../docs/adr/0007-telegram-benachrichtigung-als-eigener-kontext.md) | Telegram-Benachrichtigung als eigener Fach-Kontext | **vorgeschlagen** | 2026-11-01 | Kontext „Benachrichtigung" mit eigenem Fachmodell, Tabellen und Ports; liest die Termine über den In-Port des Müllkalender-Kontexts; Telegram-Transport nur als Adapter; 18:00-Uhr-Lauf als zweiter eingehender Adapter |

## Weitere Entscheidungen (ohne ADR, bewusst)

| Entscheidung | Begründung | Wird zu ADR, wenn … |
|--------------|------------|---------------------|
| PostgreSQL 17 | Transaktional, JSON-fähig, verfügbar; Flyway-Support | eine zweite Datenbank evaluiert wird |
| REST/JSON mit `ProblemDetail` | Standard, keine Zusatzverhandlung | ein öffentlicher API-Vertrag für Dritte entsteht |
| Vitest im Frontend | Angular-22-Standard, schnell | ein anderes Testwerkzeug nötig wird |
| Docker Compose für Development **und Betrieb** | gleiche Umgebung lokal und im Betrieb; Bau und Deploy laufen über `compose.yml` (`AGENTS.md` → Release) | ein Registry-Publish oder Continuous Deployment eingeführt wird |
| `open-in-view: false` | verhindert Lazy-Lazy-Lad-Fehler und N+1 | — (dauerhafte Festlegung) |
| Route-scoped Provider werden beim Verlassen der Route zerstört (`withAutoCleanupInjectors()`) | ein Feature liest beim erneuten Betreten erneut; ohne die Option überlebt die Instanz den Wegwechsel und `httpResource` feuert bei gleicher URL nicht (Anzeige zeigte den alten Stand) | ein Feature Zustand über Navigationswechsel hinweg halten muss |

> **Offen:** Mandantenfähigkeit im Sinne „Context pro Haushalt/Mieter" und
> Monorepo-Tooling (Nx?) — jeweils als ADR, sobald entschieden.

> **Offen (ADR 0007):** Wie eine Chat-Nachricht das Backend erreicht
> (HTTP-Webhook mit öffentlichem Endpunkt oder Long-Poll) und wie die
> Wiederholung einer fehlgeschlagenen Zustellung zeitversetzt ausgelöst wird.
> Beides verändert die Betriebsannahme (07) bzw. die Zahl der Zeitgeber — der
> Auftraggeber entscheidet, der ARC-Agent nicht.

**Hinweis:** Die Auswirkungen der ADRs stehen in den betroffenen Kapiteln
(03, 05, 06, 07, 08). Dieses Kapitel listet sie auf — es wiederholt sie
nicht.

## Wie Entscheidungen entstehen

1. Der **ARC-Agent** erkennt im Feature eine Architekturfrage und stellt
   sie als **Rückfrage** — er entscheidet nicht selbst.
2. Der Auftraggeber entscheidet; die Entscheidung wird als ADR festgehalten
   (`docs/adr/NNNN_<kurz>.md`).
3. Der ARC-Agent verlinkt die neue ADR in diesem Kapitel und zieht die
   Folgen in die betroffenen arc42-Dokumente nach.
