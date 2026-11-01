import { TestBed } from '@angular/core/testing';

import { TelegramChat } from '../model/telegram-chat.model';
import { TelegramChatSection } from './telegram-chat-section';

/**
 * Der Abschnitt „Telegram-Chats" — alle Fälle an einer Stelle (Issue #35).
 *
 * <p>Belegt AC1 (Code anzeigen), AC11 (Name und Chat-Art), AC13 (Trennen der
 * eigenen Verbindung), AC14 (keine Trenn-Aktion für fremde Verbindungen) und
 * AC16 (ohne Mitgliedschaft kein Zugriff — auf Seitenebene in
 * `households-page.spec.ts`).
 *
 * <p>Das Component redet nicht mit dem Backend: Code und Chats kommen per
 * Input, Aktionen werden per Output gemeldet.
 */
describe('TelegramChatSection', () => {
  const CHATS: TelegramChat[] = [
    {
      chatId: '-100',
      chatName: 'WG Flur',
      chatKind: 'GRUPPE',
      activatedBy: 'alice',
      ownConnection: true,
    },
    {
      chatId: '-200',
      chatName: 'Direkt',
      chatKind: 'EINZELCHAT',
      activatedBy: 'bob',
      ownConnection: false,
    },
  ];

  interface Inputs {
    code?: string | null;
    codeLoading?: boolean;
    codeFailed?: boolean;
    botConfigured?: boolean;
    chats?: readonly TelegramChat[] | null;
    chatsLoading?: boolean;
    chatsFailed?: boolean;
    disconnecting?: boolean;
    disconnectError?: string | null;
  }

  function open(inputs: Inputs = {}) {
    const fixture = TestBed.createComponent(TelegramChatSection);
    fixture.componentRef.setInput('householdId', 1);
    fixture.componentRef.setInput('householdName', 'Haus A');
    fixture.componentRef.setInput('code', inputs.code ?? '123456');
    fixture.componentRef.setInput('codeLoading', inputs.codeLoading ?? false);
    fixture.componentRef.setInput('codeFailed', inputs.codeFailed ?? false);
    fixture.componentRef.setInput('botConfigured', inputs.botConfigured ?? true);
    fixture.componentRef.setInput('chats', inputs.chats ?? CHATS);
    fixture.componentRef.setInput('chatsLoading', inputs.chatsLoading ?? false);
    fixture.componentRef.setInput('chatsFailed', inputs.chatsFailed ?? false);
    fixture.componentRef.setInput('disconnecting', inputs.disconnecting ?? false);
    fixture.componentRef.setInput('disconnectError', inputs.disconnectError ?? null);
    fixture.detectChanges();
    return fixture;
  }

  function compiled(fixture: ReturnType<typeof TestBed.createComponent>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function disconnectButtons(fixture: ReturnType<typeof TestBed.createComponent>): HTMLButtonElement[] {
    return [...compiled(fixture).querySelectorAll('.telegram-chats__disconnect')] as HTMLButtonElement[];
  }

  // --- AC1: der Code --------------------------------------------------------

  it('shows the code in a readable monospace field (AC1)', () => {
    const fixture = open({ code: '123456' });
    const value = compiled(fixture).querySelector('.telegram-chats__code-value');

    expect(value).toBeTruthy();
    expect(value?.textContent?.trim()).toBe('123456');
  });

  it('copies the code into the clipboard and confirms it (AC1)', async () => {
    const fixture = open({ code: '123456' });
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });

    (compiled(fixture).querySelector('.telegram-chats__copy') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(writeText).toHaveBeenCalledWith('123456');
    const feedback = compiled(fixture).querySelector('[role="status"]');
    expect(feedback?.textContent).toContain('Kopiert.');
  });

  it('reports a failing copy and asks the user to read the code (AC1)', async () => {
    const fixture = open({ code: '123456' });
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: vi.fn().mockRejectedValue(new Error('nicht erlaubt')) },
      configurable: true,
    });

    (compiled(fixture).querySelector('.telegram-chats__copy') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    const feedback = compiled(fixture).querySelector('[role="status"]');
    expect(feedback?.textContent).toContain('Kopieren nicht möglich');
  });

  it('disables copying while the code is loading (Zustandstabelle)', () => {
    const fixture = open({ code: null, codeLoading: true });

    const button = compiled(fixture).querySelector('.telegram-chats__copy') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });

  it('shows a loading line instead of an empty field while the code loads', () => {
    const fixture = open({ code: null, codeLoading: true });

    expect(compiled(fixture).querySelector('.telegram-chats__code-value')).toBeNull();
    expect(compiled(fixture).textContent).toContain('Freischalt-Code wird geladen');
  });

  it('offers a retry when the code could not be loaded (Zustandstabelle)', () => {
    const fixture = open({ code: null, codeFailed: true });
    const retried = vi.fn();
    fixture.componentInstance.retryCode.subscribe(retried);

    const alert = compiled(fixture).querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Der Freischalt-Code konnte nicht geladen werden');

    const button = [...(alert?.querySelectorAll('button') ?? [])][0] as HTMLButtonElement;
    button.click();
    expect(retried).toHaveBeenCalled();
  });

  // --- AC11: die Chat-Liste -------------------------------------------------

  it('lists every chat with its name and kind (AC11)', () => {
    const fixture = open();
    const rows = compiled(fixture).querySelectorAll('.telegram-chats__row');

    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('WG Flur');
    expect(rows[0].textContent).toContain('GRUPPE');
    expect(rows[1].textContent).toContain('Direkt');
    expect(rows[1].textContent).toContain('EINZELCHAT');
  });

  it('names the member who activated the chat (AC11)', () => {
    const fixture = open();

    const rows = compiled(fixture).querySelectorAll('.telegram-chats__row');
    expect(rows[0].textContent).toContain('alice');
    expect(rows[1].textContent).toContain('bob');
  });

  it('never shows the technical chat id (Out of Scope des Issues)', () => {
    const fixture = open();

    const text = compiled(fixture).textContent ?? '';
    expect(text).not.toContain('-100');
    expect(text).not.toContain('-200');
  });

  it('shows an empty hint when no chat is activated (Zustandstabelle)', () => {
    const fixture = open({ chats: [] });

    expect(compiled(fixture).textContent).toContain('Noch kein Telegram-Chat freigeschaltet');
  });

  it('offers a retry when the chat list could not be loaded (Zustandstabelle)', () => {
    const fixture = open({ chats: null, chatsFailed: true });
    const retried = vi.fn();
    fixture.componentInstance.retryChats.subscribe(retried);

    const alert = compiled(fixture).querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Die Telegram-Chats konnten nicht geladen werden');

    const button = [...(alert?.querySelectorAll('button') ?? [])][0] as HTMLButtonElement;
    button.click();
    expect(retried).toHaveBeenCalled();
  });

  it('keeps the code usable when the chat list fails (Zustandstabelle)', () => {
    const fixture = open({ code: '123456', chats: null, chatsFailed: true });

    expect(compiled(fixture).querySelector('.telegram-chats__code-value')?.textContent?.trim()).toBe(
      '123456',
    );
  });

  // --- AC13, AC14: das Trennen ---------------------------------------------

  it('offers the disconnect action for the own connection (AC13)', () => {
    const fixture = open();

    const buttons = disconnectButtons(fixture);
    expect(buttons.length).toBe(1);
  });

  it('does not render the disconnect action for a foreign connection (AC14)', () => {
    const fixture = open();

    const rows = compiled(fixture).querySelectorAll('.telegram-chats__row');
    // In der Zeile des fremden Chats steht keine Trenn-Aktion — sie wird
    // nicht gerendert, nicht nur deaktiviert.
    expect(rows[1].querySelector('.telegram-chats__disconnect')).toBeNull();
  });

  it('reports the chat to disconnect for confirmation (AC13)', () => {
    const fixture = open();
    const disconnected: TelegramChat[] = [];
    fixture.componentInstance.disconnectRequested.subscribe((chat) => disconnected.push(chat));

    disconnectButtons(fixture)[0].click();

    expect(disconnected).toEqual([CHATS[0]]);
  });

  it('shows the error of a failed disconnect below the row (Zustandstabelle)', () => {
    const fixture = open({
      disconnectError: 'Die Verbindung konnte nicht getrennt werden. Bitte später erneut versuchen.',
    });

    const alert = compiled(fixture).querySelector('.telegram-chats__error--row');
    expect(alert?.textContent).toContain('Die Verbindung konnte nicht getrennt werden');
    expect(alert?.getAttribute('role')).toBe('alert');
  });

  // --- Der Bot-Token-Hinweis ------------------------------------------------

  it('warns when the bot is not configured (Zustandstabelle)', () => {
    const fixture = open({ botConfigured: false });

    expect(compiled(fixture).textContent).toContain(
      'Der Telegram-Bot ist nicht konfiguriert. Ohne Bot-Token lassen sich keine Chats freischalten.',
    );
  });

  it('does not warn when the bot is configured', () => {
    const fixture = open({ botConfigured: true });

    expect(compiled(fixture).textContent).not.toContain('Der Telegram-Bot ist nicht konfiguriert');
  });

  it('offers no code without a configured bot (Zustandstabelle)', () => {
    // Ohne Bot-Token wird kein Code erzeugt — der Code-Bereich entfällt
    // ganz, damit keine wirkungslose Aktion angeboten wird.
    const fixture = open({ botConfigured: false, code: '123456' });

    expect(compiled(fixture).querySelector('.telegram-chats__code')).toBeNull();
    expect(compiled(fixture).querySelector('.telegram-chats__code-value')).toBeNull();
    expect(compiled(fixture).querySelector('.telegram-chats__copy')).toBeNull();
  });

  it('shows no chat list without a configured bot (Zustandstabelle)', () => {
    // Die Chat-Liste ist ohne Bot leer: eine Freischaltung wäre unmöglich.
    const fixture = open({ botConfigured: false });

    expect(compiled(fixture).querySelector('.telegram-chats__list')).toBeNull();
    expect(compiled(fixture).querySelector('.telegram-chats__rows')).toBeNull();
  });
});
