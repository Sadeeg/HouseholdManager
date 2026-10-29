import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';

import { WorkspaceService } from './workspace-service';
import { WorkspaceSwitcher } from './workspace-switcher';

function tick(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

async function presetWorkspace(
  httpMock: HttpTestingController,
  households: { id: number; name: string }[],
  selected: number | null,
): Promise<void> {
  const workspace = TestBed.inject(WorkspaceService);
  const loading = workspace.ensureLoaded();
  httpMock.expectOne('/api/households').flush(households);
  await tick();
  if (selected === null) {
    httpMock.expectOne('/api/me/workspace').flush(null, { status: 404, statusText: 'Not Found' });
  } else {
    httpMock.expectOne('/api/me/workspace').flush(selected);
  }
  httpMock.expectOne('/api/auth/me').flush({ id: 'alice', displayName: 'Alice', email: 'a@example.org' });
  await loading;
}

describe('WorkspaceSwitcher', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [WorkspaceSwitcher],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function openList(fixture: ReturnType<typeof TestBed.createComponent>): HTMLElement {
    const compiled = fixture.nativeElement as HTMLElement;
    (compiled.querySelector('.workspace-switcher__trigger') as HTMLButtonElement).click();
    fixture.detectChanges();
    return compiled;
  }

  it('shows the current household in the app header (AC23)', async () => {
    await presetWorkspace(httpMock, [{ id: 1, name: 'Haus A' }], 1);

    const fixture = TestBed.createComponent(WorkspaceSwitcher);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.workspace-switcher__trigger')?.textContent).toContain('Haus A');
  });

  it('lists only the households the person belongs to (AC12)', async () => {
    await presetWorkspace(httpMock, [{ id: 1, name: 'Haus A' }, { id: 2, name: 'Haus B' }], 1);

    const fixture = TestBed.createComponent(WorkspaceSwitcher);
    fixture.detectChanges();
    const compiled = openList(fixture);

    const options = compiled.querySelectorAll('[role="option"]');
    expect(options.length).toBe(2);
    expect(options[0].textContent).toContain('Haus A');
    expect(options[0].textContent).toContain('AKTUELL');
    expect(options[1].textContent).toContain('Haus B');
  });

  it('switches the workspace on selection without reloading the page (AC23)', async () => {
    await presetWorkspace(httpMock, [{ id: 1, name: 'Haus A' }, { id: 2, name: 'Haus B' }], 1);

    const fixture = TestBed.createComponent(WorkspaceSwitcher);
    fixture.detectChanges();
    const compiled = openList(fixture);

    const second = compiled.querySelectorAll('[role="option"]')[1] as HTMLButtonElement;
    second.click();
    httpMock.expectOne('/api/me/workspace', 'PUT').flush({});
    await tick();
    fixture.detectChanges();

    const workspace = TestBed.inject(WorkspaceService);
    expect(workspace.currentHouseholdId()).toBe(2);
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.workspace-switcher__trigger')?.textContent,
    ).toContain('Haus B');
  });

  it('shows the empty hint when the person is in no household', async () => {
    await presetWorkspace(httpMock, [], null);

    const fixture = TestBed.createComponent(WorkspaceSwitcher);
    fixture.detectChanges();
    const compiled = openList(fixture);

    expect(compiled.querySelector('.workspace-switcher__trigger')?.textContent).toContain('Kein Haushalt');
    expect(compiled.textContent).toContain('Du bist in keinem Haushalt ernannt.');
  });
});
