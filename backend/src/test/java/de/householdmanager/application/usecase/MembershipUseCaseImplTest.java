package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.householdmanager.application.port.in.MembershipUseCase.MemberView;
import de.householdmanager.application.port.out.HouseholdMemberRepository;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.UserDirectory;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.HouseholdMember;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.NotHouseholdAdminException;
import de.householdmanager.domain.model.UnknownUserException;
import de.householdmanager.domain.model.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test des Use Cases „Mitgliedschaft" gegen In-Memory-Fakes der Ports.
 */
class MembershipUseCaseImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T10:15:30Z");
    private static final long HOUSEHOLD = 1L;
    private static final String ADMIN = "alice"; // Erstellerin = Haushaltsleitung

    private InMemoryMemberRepository members;
    private MembershipUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        members = new InMemoryMemberRepository();
        InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
        households.save(Household.builder()
                .id(HOUSEHOLD).name("Haushalt Müller").createdAt(FIXED_NOW).createdBy(ADMIN).build());
        // Die Haushaltsleitung ist immer Mitglied (bei Anlage eingetragen).
        members.save(new HouseholdMember(1L, HOUSEHOLD, ADMIN, FIXED_NOW));
        UserDirectory directory = new StubUserDirectory(
                new UserAccount("alice", "Alice Admin", null),
                new UserAccount("bob", "Bob", null));
        useCase = new MembershipUseCaseImpl(
                members, households, directory, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    // --- Ernennen und Entfernen (AC11) ------------------------------------------------------

    @Test
    void addMember_byHouseholdAdmin_appointsThePersonFromTheDirectory() {
        MemberView view = useCase.addMember(ADMIN, HOUSEHOLD, "bob");

        assertThat(view.userId()).isEqualTo("bob");
        assertThat(view.displayName()).isEqualTo("Bob");
        assertThat(view.householdAdmin()).isFalse();
        assertThat(members.exists(HOUSEHOLD, "bob")).isTrue();
    }

    @Test
    void addMember_byPlainMember_isForbidden() {
        members.save(new HouseholdMember(2L, HOUSEHOLD, "bob", FIXED_NOW));

        assertThatThrownBy(() -> useCase.addMember("bob", HOUSEHOLD, "carl"))
                .isInstanceOf(NotHouseholdAdminException.class);
    }

    @Test
    void addMember_unknownPersonIsRejectedAndStaysOutsideTheList() {
        assertThatThrownBy(() -> useCase.addMember(ADMIN, HOUSEHOLD, "ghost"))
                .isInstanceOf(UnknownUserException.class);
        assertThat(members.exists(HOUSEHOLD, "ghost")).isFalse();
    }

    @Test
    void addMember_twiceIsIdempotent() {
        useCase.addMember(ADMIN, HOUSEHOLD, "bob");
        MemberView again = useCase.addMember(ADMIN, HOUSEHOLD, "bob");

        assertThat(again.userId()).isEqualTo("bob");
        assertThat(members.findByHousehold(HOUSEHOLD)).hasSize(2); // alice + bob, nur einmal
    }

    @Test
    void removeMember_byAdmin_revokesTheAccessAndKeepsTheHouseholdIntact() {
        useCase.addMember(ADMIN, HOUSEHOLD, "bob");

        useCase.removeMember(ADMIN, HOUSEHOLD, "bob");

        assertThat(members.exists(HOUSEHOLD, "bob")).isFalse(); // nur der Zugang enden soll
    }

    @Test
    void removeMember_byPlainMember_isForbidden() {
        members.save(new HouseholdMember(2L, HOUSEHOLD, "bob", FIXED_NOW));

        assertThatThrownBy(() -> useCase.removeMember("bob", HOUSEHOLD, "carl"))
                .isInstanceOf(NotHouseholdAdminException.class);
    }

    @Test
    void removeMember_ofTheHouseholdAdminItself_isRejected() {
        assertThatThrownBy(() -> useCase.removeMember(ADMIN, HOUSEHOLD, ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Haushaltsleitung");
        assertThat(members.exists(HOUSEHOLD, ADMIN)).isTrue();
    }

    @Test
    void operationsOnAMissingHousehold_areNotFound() {
        assertThatThrownBy(() -> useCase.addMember(ADMIN, 999L, "bob"))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    // --- Zugang zur Tafel (AC12) -------------------------------------------------------------

    @Test
    void requireMembership_acceptsAppointedMembers_only() {
        useCase.requireMembership(HOUSEHOLD, ADMIN);
        assertThatThrownBy(() -> useCase.requireMembership(HOUSEHOLD, "zoe"))
                .isInstanceOf(MembershipRequiredException.class);
        assertThatThrownBy(() -> useCase.requireMembership(HOUSEHOLD, null))
                .isInstanceOf(MembershipRequiredException.class);
    }

    @Test
    void listMembers_showsTheHouseholdAdminFirstAndMarksHer() {
        useCase.addMember(ADMIN, HOUSEHOLD, "bob");

        List<MemberView> views = useCase.listMembers(ADMIN, HOUSEHOLD);

        assertThat(views).extracting(MemberView::userId).containsExactly(ADMIN, "bob");
        assertThat(views.getFirst().householdAdmin()).isTrue();
        assertThat(views.getLast().householdAdmin()).isFalse();
        assertThat(views.getFirst().displayName()).isEqualTo("Alice Admin");
    }

    @Test
    void listMembers_byNonMember_isForbidden() {
        assertThatThrownBy(() -> useCase.listMembers("zoe", HOUSEHOLD))
                .isInstanceOf(MembershipRequiredException.class);
    }

    // --- Testgestell -------------------------------------------------------------------------

    static final class InMemoryMemberRepository implements HouseholdMemberRepository {

        private final List<HouseholdMember> store = new ArrayList<>();
        private long ids = 1;

        @Override
        public List<HouseholdMember> findByHousehold(long householdId) {
            return store.stream().filter(m -> m.householdId() == householdId).toList();
        }

        @Override
        public List<HouseholdMember> findByUser(String userId) {
            return store.stream().filter(m -> m.userId().equals(userId)).toList();
        }

        @Override
        public boolean exists(long householdId, String userId) {
            return store.stream().anyMatch(m -> m.householdId() == householdId && m.userId().equals(userId));
        }

        @Override
        public HouseholdMember save(HouseholdMember member) {
            HouseholdMember saved = member.id() == null
                    ? new HouseholdMember(++ids, member.householdId(), member.userId(), member.createdAt())
                    : member;
            store.removeIf(m -> m.householdId() == saved.householdId() && m.userId().equals(saved.userId()));
            store.add(saved);
            return saved;
        }

        @Override
        public void delete(long householdId, String userId) {
            store.removeIf(m -> m.householdId() == householdId && m.userId().equals(userId));
        }
    }

    static final class InMemoryHouseholdRepository implements HouseholdRepository {

        private final Map<Long, Household> store = new LinkedHashMap<>();

        @Override
        public List<Household> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public Optional<Household> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Household> findByCreatedBy(String userId) {
            return store.values().stream().filter(h -> userId.equals(h.getCreatedBy())).toList();
        }

        @Override
        public Household save(Household household) {
            store.put(household.getId(), household);
            return household;
        }
    }

    static final class StubUserDirectory implements UserDirectory {

        private final Map<String, UserAccount> accounts = new HashMap<>();

        StubUserDirectory(UserAccount... accounts) {
            for (UserAccount account : accounts) {
                this.accounts.put(account.id(), account);
            }
        }

        @Override
        public Optional<UserAccount> findById(String id) {
            return Optional.ofNullable(accounts.get(id));
        }

        @Override
        public List<UserAccount> search(String query, int limit) {
            return List.of();
        }
    }
}
