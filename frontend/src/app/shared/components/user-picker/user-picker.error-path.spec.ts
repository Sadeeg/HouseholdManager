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
