package de.householdmanager.adapters.inbound.rest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.MembershipUseCase.MemberView;
import de.householdmanager.domain.model.NotHouseholdAdminException;
import de.householdmanager.domain.model.UnknownUserException;
import de.householdmanager.domain.model.UserAccount;
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
 * Test des eingehenden Adapters: HTTP-Übersetzung des Mitgliedschafts-Ports.
 */
@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MembershipUseCase membershipUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @Test
    void listMembers_mapsMembersWithResolvedNames() throws Exception {
        givenSignedInAs("alice");
        when(membershipUseCase.listMembers("alice", 1L)).thenReturn(List.of(
                new MemberView("alice", "Alice Admin", true),
                new MemberView("bob", "Bob", false)));

        mockMvc.perform(get("/api/households/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("alice"))
                .andExpect(jsonPath("$[0].displayName").value("Alice Admin"))
                .andExpect(jsonPath("$[0].householdAdmin").value(true))
                .andExpect(jsonPath("$[1].householdAdmin").value(false));
    }

    @Test
    void listMembers_unresolvableIdentity_showsNullNameSoFrontendShowsIdentifier() throws Exception {
        givenSignedInAs("alice");
        when(membershipUseCase.listMembers("alice", 1L))
                .thenReturn(List.of(new MemberView("ghost", null, false)));

        mockMvc.perform(get("/api/households/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("ghost"))
                .andExpect(jsonPath("$[0].displayName").isEmpty());
    }

    @Test
    void addMember_byHouseholdAdmin_returnsCreated() throws Exception {
        givenSignedInAs("alice");
        when(membershipUseCase.addMember("alice", 1L, "bob"))
                .thenReturn(new MemberView("bob", "Bob", false));

        mockMvc.perform(post("/api/households/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"bob\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("bob"));
    }

    @Test
    void addMember_byNonAdmin_isForbidden() throws Exception {
        givenSignedInAs("bob");
        when(membershipUseCase.addMember(anyString(), anyLong(), anyString()))
                .thenThrow(new NotHouseholdAdminException("nur die Haushaltsleitung"));

        mockMvc.perform(post("/api/households/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"carl\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addMember_unknownPerson_isBadRequest() throws Exception {
        givenSignedInAs("alice");
        when(membershipUseCase.addMember(anyString(), anyLong(), anyString()))
                .thenThrow(new UnknownUserException("Person unbekannt"));

        mockMvc.perform(post("/api/households/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ghost\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addMember_withoutUserId_isBadRequest() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(post("/api/households/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeMember_returnsNoContent() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(delete("/api/households/1/members/bob"))
                .andExpect(status().isNoContent());
    }

    private void givenSignedInAs(String userId) {
        when(currentUserPort.currentUser())
                .thenReturn(Optional.of(new UserAccount(userId, userId, null)));
    }
}
