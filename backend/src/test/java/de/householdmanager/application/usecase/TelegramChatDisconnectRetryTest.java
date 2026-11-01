package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.TelegramChatUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.domain.model.WasteCollection;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.NotificationDeliveryRepository;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.application.port.out.TelegramChatRepository;
import de.householdmanager.application.port.out.TelegramChatRepository.CodeOwner;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.NotificationDelivery;
import de.householdmanager.domain.model.TelegramChat;
import de.householdmanager.domain.model.TelegramDeliveryException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AC15 über den <strong>Wiederholungslauf</strong>: „Nach dem Trennen eines
 * Chats erhält dieser Chat keine weiteren Benachrichtigungen."
 *
 * <p>Der vorhandene Test
 * ({@code TelegramChatDisconnectNotificationTest}) belegt AC15 nur für den
 * 18:00-Lauf. Der zweite Weg, auf dem ein Chat eine Nachricht bekommen kann,
 * ist der stündliche Wiederholungslauf: ein Vermerk, dessen Versand
 * fehlschlug, wird erneut versucht. Dieser Test deckt genau diesen Weg ab.
 *
 * <p>Der Szenario-Mix: der Versand schlägt fehl (der Vermerk bleibt offen),
 * danach trennt das freischaltende Mitglied den Chat, danach läuft die
 * Wiederholung. Der getrennte Chat darf keine Nachricht mehr bekommen.
 */
class TelegramChatDisconnectRetryTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final long HOUSEHOLD = 1L;
    private static final String MEMBER = "alice";
    private static final String CODE = "123456";
    private static final String CHAT_ID = "-1001234567890";

    /** Mittwoch, 11.03.2026 — der Termin liegt am Folgetag. */
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 3, 11);

    private InMemoryChats chats;
    private InMemoryDeliveries deliveries;
    private FailingBot bot;
    private TelegramChatUseCase chatUseCase;

    @BeforeEach
    void setUp() {
        chats = new InMemoryChats();
        deliveries = new InMemoryDeliveries();
        bot = new FailingBot();

        chatUseCase = new TelegramChatUseCaseImpl(
                chats, bot, new StubHouseholds(), new StubMembership());
    }

    /**
     * AC15: ein getrennter Chat bekommt auch über den Wiederholungslauf keine
     * Nachricht mehr.
     */
    @Test
    void aDisconnectedChatReceivesNoRetry() {
        chatUseCase.code(MEMBER, HOUSEHOLD);
        chatUseCase.handleMessage(CHAT_ID, CODE);

        // Der Versand schlägt fehl — der Vermerk bleibt offen.
        bot.failure = new TelegramDeliveryException("Telegram ist nicht erreichbar.");
        dispatchAt(WEDNESDAY.atTime(18, 0));
        assertThat(bot.messages).isEmpty();
        assertThat(deliveries.all()).hasSize(1);

        // Das Mitglied trennt den Chat.
        chatUseCase.disconnect(MEMBER, HOUSEHOLD, CHAT_ID);
        assertThat(chats.findByHousehold(HOUSEHOLD)).isEmpty();

        // Eine Stunde später: der Wiederholungslauf läuft. Der Chat ist
        // getrennt — er darf keine Nachricht mehr bekommen.
        bot.failure = null;
        retryAt(WEDNESDAY.atTime(19, 0));

        assertThat(bot.messages).isEmpty();
    }

    private void dispatchAt(LocalDateTime time) {
        new NotificationDispatchUseCaseImpl(chats, deliveries, bot, new StubWaste(),
                Clock.fixed(time.atZone(ZONE).toInstant(), ZONE), ZONE.getId()).dispatch();
    }

    private void retryAt(LocalDateTime time) {
        new NotificationDispatchUseCaseImpl(chats, deliveries, bot, new StubWaste(),
                Clock.fixed(time.atZone(ZONE).toInstant(), ZONE), ZONE.getId()).retryFailed();
    }

    /** Der Müllkalender: ein Termin am Folgetag. */
    private static final class StubWaste implements WasteCollectionUseCase {

        private final List<WasteCollection> terms = new ArrayList<>();

        StubWaste() {
            terms.add(WasteCollection.builder()
                    .householdId(HOUSEHOLD)
                    .collectionDate(LocalDate.of(2026, 3, 12))
                    .wasteType("Restmüll")
                    .build());
        }

        @Override
        public WasteOverview overview(String userId, long householdId) {
            return new WasteOverview(null, terms);
        }

        @Override
        public List<WasteCollection> collectionsOf(long householdId) {
            return terms;
        }

        @Override
        public void syncHousehold(long householdId) {
            // im Test nicht gebraucht
        }
    }

    /** Die Chats als In-Memory-Fake. */
    private static final class InMemoryChats implements TelegramChatRepository {

        private final Map<Long, Map<String, TelegramChat>> chats = new LinkedHashMap<>();
        private final Map<Long, String> codes = new LinkedHashMap<>();
        private final Map<Long, String> codeOwners = new LinkedHashMap<>();

        @Override
        public List<TelegramChat> findByHousehold(long householdId) {
            return new ArrayList<>(chats.getOrDefault(householdId, new LinkedHashMap<>()).values());
        }

        @Override
        public Optional<TelegramChat> findByHouseholdAndChatId(long householdId, String chatId) {
            return Optional.ofNullable(chats.getOrDefault(householdId, new LinkedHashMap<>()).get(chatId));
        }

        @Override
        public Optional<Long> findHouseholdByChatId(String chatId) {
            return chats.entrySet().stream()
                    .filter(entry -> entry.getValue().containsKey(chatId))
                    .map(Map.Entry::getKey)
                    .findFirst();
        }

        @Override
        public List<Long> findHouseholdsWithChats() {
            return chats.keySet().stream().sorted().toList();
        }

        @Override
        public Optional<CodeOwner> findCodeOwner(String code) {
            for (Map.Entry<Long, String> entry : codes.entrySet()) {
                if (entry.getValue().equals(code)) {
                    return Optional.of(new CodeOwner(entry.getKey(), codeOwners.get(entry.getKey())));
                }
            }
            return Optional.empty();
        }

        @Override
        public String ensureCode(long householdId, String userId) {
            return codes.computeIfAbsent(householdId, id -> {
                codeOwners.put(id, userId);
                return CODE;
            });
        }

        @Override
        public TelegramChat save(TelegramChat chat) {
            chats.computeIfAbsent(chat.getHouseholdId(), id -> new LinkedHashMap<>())
                    .put(chat.getChatId(), chat);
            return chat;
        }

        @Override
        public void delete(long householdId, String chatId) {
            Map<String, TelegramChat> ofHousehold = chats.get(householdId);
            if (ofHousehold != null) {
                ofHousehold.remove(chatId);
            }
        }
    }

    /** Die Zustellvermerke als In-Memory-Fake. */
    private static final class InMemoryDeliveries implements NotificationDeliveryRepository {

        private final List<NotificationDelivery> deliveries = new ArrayList<>();

        @Override
        public List<NotificationDelivery> findRetryCandidates() {
            return deliveries.stream().filter(NotificationDelivery::isRetryable).toList();
        }

        @Override
        public NotificationDelivery save(NotificationDelivery delivery) {
            Long id = delivery.getId();
            deliveries.removeIf(existing -> existing.getId() != null && existing.getId().equals(id));
            NotificationDelivery stored = delivery;
            if (stored.getId() == null) {
                stored = stored.toBuilder().id((long) deliveries.size() + 1).build();
            }
            deliveries.add(stored);
            return stored;
        }

        List<NotificationDelivery> all() {
            return deliveries;
        }
    }

    /** Der Bot: er kann fehlschlagen — und merkt sich jede gesendete Nachricht. */
    private static final class FailingBot implements TelegramBot {

        private final List<String> messages = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public void sendMessage(String chatId, String text) {
            if (failure != null) {
                throw failure;
            }
            messages.add(text);
        }

        @Override
        public String chatName(String chatId) {
            return "WG Flur";
        }

        @Override
        public ChatKind chatKind(String chatId) {
            return ChatKind.GROUP;
        }
    }

    /** Der Haushalt: einer ist bekannt. */
    private static final class StubHouseholds implements HouseholdRepository {

        @Override
        public List<Household> findAll() {
            return List.of();
        }

        @Override
        public Optional<Household> findById(long id) {
            return Optional.of(Household.builder().id(HOUSEHOLD).name("Familie Müller")
                    .createdBy(MEMBER).build());
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

    /** Die Mitgliedschaft: nur {@code alice} ist Mitglied. */
    private static final class StubMembership implements MembershipUseCase {

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
            return null;
        }

        @Override
        public void removeMember(String currentUserId, long householdId, String userId) {
            // im Test nicht gebraucht
        }

        @Override
        public void requireMembership(long householdId, String userId) {
            if (!MEMBER.equals(userId)) {
                throw new MembershipRequiredException("Diese Person ist kein Mitglied des Haushalts.");
            }
        }

        @Override
        public boolean isMember(long householdId, String userId) {
            return MEMBER.equals(userId);
        }
    }
}
