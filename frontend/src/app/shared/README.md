# `shared/`

Code, der von **mehreren Features** benutzt wird und **kein Fachwissen über
ein einzelnes Feature** besitzt (Regel 1 der Architektur-Matrix von Manfred
Steyer).

- Kommt hier etwas hinein, das nur von einem einzigen Feature benutzt wird,
  gehört es in `features/<feature>/`.
- Kommt hier etwas hinein, das mit dem Backend spricht, gehört es nicht hier
  hin — Backend-Zugriff liegt in der Daten-Schicht (`data`) des jeweiligen
  Features.
- Was hier liegt, muss von jedem Feature genutzt werden können, ohne dass
  Feature-Kontext mitgeliefert wird.
- **Ausnahme:** Eine dumb Component darf einen Dienst aus `core/` nutzen, der
  selbst mit dem Backend spricht — der `UserPicker` greift so auf
  `core/users/UserSearch` zu. Der Backend-Zugriff bleibt damit in `core/`
  (app-weit, eine Instanz) und nicht in `shared/`.

Aktuell:

| Ordner | Inhalt |
|--------|--------|
| `components/page-header/` | Kopfbereich einer Seite (Titel, optionaler Untertitel) — dumb, ohne Fachwissen |
| `components/user-picker/` | Suchfeld für Personen im Nutzerverzeichnis — dumb, Auswahl per Output |
| `components/confirm-dialog/` | Bestätigungsdialog — dumb, zwei Aktionen |
| `components/markdown/` | Markdown-Ausgabe — dumb, ohne Fachwissen |
