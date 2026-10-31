import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { WastePage } from './waste-page';
import { WasteData } from './data/waste-data';
import { WasteOverview } from './model/waste.model';
import { WorkspaceService } from '../../core/workspace/workspace-service';

/**
 * AC14, AC15, AC16, AC17 und R11 — die Zustände der Seite, über den
 * Data-Service gelesen. Der Fehlerpfad und die leere Liste werden über
 * dieselbe Seite geprüft, weil die Seite über sie entscheidet.
 */
describe('WastePage', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [WastePage],
      providers: [
        provideRouter([]),
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
});
