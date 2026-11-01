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
| **Architektur-Review** | Agent `architektur-reviewer` · Command `/review` · Skill `code-review-gate` | `OK` / `KORREKTUREN` | Der Reviewer ist **read-only**; er liefert einen ausführbaren **Korrekturauftrag**, den das Command an den Implementierer delegiert. Runde 1 vollständig (geschlossene Befundliste), Runden 2–3 nur offene `K`-Befunde + Regressionen — **max. 3 Runden insgesamt**, gezählt am Befund-Kommentar |
| **Abnahme (Test)** | Agent `kritischer-tester` · Command `/abnahme` · Skill `feature-testing` | `PASS` / `FAIL` | Der Tester schreibt fehlende Tests selbst (nur Testdateien). Bei `FAIL` genau **eine** Wiederholung, die nur die offenen Punkte prüft |

```text
Implementieren → Tests (feature-testing)
      → /review      → KORREKTUREN → delegieren → /review → OK
      → /abnahme     → FAIL → Befunde beheben → /abnahme → PASS
```

Beide Gates bekommen **frischen Kontext** und urteilen selbst — sie kennen
die Begründungen des Implementierers nicht. Die Urteile werden als
Issue-Kommentar protokolliert (`<!-- review: status=OK … -->`,
`<!-- abnahme: status=PASS … -->`). Der **Befundstand je Runde** steht im
Marker `<!-- review-findings: runde=<n> … -->` — er ist die Quelle der
Folgerunde, nicht der Chatverlauf.

## Skills (vendort)

Zwei Fremd-Skills sind eingecheckt, weil sie echtes Framework-Wissen liefern,
das nicht im Projekt steht. Sie werden geladen, sobald eine Änderung in den
jeweiligen Bereich geht:

| Bereich | Skills |
|---------|--------|
| Frontend | `angular-developer` (Angular 22: Signals, `httpResource`, Signal Forms, zoneless) · `vitest` (Vitest 5: Mocking, Coverage, Filter) |
| Architektur | `backend-ports-adapters` (Ports & Adapters, projektspezifisch) · `frontend-feature-architecture` (Steyer, projektspezifisch) |
| Design | `ux-design` (UX-Methode, Issue-Abschnitt `## UX`, Design-DNA) |
| Testen | `feature-testing` (Testebenen, AC-Abdeckung) |
| Review | `code-review-gate` (Kategorien, Severity, Korrekturauftrag) |

Die generischen Spring-Skills (`spring-boot`, `spring-web`, `spring-testing`
u. a.) sowie `spring-versions`, `angular-new-app` und `hexagonal-architecture`
wurden entfernt: sie enthielten keine projektspezifische Regel und lagen
doppelt im Katalog. Herkunft, Stand, Lizenz und Update-Befehle:
`skills/README.md`.

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
Lücken läuft die Schleife in die zuständige Runde zurück. Zwischen Runde 2
und Runde 3 steht die **ARC-Runde**.

Die Skill **`anforderung-loop`** enthält alle drei Runden, das Gate und das
Grilling; die Referenzen liegen in `references/`:

| Datei | Inhalt |
|-------|--------|
| `anforderung-loop/SKILL.md` | Die drei Runden, Gate-Vertrag, Epic/Phasen, Regeln |
| `anforderung-loop/references/grilling.md` | Angriffskategorien, Frageprotokoll, Stop-Kriterien |
| `anforderung-loop/references/gate.md` | Vollständigkeitscheckliste je Runde, Urteil `PASS`/`GAPS` |
| `anforderung-loop/references/technik-checkliste.md` | Kurzfassung der Konventionen für Runde 3 |

| Runde | Abschnitt | Grilling | Ergebnis |
|-------|-----------|----------|----------|
| 1 — Erfassen | `anforderung-loop` §Runde 1 | Verständnis sichern (Ziel, Kern, Verzicht) | Ziel, Kontext, Scope, Nicht-Ziele, Epic-/Phasen-Schnitt — **Issue wird angelegt** |
| 2 — Fachlich | `anforderung-loop` §Runde 2 | Regeln attackieren (Randfall, Rolle, Umkehr) | Rollen, Nutzerfluss, fachliche Regeln, prüfbare Akzeptanzkriterien, Fehlerfälle |
| 2a — UX | Agent `ux-designer` | — | **`## UX` im Issue**: Screens, Abläufe, Zustände, Texte — nach Design-DNA |
| 2b — Architektur | Agent `arc-agent` | — | **arc42 gepflegt**, Vorgaben für Runde 3, offene Architekturfragen |
| 3 — Technisch | `anforderung-loop` §Runde 3 | Bauplan attackieren (Widerspruch, Folge, Zeit) | Zielort, Datenmodell, API-Vertrag, UI, Dateiliste, Tests, Verify |

| Skill | Aufgabe |
|-------|---------|
| `anforderung-loop` | Der ganze Loop: Runden, Phasen-Übergang, Gate-Vertrag, Fortsetzen |
| `issue-formulierung` | Hausstil: Titel, Labels, Section-Reihenfolge, Verify-Block |
| `issue-github` | Issue anlegen, Body je Runde ersetzen, Protokoll kommentieren, finalisieren |

### ARC-Runde (Runde 2b)

Der ARC-Agent betrachtet die Architektur über das ganze Bild — Kontext,
Bausteine, Ports & Adapters, Feature-Grenzen, Daten, Laufzeit, Verteilung,
Querschnittskonzepte, Qualität, Risiken — und pflegt die
**arc42-Dokumentation** unter [`arc42/`](../arc42/):

| Dokument | Inhalt |
|----------|--------|
| `01_Einfuehrung_und_Ziele.md` | Zweck, Qualitätsziele, Stakeholder |
| `02_Randbedingungen.md` | Stack, Versionen, organisatorische Grenzen |
| `03_Kontextabgrenzung.md` | Fachlicher Kontext, bounded contexts, Scope |
| `04_Loesungsstrategie.md` | Technologieentscheidungen, Muster |
| `05_Bausteinsicht.md` | Bausteine und Beziehungen |
| `06_Laufzeitsicht.md` | Abläufe, Szenarien, Fehlerpfade |
| `07_Verteilungssicht.md` | Container, Ports, Konfiguration |
| `08_Querschnittliche_Konzepte.md` | Fehler, Validierung, Security, Transaktionen |
| `09_Architekturentscheidungen.md` | Verweis auf die ADRs unter `docs/adr/` |
| `10_Qualitaetsanforderungen.md` | Qualitätsszenarien mit Priorität |
| `11_Risiken_und_Schulden.md` | Risiken, technische Schulden |
| `12_Glossar.md` | Fach- und Architekturbegriffe |

Der Agent **entscheidet nichts**: Architekturfragen gehen als Rückfrage an
den Auftraggeber, Entscheidungen werden als ADR festgehalten.

### Was in arc42 gehört — und was nicht

arc42 ist die **Architektur-Dokumentation**, nicht das Issue und nicht der
Feature-Verlauf.

> **arc42 beschreibt, _wie_ das System gebaut ist und _warum_.**
> Das **Issue** beschreibt, _was_ ein Feature macht.

| In arc42 | Nicht in arc42 |
|----------|----------------|
| Bausteine, Ports, Adapter, Abhängigkeitsrichtungen | Feature-Abläufe im Detail, einzelne Nutzerflüsse |
| bounded contexts und ihre Grenzen | Akzeptanzkriterien, Testlisten |
| Laufzeitmuster, Jobs, externe Abrufe | „Zielbild"-Abschnitte pro Feature |
| Querschnittskonzepte (Security, Transaktionen, Fehler) | Issue-Status, Reifegrade, Sprint-Scope |
| Entscheidungen (als ADR, in 09 verlinkt) | API-Verträge im Detail |
| Risiken, technische Schulden | Wiederholungen aus 05, 08, 09 |
| Fachbegriffe (Glossar) | |

**Die Schwelle:** Ein Feature darf arc42 nur berühren, wenn es die
**Struktur** verändert. Nutzt ein Feature nur eine bestehende Struktur,
bleibt arc42 **unberührt** — das ist das Normalfall.

arc42 wächst damit mit der Zahl der **architektonischen Entscheidungen**,
nicht mit der Zahl der Features.

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
/review <Issue-Nr>               # Architektur-Gate: review → Korrekturen delegieren → review
/abnahme <Issue-Nr>              # Test-Gate: adversarial Prüfung der Akzeptanzkriterien
/pr <Issue-Nr>                   # committen, branch pushen, PR gegen main eröffnen
/issue-check <Issue-Nummer>      # bestehendes Issue prüfen und korrigieren
/issue-github <Issue-Nummer>     # Issue anlegen, aktualisieren, finalisieren
```

Ein Einstieg, kein Command pro Runde. `/anforderung` nimmt entweder eine
**Idee** (neues Issue, Start bei Runde 1) oder eine **Issue-Nummer**
(Fortsetzen — der Zustandsmarker im Body verrät den Stand).

### Branch, Commit, PR

Ein Feature = ein Branch = ein Issue = ein PR. Der Branch entsteht frisch
aus `main` (`feature/<issue-nr>-<kurzname>`), committet wird **erst nach
bestandenen Gates**, gepusht wird nur grün, und der PR entsteht mit
`.github/pull_request_template.md`. CI (`.github/workflows/ci.yml`) prüft
dasselbe bei jedem PR — der PR ist bei rotem CI nicht mergebar. Details in
`AGENTS.md` → „Branch, Commit, Push und PR".

### Ablauf

```text
Rohidee
  │
  ├─ Runde 1 (Erfassen) ──────▶ Grilling ─▶ Gate runde=1
  │        ▲                              │ GAPS
  │        └──────────────────────────────┘
  │                            └──▶ "Soll ich mit Runde 2 weitermachen?"
  ├─ Runde 2 (Fachlich) ──────▶ Grilling ─▶ Gate runde=2
  │        ▲                              │ GAPS
  │        └──────────────────────────────┘
  │                            └──▶ "Soll ich mit Runde 3 weitermachen?"
  ├─ Runde 3 (Technisch) ─────▶ Grilling ─▶ Gate runde=3
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
