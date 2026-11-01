package de.householdmanager.adapters.inbound.rest;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.TelegramChatUseCase;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.NotChatOwnerException;
import de.householdmanager.domain.model.TelegramChat;
import de.householdmanager.domain.model.TelegramChatNotFoundException;
import de.householdmanager.domain.model.UserAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test des eingehenden Adapters: HTTP-Übersetzung des Telegram-Port. Die
 * Fachlogik ist gemockt — hier zählt nur die Übersetzung.
 *
 * <p>Belegt AC1 (Code anfordern), AC11 (Chat-Liste), AC12 (nur die Chats des
 * Haushalts), AC13 (trennen), AC14 (403 bei einer fremden Verbindung), AC16
 * (403 ohne Mitgliedschaft) und den 404 für eine unbekannte Chat-ID.
 */
@WebMvcTest(TelegramChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class TelegramChatControllerTest {

    private static final String CHAT_ID = "-1001234567890";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelegramChatUseCase telegramChatUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    // --- AC1: der Code --------------------------------------------------------

    @Test
    void code_forMemberReturnsTheCode() throws Exception {
        givenSignedInAs("alice");
        when(telegramChatUseCase.code("alice", 1L)).thenReturn("123456");
        when(telegramChatUseCase.isBotConfigured()).thenReturn(true);

        mockMvc.perform(get("/api/households/1/telegram/code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("123456"))
                .andExpect(jsonPath("$.botConfigured").value(true));
    }

    /** Ohne Bot-Token lässt sich kein Chat freischalten — die Oberfläche weist darauf hin. */
    @Test
    void code_reportsThatTheBotIsNotConfigured() throws Exception {
        givenSignedInAs("alice");
        when(telegramChatUseCase.code("alice", 1L)).thenReturn("123456");
        when(telegramChatUseCase.isBotConfigured()).thenReturn(false);

        mockMvc.perform(get("/api/households/1/telegram/code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botConfigured").value(false));
    }

    /** AC16: ohne Mitgliedschaft gibt es keinen Code. */
    @Test
    void code_withoutMembershipReturns403() throws Exception {
        givenSignedInAs("carl");
        when(telegramChatUseCase.code("carl", 1L))
                .thenThrow(new MembershipRequiredException("Die Person ist nicht Mitglied dieses Haushalts."));

        mockMvc.perform(get("/api/households/1/telegram/code")).andExpect(status().isForbidden());
    }

    @Test
    void code_withoutSignInReturns401() throws Exception {
        when(currentUserPort.currentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/households/1/telegram/code")).andExpect(status().isUnauthorized());
    }

    @Test
    void code_forUnknownHouseholdReturns404() throws Exception {
        givenSignedInAs("alice");
        when(telegramChatUseCase.code("alice", 1L))
                .thenThrow(new HouseholdNotFoundException("Der Haushalt existiert nicht."));

        mockMvc.perform(get("/api/households/1/telegram/code")).andExpect(status().isNotFound());
    }

    // --- AC11, AC12: die Chat-Liste -----------------------------------------

    @Test
    void chats_returnsNameKindAndActivatingMember() throws Exception {
        givenSignedInAs("alice");
        when(telegramChatUseCase.listChats("alice", 1L)).thenReturn(List.of(
                TelegramChat.builder().id(1L).householdId(1L).chatId(CHAT_ID)
                        .chatName("WG Flur").kind(TelegramChat.ChatKind.GROUP)
                        .activatedBy("alice").build(),
                TelegramChat.builder().id(2L).householdId(1L).chatId("-200")
                        .chatName("Direkt").kind(TelegramChat.ChatKind.PRIVATE)
                        .activatedBy("bob").build()));

        mockMvc.perform(get("/api/households/1/telegram/chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chats[0].chatName").value("WG Flur"))
                .andExpect(jsonPath("$.chats[0].chatKind").value("GRUPPE"))
                .andExpect(jsonPath("$.chats[0].activatedBy").value("alice"))
                .andExpect(jsonPath("$.chats[0].ownConnection").value(true))
                .andExpect(jsonPath("$.chats[1].chatKind").value("EINZELCHAT"))
                .andExpect(jsonPath("$.chats[1].ownConnection").value(false));
    }

    /** Die Chat-Kennung bei Telegram wird nicht ausgegeben. */
    /** Die Chat-Kennung ist ein technisches Adressmerkmal — sie wird nicht angezeigt. */
@Test
    void chats_carriesTheChatIdForTheDisconnectCall() throws Exception {
        givenSignedInAs("alice");
        when(telegramChatUseCase.listChats("alice", 1L)).thenReturn(List.of(
                TelegramChat.builder().id(1L).householdId(1L).chatId(CHAT_ID)
                        .chatName("WG Flur").kind(TelegramChat.ChatKind.GROUP)
                        .activatedBy("alice").build()));

        mockMvc.perform(get("/api/households/1/telegram/chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chats[0].chatId").value(CHAT_ID));
    }

    /** AC16: ohne Mitgliedschaft sieht niemand die Chats. */
    @Test
    void chats_withoutMembershipReturns403() throws Exception {
        givenSignedInAs("carl");
        when(telegramChatUseCase.listChats("carl", 1L))
                .thenThrow(new MembershipRequiredException("Die Person ist nicht Mitglied dieses Haushalts."));

        mockMvc.perform(get("/api/households/1/telegram/chats")).andExpect(status().isForbidden());
    }

    // --- AC13, AC14: das Trennen ---------------------------------------------

    @Test
    void disconnect_forTheOwnConnectionReturns204() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(delete("/api/households/1/telegram/chats/{chatId}", CHAT_ID))
                .andExpect(status().isNoContent());

        verify(telegramChatUseCase).disconnect("alice", 1L, CHAT_ID);
    }

    /** AC14: eine fremde Verbindung wird abgelehnt. */
    @Test
    void disconnect_forAForeignConnectionReturns403() throws Exception {
        givenSignedInAs("bob");
        org.mockito.Mockito.doThrow(new NotChatOwnerException("Fremde Verbindung."))
                .when(telegramChatUseCase).disconnect("bob", 1L, CHAT_ID);

        mockMvc.perform(delete("/api/households/1/telegram/chats/{chatId}", CHAT_ID))
                .andExpect(status().isForbidden());
    }

    /** Eine unbekannte Chat-ID wird mit 404 beantwortet. */
    @Test
    void disconnect_forAnUnknownChatReturns404() throws Exception {
        givenSignedInAs("alice");
        org.mockito.Mockito.doThrow(new TelegramChatNotFoundException("Unbekannter Chat."))
                .when(telegramChatUseCase).disconnect("alice", 1L, CHAT_ID);

        mockMvc.perform(delete("/api/households/1/telegram/chats/{chatId}", CHAT_ID))
                .andExpect(status().isNotFound());
    }

    /** Ohne Anmeldung wird nichts getrennt. */
    @Test
    void disconnect_withoutSignInReturns401() throws Exception {
        when(currentUserPort.currentUser()).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/households/1/telegram/chats/{chatId}", CHAT_ID))
                .andExpect(status().isUnauthorized());

        verify(telegramChatUseCase, never()).disconnect(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    private void givenSignedInAs(String userId) {
        when(currentUserPort.currentUser())
                .thenReturn(Optional.of(new UserAccount(userId, userId, null)));
    }
}
