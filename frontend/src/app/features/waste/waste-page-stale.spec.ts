import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { WastePage } from './waste-page';
import { WasteData } from './data/waste-data';
import { WorkspaceService } from '../../core/workspace/workspace-service';

/**
 * AC6 (R7, R17): „Schlägt ein Synchronisationslauf fehl, bleiben die
 * gespeicherten Termine unverändert erhalten, und die Anzeige weist aus, dass
 * der Stand veraltet ist."
 *
 * <p>Die Anzeige trägt den veralteten Stand nicht nur farblich: sie zeigt den
 * Badge „STAND VERALTET" und eine Statuszeile mit `role="status"`, die
 * erklärt, dass die Termine aus dem letzten erfolgreichen Lauf stammen. Der
 * vorhandene Test `waste-page.spec.ts` prüft den Badge — die Statuszeile ist
 * damit nicht belegt. Dieser Test schließt die Lücke.
 */
describe('WastePage — veralteter Stand', () => {
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

  /** Zoneless: ein Makrotask reicht, damit die Antwort wirksam wird. */
  async function tick(fixture: ReturnType<typeof TestBed.createComponent>): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  async function open(overview: {
    sourceUrl: string | null;
    lastSyncedAt: string | null;
    stale: boolean;
    collections: { date: string; wasteType: string }[];
  }): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(WastePage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households/1/waste').flush(overview);
    await tick(fixture);
    return fixture.nativeElement as HTMLElement;
  }

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
});
