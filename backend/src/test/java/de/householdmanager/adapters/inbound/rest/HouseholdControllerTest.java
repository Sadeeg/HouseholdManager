package de.householdmanager.adapters.inbound.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.HouseholdUseCase;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.InvalidHouseholdNameException;
import de.householdmanager.domain.model.UserAccount;
import java.time.Instant;
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
 * Test des eingehenden Adapters: geprüft wird die Übersetzung zwischen HTTP
 * und eingehendem Port — nicht die Fachlogik.
 */
@WebMvcTest(HouseholdController.class)
@AutoConfigureMockMvc(addFilters = false) // Security wird separat getestet
class HouseholdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HouseholdUseCase householdUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @Test
    void listHouseholds_returnsHouseholdsOfTheSignedInPerson() throws Exception {
        givenSignedInAs("alice");
        when(householdUseCase.listHouseholds("alice")).thenReturn(List.of(Household.builder()
                .id(1L)
                .name("Haushalt Müller")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .createdBy("alice")
                .build()));

        mockMvc.perform(get("/api/households"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Haushalt Müller"))
                .andExpect(jsonPath("$[0].createdBy").value("alice"));
    }

    @Test
    void listHouseholds_withoutSignedInPerson_isUnauthorized() throws Exception {
        when(currentUserPort.currentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/households"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createHousehold_rejectsBlankName() throws Exception {
        givenSignedInAs("alice");
        mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createHousehold_mapsDomainErrorToBadRequest() throws Exception {
        givenSignedInAs("alice");
        // Die Längengrenze ist eine Invariante der Anwendung (Use Case) —
        // der Controller übersetzt nur ihren Fehler. Geprüft wird hier die
        // Abbildung, nicht die Grenze selbst (siehe HouseholdUseCaseImplTest).
        when(householdUseCase.createHousehold(anyString(), anyString()))
                .thenThrow(new InvalidHouseholdNameException(
                        "Der Name des Haushalts darf höchstens " + Household.MAX_NAME_LENGTH
                                + " Zeichen lang sein."));

        mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "x".repeat(Household.MAX_NAME_LENGTH + 1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ungültiger Haushaltsname"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("höchstens " + Household.MAX_NAME_LENGTH + " Zeichen")));
    }

    @Test
    void createHousehold_returnsCreatedHousehold() throws Exception {
        givenSignedInAs("alice");
        when(householdUseCase.createHousehold(anyString(), anyString())).thenReturn(Household.builder()
                .id(7L)
                .name("Haushalt Müller")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .createdBy("alice")
                .build());

        mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Haushalt Müller\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.createdBy").value("alice"));
    }

    private void givenSignedInAs(String userId) {
        when(currentUserPort.currentUser())
                .thenReturn(Optional.of(new UserAccount(userId, userId, null)));
    }
}
