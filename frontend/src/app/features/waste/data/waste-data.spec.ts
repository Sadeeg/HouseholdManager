import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WorkspaceService } from '../../../core/workspace/workspace-service';
import { WasteData } from './waste-data';

/**
 * Der Lesezugriff auf die Übersicht — die Ebene, die die Reihenfolge entscheidet.
 *
 * <p>AC4 (Issue #11, Befund 2): die Anzeige listet die Termine aufsteigend nach
 * Datum, bei gleichem Datum nach Tonnenart. `waste-page.spec.ts` prüft die
 * Anzeige — ein Component, der das bekommt, was er bekommt, bleibt bei falscher
 * Sortierung grün. Die Sortierung entsteht hier, in `terms()`. Dieser Test
 * belegt die Entscheidung selbst: die Antwort kommt unsortiert, `terms()`
 * liefert sie sortiert.
 */
describe('WasteData', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), WasteData],
    });
    httpMock = TestBed.inject(HttpTestingController);

    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice' });
    workspace.currentHouseholdId.set(1);
  });

  afterEach(() => {
    httpMock.verify();
  });

  /**
   * AC4: die geladene Liste wird aufsteigend nach Datum sortiert, bei gleichem
   * Datum nach Tonnenart — unabhängig von der Reihenfolge, in der die Antwort
   * sie liefert.
   */
  it('sorts the read collections by date, then by waste type (AC4)', async () => {
    const data = TestBed.inject(WasteData);

    // Zoneless: der Effekt von `httpResource` läuft erst, wenn er ausdrücklich
    // ausgelöst wird — hier durch `flushEffects()`, weil dieser Test ohne
    // Component auf der Datenebene prüft.
    TestBed.flushEffects();
    httpMock.expectOne('/api/households/1/waste').flush({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2026-01-01T03:00:00Z',
      stale: false,
      collections: [
        { date: '2026-01-20', wasteType: 'Papier' },
        { date: '2026-01-05', wasteType: 'Glas' },
        { date: '2026-01-12', wasteType: 'Singletonne (Restmüll)' },
        { date: '2026-01-12', wasteType: 'Restmülltonne' },
      ],
    });
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(data.terms().map((term) => `${term.date} ${term.wasteType}`)).toEqual([
      '2026-01-05 Glas',
      '2026-01-12 Restmülltonne',
      '2026-01-12 Singletonne (Restmüll)',
      '2026-01-20 Papier',
    ]);
  });

  /**
   * AC4, Grenzfall: die Tonnenart wird nach deutscher Kollation verglichen, nicht
   * nach Code-Points. Ein Umlaut steht in der deutschen Sortierung hinter seinem
   * Basisbuchstaben und damit vor einem 'B' — eine reine Zeichenvergleichung
   * ('a' < 'b' über Code-Points) würde hier 'Biotonne' zuerst liefern.
   */
  it('orders the waste type by German collation, not by code point (AC4)', async () => {
    const data = TestBed.inject(WasteData);

    TestBed.flushEffects();
    httpMock.expectOne('/api/households/1/waste').flush({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2026-01-01T03:00:00Z',
      stale: false,
      collections: [
        { date: '2026-01-12', wasteType: 'Biotonne' },
        { date: '2026-01-12', wasteType: 'Ähm-Tonne' },
      ],
    });
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(data.terms().map((term) => term.wasteType)).toEqual(['Ähm-Tonne', 'Biotonne']);
  });
});
