/**
 * Datenmodell des Features „Haushalte" — der Telegram-Chats.
 *
 * Die Chat-Kennung bei Telegram wird **nicht** übertragen: sie ist kein
 * Anzeigewert. Der Backend braucht sie zum Trennen — deshalb trägt das
 * Modell hier ein technisches Feld `chatId`, das nur für das Löschen
 * benötigt wird und nirgends angezeigt wird.
 */

/** Ein freigeschalteter Telegram-Chat. */
export interface TelegramChat {
  /** Der von Telegram gelieferte Anzeigename des Chats. */
  readonly chatName: string;
  /** Die Chat-Art: „GRUPPE" oder „EINZELCHAT". */
  readonly chatKind: string;
  /** Die Kennung des freischaltenden Mitglieds. */
  readonly activatedBy: string;
  /** True, wenn die Verbindung die eigene ist — nur dann darf getrennt werden. */
  readonly ownConnection: boolean;
  /** Technische Kennung des Chats — wird nicht angezeigt, nur für das Löschen gebraucht. */
  readonly chatId: string;
}

/** Antwort des Backends beim Anfordern des Freischalt-Codes. */
export interface TelegramCode {
  /** Der Code — Ziffern, dauerhaft gültig. */
  readonly code: string;
  /** False, wenn der Bot nicht konfiguriert ist — ohne Bot lassen sich keine Chats freischalten. */
  readonly botConfigured: boolean;
}

/** Die Liste der freigeschalteten Chats eines Haushalts. */
export interface TelegramChatList {
  readonly chats: readonly TelegramChat[];
}
