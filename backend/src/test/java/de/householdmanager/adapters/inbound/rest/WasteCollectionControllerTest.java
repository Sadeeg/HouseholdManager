package de.householdmanager.adapters.inbound.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.HouseholdUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.UserAccount;
import de.householdmanager.domain.model.WasteCalendar;
import de.householdmanager.domain.model.WasteCollection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test des eingehenden Adapters: HTTP-Übersetzung des Müllterm-Ports (AC10,
 * AC11, AC22). Die Fachlogik ist gemockt — hier zählt nur die Übersetzung.
 */
@WebMvcTest(WasteCollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class WasteCollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WasteCollectionUseCase wasteCollectionUseCase;

    @MockitoBean
    private HouseholdUseCase householdUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    // --- AC10: nur Mitglieder -----------------------------------------------

    @Test
    void overview_forMemberReturnsSourceStandAndTerms() throws Exception {
        givenSignedInAs("alice");
        when(wasteCollectionUseCase.overview("alice", 1L)).thenReturn(new WasteCollectionUseCase.WasteOverview(
                WasteCalendar.builder()
                        .householdId(1L)
                        .sourceUrl("https://entsorger.example/ics")
                        .lastSyncedAt(Instant.parse("2026-01-01T03:00:00Z"))
                        .build(),
                List.of(WasteCollection.builder()
                        .id(9L).householdId(1L)
                        .collectionDate(LocalDate.of(2026, 1, 12)).wasteType("Restmüll").build())));

        mockMvc.perform(get("/api/households/1/waste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value("https://entsorger.example/ics"))
                .andExpect(jsonPath("$.lastSyncedAt").value("2026-01-01T03:00:00Z"))
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.collections[0].date").value("2026-01-12"))
                .andExpect(jsonPath("$.collections[0].wasteType").value("Restmüll"));
    }

    @Test
    void overview_withoutMembershipReturns403() throws Exception {
        givenSignedInAs("carl");
        when(wasteCollectionUseCase.overview("carl", 1L))
                .thenThrow(new MembershipRequiredException("Die Person ist nicht Mitglied dieses Haushalts."));

        mockMvc.perform(get("/api/households/1/waste")).andExpect(status().isForbidden());
    }

    @Test
    void overview_withoutSignInReturns401() throws Exception {
        when(currentUserPort.currentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/households/1/waste")).andExpect(status().isUnauthorized());
    }

    @Test
    void overview_forUnknownHouseholdReturns404() throws Exception {
        givenSignedInAs("alice");
        when(wasteCollectionUseCase.overview("alice", 1L))
                .thenThrow(new HouseholdNotFoundException("Der Haushalt existiert nicht."));

        mockMvc.perform(get("/api/households/1/waste")).andExpect(status().isNotFound());
    }

    // --- AC11: Speichern löst den Lauf aus ----------------------------------

    @Test
    void saveSource_storesTheSourceAndCallsTheSyncBeforeAnswering() throws Exception {
        givenSignedInAs("alice");
        Household stored = Household.builder().id(1L).name("Haus A")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).createdBy("alice")
                .wasteCalendarUrl("https://entsorger.example/ics").build();
        when(householdUseCase.setWasteSource("alice", 1L, "https://entsorger.example/ics")).thenReturn(stored);

        mockMvc.perform(put("/api/households/1/waste/source")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://entsorger.example/ics\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value("https://entsorger.example/ics"));

        // Der Lauf gehört zum Speichern — die Antwort wartet darauf.
        verify(wasteCollectionUseCase).syncHousehold(1L);
    }

    @Test
    void removeSourceReturns204AndDoesNotSync() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(delete("/api/households/1/waste/source")).andExpect(status().isNoContent());

        verify(wasteCollectionUseCase, never()).syncHousehold(anyLong());
    }

    // --- AC22: die Grenze wird geprüft und benannt ---------------------------

    /** AC22 und R18: die Meldung nennt die Grenze. */
    @Test
    void saveSource_rejectsUrlOver500CharactersAndNamesTheLimit() throws Exception {
        givenSignedInAs("alice");

        String tooLong = "https://entsorger.example/ics" + "x".repeat(WasteCalendar.MAX_URL_LENGTH);

        mockMvc.perform(put("/api/households/1/waste/source")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString(
                        "höchstens " + WasteCalendar.MAX_URL_LENGTH + " Zeichen")));

        verify(householdUseCase, never()).setWasteSource(anyString(), anyLong(), anyString());
    }

    @Test
    void saveSource_rejectsEmptyUrl() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(put("/api/households/1/waste/source")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    private void givenSignedInAs(String id) {
        when(currentUserPort.currentUser()).thenReturn(Optional.of(new UserAccount(id, null, null)));
    }
}
