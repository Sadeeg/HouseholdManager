import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WorkspaceService } from '../../core/workspace/workspace-service';
import { HouseholdsPage } from './households-page';
import { HouseholdsData } from './data/households-data';

describe('HouseholdsPage', () => {
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

  function flushHouseholds(createdBy: string): void {
    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy },
    ]);
  }

  /** Ein Makrotask reicht, damit die Resource ihren Wert aktualisiert. */
  async function tick(fixture: ReturnType<typeof TestBed.createComponent>): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  it('renders the loaded households', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();

    flushHouseholds('alice');
    await fixture.whenStable();
    fixture.detectChanges();

    const items = (fixture.nativeElement as HTMLElement).querySelectorAll('app-household-item');
    expect(items.length).toBe(1);
    expect(items[0].textContent).toContain('Haushalt Müller');
  });

  it('shows a hint when there is no household yet', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();

    httpMock.expectOne('/api/households').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.state')?.textContent).toContain('noch kein Haushalt');
  });

  it('shows the „Mitglieder“ action only for the household admin (AC11)', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice Admin' });

    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    flushHouseholds('alice');
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.households__members-action')).not.toBeNull();

    // Eine andere Person ist nicht die Leitung.
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [HouseholdsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), HouseholdsData],
    }).compileComponents();
    const secondHttp = TestBed.inject(HttpTestingController);
    TestBed.inject(WorkspaceService).currentUser.set({ id: 'bob', displayName: 'Bob' });

    const second = TestBed.createComponent(HouseholdsPage);
    second.detectChanges();
    secondHttp.expectOne('/api/households').flush([
      { id: 1, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await second.whenStable();
    second.detectChanges();

    expect((second.nativeElement as HTMLElement).querySelector('.households__members-action')).toBeNull();
    secondHttp.verify();
  });

  it('opens the member section inline and lists the members', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice Admin' });

    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    flushHouseholds('alice');
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    (compiled.querySelector('.households__members-action') as HTMLButtonElement).click();
    fixture.detectChanges();

    await new Promise((resolve) => setTimeout(resolve, 0));
    httpMock.expectOne('/api/households/1/members').flush([
      { userId: 'alice', displayName: 'Alice Admin', householdAdmin: true },
      { userId: 'bob', displayName: 'Bob Mitglied', householdAdmin: false },
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();

    expect(compiled.querySelector('app-member-section')).not.toBeNull();
    expect(compiled.querySelectorAll('.member-section__row').length).toBe(2);
    expect(compiled.textContent).toContain('HAUSHALTSLEITUNG');
  });

  it('creates a household, adopts it as workspace and lists it', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice Admin' });

    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([]);
    await tick(fixture);

    const compiled = fixture.nativeElement as HTMLElement;
    // Das Formular öffnet sich über die Primäraktion im Kopf.
    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.includes('Haushalt anlegen'))
      ?.click();
    fixture.detectChanges();

    const input = compiled.querySelector('#household-name') as HTMLInputElement;
    expect(input).not.toBeNull();
    input.value = 'Haushalt Müller';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'Anlegen')
      ?.click();

    const create = httpMock.expectOne('/api/households', 'POST');
    expect(create.request.body).toEqual({ name: 'Haushalt Müller' });
    create.flush({
      id: 7,
      name: 'Haushalt Müller',
      createdAt: '2026-01-01T00:00:00Z',
      createdBy: 'alice',
    });

    // Die Liste lädt neu, der neue Haushalt wird zum Workspace.
    await tick(fixture);
    httpMock.expectOne('/api/households').flush([
      { id: 7, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await tick(fixture);

    const select = httpMock.expectOne('/api/me/workspace', 'PUT');
    expect(select.request.body).toEqual({ householdId: 7 });
    select.flush({});
    await tick(fixture);

    expect(workspace.currentHouseholdId()).toBe(7);
    expect(workspace.availableHouseholds().map((household) => household.name)).toContain(
      'Haushalt Müller',
    );
    expect(compiled.textContent).toContain('Haushalt Müller');
    expect(compiled.querySelector('.toast')?.textContent).toContain('Haushalt angelegt.');
  });

  it('rejects an empty household name and keeps the form open', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([]);
    await tick(fixture);

    const compiled = fixture.nativeElement as HTMLElement;
    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.includes('Haushalt anlegen'))
      ?.click();
    fixture.detectChanges();

    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'Anlegen')
      ?.click();
    fixture.detectChanges();

    expect(compiled.textContent).toContain('Der Name fehlt.');
    expect(compiled.querySelector('#household-name')).not.toBeNull();
    httpMock.expectNone('/api/households', 'POST');
  });
});
