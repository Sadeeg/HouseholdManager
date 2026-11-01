package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.application.port.in.TelegramChatUseCase;

/**
 * Ausgehendes DTO: ein freigeschalteter Telegram-Chat.
 *
 * <p>Name und Art kommen von Telegram (AC11). Der Anzeigename des Chats wird
 * als geliefert.
 *
 * <p>Die Chat-Kennung bei Telegram ist Teil der Antwort, damit die
 * Oberfläche die Verbindung später trennen kann — der Trennaufruf trägt sie
 * im Pfad. Sie wird <strong>nicht angezeigt</strong>: sie ist ein technisches
 * Adressmerkmal, kein Anzeigewert (Out of Scope des Issues).
 */
public record TelegramChatResponse(
        String chatId,
        String chatName,
        String chatKind,
        String activatedBy,
        String activatedByName,
        boolean ownConnection) {

    /**
     * Übersetzt die Anzeigewerte.
     *
     * @param currentUserId die angemeldete Person — sie entscheidet, ob die
     *     Verbindung die eigene ist und damit getrennt werden darf (AC13,
     *     AC14).
     */
    public static TelegramChatResponse from(TelegramChatUseCase.ChatView chat, String currentUserId) {
        return new TelegramChatResponse(
                chat.chatId(),
                chat.chatName(),
                chat.kind().label(),
                chat.activatedBy(),
                chat.activatedByName(),
                chat.activatedBy().equals(currentUserId));
    }
}
