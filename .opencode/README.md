# .opencode — Projekt-Tools für OpenCode

Projektlokale Skills und Commands für HouseholdManager.

## Kontext-Disziplin

Gearbeitet wird mit einem kleinen lokalen Modell (Qwen3.8-27B, 256k Kontext).
Deshalb gilt in diesem Projekt: **Grep vor Read, Ausgaben kürzen, lange
Läufe in Logdateien, ein Schritt pro Zug, Skill einmal laden.** Die
vollständigen Regeln stehen in `AGENTS.md` (Abschnitt „Kontext-Disziplin") —
sie werden mit jedem Turn geladen und deshalb kurz gehalten.

Für einen Kontextwechsel: Stand nach `.opencode/state.md` schreiben
(Template: `.opencode/state.md.template`), Session neu starten, dort
weitermachen.

## Testen, Review & Abnahme (Gates)

Ein Feature gilt erst als fertig, wenn **beide Gates** bestanden sind
(`AGENTS.md` → „Definition of Done").

| Gate | Werkzeug | Urteil | Korrigiert |
|------|----------|--------|-----------|
| **Architektur-Review** | Agent `architektur-reviewer` · Command `/review` · Skill `code-review-gate` | `OK` / `KORREKTUREN` | Der Reviewer ist **read-only**; er liefert einen ausführbaren **Korrekturauftrag**, den das Command an den Implementierer delegiert — dann Review erneut (max. 3 Runden) |
| **Abnahme (Test)** | Agent `kritischer-tester` · Command `/abnahme` · Skill `feature-testing` | `PASS` / `FAIL` | Der Tester schreibt fehlende Tests selbst (nur Testdateien) |

```text
Implementieren → Tests (feature-testing)
      → /review      → KORREKTUREN → delegieren → /review → OK
      → /abnahme     → FAIL → Befunde beheben → /abnahme → PASS
```

Beide Gates bekommen **frischen Kontext** und urteilen selbst — sie kennen
die Begründungen des Implementierers nicht. Die Urteile werden als
Issue-Kommentar protokolliert (`<!-- review: status=OK … -->`,
`<!-- abnahme: status=PASS … -->`).

## Stack-Skills (vendort)

Für den Stack des Projekts sind offizielle Skills eingecheckt — sie werden
geladen, sobald eine Änderung in den jeweiligen Bereich geht:

| Bereich | Skills |
|---------|--------|
| Frontend | `angular-developer` (Angular 22: Signals, `httpResource`, Signal Forms, zoneless, Vitest) · `angular-new-app` · `vitest` (Vitest 5: Mocking, Coverage, Filter) |
| Backend | `spring-boot` · `spring-framework` · `spring-data-jpa` · `spring-flyway` · `spring-web` · `spring-security` · `spring-testing` · `spring-versions` |
| Architektur | `hexagonal-architecture` (allgemeine Methodik) · `backend-ports-adapters` (projektspezifisch) · `frontend-feature-architecture` (Steyer, projektspezifisch) |
| Testen | `feature-testing` (projektspezifisch: Testebenen, AC-Abdeckung) |
| Review | `code-review-gate` (projektspezifisch: Kategorien, Severity, Korrekturauftrag) |

Herkunft, Stand, Lizenz und Update-Befehle: `skills/README.md`.

> **Achtung Versionsangaben:** `spring-versions` nennt Stand 2026-04-02 und
> damit Boot 4.0.5. In diesem Projekt läuft **Boot 4.1.2** — maßgeblich ist
> `backend/pom.xml`, nicht die Skill.

## Architektur

| Bereich | Muster | Entscheid |
|---------|--------|-----------|
| Backend | Ports & Adapters (hexagonal) | [ADR 0001](../docs/adr/0001-backend-ports-and-adapters.md) |
| Frontend | Feature-basiert (Manfred Steyer) | [ADR 0002](../docs/adr/0002-frontend-feature-architecture.md) |

Die Frontend-Grenzen werden von **Sheriff** maschinell geprüft
(`frontend/sheriff.config.ts`, `npm run sheriff`).

## Anforderungs-Loop

Aus einer groben Idee entsteht in drei Runden ein Issue, das ein Agent ohne
Rückfragen umsetzen kann. Jede Runde hat ein Grilling und ein Gate; bei
Lücken läuft die Schleife in die zuständige Runde zurück.

| Runde | Skill | Grilling | Ergebnis |
|-------|-------|----------|----------|
| 1 — Erfassen | `anforderung-intake` | Verständnis sichern (Ziel, Kern, Verzicht) | Ziel, Kontext, Scope, Nicht-Ziele, Epic-/Phasen-Schnitt — **Issue wird angelegt** |
| 2 — Fachlich | `anforderung-fachlich` | Regeln attackieren (Randfall, Rolle, Umkehr) | Rollen, Nutzerfluss, fachliche Regeln, prüfbare Akzeptanzkriterien, Fehlerfälle |
| 3 — Technisch | `anforderung-technisch` | Bauplan attackieren (Widerspruch, Folge, Zeit) | Zielort, Datenmodell, API-Vertrag, UI, Dateiliste, Tests, Verify |

| Skill | Aufgabe |
|-------|---------|
| `anforderung-loop` | Orchestrierung: Runden, Phasen-Übergang, Gate-Vertrag, Fortsetzen |
| `anforderung-gate` | Vollständigkeitscheckliste je Runde, Urteil `PASS`/`GAPS` |
| `issue-formulierung` | Hausstil: Titel, Labels, Section-Reihenfolge, Verify-Block |
| `issue-github` | Issue anlegen, Body je Runde ersetzen, Protokoll kommentieren, finalisieren |

### Arbeitsstand: das GitHub-Issue

**Es gibt keine lokale Draft-Datei.** Das Issue trägt den Arbeitsstand:

| Element | Ort |
|---------|-----|
| aktueller Issue-Text | Issue-Body, nach jeder Runde **ersetzt** |
| Zustand (`runde`, `status`) | HTML-Kommentar oben im Body |
| Grilling-Protokoll | ein Kommentar je Runde |
| „in Arbeit"-Signal | `[WIP]`-Präfix im Titel |

Damit ist der Loop von jeder Session und jedem Rechner fortsetzbar, und die
Runden sind unterwegs sichtbar statt erst am Ende.

Repo: `Sadeeg/HouseholdManager`.

### Grilling

Der Kern des Loops ist nicht Lückensammlung, sondern **Verständnissicherung**:
die Anforderung wird in jeder Runde kritisch hinterfragt — **eine Frage pro
Zug**, mit Kategorie, Optionen und Empfehlung. Das Grilling endet erst, wenn
sechs Stop-Kriterien erfüllt sind:

1. Ziel bestätigt
2. Keine offene Frage mit Tragweite
3. Entscheidungen entschieden
4. Durchschau-Test bestanden
5. Kern und Verzicht benannt
6. Keine Beobachtung ohne Folge

Methode, Angriffskategorien und Protokollformat:
`skills/anforderung-loop/references/grilling.md`.

### Commands

```text
/anforderung <Rohidee|Issue-Nr>  # der Loop: 3 Runden, Grilling, Phasen-Übergänge
/issue-check <Issue-Nummer>      # bestehendes Issue prüfen und korrigieren
/issue-github <Issue-Nummer>     # Issue anlegen, aktualisieren, finalisieren
```

Ein Einstieg, kein Command pro Runde. `/anforderung` nimmt entweder eine
**Idee** (neues Issue, Start bei Runde 1) oder eine **Issue-Nummer**
(Fortsetzen — der Zustandsmarker im Body verrät den Stand).

### Ablauf

```text
Rohidee
  │
  ├─ Runde 1 (anforderung-intake) ─▶ Grilling ─▶ Gate runde=1
  │        ▲                              │ GAPS
  │        └──────────────────────────────┘
  │                            └──▶ "Soll ich mit Runde 2 weitermachen?"
  ├─ Runde 2 (anforderung-fachlich) ▶ Grilling ─▶ Gate runde=2
  │        ▲                              │ GAPS
  │        └──────────────────────────────┘
  │                            └──▶ "Soll ich mit Runde 3 weitermachen?"
  ├─ Runde 3 (anforderung-technisch) ▶ Grilling ─▶ Gate runde=3
  │        ▲                              │ GAPS
  │        └──────────────────────────────┘
  │
  ├─▶ Gate final ──▶ PASS ? READY_FOR_AGENT : BLOCKED
  │
  └─▶ Issue finalisieren: [WIP] raus, Labels, Typ, Abschlusskommentar
```

**Phasen-Übergang:** Am Ende jeder Runde wird **eine** Frage gestellt:
„Soll ich mit Runde N weitermachen?" — A: weiter, B: pausieren (das Issue
bleibt `[WIP]`, Fortsetzung später über `/anforderung <Issue-Nr>`), C: eigene
Richtung. Die Frage kommt **nach** dem Gate: ein `GAPS` wird behoben, bevor
überhaupt gefragt wird.

### Hausstil

`skills/issue-formulierung/references/hausstil.md` — Title-Formate,
Label-Vokabular, Section-Reihenfolge, Vorlagen, Verify-Kommandos,
Phrasen-Regeln.

> **Stand HouseholdManager:** Das Repo ist neu — es gibt nur die
> GitHub-Default-Labels, keinen Code und keine lauffähigen Verify-Kommandos.
> Epic und Phase stehen deshalb im Titelpräfix, Labels werden nur gesetzt,
> wenn sie im Repo existieren, und Verify-Kommandos werden mit dem ersten
> Feature in `AGENTS.md` eingetragen.

## Weitere Tools

- `AGENTS.md` (im Repo-Root) — Projektkonventionen, Verify-Kommandos,
  Release-Ablauf, Gotchas. Wird mit dem ersten Feature gefüllt.
- Der Anforderungs-Loop ist die verbindliche Arbeitsweise für Issues in
  diesem Repo.
