# Skills — Herkunft und Pflege

Die Skills in diesem Ordner sind **projektspezifisch** und werden hier
gepflegt. Jeder Eintrag im Katalog kostet in *jedem* Turn Kontext — deshalb
gilt der Vertrag unten.

| Skill | Inhalt |
|-------|--------|
| `anforderung-loop/` | Der Anforderungs-Loop in 3 Runden (Erfassen → fachlich → technisch) mit Grilling und Gate. Referenzen: `grilling.md`, `gate.md`, `technik-checkliste.md` |
| `issue-formulierung/` | Hausstil: Titel, Labels, Section-Reihenfolge, Verify-Block. `references/hausstil.md` |
| `issue-github/` | Issue anlegen, Body je Runde ersetzen, Protokoll kommentieren, finalisieren |
| `backend-ports-adapters/` | Ports-&-Adapters-Aufbau des Backends (Schichten, Ports, Mapping, Transaktionen, Tests je Schicht) — Entscheid: [ADR 0001](../../docs/adr/0001-backend-ports-and-adapters.md) |
| `frontend-feature-architecture/` | Feature-Architektur nach Manfred Steyer (Architektur-Matrix, Ordnerregeln, Smart/Dumb, Sheriff-Tags) — Entscheid: [ADR 0002](../../docs/adr/0002-frontend-feature-architecture.md) |
| `feature-testing/` | Teststrategie: Testebene je Fehler, AC-Abdeckung, Definition von „vollständig getestet", Anti-Patterns, Angular-22-Testfallen |
| `code-review-gate/` | Review-Doktrin: 8 Kategorien, Severity, OK/KORREKTUREN, Format des Korrekturauftrags, Proben für Sichtbarkeit/Erreichbarkeit |
| `ux-design/` | UX-Methode: Ablauf, Screens, Zustände, Sichtprüfung (Layout ist nicht testbar), Issue-Abschnitt `## UX`, Design-DNA |
| `angular-developer/` | Angular 22 (Signals, `httpResource`, Signal Forms, zoneless) — vendort, MIT, [github.com/angular/skills](https://github.com/angular/skills), Stand `5d929e5` (2026-10-29) |
| `vitest/` | Vitest 5 (Mocking, Coverage, Filter) — vendort, MIT, [github.com/antfu/skills](https://github.com/antfu/skills), Stand `8886fe2` (2026-10-23) |

## Skill-Vertrag

1. `description` ≤ 130 Zeichen, beginnt mit „Wann laden, was liefert es".
   Der Katalog steht in **jedem** Turn — die Beschreibung ist der teuerste
   Text des Repos.
2. `SKILL.md` ≤ 6 KB. Alles darüber gehört in `references/` und wird nur
   geladen, wenn genau diese Frage gestellt wird.
3. Kein Skill wiederholt `AGENTS.md` — er verweist darauf. `AGENTS.md`
   enthält die Entscheidungen, die Skill die Umsetzung.
4. Verify-Befehle stehen **einmal**: `AGENTS.md §Verify`.
5. Kein Skill enthält Beispielcode, der nicht projekttypisch ist.
6. Neue generische Fremd-Skills werden **nicht** eingecheckt. Projektregeln
   gehören in die projektspezifischen Skills.

## Entfernte Fremd-Skills (2026-11-01)

Diese Skills lagen byte-gleich in `.opencode/skills/` **und**
`.opencode/vendor-skills/` und wurden doppelt im Katalog gezählt. Sie sind
entfernt, weil sie keine projektspezifische Regel enthielten:

| Skill | Grund |
|-------|-------|
| `spring-boot` · `spring-framework` · `spring-data-jpa` · `spring-flyway` · `spring-web` · `spring-security` · `spring-testing` | generische API-Referenz; die Projektregeln stehen in `AGENTS.md` und `backend-ports-adapters` |
| `spring-versions` | Versionsfragen beantwortet `backend/pom.xml` / `frontend/package.json` — der Skill nannte Boot 4.0.5, das Projekt läuft 4.1.2 |
| `angular-new-app` | Projekt existiert; eine Anleitung zum Anlegen einer neuen App ist gegenstandslos |
| `hexagonal-architecture` | allgemeine Methodik; die projektspezifische Fassung (Schichten, Ports, Adapter, Composition Root, Tests je Ebene, Fallen) steht vollständig in `backend-ports-adapters` |

**Wiederherstellung:** aus dem Upstream-Repo (siehe Herkunftstabelle oben)
nach `.opencode/skills/` kopieren. Nur, wenn der Inhalt nachweislich fehlt.

## Aktualisieren

```bash
git clone --depth 1 https://github.com/angular/skills /tmp/angular-skills
git clone --depth 1 https://github.com/antfu/skills /tmp/antfu-skills

cp -r /tmp/angular-skills/angular-developer .opencode/skills/
cp -r /tmp/antfu-skills/vitest .opencode/skills/
```

Danach Commit-SHA und Datum in der Tabelle oben aktualisieren.

## Hinweise zu diesem Projekt

- Die Angular-Skills zielen auf die **aktuelle** Angular-Version (v22+);
  `angular-developer` nennt Angular 22 als Mindestversion für Signal Forms.
- Boot-4-Sonderheiten (Dev Services, Testcontainers 2.x
  `org.testcontainers:testcontainers-postgresql`, Jackson 3 `tools.jackson.*`)
  stehen in `AGENTS.md §Boot-4-Fallen` und in
  `anforderung-loop/references/technik-checkliste.md`.

## Agenten

| Agent | Aufgabe |
|-------|---------|
| `.opencode/agents/arc-agent.md` | ARC-Runde (zwischen fachlicher und technischer Runde): prüft die Architektur über das ganze Bild, pflegt `arc42/`, liefert Vorgaben für Runde 3, stellt Architekturfragen — er entscheidet nichts |
| `.opencode/agents/ux-designer.md` | UX-Runde (zwischen fachlicher und technischer Runde): entwirft die Oberfläche nach der Design-DNA und schreibt den Abschnitt `## UX` in den Issue-Body |
| `.opencode/agents/architektur-reviewer.md` | Architektur-Gate: prüft read-only gegen `AGENTS.md`, ADRs und Skills, urteilt `OK`/`KORREKTUREN`, liefert den ausführbaren Korrekturauftrag (wird delegiert, nicht selbst umgesetzt) |
| `.opencode/agents/kritischer-tester.md` | Test-Gate: prüft adversarial gegen die Akzeptanzkriterien, schreibt fehlende Tests, urteilt `PASS`/`FAIL` |
| `.opencode/agents/backend-dev.md` | Backend-Implementierer: setzt Backend-Teile um, Bericht statt Dateidump |
| `.opencode/agents/frontend-dev.md` | Frontend-Implementierer: setzt Frontend-Teile um, Bericht statt Dateidump |
