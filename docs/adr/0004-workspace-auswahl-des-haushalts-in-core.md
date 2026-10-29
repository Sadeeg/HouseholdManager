# ADR 0004 — Workspace-Auswahl des Haushalts in core/

- **Status:** angenommen
- **Datum:** 2026-10-29
- **Entscheidung:** Die Auswahl des aktuellen Haushalts (Workspace) ist **app-weiter Zustand in `core/`**. Alle Features lesen ausschließlich den gewählten Haushalt; keine Ansicht mischt Haushalte.

## Kontext

Issue #1, AC23: „Die Haushaltsauswahl steht auf App-Ebene; nach dem Wechsel
zeigt jede Ansicht ausschließlich die Daten des neu gewählten Haushalts, ohne
dass die Seite neu geladen werden muss." Der UX-Entwurf verortet den
Workspace-Wähler im App-Rahmen und definiert ihn als „geteiltes Element des
App-Rahmens, kein Bestandteil eines Features". Nach ADR 0002 ist `core/`
der Ort für genau-eine-Instanz-Zustand der ganzen App; `shared/` wäre für
wiederverwendbare, zustandslose Bausteine falsch.

## Entscheidung

- `core/workspace/` (Schema-Gegenstück zu `core/auth`, `core/config`) hält:
  die Liste der Haushalte, in denen die angemeldete Person ernannt ist, und
  den gewählten Haushalt als Signal.
- Die Auswahl wird beim nächsten Besuch wiederhergestellt; der
  Wiederherstellungsmechanismus (z. B. localStorage) ist
  Implementierungsdetail der technischen Runde.
- Features und ihre Data-Services lesen **nur** die gewählte Haushaltsinstanz
  (`httpResource` auf diese Id skoped); ein Mischen von Haushalten ist
  verboten.
- Der Workspace-Wähler ist eine Component in `core/` (App-Rahmen), kein
  Feature-Bestandteil.
- Routen, die einen Workspace verlangen (z. B. Aufgaben), setzen den
  gewählten Haushalt voraus; ohne Mitgliedschaft erscheint der
  Navigationspunkt nicht.

## Konsequenzen

**Positiv**

- Workspace-Wechsel ohne Seiten-Neuladen: nur Data-Services laden neu (AC23).
- Alle Features behandeln den Haushalt einheitlich — keine feature-eigene
  Auswahl-Logik.
- Die Zugehörigkeitsfrage („welche Haushalte darf diese Person sehen?") wird
  einmal in core beantwortet, nicht in jedem Feature.

**Negativ / Kosten**

- `core` erhält fachnahen, zustandsbehafteten Zustand — bewusste Ausnahme
  zur Regel „core ist fachneutral"; die Begründung liefert diese ADR.
- Feature-Tests müssen den Workspace explizit bereitstellen.

**Neutral**

- Die Regel „ein Feature darf `core` benutzen" besteht bereits (ADR 0002,
  arc42/05); diese ADR erweitert core um einen weiteren app-weiten Dienst.
