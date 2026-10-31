package de.householdmanager.adapters.outbound.nextcloud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.householdmanager.application.port.out.UserDirectory;
import de.householdmanager.domain.model.UserAccount;
import de.householdmanager.domain.model.UserDirectoryUnavailableException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Ausgehender Adapter: Nextcloud als Nutzerverzeichnis (OCS-User-API).
 *
 * <p>Auth: Basic-Auth mit dem Service-User und seinem App-Password. Ohne
 * konfiguriertes Passwort ist der Adapter deaktiviert und liefert leere
 * Ergebnisse — die Anwendung startet trotzdem.
 */
@Slf4j
@Component
public class NextcloudUserDirectory implements UserDirectory {

    private final RestClient http;
    private final String baseUrl;
    private final boolean enabled;

    public NextcloudUserDirectory(
            RestClient.Builder restClientBuilder,
            @Value("${nextcloud.base-url:}") String baseUrl,
            @Value("${nextcloud.admin-user:}") String adminUser,
            @Value("${nextcloud.admin-app-password:}") String adminAppPassword) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.enabled = !this.baseUrl.isBlank()
                && adminUser != null && !adminUser.isBlank()
                && adminAppPassword != null && !adminAppPassword.isBlank();

        RestClient.Builder builder = restClientBuilder.baseUrl(this.baseUrl);
        if (enabled) {
            String basic = Base64.getEncoder()
                    .encodeToString((adminUser + ":" + adminAppPassword).getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        }
        builder.defaultHeader("OCS-APIRequest", "true");
        this.http = builder.build();
    }

    @Override
    public Optional<UserAccount> findById(String id) {
        if (!enabled || id == null || id.isBlank()) {
            return Optional.empty();
        }
        try {
            // /users/{id} liefert die Felder direkt in "data"
            OcsUserEnvelope envelope = http.get()
                    .uri("/ocs/v2.php/cloud/users/{id}", id)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(OcsUserEnvelope.class);
            if (envelope == null || envelope.ocs() == null || envelope.ocs().data() == null) {
                return Optional.empty();
            }
            return Optional.of(toAccount(envelope.ocs().data()));
        } catch (RuntimeException exception) {
            log.warn("Nextcloud-Nutzer {} konnte nicht geladen werden: {}", id, exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<UserAccount> search(String query, int limit) {
        if (!enabled || query == null || query.trim().length() < 2) {
            return List.of();
        }
        try {
            // Die Suche liefert nur IDs — die Details kommen je Nutzer
            OcsSearchEnvelope envelope = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ocs/v2.php/cloud/users")
                            .queryParam("search", query.trim())
                            .queryParam("limit", Math.max(1, Math.min(50, limit)))
                            .queryParam("format", "json")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(OcsSearchEnvelope.class);
            if (envelope == null || envelope.ocs() == null
                    || envelope.ocs().data() == null || envelope.ocs().data().users() == null) {
                return List.of();
            }
            List<UserAccount> accounts = new ArrayList<>();
            for (String userId : envelope.ocs().data().users()) {
                findById(userId).ifPresent(accounts::add);
            }
            return accounts;
        } catch (RuntimeException exception) {
            log.warn("Nextcloud-User-Suche nach '{}' fehlgeschlagen: {}", query, exception.getMessage());
            // Kein stilles „niemand gefunden": der Client muss den Ausfall des
            // Verzeichnisses vom Fall „keine Treffer" unterscheiden können
            // (Issue #21).
            throw new UserDirectoryUnavailableException(
                    "Das Nutzerverzeichnis ist nicht erreichbar.", exception);
        }
    }

    private UserAccount toAccount(OcsUser user) {
        return new UserAccount(user.id(), user.displayName(), user.email());
    }

    private String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    // --- OCS-DTOs (nur was gebraucht wird) ---------------------------------
    // Achtung: die OCS-API hat zwei Formen und eine Hülle:
    //   {"ocs":{"meta":{…},"data":{"id":…}}}          → einzelner Nutzer
    //   {"ocs":{"meta":{…},"data":{"users":["id"]}}}  → Suche (nur IDs)
    // Jackson 3 ist bei unbekannten Feldern strikt → "meta" wird ignoriert.

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OcsUserEnvelope(OcsUserBody ocs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OcsUserBody(OcsUser data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OcsSearchEnvelope(OcsSearchBody ocs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OcsSearchBody(OcsSearchData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OcsSearchData(List<String> users) {
    }

    /** Nextcloud liefert die Felder kleingeschrieben („displayname"). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OcsUser(String id, @JsonProperty("displayname") String displayName, String email) {
    }
}
