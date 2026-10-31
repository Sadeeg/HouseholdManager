package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.groups.Tuple.tuple;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase.WasteOverview;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.WasteCalendarFeed;
import de.householdmanager.application.port.out.WasteCalendarFeed.FeedTerm;
import de.householdmanager.application.port.out.WasteCollectionRepository;
import de.householdmanager.application.port.out.WasteSyncStateRepository;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.WasteCalendar;
import de.householdmanager.domain.model.WasteCollection;
import de.householdmanager.domain.model.WasteFeedUnavailableException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test des Use Cases gegen ausgehende Ports — ohne Spring, ohne Datenbank.
 *
 * <p>Belegt AC3 (Fenster), AC6 (fehlgeschlagener Lauf), AC7 (es entsteht
 * nichts weiter), AC12 und AC13 (Ersetzen und Leeren des Bestands), AC20
 * (veralteter Stand). Die Ports sind kleine In-Memory-Fakes.
 */
class WasteCollectionUseCaseImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T10:15:30Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);
    private static final long HOUSEHOLD = 1L;
    private static final String MEMBER = "alice";
    private static final String URL = "https://entsorger.example/feed.ics";

    private InMemoryCollections collections;
    private InMemorySyncState syncState;
    private StubFeed feed;
    private StubMembership membership;
    private StubHouseholds households;
    private WasteCollectionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        collections = new InMemoryCollections();
        syncState = new InMemorySyncState();
        feed = new StubFeed();
        membership = new StubMembership(MEMBER);
        households = new StubHouseholds();
        useCase = new WasteCollectionUseCaseImpl(
                feed, collections, syncState, households, membership,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    // --- AC3: das Fenster des Laufs ------------------------------------------

    @Test
    void syncHousehold_readsTheWindowFromRunDayAndStoresItsTerms() {
        givenHouseholdWithSource();
        feed.terms = List.of(new FeedTerm(TODAY.plusDays(3), "Restmüll", "uid-1"));

        useCase.syncHousehold(HOUSEHOLD);

        assertThat(feed.window).isEqualTo(new Window(TODAY, TODAY.plusDays(42)));
        assertThat(collections.findByHousehold(HOUSEHOLD))
                .extracting(WasteCollection::getCollectionDate, WasteCollection::getWasteType)
                .containsExactly(tuple(TODAY.plusDays(3), "Restmüll"));
    }

    @Test
    void syncHousehold_keepsTheTonnenartAsTheFeedDeliversIt() {
        givenHouseholdWithSource();
        feed.terms = List.of(new FeedTerm(TODAY.plusDays(2), "Bio(abfall) leer", null));

        useCase.syncHousehold(HOUSEHOLD);

        assertThat(collections.findByHousehold(HOUSEHOLD))
                .extracting(WasteCollection::getWasteType)
                .containsExactly("Bio(abfall) leer");
    }

    /** AC21 auf Anwendungsebene: dieselbe (Datum, Tonnenart) wird nicht zweimal gespeichert. */
    @Test
    void syncHousehold_storesOneTermPerDateAndType() {
        givenHouseholdWithSource();
        feed.terms = List.of(
                new FeedTerm(TODAY.plusDays(4), "Restmüll", "uid-1"),
                new FeedTerm(TODAY.plusDays(4), "Restmüll", "uid-2"));

        useCase.syncHousehold(HOUSEHOLD);

        assertThat(collections.findByHousehold(HOUSEHOLD)).hasSize(1);
    }

    // --- AC12, AC13: der Bestand wird ersetzt --------------------------------

    @Test
    void syncHousehold_replacesTheStoredTermsWithTheNewRun() {
        givenHouseholdWithSource();
        givenTerm(TODAY.plusDays(1), "Restmüll");
        feed.terms = List.of(new FeedTerm(TODAY.plusDays(10), "Papier", "uid-9"));

        useCase.syncHousehold(HOUSEHOLD);

        assertThat(collections.findByHousehold(HOUSEHOLD))
                .extracting(WasteCollection::getWasteType)
                .containsExactly("Papier");
    }

    @Test
    void syncHousehold_withoutTermsInWindow_removesStoredTerms() {
        givenHouseholdWithSource();
        givenTerm(TODAY.plusDays(2), "Restmüll");
        feed.terms = List.of();

        useCase.syncHousehold(HOUSEHOLD);

        assertThat(collections.findByHousehold(HOUSEHOLD)).isEmpty();
    }

    // --- AC6, R7, R17: fehlgeschlagener Lauf ---------------------------------

    @Test
    void syncHousehold_whenFeedUnreadable_keepsStoredTermsAndMarksStandStale() {
        givenHouseholdWithSource();
        givenTerm(TODAY.minusDays(1), "Restmüll");
        givenSuccessfulRun(Instant.parse("2025-12-20T03:00:00Z"));
        feed.failure = new WasteFeedUnavailableException("nicht erreichbar");

        useCase.syncHousehold(HOUSEHOLD);

        assertThat(collections.findByHousehold(HOUSEHOLD)).hasSize(1);
        WasteOverview overview = useCase.overview(MEMBER, HOUSEHOLD);
        assertThat(overview.calendar().isStale()).isTrue();
        assertThat(overview.calendar().getLastSyncedAt()).isEqualTo(Instant.parse("2025-12-20T03:00:00Z"));
        assertThat(overview.calendar().getLastAttemptAt()).isEqualTo(FIXED_NOW);
    }

    /** R3 und R4: ohne Quelle kein Lauf — der Bestand bleibt. */
    @Test
    void syncHousehold_withoutSource_doesNothing() {
        givenTerm(TODAY.plusDays(5), "Restmüll");
        households.sourceUrl = null;

        useCase.syncHousehold(HOUSEHOLD);

        assertThat(collections.findByHousehold(HOUSEHOLD)).hasSize(1);
        assertThat(feed.called).isFalse();
    }

    // --- AC20 und R13: der Stand ----------------------------------------------

    @Test
    void overview_isNotStaleAfterASuccessfulRun() {
        givenHouseholdWithSource();
        feed.terms = List.of(new FeedTerm(TODAY.plusDays(6), "Glas", "uid-3"));

        useCase.syncHousehold(HOUSEHOLD);

        WasteOverview overview = useCase.overview(MEMBER, HOUSEHOLD);
        assertThat(overview.calendar().isStale()).isFalse();
        assertThat(overview.calendar().getLastSyncedAt()).isEqualTo(FIXED_NOW);
        assertThat(overview.calendar().getSourceUrl()).isEqualTo(URL);
    }

    @Test
    void overview_withoutAnyRun_hasNoStandAndIsNotStale() {
        givenHouseholdWithSource();

        WasteOverview overview = useCase.overview(MEMBER, HOUSEHOLD);

        assertThat(overview.calendar().getLastSyncedAt()).isNull();
        assertThat(overview.calendar().isStale()).isFalse();
        assertThat(overview.collections()).isEmpty();
    }

    /** R9 und Annahme 8: aufsteigend nach Datum, bei gleichem Datum nach Tonnenart. */
    @Test
    void overview_sortsByDateThenByWasteType() {
        givenHouseholdWithSource();
        collections.store(HOUSEHOLD, List.of(
                term(TODAY.plusDays(9), "Papier"),
                term(TODAY.plusDays(2), "Restmüll"),
                term(TODAY.plusDays(2), "Bio")));

        assertThat(useCase.overview(MEMBER, HOUSEHOLD).collections())
                .extracting(WasteCollection::getWasteType)
                .containsExactly("Bio", "Restmüll", "Papier");
    }

    // --- AC10: nur Mitglieder -----------------------------------------------

    @Test
    void overview_forNonMember_throwsMembershipRequired() {
        givenHouseholdWithSource();
        membership.members = List.of("bob");

        assertThrows(MembershipRequiredException.class,
                () -> useCase.overview(MEMBER, HOUSEHOLD));
    }

    /** AC7: der Use Case kennt nur seine eigenen Ports — keine Aufgabe, keine Ausgabe, keine Benachrichtigung. */
    @Test
    void useCaseDependsOnNothingButTheWastePorts() {
        assertThat(Arrays.stream(WasteCollectionUseCaseImpl.class.getDeclaredFields())
                .filter(field -> !"log".equals(field.getName()))
                .map(Field::getType)
                .map(Class::getSimpleName))
                .containsExactlyInAnyOrder(
                        "WasteCalendarFeed", "WasteCollectionRepository", "WasteSyncStateRepository",
                        "HouseholdRepository", "MembershipUseCase", "Clock");
    }

    // --- HELPERS ------------------------------------------------------------

    private void givenHouseholdWithSource() {
        households.sourceUrl = URL;
    }

    private void givenTerm(LocalDate date, String wasteType) {
        collections.store(HOUSEHOLD, new ArrayList<>(List.of(term(date, wasteType))));
    }

    private void givenSuccessfulRun(Instant run) {
        syncState.save(HOUSEHOLD, run, run);
    }

    private static WasteCollection term(LocalDate date, String wasteType) {
        return WasteCollection.builder().householdId(HOUSEHOLD).collectionDate(date).wasteType(wasteType).build();
    }

    record Window(LocalDate from, LocalDate to) {
    }

    private static class InMemoryCollections implements WasteCollectionRepository {
        private final Map<Long, List<WasteCollection>> byHousehold = new HashMap<>();
        private long nextId = 1;

        void store(long householdId, List<WasteCollection> terms) {
            byHousehold.put(householdId, new ArrayList<>(terms));
        }

        @Override
        public List<WasteCollection> findByHousehold(long householdId) {
            return byHousehold.getOrDefault(householdId, List.of()).stream()
                    .sorted(Comparator.comparing(WasteCollection::getCollectionDate))
                    .toList();
        }

        @Override
        public void replace(long householdId, List<WasteCollection> terms) {
            List<WasteCollection> stored = new ArrayList<>();
            for (WasteCollection term : terms) {
                stored.add(term.withId(nextId++));
            }
            byHousehold.put(householdId, stored);
        }
    }

    private static class InMemorySyncState implements WasteSyncStateRepository {
        private final Map<Long, WasteCalendar> byHousehold = new HashMap<>();

        @Override
        public Optional<WasteCalendar> findByHousehold(long householdId) {
            return Optional.ofNullable(byHousehold.get(householdId));
        }

        @Override
        public WasteCalendar save(long householdId, Instant lastSyncedAt, Instant lastAttemptAt) {
            WasteCalendar saved = WasteCalendar.builder()
                    .householdId(householdId)
                    .lastSyncedAt(lastSyncedAt)
                    .lastAttemptAt(lastAttemptAt)
                    .build();
            byHousehold.put(householdId, saved);
            return saved;
        }
    }

    private static class StubFeed implements WasteCalendarFeed {
        List<FeedTerm> terms = List.of();
        WasteFeedUnavailableException failure;
        Window window;
        boolean called;

        @Override
        public List<FeedTerm> fetch(String url, LocalDate from, LocalDate to) {
            called = true;
            window = new Window(from, to);
            if (failure != null) {
                throw failure;
            }
            return terms;
        }
    }

    private static class StubMembership implements MembershipUseCase {
        private List<String> members;

        StubMembership(String... members) {
            this.members = List.of(members);
        }

        @Override
        public List<MemberView> listMembers(String currentUserId, long householdId) {
            return List.of();
        }

        @Override
        public MemberView addMember(String currentUserId, long householdId, String userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeMember(String currentUserId, long householdId, String userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void requireMembership(long householdId, String userId) {
            if (!members.contains(userId)) {
                throw new MembershipRequiredException("Die Person ist nicht Mitglied dieses Haushalts.");
            }
        }

        @Override
        public boolean isMember(long householdId, String userId) {
            return members.contains(userId);
        }
    }

    private static class StubHouseholds implements HouseholdRepository {
        private String sourceUrl = URL;

        @Override
        public List<Household> findAll() {
            return List.of();
        }

        @Override
        public Optional<Household> findById(long id) {
            return Optional.of(Household.builder().id(id).name("Haus A")
                    .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .createdBy(MEMBER).wasteCalendarUrl(sourceUrl).build());
        }

        @Override
        public List<Household> findByCreatedBy(String userId) {
            return List.of();
        }

        @Override
        public List<Household> findWithSource() {
            return List.of();
        }

        @Override
        public Household save(Household household) {
            return household;
        }
    }
}
