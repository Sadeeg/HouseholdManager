import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Member } from '../model/member.model';
import { MemberSection } from './member-section';

const MEMBERS: Member[] = [
  { userId: 'alice', displayName: 'Alice Admin', householdAdmin: true },
  { userId: 'bob', displayName: 'Bob Mitglied', householdAdmin: false },
];

describe('MemberSection', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MemberSection],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  async function open(inputs: { members?: Member[] | null; loading?: boolean; failed?: boolean } = {}) {
    const fixture = TestBed.createComponent(MemberSection);
    fixture.componentRef.setInput('members', inputs.members ?? MEMBERS);
    fixture.componentRef.setInput('loading', inputs.loading ?? false);
    fixture.componentRef.setInput('failed', inputs.failed ?? false);
    fixture.componentRef.setInput('currentUserId', 'alice');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  function clickWhere(compiled: HTMLElement, label: string, within?: Element | null): void {
    const scope = within ?? compiled;
    const target = [...scope.querySelectorAll('button')].find((b) => (b.textContent ?? '').includes(label));
    target?.click();
  }

  it('lists members with the admin first, badged as HAUSHALTSLEITUNG (AC11)', async () => {
    const fixture = await open();
    const compiled = fixture.nativeElement as HTMLElement;
    const rows = compiled.querySelectorAll('.member-section__row');

    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Alice Admin');
    expect(rows[0].textContent).toContain('HAUSHALTSLEITUNG');
    expect(rows[1].textContent).toContain('@bob');
    // Das Entfernen der Leitung ist nicht möglich.
    expect((rows[0].querySelector('.member-section__remove') as HTMLButtonElement).disabled).toBe(true);
    expect((rows[1].querySelector('.member-section__remove') as HTMLButtonElement).disabled).toBe(false);
  });

  it('emits removeRequested with the member for confirmation by the caller', async () => {
    const fixture = await open();
    const removed: Member[] = [];
    fixture.componentInstance.removeRequested.subscribe((member) => removed.push(member));

    const compiled = fixture.nativeElement as HTMLElement;
    (compiled.querySelectorAll('.member-section__remove')[1] as HTMLButtonElement).click();

    expect(removed).toEqual([MEMBERS[1]]);
  });

  it('appoints a searched person through the user directory (AC8/AC11)', async () => {
    const fixture = await open();
    const added: string[] = [];
    fixture.componentInstance.addMember.subscribe((userId) => added.push(userId));

    const compiled = fixture.nativeElement as HTMLElement;
    const searchInput = compiled.querySelector('input[type="search"]') as HTMLInputElement;
    searchInput.value = 'carl';
    searchInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    httpMock.expectOne('/api/users/search?q=carl').flush([
      { id: 'carl', displayName: 'Carl Vogel' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(compiled.textContent).toContain('Carl Vogel');
    clickWhere(compiled, 'Carl Vogel', compiled.querySelector('.user-picker__results'));
    fixture.detectChanges();

    clickWhere(compiled, 'Ernennen');
    expect(added).toEqual(['carl']);
  });

  it('keeps „Ernennen“ as a standing action, disabled until a person is selected (AC11)', async () => {
    const fixture = await open();
    const compiled = fixture.nativeElement as HTMLElement;

    const appoint = compiled.querySelector(
      '.member-section__appoint-action',
    ) as HTMLButtonElement;
    expect(appoint).not.toBeNull();
    expect(appoint.textContent).toContain('Ernennen');
    expect(appoint.disabled).toBe(true);
    expect(compiled.querySelector('.member-section__pending')).toBeNull();

    const searchInput = compiled.querySelector('input[type="search"]') as HTMLInputElement;
    searchInput.value = 'carl';
    searchInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    httpMock.expectOne('/api/users/search?q=carl').flush([{ id: 'carl', displayName: 'Carl Vogel' }]);
    await fixture.whenStable();
    fixture.detectChanges();

    clickWhere(compiled, 'Carl Vogel', compiled.querySelector('.user-picker__results'));
    fixture.detectChanges();
    expect(appoint.disabled).toBe(false);
    expect(compiled.querySelector('.member-section__pending')?.textContent).toContain('Carl Vogel');

    // Abbrechen nimmt die Auswahl zurück — die Aktion ist wieder deaktiviert.
    clickWhere(compiled, 'ABBRECHEN');
    fixture.detectChanges();
    expect(appoint.disabled).toBe(true);
    expect(compiled.querySelector('.member-section__pending')).toBeNull();
    expect(compiled.querySelector('input[type="search"]')).not.toBeNull();
  });

  it('disables „Ernennen“ for people who are already members', async () => {
    const fixture = await open();
    const compiled = fixture.nativeElement as HTMLElement;

    const searchInput = compiled.querySelector('input[type="search"]') as HTMLInputElement;
    searchInput.value = 'bob';
    searchInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    httpMock.expectOne('/api/users/search?q=bob').flush([
      { id: 'bob', displayName: 'Bob Mitglied' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    clickWhere(compiled, 'Bob Mitglied', compiled.querySelector('.user-picker__results'));
    fixture.detectChanges();

    const appoint = [...compiled.querySelectorAll('button')].find((b) =>
      (b.textContent ?? '').includes('Ernennen'),
    ) as HTMLButtonElement;
    expect(appoint.disabled).toBe(true);
    expect(compiled.textContent).toContain('Bereits Mitglied');
  });

  it('shows the empty hint when no members are appointed yet', async () => {
    const fixture = await open({ members: [] });
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Noch keine Mitglieder ernannt.',
    );
  });
});
