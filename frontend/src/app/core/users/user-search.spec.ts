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
});
