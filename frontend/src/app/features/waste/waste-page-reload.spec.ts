import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { WastePage } from './waste-page';
import { WasteData } from './data/waste-data';
import { WorkspaceService } from '../../core/workspace/workspace-service';

/** Eine zweite Route, damit das Feature verlassen und erneut betreten wird. */
@Component({ template: 'andere Seite' })
class AndereSeite {}

/**
 * AC11 (R6) in der Anzeige: „Der Server synchronisiert den Haushalt sofort …
 * die Anzeige zeigt die Termine beim nächsten Aufruf" (## Nutzerfluss,
 * Schritt 7) und „„Mülltermine" zeigt den neuen Stand beim nächsten Aufruf"
 * (## UX, Screen „Müllkalender").
 *
 * <p>Der Test betritt die Seite zweimal und verlangt, dass der Stand **beim
 * zweiten Betreten erneut gelesen** wird. Ohne diesen Test bleibt der Fehler
 * unbemerkt: Ein Feature, das nur einmal geöffnet wurde, zeigt beim zweiten
 * Mal die alten Termine — der Nutzer glaubt, das Speichern habe nicht
 * gewirkt.
 *
 * <p>Ursache (Angular 22): `httpResource` feurt einen Request nur, wenn sich
 * die Request-URL ändert. Das `WasteData` ist route-scoped, aber ohne
 * `withAutoCleanupInjectors()` zerstört der Router den Route-Injector beim
 * Verlassen der Route **nicht** — die Instanz (und ihr Resource mit dem
 * alten Wert) überlebt den Wegwechsel. Der zweite Aufruf liest deshalb
 * nichts neu.
 */
describe('WastePage — der Stand wird beim erneuten Betreten neu gelesen', () => {
  let httpMock: HttpTestingController;

  const uebersicht = {
    sourceUrl: 'https://entsorger.example/ics',
    lastSyncedAt: '2026-01-01T03:00:00Z',
    stale: false,
    collections: [{ date: '2026-01-09', wasteType: 'Restmüll' }],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'muelltermine', component: WastePage, providers: [WasteData] },
          { path: 'anders', component: AndereSeite },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
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

  async function tick(fixture: ReturnType<typeof TestBed.createComponent>): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

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
});
