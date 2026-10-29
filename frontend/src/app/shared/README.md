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

Aktuell:

| Ordner | Inhalt |
|--------|--------|
| `components/page-header/` | Kopfbereich einer Seite (Titel, optionaler Untertitel) — dumb, ohne Fachwissen |
