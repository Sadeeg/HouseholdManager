package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.WasteCalendarFeed;
import de.householdmanager.application.port.out.WasteCalendarFeed.FeedTerm;
import de.householdmanager.application.port.out.WasteCollectionRepository;
import de.householdmanager.application.port.out.WasteSyncStateRepository;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.WasteCalendar;
import de.householdmanager.domain.model.WasteCollection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AC12: „Ändert ein Mitglied die Quelle, ersetzt der anschließende Lauf die
 * gespeicherten Termine durch die Termine der neuen Quelle."
 *
 * <p>Der vorhandene Test {@code syncHousehold_replacesTheStoredTermsWithTheNewRun}
 * belegt nur die Ersetzung — die Quelle bleibt dabei dieselbe. Offen bleibt die
 * zweite Hälfte des Akzeptanzkriteriums: dass der Lauf die <em>neue</em> Adresse
 * liest. Genau das prüft dieser Test: die Quelle wird am Haushalt gewechselt,
 * danach wird der Bestand durch die Termine der neuen Quelle ersetzt.
 */
class WasteCollectionSourceChangeTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T10:15:30Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);
    private static final long HOUSEHOLD = 1L;
    private static final String MEMBER = "alice";
    private static final String ALT_QUELLE = "https://entsorger.example/alt.ics";
    private static final String NEU_QUELLE = "https://entsorger.example/neu.ics";

    private InMemoryCollections collections;
    private InMemorySyncState syncState;
    private RecordingFeed feed;
    private MutableHouseholds households;
    private WasteCollectionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        collections = new InMemoryCollections();
        syncState = new InMemorySyncState();
        feed = new RecordingFeed();
        households = new MutableHouseholds(ALT_QUELLE);
        useCase = new WasteCollectionUseCaseImpl(
                feed, collections, syncState, households, new AlwaysMember(),
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    /**
     * AC12: nach dem Wechsel liest der Lauf die neue Quelle — und der Bestand
     * enthält danach nur noch deren Termine.
     */
    @Test
    void syncHousehold_afterTheSourceChanged_readsTheNewSourceAndReplacesTheTerms() {
        feed.termsFor(ALT_QUELLE, List.of(new FeedTerm(TODAY.plusDays(2), "Restmüll", "alt-1")));
        feed.termsFor(NEU_QUELLE, List.of(
                new FeedTerm(TODAY.plusDays(4), "Papier", "neu-1"),
                new FeedTerm(TODAY.plusDays(9), "Bio", "neu-2")));

        // Erster Lauf mit der alten Quelle.
        useCase.syncHousehold(HOUSEHOLD);
        assertThat(collections.findByHousehold(HOUSEHOLD))
                .extracting(WasteCollection::getWasteType)
                .containsExactly("Restmüll");

        // Das Mitglied ändert die Quelle.
        households.sourceUrl = NEU_QUELLE;

        // Der anschließende Lauf.
        useCase.syncHousehold(HOUSEHOLD);

        assertThat(feed.requestedUrls)
                .containsExactly(ALT_QUELLE, NEU_QUELLE);
        assertThat(collections.findByHousehold(HOUSEHOLD))
                .extracting(WasteCollection::getCollectionDate, WasteCollection::getWasteType)
                .containsExactly(
                        tuple(TODAY.plusDays(4), "Papier"),
                        tuple(TODAY.plusDays(9), "Bio"));
    }

    /**
     * Die Übersicht nennt danach die neue Adresse — die Anzeige folgt der
     * Quelle des Haushalts, nicht dem Stand des letzten Laufs (AC18).
     */
    @Test
    void overview_afterTheSourceChanged_namesTheNewSource() {
        households.sourceUrl = NEU_QUELLE;
        feed.termsFor(NEU_QUELLE, List.of(new FeedTerm(TODAY.plusDays(3), "Papier", "neu-1")));

        useCase.syncHousehold(HOUSEHOLD);

        assertThat(useCase.overview(MEMBER, HOUSEHOLD).calendar().getSourceUrl()).isEqualTo(NEU_QUELLE);
    }

    // --- In-Memory-Ablagen ---------------------------------------------------

    /** Merkt sich je Adresse, welche Termine die Quelle liefert. */
    private static final class RecordingFeed implements WasteCalendarFeed {
        private final Map<String, List<FeedTerm>> byUrl = new HashMap<>();
        private final List<String> requestedUrls = new ArrayList<>();

        void termsFor(String url, List<FeedTerm> terms) {
            byUrl.put(url, terms);
        }

        @Override
        public List<FeedTerm> fetch(String url, LocalDate from, LocalDate to) {
            requestedUrls.add(url);
            return byUrl.getOrDefault(url, List.of());
        }
    }

    /** Der Haushalt, dessen Quelle sich ändern lässt. */
    private static final class MutableHouseholds implements HouseholdRepository {
        private String sourceUrl;

        MutableHouseholds(String sourceUrl) {
            this.sourceUrl = sourceUrl;
        }

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
            this.sourceUrl = household.getWasteCalendarUrl();
            return household;
        }
    }

    private static final class InMemoryCollections implements WasteCollectionRepository {
        private final Map<Long, List<WasteCollection>> byHousehold = new HashMap<>();
        private long nextId = 1L;

        @Override
        public List<WasteCollection> findByHousehold(long householdId) {
            return byHousehold.getOrDefault(householdId, List.of());
        }

        @Override
        public void replace(long householdId, List<WasteCollection> collections) {
            List<WasteCollection> stored = new ArrayList<>();
            for (WasteCollection collection : collections) {
                stored.add(collection.withId(nextId++));
            }
            byHousehold.put(householdId, stored);
        }
    }

    private static final class InMemorySyncState implements WasteSyncStateRepository {
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

    /** Jede Person ist Mitglied — die Erlaubnis wird an anderer Stelle geprüft. */
    private static final class AlwaysMember implements MembershipUseCase {
        @Override
        public List<MemberView> listMembers(String currentUserId, long householdId) {
            return List.of();
        }

        @Override
        public Optional<String> displayNameOf(String userId) {
            return Optional.empty();
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
            // jedes Mitglied darf die Quelle pflegen (AC9)
        }

        @Override
        public boolean isMember(long householdId, String userId) {
            return true;
        }
    }
}
