import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WorkspaceService } from '../../core/workspace/workspace-service';
import { HouseholdsPage } from './households-page';
import { HouseholdsData } from './data/households-data';

describe('HouseholdsPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HouseholdsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), HouseholdsData],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function flushHouseholds(createdBy: string): void {
    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy },
    ]);
  }

  /** Ein Makrotask reicht, damit die Resource ihren Wert aktualisiert. */
  async function tick(fixture: ReturnType<typeof TestBed.createComponent>): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  it('renders the loaded households', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();

    flushHouseholds('alice');
    await fixture.whenStable();
    fixture.detectChanges();

    const items = (fixture.nativeElement as HTMLElement).querySelectorAll('app-household-item');
    expect(items.length).toBe(1);
    expect(items[0].textContent).toContain('Haushalt Müller');
  });

  it('shows a hint when there is no household yet', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();

    httpMock.expectOne('/api/households').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.state')?.textContent).toContain('noch kein Haushalt');
  });

  it('shows the „Mitglieder“ action only for the household admin (AC11)', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice Admin' });

    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    flushHouseholds('alice');
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.households__members-action')).not.toBeNull();

    // Eine andere Person ist nicht die Leitung.
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [HouseholdsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), HouseholdsData],
    }).compileComponents();
    const secondHttp = TestBed.inject(HttpTestingController);
    TestBed.inject(WorkspaceService).currentUser.set({ id: 'bob', displayName: 'Bob' });

    const second = TestBed.createComponent(HouseholdsPage);
    second.detectChanges();
    secondHttp.expectOne('/api/households').flush([
      { id: 1, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await second.whenStable();
    second.detectChanges();

    expect((second.nativeElement as HTMLElement).querySelector('.households__members-action')).toBeNull();
    secondHttp.verify();
  });

  it('opens the member section inline and lists the members', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice Admin' });

    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    flushHouseholds('alice');
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    (compiled.querySelector('.households__members-action') as HTMLButtonElement).click();
    fixture.detectChanges();

    await new Promise((resolve) => setTimeout(resolve, 0));
    httpMock.expectOne('/api/households/1/members').flush([
      { userId: 'alice', displayName: 'Alice Admin', householdAdmin: true },
      { userId: 'bob', displayName: 'Bob Mitglied', householdAdmin: false },
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();

    expect(compiled.querySelector('app-member-section')).not.toBeNull();
    expect(compiled.querySelectorAll('.member-section__row').length).toBe(2);
    expect(compiled.textContent).toContain('HAUSHALTSLEITUNG');
  });

  it('creates a household, adopts it as workspace and lists it', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice Admin' });

    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([]);
    await tick(fixture);

    const compiled = fixture.nativeElement as HTMLElement;
    // Das Formular öffnet sich über die Primäraktion im Kopf.
    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.includes('Haushalt anlegen'))
      ?.click();
    fixture.detectChanges();

    const input = compiled.querySelector('#household-name') as HTMLInputElement;
    expect(input).not.toBeNull();
    input.value = 'Haushalt Müller';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'Anlegen')
      ?.click();

    const create = httpMock.expectOne('/api/households', 'POST');
    expect(create.request.body).toEqual({ name: 'Haushalt Müller' });
    create.flush({
      id: 7,
      name: 'Haushalt Müller',
      createdAt: '2026-01-01T00:00:00Z',
      createdBy: 'alice',
    });

    // Die Liste lädt neu, der neue Haushalt wird zum Workspace.
    await tick(fixture);
    httpMock.expectOne('/api/households').flush([
      { id: 7, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await tick(fixture);

    const select = httpMock.expectOne('/api/me/workspace', 'PUT');
    expect(select.request.body).toEqual({ householdId: 7 });
    select.flush({});
    await tick(fixture);

    expect(workspace.currentHouseholdId()).toBe(7);
    expect(workspace.availableHouseholds().map((household) => household.name)).toContain(
      'Haushalt Müller',
    );
    expect(compiled.textContent).toContain('Haushalt Müller');
    expect(compiled.querySelector('.toast')?.textContent).toContain('Haushalt angelegt.');
  });

  it('rejects an empty household name and keeps the form open', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([]);
    await tick(fixture);

    const compiled = fixture.nativeElement as HTMLElement;
    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.includes('Haushalt anlegen'))
      ?.click();
    fixture.detectChanges();

    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'Anlegen')
      ?.click();
    fixture.detectChanges();

    expect(compiled.textContent).toContain('Der Name fehlt.');
    expect(compiled.querySelector('#household-name')).not.toBeNull();
    httpMock.expectNone('/api/households', 'POST');
  });

  /**
   * AC1: die Quelle ist über die Oberfläche einstellbar. Das Segment selbst redet
   * nicht mit dem Backend — hier zählt die Verbindung vom „Speichern“ im Segment
   * zur API und dass die gespeicherte Adresse danach gezeigt wird.
   */
  it('sends the entered address to the API (AC1)', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice Admin' });

    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([
      {
        id: 1,
        name: 'Haushalt Müller',
        createdAt: '2026-01-01T00:00:00Z',
        createdBy: 'alice',
        wasteCalendarUrl: 'https://entsorger.example/kalender.ics',
      },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'MÜLLKALENDER')
      ?.click();
    fixture.detectChanges();

    const section = compiled.querySelector('app-waste-calendar-section') as HTMLElement;
    expect(section).not.toBeNull();

    const field = section.querySelector('input') as HTMLInputElement;
    expect(field.value).toBe('https://entsorger.example/kalender.ics');
    field.value = 'https://entsorger.example/anderer.ics';
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const saveButton = section.querySelector('.hm-button--primary') as HTMLButtonElement;
    expect(saveButton.disabled).toBe(false);
    saveButton.click();

    const request = httpMock.expectOne('/api/households/1/waste/source');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ url: 'https://entsorger.example/anderer.ics' });
    request.flush({ url: 'https://entsorger.example/anderer.ics' });
    await tick(fixture);

    // Nach dem Speichern lädt die Seite neu, damit die Adresse sichtbar bleibt.
    httpMock.expectOne('/api/households').flush([
      {
        id: 1,
        name: 'Haushalt Müller',
        createdAt: '2026-01-01T00:00:00Z',
        createdBy: 'alice',
        wasteCalendarUrl: 'https://entsorger.example/anderer.ics',
      },
    ]);
    await tick(fixture);

    expect(compiled.textContent).toContain('Müllkalender gespeichert');
    expect(section.querySelector('input')!.value).toBe('https://entsorger.example/anderer.ics');
  });

  /**
   * Das Entfernen der Quelle gehört zum Pflegen (AC9) und braucht die
   * Bestätigung aus UX-Schritt 7: erst der Dialog, dann der Löschauftrag.
   * Vor der Bestätigung geht kein DELETE raus. (AC23 ist das Abbrechen —
   * das belegt `households-page-abort.spec.ts`.)
   */
  it('removes the source only after the confirmation', async () => {
    const workspace = TestBed.inject(WorkspaceService);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice Admin' });

    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([
      {
        id: 1,
        name: 'Haushalt Müller',
        createdAt: '2026-01-01T00:00:00Z',
        createdBy: 'alice',
        wasteCalendarUrl: 'https://entsorger.example/kalender.ics',
      },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'MÜLLKALENDER')
      ?.click();
    fixture.detectChanges();

    const section = compiled.querySelector('app-waste-calendar-section') as HTMLElement;
    (section.querySelector('.waste-calendar__remove') as HTMLButtonElement).click();
    fixture.detectChanges();

    // Der Dialog fragt nach — es geht noch nichts raus.
    expect(compiled.querySelector('app-confirm-dialog')).not.toBeNull();
    httpMock.expectNone('/api/households/1/waste/source');

    const confirm = compiled.querySelector('.hm-button--danger') as HTMLButtonElement;
    confirm.click();
    fixture.detectChanges();

    const request = httpMock.expectOne('/api/households/1/waste/source');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await tick(fixture);

    httpMock.expectOne('/api/households').flush([
      {
        id: 1,
        name: 'Haushalt Müller',
        createdAt: '2026-01-01T00:00:00Z',
        createdBy: 'alice',
        wasteCalendarUrl: null,
      },
    ]);
    await tick(fixture);

    expect(compiled.textContent).toContain('Müllkalender entfernt');
  });

  // --- Der Abschnitt „Telegram-Chats" (Issue #35) ---------------------------

  /**
   * Öffnet den Telegram-Abschnitt und beantwortet die beiden Requests
   * (Code und Chat-Liste).
   */
  async function openTelegramSection(
    chats: {
      chatId: string;
      chatName: string;
      chatKind: string;
      activatedBy: string;
      ownConnection: boolean;
    }[] = [],
  ): Promise<{ fixture: ReturnType<typeof TestBed.createComponent>; compiled: HTMLElement }> {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'TELEGRAM-CHATS')
      ?.click();
    fixture.detectChanges();

    httpMock.expectOne('/api/households/1/telegram/code').flush({
      code: '123456',
      botConfigured: true,
    });
    httpMock.expectOne('/api/households/1/telegram/chats').flush({ chats });
    await fixture.whenStable();
    fixture.detectChanges();

    return { fixture, compiled };
  }

  it('opens the Telegram section and shows the code and the chats (AC1, AC11)', async () => {
    const { compiled } = await openTelegramSection([
      {
        chatId: '-100',
        chatName: 'WG Flur',
        chatKind: 'GRUPPE',
        activatedBy: 'alice',
        ownConnection: true,
      },
    ]);

    const section = compiled.querySelector('app-telegram-chat-section') as HTMLElement;
    expect(section).not.toBeNull();
    expect(section.querySelector('.telegram-chats__code-value')?.textContent?.trim()).toBe('123456');
    expect(section.querySelectorAll('.telegram-chats__row').length).toBe(1);
  });

  it('runs no request for the Telegram section while it is closed', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    // Ohne offenen Abschnitt läuft kein Request — der httpMock.verify() im
    // afterEach würde ihn sonst als unerwarteten Request melden.
    expect(fixture.nativeElement.querySelector('app-telegram-chat-section')).toBeNull();
  });

  it('warns when the bot is not configured', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z', createdBy: 'alice' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'TELEGRAM-CHATS')
      ?.click();
    fixture.detectChanges();

    httpMock.expectOne('/api/households/1/telegram/code').flush({
      code: '123456',
      botConfigured: false,
    });
    httpMock.expectOne('/api/households/1/telegram/chats').flush({ chats: [] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(compiled.textContent).toContain('Der Telegram-Bot ist nicht konfiguriert');
  });

  it('disconnects a chat only after the confirmation (AC13)', async () => {
    const { fixture, compiled } = await openTelegramSection([
      {
        chatId: '-100',
        chatName: 'WG Flur',
        chatKind: 'GRUPPE',
        activatedBy: 'alice',
        ownConnection: true,
      },
    ]);

    const section = compiled.querySelector('app-telegram-chat-section') as HTMLElement;
    (section.querySelector('.telegram-chats__disconnect') as HTMLButtonElement).click();
    fixture.detectChanges();

    // Der Dialog fragt nach — es geht noch nichts raus.
    expect(compiled.querySelector('app-confirm-dialog')).not.toBeNull();
    httpMock.expectNone('/api/households/1/telegram/chats/-100');

    const confirm = compiled.querySelector('.hm-button--danger') as HTMLButtonElement;
    confirm.click();
    fixture.detectChanges();

    const request = httpMock.expectOne('/api/households/1/telegram/chats/-100');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await tick(fixture);

    // Die Chat-Liste wird neu geladen — der getrennte Chat verschwindet.
    httpMock.expectOne('/api/households/1/telegram/chats').flush({ chats: [] });
    await tick(fixture);

    expect(compiled.textContent).toContain('Telegram-Chat getrennt');
  });

  it('shows the error when the disconnect fails (Zustandstabelle)', async () => {
    const { fixture, compiled } = await openTelegramSection([
      {
        chatId: '-100',
        chatName: 'WG Flur',
        chatKind: 'GRUPPE',
        activatedBy: 'alice',
        ownConnection: true,
      },
    ]);

    const section = compiled.querySelector('app-telegram-chat-section') as HTMLElement;
    (section.querySelector('.telegram-chats__disconnect') as HTMLButtonElement).click();
    fixture.detectChanges();
    (compiled.querySelector('.hm-button--danger') as HTMLButtonElement).click();
    fixture.detectChanges();

    httpMock
      .expectOne('/api/households/1/telegram/chats/-100')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await tick(fixture);

    expect(compiled.textContent).toContain('Die Verbindung konnte nicht getrennt werden');
  });
});
