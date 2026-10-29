# Grilling — kritisch hinterfragen, bis das Verständnis gesichert ist

Ziel des Grilling ist **nicht**, Lücken zu sammeln, sondern das Verständnis
des Features zu sichern: Versteht der Agent dasselbe wie der Auftraggeber,
und ist der Auftraggeber mit dem Verständnis einverstanden?

Grilling ist **interaktiv**. Es gibt keine Stapel-Fragen. Eine Frage,
eine Antwort, die nächste Frage. Wer das Feature bestellt, muss sich dem
Verständnis stellen — deshalb wird hier nicht mit Annahmen gearbeitet.

## Prinzip

Jede Frage geht einen konkreten Angriff auf die Anforderung an:

- **Ziel-Angriff** — Löst das die Ursache oder nur das Symptom? Was wäre der
  Unterschied, wenn wir das Feature nicht bauen?
- **Kern-Angriff** — Was müsste wegfallen, damit das Feature die halbe Größe
  hätte? Was ist der eigentliche Kern, was ist Beiwerk?
- **Randfall-Angriff** — Was passiert bei null, bei einem, bei sehr vielen,
  bei negativen, bei zukünftigen Daten?
- **Rollen-Angriff** — Wer ist betroffen, wer nicht? Was passiert, wenn die
  Rolle fehlt oder später dazukommt?
- **Widerspruchs-Angriff** — Welche bestehende Funktion kollidiert mit dem
  Feature? Wer ist dann die Quelle der Wahrheit?
- **Umkehr-Angriff** — Was passiert, wenn der Nutzer das Gegenteil will? Ist
  die Regel zu starr gefasst?
- **Folge-Angriff** — Was ist die Folge, wenn das Feature ein halbes Jahr
  genutzt wird? Datenmenge, Aufräumbedarf, was wird unbenutzbar?
- **Mess-Angriff** — Woran merkt der Nutzer, dass es funktioniert? Welche
  Zahl ändert sich, welche Handlung wird künftig seltener?
- **Referenz-Angriff** — Gibt es ein Vorbild (Referenzdokument, Altdokument,
  Screenshot, Vorgängerfunktion)? Welche **beobachtbaren Eigenschaften**
  davon werden übernommen, welche bewusst nicht? Randlos oder umrandet,
  Hoch- oder Querformat, mit oder ohne Beschriftung, welche Schrift, welche
  Abstände. Jede Eigenschaft wird Akzeptanzkriterium oder notierte
  Abweichung — nie stillschweigend vergessen.
- **Gegner-Angriff** — Wer will dieses Feature nicht, und warum? Welche
  Einwände kämen von den Beteiligten, von der Haushaltsleitung, vom Partner?
- **Zeit-Angriff** — Was davon ist jetzt nötig, was kann in eine spätere
  Phase? Was wird teuer, wenn es jetzt nicht entschieden wird?

Nach drei Fragen aus derselben Kategorie wird die Kategorie gewechselt —
das Grilling soll mehrere Winkel treffen, nicht einen wiederholen.

## Frageprotokoll

**Genau eine Frage pro Zug.** Form:

```text
Frage 3 (Randfall-Angriff):
Was passiert, wenn ein Mitglied im ausgewählten Zeitraum gar keine Ausgaben
hat — bleibt die Zeile leer, oder fällt das Mitglied ganz weg?

Das entscheidet, ob die Matrix stabile Zeilen behält (A) oder nur Mitglieder
mit Arbeit zeigt (B). Beides ist vertretbar, aber die Anzeige wirkt sonst
lückenhaft.

Empfehlung: A — die Matrix bleibt dadurch über den Zeitraum vergleichbar.
```

Aufbau jeder Frage:

1. **Nr. und Kategorie** — `Frage 3 (Randfall-Angriff):`
2. **Die Frage** — eine, konkret, entscheidungsorientiert.
3. **Was an ihr hängt** — ein Satz: welche Entscheidung oder welches
   Verständnis davon abhängt. Kein Roman.
4. **Optionen oder Antwortraum** — zwei bis drei konkrete Optionen (A, B, C)
   oder die Frage nach einer Tatsache. Bei Optionen mit Empfehlung, aber der
   Auftraggeber entscheidet.

## Die Antwort trägt ihre Folge mit

**Jede Antwort, die eine prüfbare Eigenschaft festlegt, markiert sie.** Ohne
diese Markierung geht die Information beim Übergang in Runde 2 und 3 verloren
— die Antwort steht im Protokoll, aber im Issue-Text fehlt sie, und niemand
merkt es.

```text
**A4:** B — einheitliche Zelle, Bild vollständig sichtbar. Jedes Foto bekommt
dieselbe Zelle (gleiche Breite und Höhe); abweichende Seitenverhältnisse
werden vollständig hineinskaliert, die Zelle bleibt gleich groß, Weißraum
füllt sie. Kein Bildinhalt geht verloren.
→ **AC9** Die Fotos einer Woche stehen in einem Raster mit gleich breiten
  und gleich hohen Zellen.
→ **AC10** Jedes Foto ist vollständig sichtbar; es wird nie etwas beschnitten.
→ **AC11** Die Zellen des Rasters tragen keinen Rahmen — das Vorbild ist
  randlos und das Raster übernimmt das.
```

Drei Markierungen, drei Bedeutungen:

| Markierung | Bedeutung | Folge |
|------------|-----------|--------|
| `→ **AC<n>**` | wird ein Akzeptanzkriterium | Runde 2 vergibt `AC<n>`, das Gate prüft die Übergabe |
| `→ **Abweichung**` | wird bewusst nicht übernommen | landet in `## Referenz` mit Begründung |
| `→ **Regel <Abschnitt>**` | wird eine Regel, kein Kriterium | Runde 2 schreibt sie in die fachlichen Regeln |

**Das gilt auch für Beobachtungen, die nur in der Frage auftauchen.** Wenn
die Frage ein Vorbild beschreibt („die Zellen sind randlos"), ist „randlos"
eine prüfbare Eigenschaft — auch wenn die Antwort sie nicht wiederholt. Sie
wird markiert oder ausdrücklich als Abweichung notiert.

Eigenschaften, die nur eine Präzisierung sind („nach Name sortieren"), brauchen
keine eigene Markierung — sie fließen in die AC, die daraus entsteht.

Antwortet der Auftraggeber mit „weiß ich nicht" oder „egal": die Frage als
Annahme markieren, die Annahme ins Protokoll, weiterfragen. Blockiert wird
nur, wenn ohne Antwort weder gebaut noch sinnvoll eingegrenzt werden kann —
und dann wird genau diese eine Frage gestellt.

### Nicht-interaktiver Lauf

Der Normalfall ist die Interaktion. Es gibt genau einen Ausnahmefall: der
Agent läuft **ohne** die Möglichkeit einer Antwort — als Subagent, in CI
oder in einem Batch-Lauf ohne Gespräch. Dann gilt:

- Der Agent beantwortet jede Frage **selbst**, so wie es der Auftraggeber
  getan hätte, aber mit expliziter Begründung der Voreinstellung.
- Jede so gesetzte Antwort wird als `_Annahme: …` markiert und landet im
  Protokoll mit dem Zusatz „Voreinstellung (nicht-interaktiver Lauf)".
- Der Durchschau-Test wird strenger angewendet: Wo eine Voreinstellung
  eine spätere Fehlentscheidung teuer machen würde, wird die Frage als
  **offen** im Protokoll markiert und im Bericht an den Auftraggeber
  gestellt — nicht still gesetzt.
- Der Status am Ende ist `READY_FOR_AGENT` nur, wenn keine solche offene
  Frage übrig ist. Sonst `BLOCKED` mit der Liste der offenen Fragen.

Diese Regel gilt **nur** für den nicht-interaktiven Lauf. Im Gespräch mit
dem Auftraggeber wird nie eine Voreinstellung erfunden.

## Regeln

- Nie zwei Fragen in einer Nachricht. Auch nicht als „A oder B? Und dazu…".
- Immer die **nächste** Frage, nicht die dringendste. Reihenfolge nach
  Angriffskategorie, nicht nach Wichtigkeit.
- Eine Frage, deren Antwort bereits in Runde 1–3 dokumentiert ist, wird
  nicht wieder gestellt.
- Antwortet der Auftraggeber mit einer Umleitung („mach lieber X statt Y"),
  wird die ursprüngliche Annahme **verworfen** — nicht beide Varianten
  parallel im Issue behalten.
- Widerspricht sich eine Antwort dem bisherigen Entwurf, wird der Entwurf
  geändert, nicht die Antwort nebenbeigestellt.
- Wer nicht antwortet (Timeout, „mach einfach"), bekommt eine Defaultsetzung
  mit Begründung, die als Annahme ins Protokoll wandert.

## Stop-Kriterien — „Verständnis gesichert"

Das Grilling endet erst, wenn **alle** sechs Kriterien erfüllt sind:

1. **Ziel bestätigt** — Das Ziel steht als ein Satz in der Sprache des
   Auftraggebers, und der Auftraggeber hat ihn bestätigt oder korrigiert.
2. **Keine offene Frage mit Tragweite** — Für jede gestellte Frage liegt
   eine Antwort des Auftraggebers vor, oder sie ist als Annahme akzeptiert
   und markiert.
3. **Entscheidungen sind entschieden** — Es stehen keine Alternativen mehr
   offen, die UX, Datenmodell oder Umfang verändern. `## Offene Punkte`
   enthält keine User-Entscheidung mehr.
4. **Durchschau-Test** — Für jede Runde gilt: Würde ein zweiter Mensch aus
   diesem Issue genau das bauen, was der Auftraggeber will? Wird die Frage
   mit „nein" beantwortet, ist das Verständnis nicht gesichert.
5. **Kern benannt** — Es steht ein Satz, was das Feature *nicht* ist (der
   Verzicht), und es ist entschieden, worauf verzichtet wird.
6. **Keine Beobachtung ohne Folge** — Jede im Protokoll markierte
   Eigenschaft (`→ **AC<n>**`, `→ **Abweichung**`, `→ **Regel …**`) ist
   entweder im Issue-Text angekommen oder in `## Referenz` begründet.
   Es gibt keine stillschweigend verlorenen Eigenschaften.

Kriterium 6 schließt die stille Übertragungslücke: es verlangt, dass jede
Beobachtung — auch eine, die nur in der Frage als Eigenschaft eines
Vorbilds auftaucht — im Issue eine sichtbare Spur hat.

Erst dann wechselt die Runde in den nächsten Schritt. Fehlen Kriterien, wird
weitergfragt — auch wenn das subjektiv „lange her" wirkt.

## Protokoll

Jede Frage und ihre Antwort landen im **Entscheidungsprotokoll** — das ist
ein **Kommentar am GitHub-Issue**, nicht Teil des Issue-Bodys. Je Runde ein
Kommentar, chronologisch:

```markdown
## Runde 1 — Verständnis

**F1 (Ziel-Angriff):** Löst die Wochenübersicht die Ursache oder nur das
Symptom?
**A1:** Symptom. Die Ursache ist die fehlende zeitliche Gliederung, nicht
die fehlende Anzeige. → Grundlage für Entscheidung E2.

**F2 (Kern-Angriff):** Was müsste wegfallen, damit das Feature die halbe
Größe hätte?
**A2:** Die Spaltensummen je Woche. Die sind Nice-to-have.
→ **Abweichung** Spaltensummen je Woche entfallen — bewusst nicht
  umgesetzt, siehe `## Referenz`.

### Stop-Kriterien Runde 1

Ziel bestätigt ✓ — keine offene Frage mit Tragweite ✓ — Entscheidungen
entschieden ✓ — Durchschau-Test ✓ — Kern und Verzicht benannt ✓
```

Das Protokoll ist der Beleg dafür, dass das Verständnis gesichert ist. Es
gehört **nicht** in den Issue-Text — dort stehen nur die Ergebnisse
(Regeln, Kriterien, Entscheidungen).

Beim Schreiben: `issue-github` Teil A (Runde 1) bzw. Teil B (Runde 2/3)
anhängen. Nach dem Anhängen prüfen, ob der Kommentar wirklich da ist.

## Grenzen

- Das Grilling fragt **Anforderungen**, nicht Implementierungsdetails. "Wie
  soll die Technik aussehen?" ist keine gute Frage. "Was ist die Quelle der
  Wahrheit, wenn Bestand und neue Erfassung auseinanderlaufen?" schon.
- Keine Frage, deren Antwort der Auftraggeber nicht sinnvoll geben kann
  ("Welche Datenbank sollen wir nutzen?").
- Kein Verhör. Zwischen den Fragen wird zusammengefasst, was verstanden
  wurde — der Auftraggeber soll den Fortschritt sehen.
- Die Fragen enden, sobald das Verständnis gesichert ist. Nach dem
  Stop-Kriterium wird **nicht** weitergefragt, nur um sicher zu sein.
