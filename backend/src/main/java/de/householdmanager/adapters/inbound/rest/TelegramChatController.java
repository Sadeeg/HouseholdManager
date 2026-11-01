package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.adapters.inbound.rest.dto.TelegramChatListResponse;
import de.householdmanager.adapters.inbound.rest.dto.TelegramCodeResponse;
import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.TelegramChatUseCase;
import de.householdmanager.domain.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Eingehender Adapter: HTTP → eingehender Port {@link TelegramChatUseCase}.
 *
 * <p>Der Adapter übersetzt nur. Fachlogik steht im Use Case, Mitgliedschaft
 * und Haushalt werden dort geprüft (403, 404). Das Pfadmuster folgt
 * {@code WasteCollectionController} — ohne führenden Slash.
 *
 * <p>Die Chat-Kennung bei Telegram ist Teil des Pfades: sie ist der Zugriffsweg
 * auf den Chat. Sie wird <strong>nicht</strong> in der Antwort ausgegeben
 * (Out of Scope des Issues).
 */
@RestController
@RequestMapping("api/households/{householdId}/telegram")
@RequiredArgsConstructor
public class TelegramChatController {

    private final TelegramChatUseCase telegramChatUseCase;
    private final CurrentUserPort currentUserPort;

    /**
     * Der Freischalt-Code; beim ersten Aufruf wird er erzeugt (AC1, AC2).
     *
     * <p>Ohne konfigurierten Bot wird kein Code erzeugt — die Antwort trägt
     * dann {@code code = null} und {@code botConfigured = false}. Die
     * Oberfläche weist dann auf den fehlenden Bot-Token hin, statt einen
     * wirkungslosen Code zu zeigen.
     */
    @GetMapping("code")
    public TelegramCodeResponse code(@PathVariable long householdId) {
        String code = telegramChatUseCase.code(currentUserId(), householdId).orElse(null);
        return new TelegramCodeResponse(code, telegramChatUseCase.isBotConfigured());
    }

    /** Die freigeschalteten Chats des Haushalts (AC11, AC12). */
    @GetMapping("chats")
    public TelegramChatListResponse chats(@PathVariable long householdId) {
        return TelegramChatListResponse.from(
                telegramChatUseCase.listChats(currentUserId(), householdId), currentUserId());
    }

    /**
     * Trennt eine eigene Verbindung (AC13). Eine fremde Verbindung wird mit
     * 403 abgelehnt (AC14).
     */
    @DeleteMapping("chats/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@PathVariable long householdId, @PathVariable String chatId) {
        telegramChatUseCase.disconnect(currentUserId(), householdId, chatId);
    }

    private String currentUserId() {
        return currentUserPort.currentUser().map(UserAccount::id)
                .orElseThrow(AuthController.NotAuthenticatedException::new);
    }
}
