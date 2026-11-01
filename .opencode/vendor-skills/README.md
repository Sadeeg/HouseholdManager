# vendor-skills — bewusst nicht registriert

Hier liegen Skills, die **nicht** im Katalog stehen und deshalb nie
automatisch geladen werden. Sie sind Archiv, keine Empfehlung.

| Skill | Warum nicht im Katalog |
|-------|------------------------|
| `hexagonal-architecture` | allgemeine Methodik; die projektspezifische Fassung steht in `../skills/backend-ports-adapters/` (Konzept, Pakete, Tests, Fallen) |
| `spring-versions` | Versionsfragen beantwortet `backend/pom.xml` / `frontend/package.json` |
| `spring-boot` · `spring-framework` · `spring-data-jpa` · `spring-flyway` · `spring-web` · `spring-security` · `spring-testing` | generische API-Referenz; die Projektregeln stehen in `AGENTS.md` und `../skills/backend-ports-adapters/` |
| `angular-new-app` | Projekt existiert; Anleitung zum Anlegen einer neuen App ist gegenstandslos |

**Zurückholen:** Ordner nach `../skills/` verschieben — er erscheint dann
wieder im Katalog. Bitte nur, wenn der Inhalt nachweislich fehlt (Regel in
`../skills/README.md`).

**Warum das überhaupt zählt:** Jeder Katalog-Eintrag kostet in *jedem* Turn
Kontext, jede Skill-Datei kostet bei jeder Ladung. Generische Zweitquellen
zur selben Frage erzeugen außerdem widersprüchliche Autorität — im Konflikt
gewinnt der zuletzt geladene Text, nicht die bessere Regel.
