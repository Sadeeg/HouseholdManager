# Die Angular-22-Fallen (zoneless, Signals, Vitest)

Diese Fallen kosten bei jedem neuen Frontend-Test Zeit. Sie sind hier
gesammelt, damit der Test nicht daran scheitert.

| Falle | Folge | Richtig |
|-------|-------|---------|
| Effekte laufen erst nach `detectChanges()` | `expectOne` findet keinen Request | erst rendern, **dann** erwarten |
| Nach `flush()` folgt ein Reload | `whenStable()` wartet auf den **nicht** geflushten Request und hängt | Makrotask: `await new Promise(r => setTimeout(r, 0)); fixture.detectChanges()` |
| `httpResource` dedupliziert | der zweite GET geht nie aus | `.reload()` — es gibt **kein** `.refresh()` |
| `error()` liefert `undefined` | `!== null` ist immer wahr | `!!x.error()` prüfen |
| Signal-Forms-Knoten | `required`/`maxlength` im Template greifen nicht | Validatoren `required()`, `maxLength()` im Code |
| `type="submit"` | löst im Test kein Submit aus | Handler über `type="button"` aufrufen |
| `TestBed.configureTestingModule` | zweiter Aufruf wirft | einmal pro Test konfigurieren |
| Formularmeldung hängt an `touched()` | im Test fehlt die Fehlermeldung, obwohl das Feld ungültig ist | nach der Eingabe `input.dispatchEvent(new Event('blur'))`, dann erst die Assertion |
| Datumsanzeige hängt von der Zeitzone ab | derselbe Test schlägt auf einer anderen Maschine durch | Ganztagsdaten aus dem ISO-Wert lesen (kein `DatePipe`-Parse); ein Test leitet sein erwartetes Bild aus demselben Zeitpunkt ab, nicht aus einem festen Zeichenstring |
