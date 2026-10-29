package de.householdmanager.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.HouseholdUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test des Zustands ohne konfigurierten Issuer: die API startet ohne
 * Authentifizierung — so, wie es in {@code AGENTS.md}, {@code README.md}
 * und {@code arc42/08} beschrieben ist.
 *
 * <p>Dieser Zustand darf keinen Decoder bauen: ein leerer Issuer würde
 * sonst mit „URI is not absolute" den Start abbrechen.
 */
@WebMvcTest
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class) // der Slice lädt @Configuration-Klassen nicht von selbst
@TestPropertySource(properties = "nextcloud.oidc.issuer-uri=")
class SecurityConfigWithoutIssuerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private HouseholdUseCase householdUseCase;

    @Test
    void noJwtDecoder_isCreated() {
        assertThat(context.getBeanNamesForType(JwtDecoder.class)).isEmpty();
    }

    @Test
    void apiIsNotProtectedWithoutIssuer() throws Exception {
        mockMvc.perform(get("/api/households"))
                .andExpect(status().isOk());
    }
}
