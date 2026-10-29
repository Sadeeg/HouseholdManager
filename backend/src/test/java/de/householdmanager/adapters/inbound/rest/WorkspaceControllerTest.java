package de.householdmanager.adapters.inbound.rest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.WorkspaceUseCase;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.UserAccount;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test des eingehenden Adapters: HTTP-Übersetzung der Workspace-Auswahl (AC23).
 */
@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceUseCase workspaceUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @Test
    void currentWorkspace_returnsTheSelectedHouseholdId() throws Exception {
        givenSignedInAs("alice");
        when(workspaceUseCase.currentWorkspace("alice")).thenReturn(Optional.of(7L));

        mockMvc.perform(get("/api/me/workspace"))
                .andExpect(status().isOk())
                .andExpect(content().json("7"));
    }

    @Test
    void currentWorkspace_withoutSelection_isNotFound() throws Exception {
        givenSignedInAs("alice");
        when(workspaceUseCase.currentWorkspace("alice")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/me/workspace"))
                .andExpect(status().isNotFound());
    }

    @Test
    void selectWorkspace_returnsNoContent() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(put("/api/me/workspace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"householdId\":7}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void selectWorkspace_byNonMember_isForbidden() throws Exception {
        givenSignedInAs("zoe");
        doThrow(new MembershipRequiredException("kein Mitglied"))
                .when(workspaceUseCase).selectWorkspace(anyString(), anyLong());

        mockMvc.perform(put("/api/me/workspace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"householdId\":7}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void selectWorkspace_withoutHouseholdId_isBadRequest() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(put("/api/me/workspace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private void givenSignedInAs(String userId) {
        when(currentUserPort.currentUser())
                .thenReturn(Optional.of(new UserAccount(userId, userId, null)));
    }
}
