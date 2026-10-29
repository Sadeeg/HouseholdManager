package de.householdmanager.config;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.HouseholdUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test der Filterkette: die API ist ohne Token geschlossen, mit gültigem
 * Token offen.
 *
 * <p>Der {@link JwtDecoder} ist gemockt — der Test darf das Discovery-
 * Dokument des Identity Providers nicht laden.
 */
@WebMvcTest
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class) // der Slice lädt @Configuration-Klassen nicht von selbst
@TestPropertySource(properties = "nextcloud.oidc.issuer-uri=https://cloud.example.org")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private HouseholdUseCase householdUseCase;

    @Test
    void apiWithoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/households"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiWithValidToken_isAllowed() throws Exception {
        when(householdUseCase.listHouseholds()).thenReturn(List.of());

        mockMvc.perform(get("/api/households").with(jwt().jwt(token -> token.subject("hm-test"))))
                .andExpect(status().isOk());
    }
}
