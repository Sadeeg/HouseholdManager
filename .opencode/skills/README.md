# Skills — Herkunft und Lizenz

Die Skills in diesem Ordner sind **vendored** (fest im Repo eingecheckt), damit
das Projekt ohne Netzwerkzugriff und ohne nachinstallierte Pakete arbeitet.

| Skill | Quelle | Stand | Lizenz |
|-------|--------|-------|--------|
| `angular-developer/` | [github.com/angular/skills](https://github.com/angular/skills) | `5d929e5` (2026-10-29) | MIT, Copyright Google LLC |
| `angular-new-app/` | [github.com/angular/skills](https://github.com/angular/skills) | `5d929e5` (2026-10-29) | MIT, Copyright Google LLC |
| `spring-boot/` | [github.com/rynr/spring-skills](https://github.com/rynr/spring-skills) | `fe9d704` (2026-04-02) | MIT |
| `spring-framework/` | dito | dito | MIT |
| `spring-data-jpa/` | dito | dito | MIT |
| `spring-flyway/` | dito | dito | MIT |
| `spring-security/` | dito | dito | MIT |
| `spring-testing/` | dito | dito | MIT |
| `spring-versions/` | dito | dito | MIT |
| `spring-web/` | dito | dito | MIT |
| `hexagonal-architecture/` | [github.com/affaan-m/ECC](https://github.com/affaan-m/ECC) | Stand 2026-10-29 | MIT |
| `vitest/` | [github.com/antfu/skills](https://github.com/antfu/skills) | `8886fe2` (2026-10-23) | MIT |

## Eigene Skills des Projekts

Diese Skills sind **projektspezifisch** und werden hier gepflegt:

| Skill | Inhalt |
|-------|--------|
| `backend-ports-adapters/` | Ports-&-Adapters-Aufbau des Backends (Schichten, Ports, Mapping, Transaktionen, Tests je Schicht) — Entscheid: [ADR 0001](../../docs/adr/0001-backend-ports-and-adapters.md) |
| `frontend-feature-architecture/` | Feature-Architektur nach Manfred Steyer (Architektur-Matrix, Ordnerregeln, Smart/Dumb, Sheriff-Tags) — Entscheid: [ADR 0002](../../docs/adr/0002-frontend-feature-architecture.md) |
| `feature-testing/` | Teststrategie: Testebene je Fehler, AC-Abdeckung, Definition von „vollständig getestet", Anti-Patterns |
| `code-review-gate/` | Review-Doktrin: 7 Kategorien, Severity, OK/KORREKTUREN, Format des Korrekturauftrags |
| `anforderung-*`, `issue-formulierung`, `issue-github` | Anforderungs-Loop und GitHub-Schreibvorgänge (siehe `.opencode/README.md`) |

## Agenten

| Agent | Aufgabe |
|-------|---------|
| `.opencode/agents/arc-agent.md` | ARC-Runde (zwischen fachlicher und technischer Runde): prüft die Architektur über das ganze Bild, pflegt `arc42/`, liefert Vorgaben für Runde 3, stellt Architekturfragen — er entscheidet nichts |
| `.opencode/agents/architektur-reviewer.md` | Architektur-Gate: prüft read-only gegen `AGENTS.md`, ADRs und Skills, urteilt `OK`/`KORREKTUREN`, liefert den ausführbaren Korrekturauftrag (wird delegiert, nicht selbst umgesetzt) |
| `.opencode/agents/kritischer-tester.md` | Test-Gate: prüft adversarial gegen die Akzeptanzkriterien, schreibt fehlende Tests, urteilt `PASS`/`FAIL` |

## Aktualisieren

```bash
git clone --depth 1 https://github.com/angular/skills /tmp/angular-skills
git clone --depth 1 https://github.com/rynr/spring-skills /tmp/spring-skills

cp -r /tmp/angular-skills/angular-developer /tmp/angular-skills/angular-new-app .opencode/skills/
for s in spring-boot spring-framework spring-data-jpa spring-flyway \
         spring-security spring-testing spring-versions spring-web; do
  cp -r /tmp/spring-skills/skills/$s .opencode/skills/
done
```

Danach Commit-SHA und Datum in der Tabelle oben aktualisieren.

## Hinweise zu diesem Projekt

- Die Angular-Skills zielen auf die **aktuelle** Angular-Version (v22+);
  `angular-developer` nennt Angular 22 als Mindestversion für Signal Forms.
- `spring-versions` war zuletzt am 2026-04-02 aktualisiert und nennt daher
  Boot 4.0.5 als „current GA". **In diesem Projekt läuft Spring Boot 4.1.2** —
  die Versionsangaben der Skill sind nicht die Projekt-Version. Maßgeblich ist
  `backend/pom.xml`.
- `spring-boot-docker-compose` (Dev Services), Testcontainers 2.x
  (`org.testcontainers:testcontainers-postgresql`) und Jackson 3
  (`tools.jackson.*`) sind Boot-4-Sonderheiten — siehe
  `.opencode/skills/anforderung-technisch/references/technik-checkliste.md`.
- `hexagonal-architecture` ist sprachübergreifend und allgemein gehalten. Die
  **projektspezifische** Fassung (Pakete, Teststrategie, Fallen) steht in
  `backend-ports-adapters`.

## Eigene Skills des Projekts

`anforderung-*`, `issue-formulierung` und `issue-github` sind projektspezifisch
und werden hier gepflegt (siehe `.opencode/README.md`).
