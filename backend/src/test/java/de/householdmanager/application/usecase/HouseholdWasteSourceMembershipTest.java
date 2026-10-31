package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.out.HouseholdMemberRepository;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.UserDirectory;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.HouseholdMember;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AC9 (R2) und AC10 (R1): die ICS-Quelle gehört <em>jedem</em> Mitglied des
 * Haushalts — nicht nur der Haushaltsleitung — und niemandem außerhalb.
 *
 * <p>Die Mitgliedschaft ist hier echt aufgebaut: der Test verwendet die
 * echte {@link MembershipUseCaseImpl} über eine In-Memory-Ablage der
 * Mitglieder. Nur so unterscheidet er ein einfaches Mitglied ({@code bob})
 * von der Haushaltsleitung ({@code alice}, die den Haushalt angelegt hat).
 * Ein gemockter Mitgliedschafts-Port kann das nicht: er würde die
 * Beschränkung auf die Leitung unentdeckt lassen.
 */
class HouseholdWasteSourceMembershipTest {

    private static final long HOUSEHOLD = 1L;
    private static final String ADMIN = "alice"; // Haushaltsleitung (Erstellerin)
    private static final String MEMBER = "bob"; // einfaches Mitglied, nicht Leitung
    private static final String OUTSIDER = "carol"; // kein Mitglied

    private static final String URL = "https://entsorger.example/kalender.ics";

    private InMemoryHouseholds households;
    private InMemoryMembers members;
    private HouseholdUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        households = new InMemoryHouseholds();
        members = new InMemoryMembers();
        MembershipUseCase membership = new MembershipUseCaseImpl(
                members, households, new StubDirectory(), Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC));
        useCase = new HouseholdUseCaseImpl(
                households, members, membership,
                Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC));

        households.store(Household.builder()
                .id(HOUSEHOLD).name("Haus A")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).createdBy(ADMIN)
                .build());
        members.store(ADMIN);
        members.store(MEMBER);
    }

    /**
     * AC9: ein einfaches Mitglied pflegt die Quelle — die Quelle ist nicht der
     * Leitung vorbehalten (anders als der Mitgliederbereich).
     */
    @Test
    void plainMemberMaySetTheSource() {
        Household result = useCase.setWasteSource(MEMBER, HOUSEHOLD, URL);

        assertThat(result.getWasteCalendarUrl()).isEqualTo(URL);
        assertThat(households.findById(HOUSEHOLD).orElseThrow().getWasteCalendarUrl()).isEqualTo(URL);
    }

    /** AC9: ein einfaches Mitglied ändert die Quelle. */
    @Test
    void plainMemberMayChangeTheSource() {
        households.replaceSource(HOUSEHOLD, URL);

        Household result = useCase.setWasteSource(MEMBER, HOUSEHOLD, "https://entsorger.example/anders.ics");

        assertThat(result.getWasteCalendarUrl()).isEqualTo("https://entsorger.example/anders.ics");
    }

    /** AC9: ein einfaches Mitglied entfernt die Quelle. */
    @Test
    void plainMemberMayRemoveTheSource() {
        households.replaceSource(HOUSEHOLD, URL);

        useCase.removeWasteSource(MEMBER, HOUSEHOLD);

        assertThat(households.findById(HOUSEHOLD).orElseThrow().getWasteCalendarUrl()).isNull();
    }

    /** AC10: eine Person außerhalb des Haushalts setzt die Quelle nicht. */
    @Test
    void outsiderMayNotSetTheSource() {
        assertThatThrownBy(() -> useCase.setWasteSource(OUTSIDER, HOUSEHOLD, URL))
                .isInstanceOf(MembershipRequiredException.class);
        assertThat(households.findById(HOUSEHOLD).orElseThrow().getWasteCalendarUrl()).isNull();
    }

    /**
     * AC10: eine Person außerhalb des Haushalts entfernt die Quelle nicht —
     * und die hinterlegte Quelle bleibt unverändert.
     */
    @Test
    void outsiderMayNotRemoveTheSource() {
        households.replaceSource(HOUSEHOLD, URL);

        assertThatThrownBy(() -> useCase.removeWasteSource(OUTSIDER, HOUSEHOLD))
                .isInstanceOf(MembershipRequiredException.class);
        assertThat(households.findById(HOUSEHOLD).orElseThrow().getWasteCalendarUrl()).isEqualTo(URL);
    }

    // --- In-Memory-Ablagen ---------------------------------------------------

    /** Haushalte im Speicher — nur die Lese- und Schreibwege, die die Use Cases nutzen. */
    private static final class InMemoryHouseholds implements HouseholdRepository {
        private final Map<Long, Household> byId = new LinkedHashMap<>();

        void store(Household household) {
            byId.put(household.getId(), household);
        }

        void replaceSource(long householdId, String url) {
            byId.put(householdId, byId.get(householdId).withWasteCalendarUrl(url));
        }

        @Override
        public List<Household> findAll() {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<Household> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Household> findByCreatedBy(String userId) {
            return byId.values().stream().filter(h -> userId.equals(h.getCreatedBy())).toList();
        }

        @Override
        public List<Household> findWithSource() {
            return byId.values().stream()
                    .filter(h -> h.getWasteCalendarUrl() != null && !h.getWasteCalendarUrl().isBlank())
                    .toList();
        }

        @Override
        public Household save(Household household) {
            byId.put(household.getId(), household);
            return household;
        }
    }

    /** Mitgliedschaften im Speicher. */
    private static final class InMemoryMembers implements HouseholdMemberRepository {
        private final List<HouseholdMember> entries = new ArrayList<>();
        private long nextId = 1L;

        void store(String userId) {
            entries.add(new HouseholdMember(nextId++, HOUSEHOLD, userId, Instant.parse("2026-01-01T00:00:00Z")));
        }

        @Override
        public List<HouseholdMember> findByHousehold(long householdId) {
            return entries.stream().filter(m -> m.householdId() == householdId).toList();
        }

        @Override
        public List<HouseholdMember> findByUser(String userId) {
            return entries.stream().filter(m -> m.userId().equals(userId)).toList();
        }

        @Override
        public boolean exists(long householdId, String userId) {
            return entries.stream().anyMatch(m -> m.householdId() == householdId && m.userId().equals(userId));
        }

        @Override
        public HouseholdMember save(HouseholdMember member) {
            entries.add(member);
            return member;
        }

        @Override
        public void delete(long householdId, String userId) {
            entries.removeIf(m -> m.householdId() == householdId && m.userId().equals(userId));
        }
    }

    /** Das Nutzerverzeichnis ist für die Mitgliedschaft unerheblich. */
    private static final class StubDirectory implements UserDirectory {
        @Override
        public Optional<UserAccount> findById(String id) {
            return Optional.of(new UserAccount(id, id, null));
        }

        @Override
        public List<UserAccount> search(String query, int limit) {
            return List.of();
        }
    }
}
