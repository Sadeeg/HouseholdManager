import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UserPicker } from './user-picker';

/**
 * Fehlerpfad der Personensuche (Issue #18).
 *
 * Der Fehlerzustand der Suche darf die Komponente nicht mehr während des
 * Renderns abbrechen; stattdessen muss die Meldung „Nutzerverzeichnis nicht
 * erreichbar." als Alert erreichbar sein — und zwar sichtbar, ohne den
 * Leere-Hinweis daneben, und wieder verschwindend, sobald eine neue Suche
 * läuft.
 */
describe('UserPicker — Fehlerpfad (Issue #18)', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [UserPicker],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function typeQuery(fixture: ReturnType<typeof TestBed.createComponent>, query: string) {
    const compiled = fixture.nativeElement as HTMLElement;
    const input = compiled.querySelector('input[type="search"]') as HTMLInputElement;
    input.value = query;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function element(fixture: ReturnType<typeof TestBed.createComponent>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function failTheSearch(fixture: ReturnType<typeof TestBed.createComponent>, query: string) {
    await typeQuery(fixture, query);
    httpMock.expectOne(`/api/users/search?q=${query}`).flush('boom', {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('shows the failure in an alert instead of breaking the rendering', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await failTheSearch(fixture, 'car');

    const alert = element(fixture).querySelector('[role="alert"]');
    expect(alert).not.toBeNull();
    expect(alert?.textContent).toContain('Nutzerverzeichnis nicht erreichbar.');
  });

  it('shows neither results nor the empty hint while the failure is displayed', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await failTheSearch(fixture, 'car');

    expect(element(fixture).querySelector('[role="option"]')).toBeNull();
    expect(element(fixture).textContent).not.toContain('Keine Person gefunden.');
  });

  it('drops the failure message as soon as a new search starts', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await failTheSearch(fixture, 'car');
    expect(element(fixture).textContent).toContain('Nutzerverzeichnis nicht erreichbar.');

    // Neue Suche: die Meldung muss verschwinden, noch bevor die Antwort da ist.
    await typeQuery(fixture, 'carla');

    expect(element(fixture).textContent).not.toContain('Nutzerverzeichnis nicht erreichbar.');
    expect(element(fixture).textContent).toContain('Personen werden gesucht …');

    httpMock.expectOne('/api/users/search?q=carla').flush([
      { id: 'carla', displayName: 'Carla Simpson' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element(fixture).textContent).toContain('Carla Simpson');
    expect(element(fixture).textContent).not.toContain('Nutzerverzeichnis nicht erreichbar.');
  });

  it('drops the failure message when the query falls back below two characters', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await failTheSearch(fixture, 'car');
    expect(element(fixture).textContent).toContain('Nutzerverzeichnis nicht erreichbar.');

    await typeQuery(fixture, 'c');

    expect(element(fixture).textContent).not.toContain('Nutzerverzeichnis nicht erreichbar.');
    expect(element(fixture).querySelector('[role="alert"]')).toBeNull();
    httpMock.expectNone('/api/users/search?q=c');
  });

  it('keeps the picker usable after a failure', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await failTheSearch(fixture, 'car');

    // Das Suchfeld bleibt bedienbar — der Fehler hat das Feld nicht zerstört.
    const input = element(fixture).querySelector('input[type="search"]') as HTMLInputElement;
    expect(input.value).toBe('car');
    expect(input.disabled).toBe(false);
  });
});

/**
 * Erneut versuchen (Issue #20): nach einem Fehler muss derselbe Begriff
 * erneut gesucht werden können — ohne ihn zu verändern. Der Knopf steht an
 * der Meldung, und ein erneuter Fehlschlag lässt die Meldung stehen.
 */
describe('UserPicker — Erneut versuchen (Issue #20)', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [UserPicker],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function typeQuery(fixture: ReturnType<typeof TestBed.createComponent>, query: string) {
    const compiled = fixture.nativeElement as HTMLElement;
    const input = compiled.querySelector('input[type="search"]') as HTMLInputElement;
    input.value = query;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function element(fixture: ReturnType<typeof TestBed.createComponent>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function retryButton(fixture: ReturnType<typeof TestBed.createComponent>): HTMLButtonElement {
    return element(fixture).querySelector('.user-picker__retry') as HTMLButtonElement;
  }

  it('offers the retry only in the failure', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    expect(retryButton(fixture)).toBeNull();

    await typeQuery(fixture, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(retryButton(fixture)).not.toBeNull();
    expect(retryButton(fixture).textContent).toContain('Erneut versuchen');
  });

  it('searches the same term again and shows the results', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await typeQuery(fixture, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    retryButton(fixture).click();
    fixture.detectChanges();

    // Genau ein weiterer Request — mit demselben Begriff.
    const request = httpMock.expectOne('/api/users/search?q=car');
    request.flush([{ id: 'carl', displayName: 'Carl Vogel' }]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element(fixture).textContent).toContain('Carl Vogel');
    expect(element(fixture).textContent).not.toContain('Nutzerverzeichnis nicht erreichbar.');
  });

  it('keeps the message when the retry fails as well', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await typeQuery(fixture, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    retryButton(fixture).click();
    fixture.detectChanges();
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 503,
      statusText: 'Service Unavailable',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    // Die Meldung bleibt sichtbar — und die Komponente lebt noch.
    expect(element(fixture).querySelector('[role="alert"]')).not.toBeNull();
    expect(element(fixture).textContent).toContain('Nutzerverzeichnis nicht erreichbar.');
  });
});
