import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UserPicker } from './user-picker';

describe('UserPicker', () => {
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

  async function typeQuery(fixture: ReturnType<typeof TestBed.createComponent>, query: string) {
    const compiled = fixture.nativeElement as HTMLElement;
    const input = compiled.querySelector('input[type="search"]') as HTMLInputElement;
    input.value = query;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('searches the user directory from two characters and lists name plus @login', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await typeQuery(fixture, 'car');
    httpMock.expectOne('/api/users/search?q=car').flush([
      { id: 'carl', displayName: 'Carl Vogel' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const option = compiled.querySelector('[role="option"]');
    expect(option?.textContent).toContain('Carl Vogel');
    expect(option?.textContent).toContain('@carl');
  });

  it('emits the selected person', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    const selected: { id: string; displayName: string }[] = [];
    fixture.componentInstance.selected.subscribe((user) => selected.push(user));
    fixture.detectChanges();

    await typeQuery(fixture, 'carla');
    httpMock.expectOne('/api/users/search?q=carla').flush([
      { id: 'carla', displayName: 'Carla Simpson' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const option = (fixture.nativeElement as HTMLElement).querySelector('[role="option"]') as HTMLButtonElement;
    option.click();

    expect(selected).toEqual([{ id: 'carla', displayName: 'Carla Simpson' }]);
  });

  it('runs no request for queries shorter than two characters', () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    // Der leere Ladezustand erzeugt genau nichts.
    httpMock.verify();
  });

  it('says when no person was found', async () => {
    const fixture = TestBed.createComponent(UserPicker);
    fixture.detectChanges();

    await typeQuery(fixture, 'zzz');
    httpMock.expectOne('/api/users/search?q=zzz').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Keine Person gefunden.');
  });
});
