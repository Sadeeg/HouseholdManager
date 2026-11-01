package de.householdmanager.adapters.inbound.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.domain.model.UserAccount;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test des eingehenden Adapters: geprüft wird die Abbildung des Ports auf
 * das HTTP-Format — nicht die Sicherheitskonfiguration.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Security wird separat getestet
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @Test
    void currentUser_returnsTheAuthenticatedUser() throws Exception {
        when(currentUserPort.currentUser())
                .thenReturn(Optional.of(new UserAccount("sascha", "Sascha Deeg", "sascha@example.org")));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sascha"))
                .andExpect(jsonPath("$.displayName").value("Sascha Deeg"))
                .andExpect(jsonPath("$.email").value("sascha@example.org"))
                // Der Vertrag ist genau diese drei Felder (Issue #5, AC2).
                .andExpect(content().json(
                        "{\"id\":\"sascha\",\"displayName\":\"Sascha Deeg\",\"email\":\"sascha@example.org\"}",
                        true));
    }

    @Test
    void currentUser_isUnauthorized_withoutUser() throws Exception {
        when(currentUserPort.currentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
