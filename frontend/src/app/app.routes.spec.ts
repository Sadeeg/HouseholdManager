import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterOutlet,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { startRouteGuard } from './core/auth/start-route.guard';

function flushMicrotasks(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

function currentUser(id: string): { id: string; displayName: string; email: string } {
  return { id, displayName: id, email: `${id}@example.org` };
}

/**
 * Die Entscheidung des Guards: wohin führt die Start-Route nach der Anmeldung?
 * Alles ohne echten Backend-Zugriff — der Workspace wird über
 * provideHttpClientTesting() bedient (AC3).
 */
describe('Start-Route-Guard', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function runGuard(): Promise<UrlTree> {
    // Der Guard nutzt Route und State nicht — die Signatur verlangt sie aber.
    const route = {} as ActivatedRouteSnapshot;
    const state = {} as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => startRouteGuard(route, state)) as Promise<UrlTree>;
  }

  it('routes to the tasks board when a household is selected (AC1)', async () => {
    const route = runGuard();

    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haus A', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await flushMicrotasks();
    httpMock.expectOne('/api/me/workspace').flush(1);
    httpMock.expectOne('/api/auth/me').flush(currentUser('alice'));

    const target = await route;
    expect(target.toString()).toBe('/aufgaben');
  });

  it('routes to the households page when no membership exists (AC2)', async () => {
    const route = runGuard();

    // Keine Mitgliedschaft — und keine gemerkte Auswahl (404 = normaler Zustand).
    httpMock.expectOne('/api/households').flush([]);
    await flushMicrotasks();
    httpMock.expectOne('/api/me/workspace').flush(null, { status: 404, statusText: 'Not Found' });
    httpMock.expectOne('/api/auth/me').flush(currentUser('alice'));

    const target = await route;
    expect(target.toString()).toBe('/haushalte');
  });

  // Gegen die Tautologie „entscheidet nach irgendeiner Mitgliedschaft“:
  // Mitgliedschaft vorhanden, Auswahl nicht erreichbar (500) → kein gewählter
  // Haushalt. Die Entscheidung muss aus der aktiven Auswahl folgen.
  it('keeps a membership with an unreachable selection at the households page (AC2, error path)', async () => {
    const route = runGuard();

    httpMock.expectOne('/api/households').flush([
      { id: 2, name: 'Haus B', createdAt: '2026-01-01T00:00:00Z', createdBy: 'bob' },
    ]);
    await flushMicrotasks();
    httpMock.expectOne('/api/me/workspace').flush(null, { status: 500, statusText: 'Server Error' });
    httpMock.expectOne('/api/auth/me').flush(currentUser('bob'));

    const target = await route;
    expect(target.toString()).toBe('/haushalte');
  });

  // Fehlerpfad „Haushaltsliste nicht erreichbar“ (loadFailed): ohne Daten
  // bleibt die Auswahl null → Haushaltsverwaltung.
  it('routes to the households page when the household list fails to load (error path)', async () => {
    const route = runGuard();

    httpMock.expectOne('/api/households').flush(null, {
      status: 503,
      statusText: 'Service Unavailable',
    });

    const target = await route;
    expect(target.toString()).toBe('/haushalte');
  });
});

/**
 * Die Start-Route selbst: sie muss in Dev-Modus validierbar sein (NG04014
 * verbietet reine Guard-Routen ohne Ziel) und den Guard tatsächlich aufrufen.
 */
describe('Start-Route wiring in app.routes', () => {
  let httpMock: HttpTestingController;

  @Component({ imports: [RouterOutlet], template: '<router-outlet />' })
  class TestHost {}

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestHost],
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: OAuthService, useValue: { hasValidAccessToken: () => true } },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('resolves the start route over the guard to the tasks board (AC1)', async () => {
    const router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(TestHost);
    fixture.detectChanges();

    const navigating = router.navigateByUrl('');

    // Die Guard-Kette läuft über Mikrotasks — der erste Request ist erst danach da.
    await flushMicrotasks();
    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haus A', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await flushMicrotasks();
    httpMock.expectOne('/api/me/workspace').flush(1);
    httpMock.expectOne('/api/auth/me').flush(currentUser('alice'));

    await navigating;
    expect(router.url).toBe('/aufgaben');

    // Nach der Umleitung aktiviert sich das Aufgaben-Feature; seine Lesezugriffe
    // sind hier nicht gemeint und werden entleert, damit verify() leer bleibt.
    httpMock.match(() => true).forEach((req) => req.flush([]));
  });

  // Der andere Zweig durch den echten Router: ohne Auswahl führt die Start-Route
  // zur Haushaltsverwaltung. Der Guard-Aufruf allein beweist nicht, dass der
  // Router das Ergebnis übernimmt und die Route erreichbar ist — beides wird geprüft.
  it('resolves the start route over the guard to the households page (AC2)', async () => {
    const router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(TestHost);
    fixture.detectChanges();

    const navigating = router.navigateByUrl('');

    await flushMicrotasks();
    httpMock.expectOne('/api/households').flush([]);
    await flushMicrotasks();
    httpMock.expectOne('/api/me/workspace').flush(null, { status: 404, statusText: 'Not Found' });
    httpMock.expectOne('/api/auth/me').flush(currentUser('alice'));

    await navigating;
    expect(router.url).toBe('/haushalte');

    httpMock.match(() => true).forEach((req) => req.flush([]));
  });
});
