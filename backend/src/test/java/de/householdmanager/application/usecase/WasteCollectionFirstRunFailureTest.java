package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AC6, R13 und R17 — ein fehlgeschlagener Lauf, obwohl **noch kein
 * erfolgreicher Lauf** stattgefunden hat.
 *
 * <p>Der vorhandene Test {@code syncHousehold_whenFeedUnreadable_keepsStoredTermsAndMarksStandStale}
 * legt vorher einen erfolgreichen Lauf an. Der Fall ohne Vorgeschichte ist damit
 * unbelegt — und er ist der häufigere: der Nutzer trägt zum ersten Mal eine
 * Adresse ein, die nicht lesbar ist.
 *
 * <p>Was das Issue verlangt:
 * <ul>
 *   <li>AC6: „Schlägt ein Synchronisationslauf fehl, bleiben die gespeicherten
 *       Termine unverändert erhalten, **und die Anzeige weist aus, dass der Stand
 *       veraltet ist**." — ohne Einschränkung auf einen vorherigen Lauf.</li>
 *   <li>R13: „Die Anzeige weist den Stand als veraltet aus, **wenn seit dem
 *       letzten erfolgreichen Lauf ein weiterer geplanter Lauf stattgefunden
 *       hat**." — ohne vorherigen erfolgreichen Lauf ist „seit dem letzten
 *       erfolgreichen Lauf" der Anfang, jeder Lauf zählt.</li>
 *   <li>R17: „Ist die Quelle nicht lesbar, bleiben die gespeicherten Termine
 *       sichtbar **und der Stand gilt als veraltet**."</li>
 * </ul>
 *
 * <p>Die gelieferte Dokumentation sagt dasselbe: {@code arc42/08} („Bestand
 * bleibt unverändert (R7), Stand gilt als veraltet (R13)"),
 * {@code arc42/10} Q12 und das Glossar in {@code arc42/12} („Der Zustand, in
 * dem seit dem letzten erfolgreichen Lauf ein Lauf fehlgeschlagen ist") sowie
 * ADR 0005.
 *
 * <p>Der Test schreibt den fehlgeschlagenen **ersten** Lauf fest. Er steht
 * absichtlich neben dem Bestand: R16 („Ohne erfolgreichen Lauf nennt die
 * Anzeige ‚Noch keine Synchronisation'") bleibt unberührt — der Zeitpunkt des
 * letzten erfolgreichen Laufs bleibt null. Nur das Veraltet-Flag muss wahr sein:
 * der Nutzer muss sehen, dass der Lauf fehlgeschlagen ist, und nicht den
 * Eindruck bekommen, die Quelle liefere schlicht keine Termine.
 */
class WasteCollectionFirstRunFailureTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T10:15:30Z");
    private static final long HOUSEHOLD = 1L;
    private static final String MEMBER = "alice";
    private static final String URL = "https://entsorger.example/feed.ics";

    private InMemoryCollections collections;
    private InMemorySyncState syncState;
    private StubFeed feed;
    private WasteCollectionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        collections = new InMemoryCollections();
        syncState = new InMemorySyncState();
        feed = new StubFeed();
        useCase = new WasteCollectionUseCaseImpl(
                feed, collections, syncState, new StubHouseholds(), new StubMembership(MEMBER),
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    /**
     * AC6, R13, R17: der erste Lauf schlägt fehl — der Stand gilt als veraltet.
     *
     * <p>Ohne vorherigen erfolgreichen Lauf gibt es keinen Zeitpunkt, der
     * veraltet sein könnte — der Lauf selbst ist aber gescheitert, und genau
     * das muss die Anzeige sagen. Sonst behauptet die Seite „Für die nächsten
     * sechs Wochen liegen keine Termine vor", obwohl die Quelle gar nicht
     * gelesen werden konnte.
     */
    @Test
    void syncHousehold_whenTheFirstRunFails_marksTheStandStale() {
        feed.failure = new WasteFeedUnavailableException("Der Entsorger ist nicht erreichbar.");

        useCase.syncHousehold(HOUSEHOLD);

        WasteOverview overview = useCase.overview(MEMBER, HOUSEHOLD);

        // AC6/R13/R17: der fehlgeschlagene Lauf macht den Stand veraltet.
        assertThat(overview.calendar().isStale())
                .as("Nach einem fehlgeschlagenen Lauf gilt der Stand als veraltet (AC6, R13, R17)")
                .isTrue();

        // R16 bleibt: ohne erfolgreichen Lauf gibt es keinen Zeitpunkt.
        assertThat(overview.calendar().getLastSyncedAt()).isNull();

        // Der Versuch ist vermerkt — er ist der Grund für das Veraltet-Flag.
        assertThat(overview.calendar().getLastAttemptAt()).isEqualTo(FIXED_NOW);

        // Der Bestand bleibt unangetastet (R7).
        assertThat(overview.collections()).isEmpty();
    }

    // --- Fakes ---------------------------------------------------------------

    private static class InMemoryCollections implements WasteCollectionRepository {
        private final Map<Long, List<WasteCollection>> byHousehold = new HashMap<>();
        private long nextId = 1;

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
        WasteFeedUnavailableException failure;

        @Override
        public List<FeedTerm> fetch(String url, LocalDate from, LocalDate to) {
            if (failure != null) {
                throw failure;
            }
            return List.of();
        }
    }

    private static class StubMembership implements MembershipUseCase {
        private final List<String> members;

        StubMembership(String... members) {
            this.members = List.of(members);
        }

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
        @Override
        public List<Household> findAll() {
            return List.of();
        }

        @Override
        public Optional<Household> findById(long id) {
            return Optional.of(Household.builder().id(id).name("Haus A")
                    .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .createdBy(MEMBER).wasteCalendarUrl(URL).build());
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
