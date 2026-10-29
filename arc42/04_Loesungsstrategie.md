# 4. Lösungsstrategie

## Kurzfassung

Das System ist als **Webanwendung** aufgebaut: ein Spring-Boot-Service
liefert eine REST-Schnittstelle, eine Angular-SPA bedient sie. Beide Seiten
sind in einem Repository enthalten (Modulith mit getrennten Builds).

## Entscheidungen im Überblick

| Thema | Entscheidung | Begründung | ADR |
|-------|--------------|------------|-----|
| Backend-Architektur | **Ports & Adapters** (hexagonal) | Fachlogik ohne Framework testbar, Persistenz austauschbar | [ADR 0001](../docs/adr/0001-backend-ports-and-adapters.md) |
| Frontend-Architektur | **Feature-basiert** (Manfred Steyer) mit Architektur-Matrix | Feature = bounded context, Grenzen per Sheriff prüfbar | [ADR 0002](../docs/adr/0002-frontend-feature-architecture.md) |
| Datenbank | PostgreSQL 17, Migrationen über Flyway | Schema-Historie im Repo, `ddl-auto: validate` erzwingt Passgenauigkeit | — |
| API-Stil | REST/JSON, `ProblemDetail` für Fehler | Standard, keine zusätzliche Verhandlung | — |
| Zustand im Frontend | Signals, `httpResource` für Serverdaten | Angular-22-Standard, zoneless-tauglich | — |
| Modulgrenzen | Sheriff als ESLint-Plugin | Grenzen maschinell prüfen, nicht nur im Review | — |
| Teststrategie | Tests je Schicht, Testcontainers für die DB | Fehler auf der tiefsten Ebene finden | — |

## Muster

| Muster | Ort |
|--------|-----|
| Ports & Adapters | Backend: `domain` / `application` / `adapters` |
| Ports & Adapters | Frontend: `features/<f>/{model,data,ui}` |
| Composition Root | `config/ApplicationConfig`, Spring-Konfiguration |
| Feature-Slices / vertical slices | Ein Issue = ein Branch = ein Feature |
| Gate-Pattern | Subagenten `architektur-reviewer` und `kritischer-tester` |

## Technologieentscheidungen im Detail

### Warum Ports & Adapters statt Schichtenarchitektur?

Eine Schichtenarchitektur (Controller → Service → Repository) koppelt die
Fachlogik an Spring und JPA: Jede Regel braucht dann eine Datenbank zum
Testen, und ein Persistenzwechsel reißt die Fachlogik mit. Die hexagonale
Variante legt die Ports in die Anwendung und die Adapter an den Rand —
der Preis ist eine höhere Zahl an Klassen (Fachmodell und Persistenzmodell
sind getrennt).

### Warum Feature-Ordner statt eines flachen `shared/`-Baums?

Ein flacher Baum wächst unkontrolliert, und es bleibt unklar, welcher Code
zu welchem Feature gehört. Feature-Ordner machen die Zugehörigkeit am
Pfad ablesbar; die Matrix-Subordner (`model`, `data`, `ui`) verhindern,
dass ein dumb Component den Datenzugriff anfasst.

> **Entschieden (ADR 0003):** Fach-Contexte sind Module derselben
> Anwendung, kein eigener Service je Context.
> **Offen:** Mandantenfähigkeit (Context pro Haushalt statt pro Anwendung).
