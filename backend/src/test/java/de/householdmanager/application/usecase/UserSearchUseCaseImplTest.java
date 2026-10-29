package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.application.port.in.UserSearchUseCase;
import de.householdmanager.application.port.out.UserDirectory;
import de.householdmanager.domain.model.UserAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserSearchUseCaseImplTest {

    private StubUserDirectory directory;
    private UserSearchUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        directory = new StubUserDirectory();
        directory.add(new UserAccount("carl", "Carl Vogel", "carl@example.org"));
        directory.add(new UserAccount("carla", "Carla Simpson", "carla@example.org"));
        useCase = new UserSearchUseCaseImpl(directory);
    }

    @Test
    void searchUsers_returnsDirectoryMatches() {
        List<UserSearchUseCase.UserRef> refs = useCase.searchUsers("car");

        assertThat(refs)
                .extracting(UserSearchUseCase.UserRef::id, UserSearchUseCase.UserRef::displayName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("carl", "Carl Vogel"),
                        org.assertj.core.groups.Tuple.tuple("carla", "Carla Simpson"));
        // Die E-Mail bleibt im Verzeichnis — Vorschläge nennen sie nicht.
        assertThat(refs.toString()).doesNotContain("example.org");
    }

    @Test
    void searchUsers_withBlankQuery_returnsNoSuggestions() {
        assertThat(useCase.searchUsers("")).isEmpty();
        assertThat(useCase.searchUsers("   ")).isEmpty();
        assertThat(useCase.searchUsers(null)).isEmpty();
        assertThat(directory.lastQuery).isNull();
    }

    @Test
    void searchUsers_trimsQuery() {
        useCase.searchUsers("  carl ");
        assertThat(directory.lastQuery).isEqualTo("carl");
    }

    /** In-Memory-Fake des Nutzerverzeichnisses. */
    private static final class StubUserDirectory implements UserDirectory {

        private final List<UserAccount> users = new java.util.ArrayList<>();
        private String lastQuery;

        void add(UserAccount user) {
            users.add(user);
        }

        @Override
        public Optional<UserAccount> findById(String id) {
            return users.stream().filter(user -> user.id().equals(id)).findFirst();
        }

        @Override
        public List<UserAccount> search(String query, int limit) {
            lastQuery = query;
            return users.stream()
                    .filter(user -> user.id().toLowerCase().contains(query.toLowerCase())
                            || user.displayName().toLowerCase().contains(query.toLowerCase()))
                    .limit(limit)
                    .toList();
        }
    }
}
