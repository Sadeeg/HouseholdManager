import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withAutoCleanupInjectors } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { WastePage } from './waste-page';
import { WasteData } from './data/waste-data';
import { WorkspaceService } from '../../core/workspace/workspace-service';

/** Eine zweite Route, damit das Feature verlassen und erneut betreten wird. */
@Component({ template: 'andere Seite' })
class AndereSeite {}

/**
 * AC11 (R6) in der Konfiguration der Anwendung: „der Stand wird beim erneuten
 * Betreten neu gelesen" — und zwar **genau einmal** je Betreten.
 *
 * <p>`waste-page-reload.spec.ts` prüft das mit einem Router ohne
 * `withAutoCleanupInjectors()`. Die Anwendung konfiguriert den Router aber
 * genau so (siehe `app.config.ts`): die route-scoped Provider werden beim
 * Verlassen der Route zerstört. Das ist ein anderer Lebenszyklus für den
 * Data-Service — der Test muss die Konfiguration der Anwendung treffen,
 * sonst belegt er einen Zustand, den es im Betrieb nicht gibt.
 *
 * <p>Die Zählung ist der Kern: ein Betreten, ein GET. Zwei GETs wären ein
 * Fehler (doppelte Last, doppelter Lesezugriff), null GETs ein zweiter
 * (die Anzeige zeigte den alten Stand).
 */
describe('WastePage — genau ein GET je Betreten (Konfiguration der Anwendung)', () => {
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
        provideRouter(
          [
            { path: 'muelltermine', component: WastePage, providers: [WasteData] },
            { path: 'anders', component: AndereSeite },
          ],
          withAutoCleanupInjectors(),
        ),
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

  /**
   * Die Anfragen an die Übersicht — genau eine je Betreten. `match` nimmt die
   * Anfragen aus der Warteschlange, deshalb wird hier auch geantwortet.
   */
  function requests(): TestRequest[] {
    return httpMock.match('/api/households/1/waste');
  }

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
