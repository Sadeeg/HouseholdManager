import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, inject } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { UserSearch } from './user-search';

/** Sonde: injiziert die Suche so, wie eine Komponente im Feature es tut. */
@Component({ selector: 'app-probe', template: '' })
class Probe {
  readonly search = inject(UserSearch);
}

describe('UserSearch', () => {
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

  it('is root-provided: every injection gets the app-wide instance', () => {
    const fixture = TestBed.createComponent(Probe);

    expect(fixture.componentInstance.search).toBe(TestBed.inject(UserSearch));
  });

  it('searches the directory only from two characters on', () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);

    search.search('c');
    fixture.detectChanges();
    httpMock.expectNone('/api/users/search?q=c');

    search.search('ca');
    fixture.detectChanges();
    httpMock.expectOne('/api/users/search?q=ca').flush([]);
  });

  it('drops query and results on reset', async () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);
    fixture.detectChanges();

    search.search('car');
    fixture.detectChanges();
    httpMock.expectOne('/api/users/search?q=car').flush([{ id: 'carl', displayName: 'Carl Vogel' }]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(search.results().length).toBe(1);

    search.reset();
    fixture.detectChanges();

    expect(search.query()).toBe('');
    expect(search.results()).toEqual([]);
  });

  it('reports the error and returns no results instead of throwing', async () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);
    fixture.detectChanges();

    search.search('car');
    fixture.detectChanges();
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    // `httpResource.value()` wirft im Fehlerzustand — der Dienst darf den
    // Wert dort nicht lesen (Issue #18).
    expect(search.failed()).toBe(true);
    expect(search.results()).toEqual([]);
  });

  it('reports the error for a 404 as well', async () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);
    fixture.detectChanges();

    search.search('car');
    fixture.detectChanges();
    httpMock.expectOne('/api/users/search?q=car').flush('not found', {
      status: 404,
      statusText: 'Not Found',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(search.failed()).toBe(true);
    expect(search.results()).toEqual([]);
  });

  it('reports the error even when the error body is empty', async () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);
    fixture.detectChanges();

    search.search('car');
    fixture.detectChanges();
    httpMock.expectOne('/api/users/search?q=car').flush(null, {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(search.failed()).toBe(true);
    expect(search.results()).toEqual([]);
  });

  it('reports the error when the backend is unreachable (network error)', async () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);
    fixture.detectChanges();

    search.search('car');
    fixture.detectChanges();
    // Status 0: der Request kommt gar nicht erst an (Backend gestoppt).
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 0,
      statusText: 'Unknown Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(search.failed()).toBe(true);
    expect(search.results()).toEqual([]);
  });

  it('reports no error when the query changes during a running request', async () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);
    fixture.detectChanges();

    search.search('car');
    fixture.detectChanges();
    const first = httpMock.expectOne('/api/users/search?q=car');

    // Der Begriff wird gewechselt, während der erste Request läuft: der
    // Request wird abgebrochen — das ist kein Fehler des Verzeichnisses.
    search.search('carla');
    fixture.detectChanges();

    expect(first.cancelled).toBe(true);
    httpMock.expectOne('/api/users/search?q=carla').flush([
      { id: 'carla', displayName: 'Carla Simpson' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(search.failed()).toBe(false);
    expect(search.results()).toEqual([{ id: 'carla', displayName: 'Carla Simpson' }]);
  });

  it('clears the error when the query is reset', async () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);
    fixture.detectChanges();

    search.search('car');
    fixture.detectChanges();
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(search.failed()).toBe(true);

    search.reset();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(search.failed()).toBe(false);
    expect(search.results()).toEqual([]);
  });

  /**
   * Issue #20: derselbe Suchbegriff lässt sich erneut suchen — ohne den
   * Begriff zu verändern. Vorher löste das Setzen desselben Begriffs keinen
   * zweiten Request aus, der Fehlerzustand blieb stehen.
   */
  it('starts a new request with the same query on retry', async () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);
    fixture.detectChanges();

    search.search('car');
    fixture.detectChanges();
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(search.failed()).toBe(true);

    search.retry();
    fixture.detectChanges();

    // Genau ein neuer Request mit derselben URL.
    httpMock.expectOne('/api/users/search?q=car').flush([{ id: 'carl', displayName: 'Carl Vogel' }]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(search.failed()).toBe(false);
    expect(search.results()).toEqual([{ id: 'carl', displayName: 'Carl Vogel' }]);
  });

  /** Ohne Suche gibt es nichts zu wiederholen. */
  it('does nothing on retry without a query', () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(Probe);
    fixture.detectChanges();

    search.retry();
    fixture.detectChanges();

    httpMock.expectNone('/api/users/search?q=');
  });
});
