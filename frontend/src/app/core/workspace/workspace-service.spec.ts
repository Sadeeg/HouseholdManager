import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WorkspaceService } from './workspace-service';

describe('WorkspaceService', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  async function load(service: WorkspaceService) {
    const loading = service.ensureLoaded();
    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haus A', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
      { id: 2, name: 'Haus B', createdAt: '2026-01-02T00:00:00Z', createdBy: 'bob' },
    ]);
    await flushMicrotasks();
    httpMock.expectOne('/api/me/workspace').flush(1);
    httpMock.expectOne('/api/auth/me').flush({ id: 'alice', displayName: 'Alice', email: 'a@example.org' });
    await loading;
  }

  function flushMicrotasks(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, 0));
  }

  it('loads households, selection and current user once (AC23)', async () => {
    const service = TestBed.inject(WorkspaceService);
    await load(service);

    expect(service.availableHouseholds()).toHaveLength(2);
    expect(service.currentHouseholdId()).toBe(1);
    expect(service.currentHousehold()?.name).toBe('Haus A');
    expect(service.currentUser()?.id).toBe('alice');

    // Der zweite Aufruf erzeugt keine weiteren Requests.
    await service.ensureLoaded();
    httpMock.verify();
  });

  it('treats a missing selection (404) as no workspace', async () => {
    const service = TestBed.inject(WorkspaceService);
    const loading = service.ensureLoaded();
    httpMock.expectOne('/api/households').flush([{ id: 1, name: 'Haus A', createdBy: 'alice' }]);
    await flushMicrotasks();
    httpMock.expectOne('/api/me/workspace').flush(null, { status: 404, statusText: 'Not Found' });
    httpMock.expectOne('/api/auth/me').flush({ id: 'alice', displayName: 'Alice', email: 'a@example.org' });
    await loading;

    expect(service.currentHouseholdId()).toBeNull();
  });

  it('drops a stored selection that is no longer a reachable household', async () => {
    const service = TestBed.inject(WorkspaceService);
    const loading = service.ensureLoaded();
    httpMock.expectOne('/api/households').flush([{ id: 1, name: 'Haus A', createdBy: 'alice' }]);
    await flushMicrotasks();
    httpMock.expectOne('/api/me/workspace').flush(99);
    httpMock.expectOne('/api/auth/me').flush({ id: 'alice', displayName: 'Alice', email: 'a@example.org' });
    await loading;

    expect(service.currentHouseholdId()).toBeNull();
  });

  it('switches the workspace with a PUT and without any reload (AC23)', async () => {
    const service = TestBed.inject(WorkspaceService);
    await load(service);

    const switching = service.selectHousehold(2);
    httpMock.expectOne('/api/me/workspace', 'PUT').flush({});
    const switched = await switching;

    expect(switched).toBe(true);
    expect(service.currentHouseholdId()).toBe(2);
    expect(service.currentHousehold()?.name).toBe('Haus B');
    // Kein einziger GET wurde erneut ausgelöst.
    httpMock.verify();
  });

  it('keeps the old workspace when the backend rejects the switch (403)', async () => {
    const service = TestBed.inject(WorkspaceService);
    await load(service);

    const switching = service.selectHousehold(2);
    httpMock.expectOne('/api/me/workspace', 'PUT').flush(null, { status: 403, statusText: 'Forbidden' });
    const accepted = await switching;

    expect(accepted).toBe(false);
    expect(service.currentHouseholdId()).toBe(1);
  });
});
