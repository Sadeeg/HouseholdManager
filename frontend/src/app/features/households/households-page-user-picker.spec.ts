import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WorkspaceService } from '../../core/workspace/workspace-service';
import { HouseholdsData } from './data/households-data';
import { HouseholdsPage } from './households-page';

/**
 * Sonde des kritischen Testers zu Issue #4: der geteilte Zustand von
 * `UserSearch` ist nur sicher, wenn nie zwei Suchfelder gleichzeitig offen
 * sind. Dieser Test belegt das für die Seite „Haushalte".
 */
describe('HouseholdsPage — nur ein Suchfeld gleichzeitig', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HouseholdsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), HouseholdsData],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  async function tick(fixture: ReturnType<typeof TestBed.createComponent>): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  it('shows exactly one user search field, also with several households', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice Admin' });

    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
      { id: 2, name: 'Haushalt Schmidt', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await tick(fixture);

    const compiled = fixture.nativeElement as HTMLElement;
    const actions = compiled.querySelectorAll('.households__members-action');
    expect(actions.length).toBe(2);

    (actions[0] as HTMLButtonElement).click();
    fixture.detectChanges();
    await tick(fixture);
    httpMock.expectOne('/api/households/1/members').flush([]);
    await tick(fixture);

    expect(compiled.querySelectorAll('app-user-picker').length).toBe(1);

    // Der zweite Haushalt öffnet: das erste Feld verschwindet, es bleibt
    // genau eines — der geteilte Zustand kann so nicht auseinanderlaufen.
    (compiled.querySelectorAll('.households__members-action')[1] as HTMLButtonElement).click();
    fixture.detectChanges();
    await tick(fixture);
    httpMock.expectOne('/api/households/2/members').flush([]);
    await tick(fixture);

    expect(compiled.querySelectorAll('app-user-picker').length).toBe(1);
  });
});
