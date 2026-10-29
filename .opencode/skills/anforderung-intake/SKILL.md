---
name: Anforderung-Intake
description: Use for round 1 of the requirements loop to turn a rough feature idea into a draft issue. Establishes goal, context, scope, non-goals, the epic/phase split, and the open questions. Load this when a new issue or epic is captured from an idea, a chat message, or a vague request.
---

# Runde 1 — Erfassen

Aus einer groben Idee wird ein Draft v0: Ziel, Kontext, Scope, Abgrenzung,
Phasen-Schnitt und die Liste der offenen Fragen. Noch keine Fachlogik, noch
keine Technik. Das Ergebnis ist bewusst lückenhaft — die Lücken sind der
Input für Runde 2 und Runde 3.

## Schritte

### 1. Ziel in einem Satz

Formuliere das Ziel aus Sicht des Nutzers: „<Rolle> kann <Fähigkeit>".
Wenn das nicht in einem Satz geht, ist die Idee zu groß → Epic.

### 2. Bestand im Repo prüfen

Nicht raten. Existiert die Funktion schon teilweise? Delegiere die Suche an
einen `explore`-Subagent mit **benannten** Suchzielen:

```text
Prüfe im Repo HouseholdManager, ob folgende Bestandteile existieren:
- Route <pfad> in frontend/src/app/app.routes.ts
- Komponente/Klasse <name> unter frontend/src/app und
  backend/src/main/java/de/householdmanager
- Controller <name> unter .../endpoint/
- Migrationen V<nr> in backend/src/main/resources/db/migration/
Gib für jeden Punkt zurück: existiert ja/nein, exakter Pfad, und in welchem
Zustand (leer / teilweise / fertig).
```

Antwort nur mit dem Ergebnis, keine Prosa. Wenn nichts existiert, ist das
ein Ergebnis — kein Anlass, die Suche zu wiederholen.

Werkzeuge: `rg` ist auf dem Host nicht installiert, `grep`/`find` sind der
Weg.

### 3. Scope und Nicht-Ziele

- `## Scope`: Stichpunkte, jeder mit Verb am Satzanfang, Ergebnis formuliert.
- `## Out of Scope`: mindestens drei Punkte. Aus der Idee ableiten, was
  üblichweise mitgemeint wäre, aber nicht gemeint ist (z. B. Export,
  Admin-UI, Benachrichtigungen, Mehrfamilienhaus).

### 4. Epic-Entscheidung

Der Schnitt entscheidet: **vertikaler Schnitt = ein Issue**. Braucht ein
Feature Migration **und** Entity **und** Service **und** Controller
**und** Frontend, ist das nach `AGENTS.md` *ein* Issue in *einem* Branch —
kein Epic. Ein Epic ist nur richtig, wenn die Teile **unabhängig
voneinander** geliefert werden können, also je allein nutzbar sind.

| Kriterium | Ergebnis |
|-----------|----------|
| Mehrere Nutzerrollen mit getrennten Rechten | Epic |
| Unabhängige Liefergegenstände, je allein nutzbar (z. B. „Export" neben „Dashboard-Umbau") | Epic |
| Braucht Schema **und** UI **und** Logik, aber nur gemeinsam nutzbar | **Einzelissue** (vertikaler Schnitt) |
| Eine Arbeitseinheit, in einem Branch umsetzbar | Einzelissue |

Der Tie-Breaker: **Kann Teil 1 allein ausgeliefert werden und ist es dann
nutzbar?** Wenn nein → vertikaler Schnitt → ein Issue.

Epic → Phasenliste mit `N.M`-Nummern, Reihenfolge und Abhängigkeiten. Die
nächste Epic-Nummer ist die höchste bestehende + 1. Die Nummer steht im
**Titelpräfix** `[Epic N]` — es gibt kein `epic`-Label im Repo. Also über
die GitHub-Tools:

```text
list_issues({owner: "Sadeeg", repo: "HouseholdManager", state: "OPEN", fields: ["number", "title"]})
```

Dann im Ergebnis die Titel prüfen und das höchste `[Epic N]` ablesen.

### 5. Grilling — Verständnis sichern

Jetzt wird die Anforderung **kritisch hinterfragt**, nicht nur ergänzt.
Lade vorher `.opencode/skills/anforderung-loop/references/grilling.md` und
folge dem Frageprotokoll.

Ziel dieser Grilling-Phase: **das Verständnis des Features sichern**.
Deshalb wird hier nicht nach Details gefragt, sondern das Verständnis
selbst angegriffen.

**Genau eine Frage pro Zug.** Nach jeder Antwort: Antwort in den
Protokoll-Abschnitt der Runde schreiben, den Issue-Text bei Bedarf anpassen,
dann die nächste Frage. Nie zwei Fragen zusammen, nie einen Fragebogen
schicken.

Runde 1 attackiert das Verständnis:

| Reihenfolge | Kategorie | Worum es geht |
|-------------|-----------|----------------|
| F1 | Ziel-Angriff | Löst das die Ursache oder nur das Symptom? |
| F2 | Kern-Angriff | Was ist der Kern, was ist Beiwerk? |
| F3 | Mess-Angriff | Woran merkt der Nutzer, dass es funktioniert? |
| F4 | Gegner-Angriff | Wer will das nicht, und warum? |
| F5 | Zeit-Angriff | Was davon ist jetzt nötig, was kann später? |
| F6 | Verzicht | Worauf wird verzichtet? Der Kern des Verzichts |

Weicht die Vorstellung des Auftraggebers von deiner Lesart ab, ist das der
wichtigste Fund der ganzen Runde — **nicht** mit einer Annahme überbrücken,
sondern nachfragen, bis beide dasselbe meinen.

Nach sechs Fragen ist die Verständnis-Phase normalerweise abgeschlossen.
Stop-Kriterien aus `grilling.md` prüfen:

1. Ziel als ein Satz in der Sprache des Auftraggebers, bestätigt.
2. Keine offene Frage mit Tragweite.
3. Keine offene User-Entscheidung in `## Offene Punkte`.
4. Durchschau-Test bestanden.
5. Kern und Verzicht benannt.
6. Keine Beobachtung ohne Folge — jede im Protokoll markierte
   Eigenschaft ist angekommen.

Zu Punkt 6: Jede Antwort, die eine prüfbare Eigenschaft festlegt, wird
im Protokoll markiert — `→ **AC<n>**`, `→ **Abweichung**` oder
`→ **Regel …**`. In Runde 1 gibt es die AC-IDs noch nicht; die Markierung
`→ **AC<n>**` reserviert die Nummer, Runde 2 vergibt sie dann.

Sind sie nicht erfüllt, wird **weiteregefragt** — auch wenn es subjektiv
„lange her" wirkt. Der Auftraggeber entscheidet, nicht die Vorschrift.

### 6. Issue anlegen

Der Arbeitsstand ist das GitHub-Issue — es gibt keine lokale Datei.

- Issue-Text in der Struktur aus `issue-formulierung` schreiben.
- `## Acceptance Criteria` enthält in Runde 1 zunächst nur die fachlichen
  Kriterien, die sich bereits aus der Idee ergeben. Leere Checkliste ist
  in Runde 1 erlaubt, in Runde 2 nicht.
- `## Tests` und `## Verify` bleiben in Runde 1 leer bzw. sind als
  „folgt in Runde 3" markiert.
- Das Protokoll der Runde 1 (alle F/A-Paare plus Stop-Kriterien-Zeile) wird
  **nicht** in den Body geschrieben, sondern als Kommentar angehängt.

Dann `issue-github` **Teil A** ausführen: Issue anlegen (Titel mit
`[WIP]`, Body mit Zustandsmarker), Protokoll der Runde 1 als Kommentar
anhängen. Danach `anforderung-gate` mit `runde: 1` ausführen — das Gate
liest Issue-Body und Kommentare.

## Regeln

- Kein Code, kein Diff, kein Datei-Inhalt — nur das Issue.
- Nichts erfinden. Was nicht belegt ist, wird `_Annahme: …` und landet in
  `Offene Punkte` unter **Annahmen**.
- **Eine Frage pro Zug.** Das Grilling ist der Kern dieser Runde, kein
  Anhang. Es wird nicht abgekürzt, wenn die Idee schon konkret wirkt.
- Weicht die Vorstellung des Auftraggebers von deiner Lesart ab, wird
  nachgefragt — nicht mit einer Annahme überbrückt.
- Wenn die Idee bereits konkret genug für ein Issue ist, wird nicht künstlich
  aufbläht — die offenen Punkte sind dann leer, die Fragen trotzdem gestellt.

## Ergebnisformat

```text
Runde 1 abgeschlossen: <Titel>
Issue: #<N> <URL>
Epic: <ja/nein>, Phase: <N.M>
Grilling: <n> Fragen, <n> offen
User-Entscheidungen: <n>, Annahmen: <n>

GATE: runde=1 status=<PASS|GAPS>
LÜCKEN:
- …
KORREKTUREN:
- …
```
