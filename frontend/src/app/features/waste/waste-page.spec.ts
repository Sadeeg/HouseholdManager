import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withAutoCleanupInjectors } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { WorkspaceService } from '../../core/workspace/workspace-service';
import { WasteData } from './data/waste-data';
import { WasteOverview } from './model/waste.model';
import { WastePage } from './waste-page';

/** Eine zweite Route, damit das Feature verlassen und erneut betreten wird. */
@Component({ template: 'andere Seite' })
class AndereSeite {}

/**
 * Die Seite „Mülltermine" — alle Fälle an einer Stelle (Issue #15).
 *
 * <p>AC14, AC15, AC16, AC17 und R11: die Zustände der Seite, über den
 * Data-Service gelesen. Der Fehlerpfad und die leere Liste werden über
 * dieselbe Seite geprüft, weil die Seite über sie entscheidet.
 *
 * <p>AC16: Liegt noch kein erfolgreicher Lauf vor, nennt die Anzeige
 * „Noch keine Synchronisation" — auch dann, wenn zugleich keine Quelle
 * hinterlegt ist: Der Stand steht im Kopf, unabhängig davon, ob eine Quelle
 * gesetzt ist.
 *
 * <p>AC4: Die Anzeige listet alle synchronisierten Termine aufsteigend nach
 * Datum (bei gleichem Datum nach Tonnenart). Die Reihenfolge entsteht eine
 * Ebene über der Liste — der Data-Service sortiert (R9, Annahme 8). Der Test
 * prüft deshalb die Anzeige als Ganzes: die Seite liefert die Termine
 * unsortiert vom Server und muss sie sortiert darstellen.
 *
 * <p>AC6 (R7, R17): Schlägt ein Synchronisationslauf fehl, bleiben die
 * gespeicherten Termine unverändert erhalten, und die Anzeige weist aus, dass
 * der Stand veraltet ist — nicht nur farblich, sondern mit einer
 * Statuszeile, die erklärt, dass die Termine aus dem letzten erfolgreichen
 * Lauf stammen.
 *
 * <p>AC11 (R6): Der Server synchronisiert den Haushalt sofort; die Anzeige
 * zeigt die Termine beim nächsten Aufruf. Der Stand wird beim erneuten
 * Betreten **neu gelesen** — und **genau einmal** je Betreten.
 *
 * <p>Die Fälle waren über mehrere Dateien verteilt (je eine eigene
 * TestBed-Einrichtung, je ein eigener `tick()`-Helfer). Jetzt gibt es eine
 * Datei und eine Einrichtung — jeder Fall behält seinen Test und seinen
 * Namen.
 */
describe('WastePage', () => {
  let httpMock: HttpTestingController;

  const uebersicht = {
    sourceUrl: 'https://entsorger.example/ics',
    lastSyncedAt: '2026-01-01T03:00:00Z',
    stale: false,
    collections: [{ date: '2026-01-09', wasteType: 'Restmüll' }],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [WastePage],
      providers: [
        // Die Konfiguration der Anwendung: `withAutoCleanupInjectors()`
        // zerstört die route-scoped Provider beim Verlassen der Route
        // (siehe `app.config.ts`). Ein Test, der eine andere Router-
        // Konfiguration prüft, hält einen Zustand fest, den es im Betrieb
        // nicht gibt.
        provideRouter(
          [
            { path: 'muelltermine', component: WastePage, providers: [WasteData] },
            { path: 'anders', component: AndereSeite },
          ],
          withAutoCleanupInjectors(),
        ),
        provideHttpClient(),
        provideHttpClientTesting(),
        WasteData,
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);

    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice' });
    workspace.currentHouseholdId.set(1);
  });

  afterEach(() => {
    httpMock.verify();
  });

  /** Zoneless: ein Makrotask reicht, damit die Antwort wirksam wird. */
  async function tick(fixture: ReturnType<typeof TestBed.createComponent>): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  async function open(overview: WasteOverview): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(WastePage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households/1/waste').flush(overview);
    await tick(fixture);
    return fixture.nativeElement as HTMLElement;
  }

  /** Die Zeilen der Terminliste in der Reihenfolge, in der sie erscheinen. */
  function rowsIn(compiled: HTMLElement): string[] {
    return [...compiled.querySelectorAll('.waste-list__row')].map((row) =>
      (row.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
  }

  /** Die Anfragen an die Übersicht. `match` nimmt sie aus der Warteschlange. */
  function requests(): TestRequest[] {
    return httpMock.match('/api/households/1/waste');
  }

  // --- Zustände der Seite (AC14, AC15, AC16, AC17, R11) ---------------------

  /** AC14: ohne gewählten Haushalt zeigt die Seite den Grund, nicht eine leere Liste. */
  it('names the missing household instead of an empty list', async () => {
    TestBed.inject(WorkspaceService).currentHouseholdId.set(null);

    const fixture = TestBed.createComponent(WastePage);
    fixture.detectChanges();
    await tick(fixture);

    expect(fixture.nativeElement.textContent).toContain('Kein Haushalt gewählt');
    expect(fixture.nativeElement.querySelector('app-waste-collection-list')).toBeNull();
  });

  /** AC15: ohne Quelle nennt die Seite, dass jedes Mitglied eine Adresse eintragen kann. */
  it('says that any member can enter a source', async () => {
    const compiled = await open({ sourceUrl: null, lastSyncedAt: null, stale: false, collections: [] });

    expect(compiled.textContent).toContain('Kein Müllkalender hinterlegt');
    expect(compiled.textContent).toContain('Jedes Mitglied');
    expect(compiled.textContent).toContain('Haushaltsverwaltung');
  });

  /** AC14: Quelle vorhanden, kein Termin im Fenster. */
  it('names the six-week window when there are no terms', async () => {
    const compiled = await open({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2026-01-01T03:00:00Z',
      stale: false,
      collections: [],
    });

    expect(compiled.textContent).toContain('Für die nächsten sechs Wochen liegen keine Termine vor');
  });

  /** AC16: ohne erfolgreichen Lauf kein Zeitstempel, sondern der Hinweis. */
  it('names the missing run instead of a timestamp', async () => {
    const compiled = await open({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: null,
      stale: false,
      collections: [],
    });

    expect(compiled.textContent).toContain('Noch keine Synchronisation');
    expect(compiled.querySelector('.hm-badge--warning')).toBeNull();
  });

  /**
   * AC16: Liegt noch kein erfolgreicher Lauf vor, nennt die Anzeige
   * „Noch keine Synchronisation" — auch dann, wenn zugleich keine Quelle
   * hinterlegt ist: Der Stand steht im Kopf, unabhängig davon, ob eine Quelle
   * gesetzt ist.
   */
  it('names the missing run even without a stored source', async () => {
    const compiled = await open({ sourceUrl: null, lastSyncedAt: null, stale: false, collections: [] });

    expect(compiled.textContent).toContain('Kein Müllkalender hinterlegt');
    expect(compiled.textContent).toContain('Noch keine Synchronisation');
  });

  /** AC6: nicht lesbare Quelle — die gespeicherten Termine bleiben sichtbar, der Stand gilt als veraltet. */
  it('keeps stored terms and marks the stand stale', async () => {
    const compiled = await open({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2025-12-20T03:00:00Z',
      stale: true,
      collections: [{ date: '2026-01-08', wasteType: 'Restmüll' }],
    });

    expect(compiled.textContent).toContain('Restmüll');
    expect(compiled.querySelector('.hm-badge--warning')?.textContent).toContain('STAND VERALTET');
  });

  /** R11: die Seite folgt dem gewählten Haushalt. */
  it('follows the active household (AC17)', async () => {
    const fixture = TestBed.createComponent(WastePage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households/1/waste').flush({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2026-01-01T03:00:00Z',
      stale: false,
      collections: [{ date: '2026-01-09', wasteType: 'Papier' }],
    });
    await tick(fixture);
    expect(fixture.nativeElement.textContent).toContain('Papier');

    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentHouseholdId.set(2);
    await tick(fixture); // Zoneless: der Request folgt dem gewechselten Haushalt.
    httpMock.expectOne('/api/households/2/waste').flush({
      sourceUrl: 'https://other.example/ics',
      lastSyncedAt: '2026-01-02T03:00:00Z',
      stale: false,
      collections: [{ date: '2026-01-11', wasteType: 'Glas' }],
    });
    await tick(fixture);

    expect(fixture.nativeElement.textContent).toContain('Glas');
    expect(fixture.nativeElement.textContent).not.toContain('Papier');
  });

  it('shows the loading state before the answer', async () => {
    const fixture = TestBed.createComponent(WastePage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Termine werden geladen');

    // Der Request wird geschlossen, damit der Test keine offene Anfrage hinterlässt.
    httpMock.expectOne('/api/households/1/waste').flush({
      sourceUrl: null,
      lastSyncedAt: null,
      stale: false,
      collections: [],
    });
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

  /** Der Fehler ist benannt, die Seite bleibt bedienbar. */
  it('names the load failure', async () => {
    const fixture = TestBed.createComponent(WastePage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households/1/waste').error(new ProgressEvent('error'), {
      status: 500,
      statusText: 'Serverfehler',
    });
    await tick(fixture);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('nicht geladen');
    expect(compiled.querySelector('[role="alert"]')).not.toBeNull();
  });

  // --- Veralteter Stand (AC6, R7, R17) --------------------------------------

  it('explains in a status line that the shown terms come from the last successful run', async () => {
    const compiled = await open({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2025-12-20T03:00:00Z',
      stale: true,
      collections: [{ date: '2026-01-08', wasteType: 'Restmüll' }],
    });

    const status = compiled.querySelector('[role="status"]');
    expect(status?.textContent).toContain(
      'Der letzte Lauf ist fehlgeschlagen. Die angezeigten Termine stammen aus dem letzten erfolgreichen Lauf.',
    );
    expect(compiled.querySelector('.hm-badge--warning')?.textContent).toContain('STAND VERALTET');
  });

  it('keeps the stored terms visible', async () => {
    const compiled = await open({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2025-12-20T03:00:00Z',
      stale: true,
      collections: [
        { date: '2026-01-08', wasteType: 'Restmüll' },
        { date: '2026-01-09', wasteType: 'Papier' },
      ],
    });

    const rows = [...compiled.querySelectorAll('.waste-list__row')];
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('Restmüll');
    expect(rows[1].textContent).toContain('Papier');
  });

  it('shows neither badge nor status line when the server does not call the stand stale', async () => {
    const compiled = await open({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2026-01-01T03:00:00Z',
      stale: false,
      collections: [{ date: '2026-01-08', wasteType: 'Restmüll' }],
    });

    expect(compiled.querySelector('.hm-badge--warning')).toBeNull();
    expect(compiled.querySelector('.waste-source-panel__status')).toBeNull();
  });

  // --- Reihenfolge der angezeigten Termine (AC4) ----------------------------

  it('lists every term ascending by date even when the server delivers them unsorted', async () => {
    const compiled = await open({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2026-01-01T03:00:00Z',
      stale: false,
      collections: [
        { date: '2026-01-20', wasteType: 'Papier' },
        { date: '2026-01-05', wasteType: 'Glas' },
        { date: '2026-01-12', wasteType: 'Restmüll' },
      ],
    });

    const rows = rowsIn(compiled);

    expect(rows).toHaveLength(3);
    expect(rows[0]).toContain('05.01.2026');
    expect(rows[0]).toContain('Glas');
    expect(rows[1]).toContain('12.01.2026');
    expect(rows[1]).toContain('Restmüll');
    expect(rows[2]).toContain('20.01.2026');
    expect(rows[2]).toContain('Papier');
  });

  it('orders by waste type when two terms fall on the same day', async () => {
    const compiled = await open({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2026-01-01T03:00:00Z',
      stale: false,
      collections: [
        { date: '2026-01-12', wasteType: 'Singletonne (Restmüll)' },
        { date: '2026-01-12', wasteType: 'Restmülltonne' },
      ],
    });

    const rows = rowsIn(compiled);

    expect(rows).toHaveLength(2);
    expect(rows[0]).toContain('Restmülltonne');
    expect(rows[1]).toContain('Singletonne (Restmüll)');
  });

  // --- Der Stand wird beim erneuten Betreten neu gelesen (AC11, R6) ---------

  /**
   * AC11: Der Stand wird beim erneuten Betreten neu gelesen. Ohne diesen Test
   * bleibt der Fehler unbemerkt: Ein Feature, das nur einmal geöffnet wurde,
   * zeigt beim zweiten Mal die alten Termine — der Nutzer glaubt, das Speichern
   * habe nicht gewirkt.
   */
  it('reads the overview again after the page was left and entered again', async () => {
    const harness = await RouterTestingHarness.create();

    // Erster Aufruf: der Stand wird gelesen.
    await harness.navigateByUrl('/muelltermine');
    httpMock.expectOne('/api/households/1/waste').flush(uebersicht);
    await tick(harness.fixture);
    expect(harness.fixture.nativeElement.textContent).toContain('Restmüll');

    // Das Feature wird verlassen …
    await harness.navigateByUrl('/anders');

    // … und erneut betreten: der Stand muss neu gelesen werden, sonst zeigt
    // die Seite die Termine des letzten Besuchs (R6, ## Nutzerfluss Schritt 7).
    await harness.navigateByUrl('/muelltermine');
    httpMock.expectOne('/api/households/1/waste').flush(uebersicht);
    await tick(harness.fixture);

    expect(harness.fixture.nativeElement.textContent).toContain('Restmüll');
  });

  /**
   * AC11 in der Konfiguration der Anwendung: der Stand wird beim erneuten
   * Betreten neu gelesen — und zwar **genau einmal** je Betreten.
   *
   * <p>Ursache (Angular 22): `httpResource` feuert einen Request nur, wenn sich
   * die Request-URL ändert. Das `WasteData` ist route-scoped; mit
   * `withAutoCleanupInjectors()` zerstört der Router den Route-Injector beim
   * Verlassen der Route — die Instanz (und ihr Resource mit dem alten Wert)
   * überlebt den Wegwechsel nicht. Ohne diese Aufräumregel zeigte der zweite
   * Aufruf die alten Termine.
   *
   * <p>Die Zählung ist der Kern: ein Betreten, ein GET. Zwei GETs wären ein
   * Fehler (doppelte Last, doppelter Lesezugriff), null GETs ein zweiter.
   */
  it('reads the overview exactly once per entry', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/muelltermine');
    const first = requests();
    expect(first).toHaveLength(1);
    first[0].flush(uebersicht);
    await tick(harness.fixture);
    expect(harness.fixture.nativeElement.textContent).toContain('Restmüll');

    // Das Feature wird verlassen …
    await harness.navigateByUrl('/anders');
    expect(requests()).toHaveLength(0);

    // … und erneut betreten: genau ein neuer Lesezugriff.
    await harness.navigateByUrl('/muelltermine');
    const second = requests();
    expect(second).toHaveLength(1);
    second[0].flush(uebersicht);
    await tick(harness.fixture);

    expect(harness.fixture.nativeElement.textContent).toContain('Restmüll');

    // Und noch einmal: kein zweiter GET je Betreten.
    await harness.navigateByUrl('/anders');
    await harness.navigateByUrl('/muelltermine');
    const third = requests();
    expect(third).toHaveLength(1);
    third[0].flush(uebersicht);
    await tick(harness.fixture);
  });
});
