# 9. Architekturentscheidungen

Die **ADR** unter [`docs/adr/`](../docs/adr/) sind die Quelle der Wahrheit.
Dieses Kapitel ist das Inhaltsverzeichnis und die Begründung je
Entscheidung — **keine** Dublette.

| ADR | Titel | Status | Datum | Auswirkung |
|-----|-------|--------|-------|------------|
| [0001](../docs/adr/0001-backend-ports-and-adapters.md) | Backend: Ports-and-Adapters-Architektur | angenommen | 2026-10-29 | Fachkern ohne Framework, Adapter an den Rändern, Tests je Schicht |
| [0002](../docs/adr/0002-frontend-feature-architecture.md) | Frontend: Feature-basierte Architektur (Manfred Steyer) | angenommen | 2026-10-29 | Feature = bounded context, Matrix-Schichten, Grenzen per Sheriff |

## Weitere Entscheidungen (ohne ADR, bewusst)

| Entscheidung | Begründung | Wird zu ADR, wenn … |
|--------------|------------|---------------------|
| PostgreSQL 17 | Transaktional, JSON-fähig, verfügbar; Flyway-Support | eine zweite Datenbank evaluiert wird |
| REST/JSON mit `ProblemDetail` | Standard, keine Zusatzverhandlung | ein öffentlicher API-Vertrag für Dritte entsteht |
| Vitest im Frontend | Angular-22-Standard, schnell | ein anderes Testwerkzeug nötig wird |
| Docker Compose für Development | gleiche Umgebung lokal und im Betrieb | Continuous Deployment eingeführt wird |
| `open-in-view: false` | verhindert Lazy-Lazy-Lad-Fehler und N+1 | — (dauerhafte Festlegung) |

> **Offen:** Mandantenfähigkeit (ein Service pro Context oder Context pro
> Haushalt), Authentifizierungskonzept, Monorepo-Tooling (Nx?) — jeweils als
> ADR, sobald entschieden.

## Wie Entscheidungen entstehen

1. Der **ARC-Agent** erkennt im Feature eine Architekturfrage und stellt
   sie als **Rückfrage** — er entscheidet nicht selbst.
2. Der Auftraggeber entscheidet; die Entscheidung wird als ADR festgehalten
   (`docs/adr/NNNN_<kurz>.md`).
3. Der ARC-Agent verlinkt die neue ADR in diesem Kapitel und zieht die
   Folgen in die betroffenen arc42-Dokumente nach.
