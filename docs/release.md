# Release

Ausgelagert aus `AGENTS.md`, damit die Regeldatei kurz bleibt. Gilt
unverändert.

Ein Feature ist ausgeliefert, wenn sein PR gemergt ist. Der Ablauf:

1. **Versions-Bump** — `backend/pom.xml` und `frontend/package.json` auf die
   neue Version setzen (in beiden dieselbe Nummer).
2. **Tag** — `git tag -a v<version> -m "<version>"`, dann
   `git push origin v<version>`.
3. **Bau** — `docker compose -f compose.yml build` baut die Images
   `householdmanager-backend` und `householdmanager-frontend`.
4. **Deploy** — auf dem Zielsystem `docker compose up -d --build`; der Stack
   bringt Postgres (5432), Backend (8080) und Frontend (4200) hoch und startet
   das Backend erst, wenn die Datenbank gesund ist.
5. **Rollback** — auf den vorherigen Tag zurücksetzen
   (`git checkout v<vorherige Version>`), erneut bauen und deployen.

Ein Publish in eine Container-Registry ist nicht eingerichtet: die Images
werden auf dem Zielsystem gebaut. Nach einem Deploy immer mit `--build`
starten — ohne baut Docker das zuletzt vorhandene Image nicht neu.
