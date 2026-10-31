package de.householdmanager.adapters.outbound.nextcloud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.householdmanager.domain.model.UserAccount;
import de.householdmanager.domain.model.UserDirectoryUnavailableException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test des ausgehenden Adapters gegen einen gemockten OCS-Endpunkt: geprüft
 * werden URL, Auth-Header und das Mapping der Antwort.
 */
class NextcloudUserDirectoryTest {

    private MockRestServiceServer server;
    private NextcloudUserDirectory directory;

    @BeforeEach
    void setUp() {
        // Jackson 3 (Boot 4): der Standard-RestClient bringt keinen
        // JSON-Converter mit, im Spring-Kontext liefert Boot ihn.
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(converters ->
                        converters.add(new JacksonJsonHttpMessageConverter(JsonMapper.builder().build())));
        server = MockRestServiceServer.bindTo(builder).build();
        directory = new NextcloudUserDirectory(builder, "https://cloud.example.org", "svc", "app-password");
    }

    @Test
    void findById_mapsUserFields() {
        server.expect(requestTo("https://cloud.example.org/ocs/v2.php/cloud/users/sascha"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("OCS-APIRequest", "true"))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Basic ")))
                .andRespond(withSuccess("""
                        {"ocs":{"meta":{"status":"ok","statuscode":200},"data":{
                          "id":"sascha","displayname":"Sascha Deeg","email":"sascha@example.org"}}}""",
                        MediaType.APPLICATION_JSON));

        assertThat(directory.findById("sascha"))
                .contains(new UserAccount("sascha", "Sascha Deeg", "sascha@example.org"));
    }

    @Test
    void findById_isEmpty_whenUserIsUnknown() {
        // Nextcloud antwortet bei unbekannten Nutzern mit 404
        server.expect(requestTo("https://cloud.example.org/ocs/v2.php/cloud/users/ghost"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(directory.findById("ghost")).isEmpty();
    }

    @Test
    void search_resolvesDetailsForEachHit() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://cloud.example.org/ocs/v2.php/cloud/users?search=sas")))
                .andRespond(withSuccess("""
                        {"ocs":{"meta":{"status":"ok","statuscode":200},"data":{"users":["sascha"]}}}""",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://cloud.example.org/ocs/v2.php/cloud/users/sascha"))
                .andRespond(withSuccess("""
                        {"ocs":{"meta":{"status":"ok","statuscode":200},"data":{
                          "id":"sascha","displayname":"Sascha Deeg","email":"sascha@example.org"}}}""",
                        MediaType.APPLICATION_JSON));

        List<UserAccount> accounts = directory.search("sascha", 10);

        assertThat(accounts).containsExactly(new UserAccount("sascha", "Sascha Deeg", "sascha@example.org"));
    }

    @Test
    void search_isEmpty_forShortQuery() {
        assertThat(directory.search("a", 10)).isEmpty();
        server.verify();
    }

    @Test
    void search_reportsUnreachableDirectory_insteadOfAnEmptyResult() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://cloud.example.org/ocs/v2.php/cloud/users?search=sas")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> directory.search("sascha", 10))
                .isInstanceOf(UserDirectoryUnavailableException.class)
                .hasMessageContaining("nicht erreichbar");
    }

    @Test
    void directory_isDisabled_withoutCredentials() {
        NextcloudUserDirectory disabled = new NextcloudUserDirectory(
                RestClient.builder(), "https://cloud.example.org", "", "");

        assertThat(disabled.search("sascha", 10)).isEmpty();
        assertThat(disabled.findById("sascha")).isEmpty();
    }
}
