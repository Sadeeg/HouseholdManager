# Abschluss-Checkliste — vor „fertig"

Für den **Implementierer**, nicht für den Test-Gate-Agent. Der
`kritischer-tester` hat diese Schritte bereits in seinem Ablauf.

1. Matrix bauen (AC → Test).
2. Lücken mit Tests schließen (nur Testdateien).
3. Alle Tests + Lint + Sheriff laufen lassen (`AGENTS.md §Verify`).
4. **Bei UI-Anteil: den manuellen Pfad einmal im Browser durchspielen.**
   Gibt es einen Einstieg? Ist der Stack erreichbar? Sind Daten vorhanden?
   Sind alle Aktionen sichtbar? Ein AC ohne Einstieg ist nicht erfüllt,
   nur behauptet.
5. **Bei UI-Anteil: Sichtprüfung** (Screenshot des echten SCSS) für die
   Ansichten, die geändert wurden — Layout ist nicht testbar.
6. **Den kritischen Tester laufen lassen** (`@kritischer-tester` oder
   `/abnahme`) — er urteilt, nicht du.
