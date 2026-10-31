import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { WastePage } from './waste-page';
import { WasteData } from './data/waste-data';
import { WorkspaceService } from '../../core/workspace/workspace-service';

/**
 * AC16: Liegt noch kein erfolgreicher Lauf vor, nennt die Anzeige
 * „Noch keine Synchronisation". Das gilt auch dann, wenn zugleich keine
 * Quelle hinterlegt ist — die UX des Issues sagt das ausdrücklich
 * („gilt auch zusammen mit ‚Kein Müllkalender hinterlegt'"): Der Stand
 * steht im Kopf, unabhängig davon, ob eine Quelle gesetzt ist.
 *
 * <p>Der vorhandene Test in `waste-page.spec.ts` deckt den Fall „Quelle
 * vorhanden, kein Lauf" ab. Dieser deckt die Kombination ab, die in der
 * Oberfläche den ganzen Bereich „Quelle und Stand" unterdrückt.
 */
describe('WastePage ohne hinterlegte Quelle', () => {
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

  it('names the missing run even without a stored source', async () => {
    const fixture = TestBed.createComponent(WastePage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households/1/waste').flush({
      sourceUrl: null,
      lastSyncedAt: null,
      stale: false,
      collections: [],
    });
    await tick(fixture);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Kein Müllkalender hinterlegt');
    expect(compiled.textContent).toContain('Noch keine Synchronisation');
  });
});
