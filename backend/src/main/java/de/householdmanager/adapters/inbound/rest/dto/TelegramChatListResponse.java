package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.domain.model.TelegramChat;
import java.util.List;

/**
 * Ausgehendes DTO: die freigeschalteten Chats eines Haushalts.
 *
 * <p>Nur die Chats des angefragten Haushalts (AC12) — die Liste entsteht aus
 * dem Use Case, nicht aus einer Abfrage über alle Haushalte.
 */
public record TelegramChatListResponse(List<TelegramChatResponse> chats) {

    public static TelegramChatListResponse from(List<TelegramChat> chats, String currentUserId) {
        return new TelegramChatListResponse(chats.stream()
                .map(chat -> TelegramChatResponse.from(chat, currentUserId))
                .toList());
    }
}
