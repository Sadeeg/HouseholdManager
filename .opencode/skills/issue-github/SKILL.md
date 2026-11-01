---
name: Issue-GitHub
description: "Use to maintain the GitHub issue that carries the loop: create, replace body per round, comment the protocol, finalize."
---
# Issue-GitHub

Das GitHub-Issue **ist** der Arbeitsstand des Anforderungs-Loops. Es gibt
keine lokale Draft-Datei mehr.

| Runde | Was passiert |
|-------|--------------|
| Ende Runde 1 | Issue wird **angelegt** (Titel mit `[WIP]`, Body = Issue-Text, ein Kommentar mit dem Protokoll der Runde 1) |
| Ende Runde 2 | Body wird **ersetzt** (neuer Issue-Text), ein Kommentar mit dem Protokoll der Runde 2 kommt dazu |
| Ende Runde 3 | Body wird **ersetzt**, ein Kommentar mit dem Protokoll der Runde 3 kommt dazu |
| Nach dem finalen Gate | Body wird **finalisiert**, `[WIP]` fällt aus dem Titel, Labels und Typ werden gesetzt, ein Abschlusskommentar kommt dazu |

Repo: `Sadeeg/HouseholdManager`.

## Zustandsmarker im Body

Jeder Body beginnt mit einem unsichtbaren HTML-Kommentar. Er ist in der
Ansicht unsichtbar, in der Quelle maschinell lesbar:

```markdown
<!-- anforderung-loop: runde=2 status=IN_PROGRESS issue=129 -->
```

`status` ist genau einer von: `IN_PROGRESS`, `READY_FOR_AGENT`, `BLOCKED`.
`runde` ist `1`, `2`, `3` oder `final`.

Im **Titel** steht während der Arbeit das Präfix `[WIP] ` — das ist für
Menschen sichtbar. Es fällt beim Finalisieren weg.

## Teil A — Issue anlegen (Ende Runde 1)

1. **Duplikat prüfen** mit `search_issues` (natürlichsprachig). Gibt es ein
   offenes Issue mit gleichem Ziel: **nicht** anlegen, dem User das
   Existing Issue nennen und die Entscheidung überlassen.
2. **Labels prüfen** mit `get_label` je Label. Ein Label, das es nicht gibt,
   wird **nicht** erfunden — es entfällt und wird im Bericht vermerkt.
   Vorhanden im Repo: `accessibility`, `bug`, `documentation`, `duplicate`,
   `enhancement`, `good first issue`, `help wanted`, `invalid`, `question`,
   `wontfix`. **Nicht vorhanden** (und daher nicht zu vergeben): `epic`,
   `phase:0`, `feature` (das ist ein Issue-*Typ*). Epic und Phase stehen im
   **Titelpräfix**.
3. **Typ bestimmen** mit `list_issue_types` → `Task`, `Bug`, `Feature`.
   Arbeitseinheiten aus dem Loop sind `Task`.
4. **Anlegen:**

```text
issue_write({
  method: "create",
  owner: "Sadeeg",
  repo: "HouseholdManager",
  title: "[WIP] <Titel>",
  body: "<!-- anforderung-loop: runde=1 status=IN_PROGRESS -->\n\n<Issue-Text>",
  type: "Task",
  labels: [<geprüfte Labels>]
})
```

5. **Protokoll der Runde anhängen:**

**Phasen eines Epics** werden als Sub-Issue des Epic-Issues angelegt —
beim Anlegen mit `parent_issue_number` (Epic-Nummer), nicht danach:

```text
issue_write({
  method: "create",
  owner: "Sadeeg",
  repo: "HouseholdManager",
  title: "[WIP] [1.2] Backend — HaushaltService + Repository",
  body: "<!-- anforderung-loop: runde=1 status=IN_PROGRESS -->\n\n<Issue-Text>",
  type: "Task",
  parent_issue_number: <Epic-Nr>,
  labels: [<geprüfte Labels>]
})
```

```text
add_issue_comment({
  owner: "Sadeeg",
  repo: "HouseholdManager",
  issue_number: <N>,
  body: "## Runde 1 — Verständnis\n\n<Protokoll der Runde 1>"
})
```

Protokoll-Format je Runde:

```markdown
## Runde 1 — Verständnis

**F1 (Ziel-Angriff):** <Frage>
**A1:** <Antwort>
→ **AC1** <was daraus folgt>

**F2 (Kern-Angriff):** <Frage>
**A2:** <Antwort>
→ **Abweichung** <was bewusst nicht umgesetzt wird>

### Stop-Kriterien Runde 1

Ziel bestätigt ✓ — keine offene Frage mit Tragweite ✓ — Entscheidungen
entschieden ✓ — Durchschau-Test ✓ — Kern und Verzicht benannt ✓ — keine
Beobachtung ohne Folge ✓
```

Die Markierung unter jeder Antwort (`→ **AC<n>**`, `→ **Abweichung**`,
`→ **Regel …**`) ist Pflicht. Sie ist das, woran das Gate in Runde 2 und 3
prüft, ob eine Eigenschaft im Issue-Text angekommen ist — ohne sie geht
die Information stillschweigend verloren.

## Teil B — Runde aktualisieren (Ende Runde 2 und 3)

1. Issue lesen: `issue_read` mit `method: "get"` (Body) und
   `method: "get_comments"` (bisherige Protokolle).

   **Achtung Form:** `get_comments` liefert ein **Array direkt** zurück,
   nicht ein Objekt mit einem `comments`-Schlüssel:

   ```text
   issue_read({owner, repo, issue_number, method: "get_comments"})
   → [{ id, body, user, … }, …]
   ```

   Ein Zugriff auf `.comments` liefert `undefined` — die Protokolle wären
   unsichtbar.
2. **Body ersetzen** — der alte Body wird **vollständig** überschrieben,
   nicht ergänzt. Der neue Body beginnt mit dem Marker:

```text
issue_write({
  method: "update",
  owner: "Sadeeg",
  repo: "HouseholdManager",
  issue_number: <N>,
  body: "<!-- anforderung-loop: runde=2 status=IN_PROGRESS -->\n\n<Issue-Text>"
})
```

3. **Protokoll der Runde anhängen** — ein Kommentar je Runde, chronologisch:

```text
add_issue_comment({
  owner: "Sadeeg",
  repo: "HouseholdManager",
  issue_number: <N>,
  body: "## Runde 2 — Fachlich\n\n<Protokoll der Runde 2>\n\n### Stop-Kriterien Runde 2\n\n…"
})
```

4. **Verifizieren**: Body mit `issue_read` zurücklesen. Der Body muss mit
   dem neuen Issue-Text beginnen (nach dem Marker) und darf kein
   Entscheidungsprotokoll enthalten — das steht in den Kommentaren.

## Teil C — Finalisieren (nach dem finalen Gate)

**Vorher: Runde 4 (Redaktion).** Der Body wird auf das für die Umsetzung
Nötige reduziert (`anforderung-loop` → Runde 4). Teil C bekommt den
**redigierten** Body, nicht den vollen Runde-3-Text.

Nur bei `status=READY_FOR_AGENT`:

```text
issue_write({
  method: "update",
  owner: "Sadeeg",
  repo: "HouseholdManager",
  issue_number: <N>,
  title: "<Titel ohne [WIP]>",
  body: "<!-- anforderung-loop: runde=final status=READY_FOR_AGENT -->\n\n<Redigierter Issue-Text mit Lesehinweis>",
  labels: [<geprüfte finale Labels>],
  type: "Task"
})
```

Der redigierte Body beginnt mit dem Marker, danach der Lesehinweis:

```markdown
> **Lesen:** Dieser Body ist die vollständige Spezifikation. Die
> Kommentare enthalten das Grilling-Protokoll (Begründung, Historie) und
> werden nur gelesen, wenn hier etwas nicht ermittelbar ist.
```

Dann ein Abschlusskommentar:

```markdown
## Fertiggestellt

Anforderungs-Loop abgeschlossen: 3 Runden, 22 Grilling-Fragen, finales Gate
PASS. Das Issue ist umsetzungsbereit.

**Regeln:** R1–R18 · **Akzeptanzkriterien:** 30 · **Tests:** 7 Testdateien
```

Der Kommentar nennt zusätzlich die **Redaktion**: von wie vielen auf wie
viele Zeilen der Body gekürzt wurde und dass die Kommentare nur bei
nicht Ermittelbarem gelesen werden.

## Wiederaufnahme

Der Zustand liegt im Issue, nicht in einer Datei. Ein neuer Lauf findet das
Issue so:

- Über die Nummer im Argument (`/anforderung 12`).
- Ohne Argument: offene Issues holen und im Titel auf `[WIP]` filtern. Es
  gibt keinen Titel-Filter im Tool, also selbst filtern:

  ```text
  list_issues({owner: "Sadeeg", repo: "HouseholdManager", state: "OPEN", fields: ["number", "title", "labels"]})
  ```

  Das Issue mit passendem Titel ist das gesuchte. Bei mehreren Kandidaten
  den User fragen, welches gemeint ist.

Der Marker im Body verrät den letzten Stand (`runde=2` → Runde 2 ist
gelaufen, Runde 3 fehlt).

## Regeln

- **Nie** ein Issue anlegen, ohne vorher auf Duplikate zu prüfen.
- **Nie** Labels erfinden. Fehlt ein Label, wird es weggelassen und
  gemeldet.
- **Nie** das Protokoll in den Body schreiben — es gehört in Kommentare.
- **Nie** den Body ergänzen statt ersetzen. Der Body ist immer der
  vollständige aktuelle Issue-Text.
- **Nie** finalisieren ohne Redaktion (Runde 4). Der Body ist die einzige
  Quelle des Umsetzungs-Agenten — Kommentare werden nur bei nicht
  Ermittelbarem gelesen.
- Der `[WIP]`-Präfix bleibt, bis das finale Gate `PASS` ist.
- Bestehende Issues werden nicht automatisch geschlossen oder editiert.
- Wird ein Epic angelegt, passiert es **vor** seinen Phasen. Die Phasen
  werden mit `parent_issue_number` als Sub-Issues verknüpft.

## Fehlerfälle

| Fall | Verhalten |
|------|-----------|
| Duplikat gefunden | Nicht anlegen, Existing Issue nennen, User entscheidet |
| Label existiert nicht | Label weglassen, im Bericht nennen |
| `issue_write` schlägt fehl | Fehler melden; der Stand bleibt ungesichert, die Runde wird nicht als abgeschlossen gemeldet |
| Kommentar schlägt fehl | Body-Update gilt trotzdem; der Kommentar wird als fehlend ausgewiesen und im nächsten Lauf nachgeholt |
| Body-Update schlägt fehl | **Nicht** protokollieren: das Protokoll gehört zum Text. Der alte Body bleibt, die Runde gilt als nicht geschrieben |
