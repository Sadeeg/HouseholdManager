---
name: Anforderung-Fachlich
description: Use for round 2 of the requirements loop to complete the business content of an issue. Derives user roles, user flow, domain rules, testable acceptance criteria, error cases and non-goals from the draft. Load this when a captured draft needs to become functionally complete before technical refinement.
---

# Runde 2 — Fachliches Refinement

Der Draft wird fachlich vollständig: wer macht was, in welcher Reihenfolge,
unter welchen Regeln, und woran erkennt man fachlich, dass es fertig ist.
Technik bleibt außen vor — keine Dateinamen, keine Endpunkte, keine
Klassennamen.

## Fachvokabular HouseholdManager

Das Repo ist neu — es gibt **noch kein festgeschriebenes Vokabular**. Die
Formulierung nutzt die Domänenbegriffe des Features, nicht generische Wörter:

| Begriff | Bedeutung |
|---------|-----------|
| <Begriff 1> | <Definition — im ersten Vorkommen im Issue festlegen> |
| <Begriff 2> | <Definition> |

Die Tabelle ist **wachsend**: Wird ein neuer Fachbegriff eingeführt, wird er
hier in dieser Skill ergänzt und im Issue beim ersten Vorkommen definiert.
Begriffe aus der Kategorie Haushaltsverwaltung (Haushalt, Mitglied, Aufgabe,
Ausgabe, Termin, Einkaufsliste, Putzplan …) gelten als Kandidaten, nicht als
festgelegt — was tatsächlich gilt, entscheidet das Issue.

Kein Begriff wird geraten: was nicht belegt ist, wird `_Annahme: …` und
landet in `Offene Punkte`.

## Schritte

### 1. Rollen und Berechtigungen

Für jede beteiligte Rolle: was darf sie sehen, was darf sie tun, was wird
ihr verweigert? Wenn Rollen im Entwurf fehlen, ist das eine Lücke der Art
`FACH` → Runde 2, und die Rolle wird hier ergänzt.

### 2. Nutzerfluss

Der Ablauf als geordnete Schritte, aus Sicht der handelnden Rolle:

```text
1. Die Haushaltsleitung öffnet „Ausgaben".
2. Wählt den Zeitraum.
3. Sieht die Liste der Mitglieder mit Summen.
4. …
```

Bei mehreren Rollen: je Rolle ein eigener Ablauf, klar getrennt.

### 3. Fachliche Regeln

Explizit ausformulieren, was das Issue festlegt:

- **Berechnung**: wie Summen, Anteile, Zeiträume gerechnet werden.
- **Zustände**: welche Status, welche Übergänge, was passiert bei erneutem
  Anstoßen.
- **Grenzwerte**: Mindest-/Höchstwerte, Laufzeiten, Fristen.
- **Berechtigungen**: wer darf starten, wer darf zurücksetzen, wer darf
  freigeben.
- **Sortierung und Darstellung**: Default-Sortierung, Gruppierung.

### 4. Akzeptanzkriterien

Jedes Kriterium ist eine prüfbare Aussage **ohne** Code-Begriffe und trägt
eine **ID**:

```text
- [ ] **AC7** Ein Aufgabeneintrag mit 8 Stunden Aufwand am selben Tag wird abgelehnt.
- [ ] **AC8** Die Summe im Kopfbereich zeigt Beträge ohne Nachkommastellen.
- [ ] **AC9** Ein Mitglied ohne Einträge erscheint nicht in der Liste.
```

Regeln:

- Je Zeile genau eine Aussage.
- Kein „und" zwischen zwei Prüfgrößen.
- Wertet ab, nicht implementiert: nicht „Komponente nutzt `signal()`",
  sondern „beim Wechsel des Haushalts aktualisiert sich die Liste ohne Neuladen".
- Deckt jede Regel aus Schritt 3 mindestens einmal ab.
- **IDs fortlaufend** (`AC1`, `AC2`, …), nie wiederverwendet, nie
  umnummeriert. Runde 3 und der Testplan verweisen auf sie.
- Jede im Grilling markierte Eigenschaft (`→ **AC<n>**`) wird hier ein
  Akzeptanzkriterium mit genau dieser ID. Markierungen, die zu keinem
  Kriterium führen, sind ein Fehler — siehe Stop-Kriterium 6 in
  `references/grilling.md`.
- Gruppierung nach Themen ist erlaubt (`**Deckblatt**`, `**Fotos**`), die
  IDs laufen durch.
- **Jedes „kann …" nennt Einstieg und Voraussetzung**: wo startet der Nutzer
  die Handlung (Screen, Aktion) und was muss vorhanden sein (Haushalt, Rolle,
  Datensatz)? Ein Endpunkt genügt nicht — ohne Einstieg in der Oberfläche ist
  das Kriterium für den Auftraggeber nicht prüfbar.
- **Sichtbare Aktionen existieren im Default-Zustand.** Formuliert die
  Anforderung einen `disabled`-Zustand einer Aktion, ist diese dauerhaft
  sichtbar — nicht erst nach einer Auswahl.
- **Grenzwerte werden benannt, nicht verworfen.** „Wird abgelehnt" ohne
  Meldung an den Nutzer ist keine fertige Regel, sondern eine offene Lücke
  (der Wert verschwindet sonst unbemerkt).

### 5. Eingaben, Pflegeorte und Leerfälle

Jede Angabe, die im Ergebnis des Features erscheint, braucht drei Dinge:

1. **Pflegeschritt im Nutzerfluss** — wo der Nutzer sie eingibt.
2. **Benannter Ort** — in welcher Seite bzw. welchem Dialog. „An einer
   geeigneten Stelle" ist keine Angabe.
3. **Leerfall-Regel** — was passiert, wenn der Wert fehlt.

Der Leerfall ist nie „nichts". Leer bedeutet meist: ein Platzhalter im
Ergebnis, ein ausgelassener Abschnitt, eine unvollständige Liste. Jeder
solche Platzhalter braucht eine **Rückmeldung an den Nutzer** — eine
Warnung, ein Hinweisfeld oder eine Fehlermeldung. „Leer → —" ohne
Rückmeldung ist keine fertige Regel, sondern eine offene Lücke.

Beispiel:

```text
Eingabe: Name des Haushalts
Pflegeschritt: Nutzerfluss Schritt 5 — der Nutzer pflegt die
  Haushaltsangaben in der Haushaltsverwaltung.
Ort: Haushalt bearbeiten, Abschnitt „Stammdaten", Feld „Name".
Leerfall: Das Vorwort zeigt „—" an der Stelle und die Hinweisliste meldet
  „Haushaltsangaben fehlen".
```

### 6. Eigenschaften der Referenz

Gibt es ein **Vorbild** — ein Referenzdokument, ein Altdokument, ein
Screenshot, eine Vorgängerfunktion —, dann werden seine beobachtbaren
Eigenschaften einzeln aufgelistet und je Eigenschaft entschieden:

```text
| Eigenschaft | übernommen als |
|--------------|---------------|
| Fotos randlos in gleich großen Zellen | AC14, AC15 |
| Fotos tragen keine Bildunterschrift | AC16 |
| Serifenschrift für Fließtext | AC17 |
| A4 mit 2,5 cm Rändern | AC18 |
```

Jede Eigenschaft wird **entweder** ein Akzeptanzkriterium **oder** eine
notierte Abweichung in `## Referenz`. Eigenschaften, die weder das eine
noch das andere sind, sind der Verlust, den dieses Issue verhindert: sie
fallen im Protokoll auf, im Dokument aber nicht auf.

Das Ergebnis steht im Issue unter `## Referenz`.

### 7. Fehler- und Randfälle

| Fall | Erwartetes Verhalten |
|------|---------------------|
| leere Eingabe | Verhalten festlegen |
| ungültiger Wert | Verhalten festlegen |
| Wert außerhalb der Grenze (Länge, Anzahl) | Verhalten festlegen — **mit Meldung an den Nutzer**, nicht stilles Verwerfen |
| Berechtigung fehlt | Verhalten festlegen |
| Aktion doppelt ausgelöst | Verhalten festlegen |
| Unterbrechung/Abbruch | Verhalten festlegen |

Nicht jedes Issue braucht alle fünf Zeilen — aber die, die auftreten
können, gehören hinein.

### 8. Abhängigkeiten

Was muss fachlich fertig sein, bevor dieses Issue sinnvoll ist? Als
`depends_on` im Status-Block und als `## Depends on` im Text — mit
Referenz auf Phase oder Issue-Nummer.

### 9. Grilling — fachliche Regeln attackieren

Jetzt wird die **fachliche** Ausarbeitung angegriffen, nicht das Verständnis
des Features (das ist Runde 1). Lade vorher
`.opencode/skills/anforderung-loop/references/grilling.md` und folge dem
Frageprotokoll: **eine Frage pro Zug**, Antwort ins Protokoll, dann die
nächste Frage.

Runde 2 attackiert die Regeln:

| Reihenfolge | Kategorie | Worum es geht |
|-------------|-----------|----------------|
| F1 | Randfall-Angriff | Was passiert bei 0, 1, sehr vielen, negativen Daten? |
| F2 | Rollen-Angriff | Wer darf es sehen, wer darf es tun, wer nicht? |
| F3 | Umkehr-Angriff | Was, wenn der Nutzer das Gegenteil will? Ist die Regel zu starr? |
| F4 | Widerspruchs-Angriff | Welche bestehende Funktion kollidiert — wer ist die Quelle der Wahrheit? |
| F5 | Folge-Angriff | Was wird nach einem halben Jahr unbrauchbar? |
| F6 | Mess-Angriff | Woran merkt der Nutzer, dass es funktioniert? Welche Handlung wird seltener? |
| F7 | Referenz-Angriff | Welche beobachtbaren Eigenschaften des Vorbilds werden übernommen, welche nicht? |

Stop-Kriterien aus `grilling.md` prüfen. Sind sie nicht erfüllt, wird
weiteregefragt. Die Antworten fließen direkt in die Regeln, Akzeptanzkriterien
und Fehlerfälle des Entwurfs ein.

### 10. Issue aktualisieren

Der Arbeitsstand ist das GitHub-Issue — es gibt keine lokale Datei.

- Issue-Text in der Struktur aus `issue-formulierung` schreiben. Runde 1
  fügt Runde 2 hinzu: die fachlichen Abschnitte kommen **vor** den
  technischen, `## Context` wird ergänzt, nicht verdoppelt.
- Der Body enthält jetzt `## Referenz` (aus Schritt 6) und die
  Akzeptanzkriterien mit IDs (aus Schritt 4).
- Das Protokoll der Runde 2 (alle F/A-Paare plus Stop-Kriterien-Zeile) wird
  **nicht** in den Body geschrieben, sondern als Kommentar angehängt.

Dann `issue-github` **Teil B** ausführen: Body ersetzen, Protokoll der
Runde 2 als Kommentar anhängen, Body zurücklesen und prüfen. Danach
`anforderung-gate` mit `runde: 2` ausführen — das Gate liest Issue-Body
und Kommentare.

## Regeln

- Keine Technik in Runde 2. Ein Kriterium, das nur technisch prüfbar ist,
  wird hier fachlich formuliert und in Runde 3 mit einem Testfall
  hinterlegt.
- **Jeder Platzhalter braucht eine Rückmeldung.** „Leer → —" ist nie eine
  fertige Regel. Es gehört dazu, wie der Nutzer es erfährt.
- **Jede Grilling-Markierung wird eingearbeitet.** `→ **AC<n>**` wird ein
  Akzeptanzkriterium mit genau dieser ID, `→ **Abweichung**` wandert nach
  `## Referenz`, `→ **Regel …**` wird eine Regel in Schritt 3. Was
  übrig bleibt, ist ein Verlust — und fällt als Lücke der Art `FACH` auf.
- **Jede Eigenschaft eines Vorbilds** wird eingearbeitet: als Kriterium
  oder als Abweichung. Siehe Schritt 6.
- Jede Regel, die aus der Idee stammt, aber nicht belegt ist, wird
  `_Annahme: …` und wandert nach `Offene Punkte`.
- Widersprüche zwischen Annahme und Realität werden benannt, nicht
  aufgelöst — die Entscheidung gehört dem User.
- Der Scope aus Runde 1 wird nicht stillschweigend erweitert. Neuer Scope
  heißt: Lücke `[SCOPE]` melden und in Runde 1 nachziehen.
- **Eine Frage pro Zug**, in jeder Runde. Das Grilling wird nicht
  übersprungen, wenn die Regeln schon vollständig wirken.

## Ergebnisformat

```text
Runde 2 abgeschlossen: <Titel>
Issue: #<N> <URL>
Grilling: <n> Fragen, <n> offen
Fachliche Kriterien: <n> (AC1–AC<n>)
Eingaben ohne Pflegeort: <n>
Referenz-Eigenschaften übernommen: <n>, als Abweichung notiert: <n>
Offene Punkte: <n>

GATE: runde=2 status=<PASS|GAPS>
LÜCKEN:
- …
KORREKTUREN:
- …
```
