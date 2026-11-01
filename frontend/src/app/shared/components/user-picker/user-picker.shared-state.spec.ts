import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UserSearch } from '../../../core/users/user-search';
import { UserPicker } from './user-picker';

/**
 * Sonden des kritischen Testers zu Issue #4 (Instanz-Strategie der
 * Nutzersuche).
 *
 * Geprüft werden die Verhaltenswege, die die vorhandenen Tests nicht
 * abdecken: Fehlerpfad der Suche, Rückschritt unter zwei Zeichen,
 * Wechselwirkung zweier gleichzeitiger Suchfelder (geteilter Zustand) und
 * das Verhalten beim Verlassen der Route.
 *
 * Zwei Tests sind mit `it.fails` markiert: der Fehlerpfad der Suche ist ein
 * **vorbestehender** Defekt (gegen `origin/main` verifiziert — dort bricht
 * `user-picker.html:20` mit `ResourceValueError` genauso). Sie bleiben als
 * Tripwire stehen: sobald der Defekt behoben ist, schlägt `it.fails` fehl
 * und weist auf die Reparatur hin — Issue #18.
 */
describe('UserPicker — geteilter Zustand (Issue #4)', () => {
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

  function inputOf(fixture: ReturnType<typeof TestBed.createComponent>): HTMLInputElement {
    return (fixture.nativeElement as HTMLElement).querySelector(
      'input[type="search"]',
    ) as HTMLInputElement;
  }

  it.fails('shows the error when the user directory is unreachable (Defekt vorbestehend)', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await typeQuery(fixture, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Nutzerverzeichnis nicht erreichbar.',
    );
  });

  it.fails('recovers from an error: the next search shows results again (Defekt vorbestehend)', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await typeQuery(fixture, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush('boom', {
      status: 500,
      statusText: 'Server Error',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    await typeQuery(fixture, 'carla');
    httpMock.expectOne('/api/users/search?q=carla').flush([
      { id: 'carla', displayName: 'Carla Simpson' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Carla Simpson');
    expect(text).not.toContain('Nutzerverzeichnis nicht erreichbar.');
  });

  it('drops the results when the query falls back below two characters', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await typeQuery(fixture, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush([
      { id: 'carl', displayName: 'Carl Vogel' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Carl Vogel');

    // Der Suchbegriff wird auf ein Zeichen gekürzt: keine Treffer mehr,
    // aber auch keine „Keine Person gefunden."-Meldung (unter zwei Zeichen).
    await typeQuery(fixture, 'c');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Carl Vogel');
    expect(text).not.toContain('Keine Person gefunden.');
    httpMock.expectNone('/api/users/search?q=c');
  });

  it('a second field clears the query and the results of the first field', async () => {
    const first = TestBed.createComponent(UserPicker);
    first.detectChanges();

    await typeQuery(first, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush([
      { id: 'carl', displayName: 'Carl Vogel' },
    ]);
    await first.whenStable();
    first.detectChanges();
    expect((first.nativeElement as HTMLElement).textContent).toContain('Carl Vogel');

    // Ein zweites Suchfeld entsteht: der geteilte Zustand wird zurückgesetzt.
    const second = TestBed.createComponent(UserPicker);
    second.detectChanges();
    first.detectChanges();

    expect(inputOf(first).value).toBe('');
    expect((first.nativeElement as HTMLElement).textContent).not.toContain('Carl Vogel');
  });

  it('shows the shared query in the input of every field', async () => {
    const first = TestBed.createComponent(UserPicker);
    const second = TestBed.createComponent(UserPicker);
    first.detectChanges();
    second.detectChanges();

    await typeQuery(first, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush([
      { id: 'carl', displayName: 'Carl Vogel' },
    ]);
    await first.whenStable();
    first.detectChanges();
    second.detectChanges();

    // Beide Felder hängen am selben Query-Signal.
    expect(inputOf(first).value).toBe('car');
    expect(inputOf(second).value).toBe('car');
  });

  it('keeps the state when the field is left, and starts fresh on the next field', async () => {
    const search = TestBed.inject(UserSearch);
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await typeQuery(fixture, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush([
      { id: 'carl', displayName: 'Carl Vogel' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    // Route verlassen: die Komponente stirbt, der Dienst lebt weiter.
    fixture.destroy();
    expect(search.query()).toBe('car');
    expect(search.results().length).toBe(1);

    // Ein neu geöffnetes Feld startet frisch.
    const next = TestBed.createComponent(UserPicker);
    next.detectChanges();

    expect(inputOf(next).value).toBe('');
    expect((next.nativeElement as HTMLElement).textContent).not.toContain('Carl Vogel');
  });
});
