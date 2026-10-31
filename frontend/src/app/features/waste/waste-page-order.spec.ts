import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { WastePage } from './waste-page';
import { WasteData } from './data/waste-data';
import { WorkspaceService } from '../../core/workspace/workspace-service';

/**
 * AC4: „Die Anzeige listet alle synchronisierten Termine des aktiven Haushalts
 * aufsteigend nach Datum mit Tonnenart und Datum."
 *
 * <p>`waste-collection-list.spec.ts` prüft die Liste als Component: sie rendert
 * die Termine in der Reihenfolge, in der sie ihr übergeben werden. Die
 * Reihenfolge entsteht aber eine Ebene darüber — der Data-Service sortiert
 * (R9, Annahme 8). Dieser Test prüft deshalb die Anzeige als Ganzes: die Seite
 * liefert die Termine unsortiert vom Server und muss sie aufsteigend nach Datum,
 * bei gleichem Datum nach Tonnenart, darstellen.
 */
describe('WastePage — Reihenfolge der angezeigten Termine', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [WastePage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), WasteData],
    });
    httpMock = TestBed.inject(HttpTestingController);

    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice' });
    workspace.currentHouseholdId.set(1);
  });

  afterEach(() => {
    httpMock.verify();
  });

  async function tick(fixture: ReturnType<typeof TestBed.createComponent>): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  /** Die Zeilen der Terminliste in der Reihenfolge, in der sie erscheinen. */
  function rowsIn(compiled: HTMLElement): string[] {
    return [...compiled.querySelectorAll('.waste-list__row')].map((row) =>
      (row.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
  }

  it('lists every term ascending by date even when the server delivers them unsorted', async () => {
    const fixture = TestBed.createComponent(WastePage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households/1/waste').flush({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2026-01-01T03:00:00Z',
      stale: false,
      collections: [
        { date: '2026-01-20', wasteType: 'Papier' },
        { date: '2026-01-05', wasteType: 'Glas' },
        { date: '2026-01-12', wasteType: 'Restmüll' },
      ],
    });
    await tick(fixture);

    const rows = rowsIn(fixture.nativeElement as HTMLElement);

    expect(rows).toHaveLength(3);
    expect(rows[0]).toContain('05.01.2026');
    expect(rows[0]).toContain('Glas');
    expect(rows[1]).toContain('12.01.2026');
    expect(rows[1]).toContain('Restmüll');
    expect(rows[2]).toContain('20.01.2026');
    expect(rows[2]).toContain('Papier');
  });

  it('orders by waste type when two terms fall on the same day', async () => {
    const fixture = TestBed.createComponent(WastePage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households/1/waste').flush({
      sourceUrl: 'https://entsorger.example/ics',
      lastSyncedAt: '2026-01-01T03:00:00Z',
      stale: false,
      collections: [
        { date: '2026-01-12', wasteType: 'Singletonne (Restmüll)' },
        { date: '2026-01-12', wasteType: 'Restmülltonne' },
      ],
    });
    await tick(fixture);

    const rows = rowsIn(fixture.nativeElement as HTMLElement);

    expect(rows).toHaveLength(2);
    expect(rows[0]).toContain('Restmülltonne');
    expect(rows[1]).toContain('Singletonne (Restmüll)');
  });
});
