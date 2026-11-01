# Grilling — kritisch hinterfragen, bis das Verständnis gesichert ist

Ziel ist **nicht**, Lücken zu sammeln, sondern das Verständnis des Features zu
sichern: Versteht der Agent dasselbe wie der Auftraggeber, und ist der
Auftraggeber mit diesem Verständnis einverstanden? Grilling ist **interaktiv**
— keine Stapel-Fragen, keine stillen Annahmen. Eine Frage, eine Antwort, die
nächste Frage.

## Prinzip: jede Frage ist ein Angriff

- **Ziel-Angriff** — Löst das die Ursache oder nur das Symptom? Was wäre ohne
  das Feature anders?
- **Kern-Angriff** — Was müsste wegfallen, damit es die halbe Größe hätte? Was
  ist Kern, was Beiwerk?
- **Randfall-Angriff** — Was bei null, einem, sehr vielen, negativen,
  zukünftigen Daten?
- **Rollen-Angriff** — Wer ist betroffen, wer nicht? Was, wenn die Rolle fehlt
  oder später dazukommt?
- **Widerspruchs-Angriff** — Welche bestehende Funktion kollidiert? Wer ist
  dann die Quelle der Wahrheit?
- **Umkehr-Angriff** — Was, wenn der Nutzer das Gegenteil will? Ist die Regel
  zu starr?
- **Folge-Angriff** — Was ist in einem halben Jahr? Datenmenge,
  Aufräumbedarf, was wird unbenutzbar?
- **Mess-Angriff** — Woran merkt der Nutzer, dass es funktioniert? Welche Zahl
  ändert sich, welche Handlung wird seltener?
- **Referenz-Angriff** — Gibt es ein Vorbild (Dokument, Screenshot,
  Vorgängerfunktion)? Welche **beobachtbaren Eigenschaften** werden übernommen,
  welche bewusst nicht: Rand, Format, Beschriftung, Schrift, Abstände? Jede
  Eigenschaft wird Akzeptanzkriterium oder notierte Abweichung — nie
  stillschweigend vergessen.
- **Gegner-Angriff** — Wer will das nicht, und warum? Welche Einwände kämen von
  Beteiligten, Leitung, Partner?
- **Zeit-Angriff** — Was ist jetzt nötig, was später? Was wird teuer, wenn es
  jetzt nicht entschieden wird?

Nach drei Fragen derselben Kategorie wird gewechselt — mehrere Winkel, nicht
einer wiederholt.

## Frageprotokoll

**Genau eine Frage pro Zug.** Aufbau:

1. **Nr. und Kategorie** — `Frage 3 (Randfall-Angriff):`
2. **Die Frage** — eine, konkret, entscheidungsorientiert.
3. **Was an ihr hängt** — ein Satz: welche Entscheidung davon abhängt.
4. **Optionen oder Antwortraum** — zwei bis drei konkrete Optionen (A, B, C)
   oder die Frage nach einer Tatsache. Bei Optionen mit Empfehlung; entscheiden
   tut der Auftraggeber.

## Die Antwort trägt ihre Folge mit

**Jede Antwort, die eine prüfbare Eigenschaft festlegt, wird markiert.** Sonst
steht sie im Protokoll, fehlt aber im Issue-Text — und niemand merkt es.

| Markierung | Bedeutung | Folge |
|------------|-----------|-------|
| `→ **AC<n>**` | wird ein Akzeptanzkriterium | Runde 2 vergibt `AC<n>`, das Gate prüft die Übergabe |
| `→ **Abweichung**` | wird bewusst nicht übernommen | landet in `## Referenz` mit Begründung |
| `→ **Regel <Abschnitt>**` | wird eine Regel, kein Kriterium | Runde 2 schreibt sie in die fachlichen Regeln |

Beispiel:

```text
**A4:** B — einheitliche Zelle, Bild vollständig sichtbar.
→ **AC9** Die Fotos einer Woche stehen in gleich großen Zellen.
→ **AC10** Jedes Foto ist vollständig sichtbar, nie beschnitten.
→ **Abweichung** Das Raster trägt keinen Rahmen (Vorbild ist randlos).
```

**Das gilt auch für Beobachtungen, die nur in der Frage auftauchen.** Beschreibt
die Frage ein Vorbild („die Zellen sind randlos"), ist „randlos" eine prüfbare
Eigenschaft — auch wenn die Antwort sie nicht wiederholt. Sie wird markiert oder
ausdrücklich als Abweichung notiert. Reine Präzisierungen („nach Name sortieren")
brauchen keine eigene Markierung; sie fließen in die entstehende AC.

Antwortet der Auftraggeber „weiß ich nicht" oder „egal": Frage als Annahme
markieren, ins Protokoll, weiterfragen. Blockiert wird nur, wenn ohne Antwort
weder gebaut noch sinnvoll eingegrenzt werden kann — dann genau diese eine
Frage stellen.

## Nicht-interaktiver Lauf

Normalfall ist die Interaktion. Der einzige Ausnahmefall: Der Agent läuft
**ohne** Antwortmöglichkeit (Subagent, CI, Batch). Dann gilt:

- Er beantwortet jede Frage **selbst** wie der Auftraggeber, aber mit expliziter
  Begründung der Voreinstellung.
- Jede so gesetzte Antwort wird `_Annahme: …` und im Protokoll mit „Voreinstellung
  (nicht-interaktiver Lauf)" markiert.
- Der Durchschau-Test gilt strenger: Wo eine Voreinstellung später teuer würde,
  wird die Frage als **offen** markiert und dem Auftraggeber gestellt — nicht
  still gesetzt.
- `READY_FOR_AGENT` nur ohne offene Frage, sonst `BLOCKED` mit der Liste.

Im Gespräch wird nie eine Voreinstellung erfunden.

## Regeln

- Nie zwei Fragen in einer Nachricht, auch nicht als „A oder B? Und dazu …".
- Immer die **nächste** Frage, nicht die dringendste; Reihenfolge nach
  Angriffskategorie, nicht nach Wichtigkeit.
- Eine bereits in Runde 1–3 dokumentierte Antwort wird nicht erneut erfragt.
- Umleitung („mach lieber X statt Y"): die ursprüngliche Annahme wird
  **verworfen**, nicht beide Varianten parallel behalten.
- Widerspricht eine Antwort dem Entwurf, wird der Entwurf geändert, nicht die
  Antwort danebengestellt.
- Wer nicht antwortet (Timeout, „mach einfach"), bekommt eine Defaultsetzung
  mit Begründung, die als Annahme ins Protokoll wandert.

## Stop-Kriterien — „Verständnis gesichert"

Das Grilling endet erst, wenn **alle sechs** erfüllt sind:

1. **Ziel bestätigt** — als ein Satz in der Sprache des Auftraggebers, bestätigt
   oder korrigiert.
2. **Keine offene Frage mit Tragweite** — jede gestellte Frage hat eine Antwort
   oder ist als Annahme akzeptiert und markiert.
3. **Entscheidungen sind entschieden** — keine Alternative offen, die UX,
   Datenmodell oder Umfang ändert; `## Offene Punkte` enthält keine
   User-Entscheidung.
4. **Durchschau-Test** — Würde ein zweiter Mensch aus diesem Issue genau das
   bauen, was der Auftraggeber will? „Nein" heißt: Verständnis nicht gesichert.
5. **Kern benannt** — ein Satz, was das Feature *nicht* ist, und worauf
   verzichtet wird.
6. **Keine Beobachtung ohne Folge** — jede markierte Eigenschaft ist im
   Issue-Text angekommen oder in `## Referenz` begründet. Keine stillschweigend
   verlorene Eigenschaft; das schließt die stille Übertragungslücke.

Erst dann geht die Runde weiter. Fehlen Kriterien, wird weitergefragt — auch
wenn es subjektiv „lange her" wirkt.

## Protokoll

Alle Fragen und Antworten landen im **Entscheidungsprotokoll**: ein
**Kommentar am GitHub-Issue**, je Runde einer, chronologisch — **nicht** im
Issue-Body, dort stehen nur Ergebnisse. Form:

```markdown
## Runde 1 — Verständnis

**F1 (Ziel-Angriff):** Löst die Wochenübersicht die Ursache oder nur das Symptom?
**A1:** Symptom; die Ursache ist die fehlende zeitliche Gliederung.
**F2 (Kern-Angriff):** Was fiele weg, damit das Feature die halbe Größe hätte?
**A2:** Die Spaltensummen. → **Abweichung** Spaltensummen entfallen.

### Stop-Kriterien Runde 1
Ziel bestätigt ✓ — keine offene Frage ✓ — Entscheidungen ✓ — Durchschau ✓ —
Kern/Verzicht ✓ — keine Beobachtung ohne Folge ✓
```

Beim Schreiben `issue-github` Teil A (Runde 1) bzw. B (Runde 2/3); nach dem
Anhängen prüfen, ob der Kommentar da ist.

## Grenzen

- Gefragt werden **Anforderungen**, nicht Implementierungsdetails. „Wie soll die
  Technik aussehen?" ist keine gute Frage; „Was ist die Quelle der Wahrheit,
  wenn Bestand und neue Erfassung auseinanderlaufen?" schon.
- Keine Frage, deren Antwort der Auftraggeber nicht sinnvoll geben kann
  („Welche Datenbank?").
- Kein Verhör: zwischen den Fragen zusammenfassen, was verstanden wurde.
- Nach erfülltem Stop-Kriterium wird nicht weitergefragt, nur um sicher zu sein.
