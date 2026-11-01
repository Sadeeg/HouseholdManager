import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import { TelegramChat } from '../model/telegram-chat.model';

/**
 * Dumb Component (UI-Schicht): der Abschnitt „Telegram-Chats" eines
 * Haushalts — Code-Feld und Chat-Liste.
 *
 * Das Component kennt kein Backend: Code und Chats kommen per Input, jede
 * Aktion wird per Output gemeldet — der Aufrufer führt sie aus. Es folgt
 * dem Muster des Müllkalender-Abschnitts (AC1–AC16).
 *
 * **Der Code ist ein Anzeigewert, kein Formularfeld.** Er wird im Chat an
 * den Bot gesendet, nicht über die Oberfläche. Das Feld ist deshalb
 * schreibgeschützt, nie maskiert und nie abgeschnitten — der Nutzer soll ihn
 * ablesen und abtippen können.
 *
 * **„Trennen" wird nicht gerendert**, wenn die Verbindung nicht die eigene
 * ist (AC14): ein deaktivierter Knopf wäre eine wirkungslose Aktion, die
 * trotzdem im Weg steht.
 */
@Component({
  selector: 'app-telegram-chat-section',
  templateUrl: './telegram-chat-section.html',
  styleUrl: './telegram-chat-section.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TelegramChatSection {
  readonly householdId = input.required<number>();
  readonly householdName = input.required<string>();

  /** Der Freischalt-Code; null, solange er nicht geladen ist. */
  readonly code = input<string | null>(null);
  /** Der Code wird gerade geladen — die Zeile ersetzt das Feld. */
  readonly codeLoading = input<boolean>(false);
  /** Der Code konnte nicht geladen werden. */
  readonly codeFailed = input<boolean>(false);
  /** Ist der Bot konfiguriert? Ohne Bot lassen sich keine Chats freischalten. */
  readonly botConfigured = input<boolean>(true);

  /** Die freigeschalteten Chats; null, solange sie nicht geladen sind. */
  readonly chats = input<readonly TelegramChat[] | null>(null);
  /** Die Chat-Liste wird gerade geladen. */
  readonly chatsLoading = input<boolean>(false);
  /** Die Chat-Liste konnte nicht geladen werden. */
  readonly chatsFailed = input<boolean>(false);

  /** True, solange ein Trennen läuft. */
  readonly disconnecting = input<boolean>(false);
  /** Der Fehler des letzten Trennens — steht unter der betroffenen Zeile. */
  readonly disconnectError = input<string | null>(null);

  /**
   * Der Chat, zu dem der Fehler gehört — die Fehlerzeile steht nur unter
   * dieser Zeile, nicht unter allen.
   */
  readonly disconnectErrorChatId = input<string | null>(null);

  readonly retryCode = output<void>();
  readonly retryChats = output<void>();
  readonly disconnectRequested = output<TelegramChat>();

  /**
   * Die Rückmeldung des Kopierens — „Kopiert." oder der Fehlertext.
   * `null` heißt: noch nichts kopiert.
   */
  protected readonly copyFeedback = signal<string | null>(null);
  protected readonly copyFailed = signal(false);

  /** Ohne Code gibt es nichts zu kopieren. */
  protected readonly canCopy = computed(() => !!this.code() && !this.codeLoading());

  /** Der Code als Anzeigewert — nie maskiert, nie abgeschnitten. */
  protected readonly codeValue = computed(() => this.code() ?? '');

  /**
   * Kopiert den Code in die Zwischenablage. Es gibt keinen Request — der
   * Code ist bereits da, er wird nur ablesbar gemacht.
   */
  protected async copyCode(): Promise<void> {
    const code = this.code();
    if (!code) {
      return;
    }
    try {
      await navigator.clipboard.writeText(code);
      this.copyFailed.set(false);
      this.copyFeedback.set('Kopiert.');
    } catch {
      this.copyFailed.set(true);
      this.copyFeedback.set('Kopieren nicht möglich — bitte den Code von Hand ablesen.');
    }
  }

  /** Meldet das Trennen — der Aufrufer bestätigt es im Dialog. */
  protected requestDisconnect(chat: TelegramChat): void {
    this.disconnectRequested.emit(chat);
  }
}
