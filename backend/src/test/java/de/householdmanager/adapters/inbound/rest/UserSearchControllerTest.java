package de.householdmanager.adapters.inbound.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.UserSearchUseCase;
import de.householdmanager.application.port.in.UserSearchUseCase.UserRef;
import de.householdmanager.domain.model.UserDirectoryUnavailableException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test des eingehenden Adapters: HTTP-Übersetzung der Personensuche.
 * Das Verzeichnis ist gemockt — geprüft werden Pfad, Query-Parameter und DTO.
 */
@WebMvcTest(UserSearchController.class)
@AutoConfigureMockMvc(addFilters = false) // Security wird separat getestet
class UserSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSearchUseCase userSearchUseCase;

    @Test
    void search_returnsSuggestions() throws Exception {
        when(userSearchUseCase.searchUsers("car"))
                .thenReturn(List.of(new UserRef("carl", "Carl Vogel")));

        mockMvc.perform(get("/api/users/search").param("q", "car"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("carl"))
                .andExpect(jsonPath("$[0].displayName").value("Carl Vogel"));
    }

    @Test
    void search_withoutQuery_returnsEmptyList() throws Exception {
        when(userSearchUseCase.searchUsers(""))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/users/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_withUnreachableDirectory_isServiceUnavailable() throws Exception {
        when(userSearchUseCase.searchUsers("car"))
                .thenThrow(new UserDirectoryUnavailableException("Das Nutzerverzeichnis ist nicht erreichbar."));

        mockMvc.perform(get("/api/users/search").param("q", "car"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Nutzerverzeichnis nicht erreichbar"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("nicht erreichbar")));
    }
}
