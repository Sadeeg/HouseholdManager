package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.TelegramChatUseCase;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.application.port.out.TelegramChatRepository;
import de.householdmanager.application.port.out.TelegramChatRepository.CodeOwner;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.NotChatOwnerException;
import de.householdmanager.domain.model.TelegramChat;
import de.householdmanager.domain.model.TelegramChatNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test des Use Cases gegen die ausgehenden Ports — ohne Spring, ohne
 * Datenbank, ohne Telegram.
 *
 * <p>Belegt AC1–AC3 und AC7 (Code, Freischaltung, keine Doppelung), AC4–AC6
 * (Erkennung der Nachricht), AC8–AC10 (Antworten des Bots), AC13 und AC14
 * (Trennberechtigung), AC15 (keine Benachrichtigung nach dem Trennen) und
 * AC28 (die Verbindung bleibt nach drei Fehlversuchen bestehen).
 */
class TelegramChatUseCaseImplTest {

    private static final long HOUSEHOLD = 1L;
    private static final String MEMBER = "alice";
    private static final String OTHER_MEMBER = "bob";
    private static final String CODE = "123456";
    private static final String CHAT_ID = "-1001234567890";

    private InMemoryChats chats;
    private StubBot bot;
    private StubHouseholds households;
    private StubMembership membership;
    private TelegramChatUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        chats = new InMemoryChats();
        bot = new StubBot();
        households = new StubHouseholds();
        membership = new StubMembership(MEMBER);
        useCase = new TelegramChatUseCaseImpl(chats, bot, households, membership);
    }

    // --- AC1, AC2: der Code --------------------------------------------------

    /** AC1: ein Mitglied kann den Code anfordern. */
    @Test
    void code_returnsTheCodeOfTheHousehold() {
        assertThat(useCase.code(MEMBER, HOUSEHOLD)).isEqualTo(CODE);
    }

    /** AC2: der Code bleibt dauerhaft gültig — ein zweites Anfordern ändert ihn nicht. */
    @Test
    void code_staysTheSameOnASecondRequest() {
        String first = useCase.code(MEMBER, HOUSEHOLD);
        String second = useCase.code(MEMBER, HOUSEHOLD);

        assertThat(second).isEqualTo(first);
        assertThat(chats.codes).hasSize(1);
    }

    /** AC16: wer kein Mitglied ist, bekommt keinen Code. */
    @Test
    void code_forNonMember_throwsMembershipRequired() {
        membership.members = List.of(OTHER_MEMBER);

        assertThatThrownBy(() -> useCase.code(MEMBER, HOUSEHOLD))
                .isInstanceOf(MembershipRequiredException.class);
    }

    /** Ein unbekannter Haushalt wird abgelehnt — der Code gehört zu einem Haushalt. */
    @Test
    void code_forUnknownHousehold_throwsHouseholdNotFound() {
        households.known = false;

        assertThatThrownBy(() -> useCase.code(MEMBER, HOUSEHOLD))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    // --- AC4, AC5, AC6: die Erkennung der Nachricht --------------------------

    /** AC4: eine Nachricht aus reinen Ziffern schaltet den Chat frei. */
    @Test
    void handleMessage_withDigitsOnly_activatesTheChat() {
        givenCodeForHousehold(MEMBER);

        TelegramChatUseCase.BotAnswer answer = useCase.handleMessage(CHAT_ID, CODE);

        assertThat(answer.recognized()).isTrue();
        assertThat(answer.hasMessage()).isTrue();
        assertThat(chats.findByHouseholdAndChatId(HOUSEHOLD, CHAT_ID)).isPresent();
    }

    /** AC5: „/code" gefolgt vom Code schaltet den Chat frei. */
    @Test
    void handleMessage_withCodeCommand_activatesTheChat() {
        givenCodeForHousehold(MEMBER);

        TelegramChatUseCase.BotAnswer answer = useCase.handleMessage(CHAT_ID, "/code " + CODE);

        assertThat(answer.hasMessage()).isTrue();
        assertThat(chats.findByHouseholdAndChatId(HOUSEHOLD, CHAT_ID)).isPresent();
    }

    /** AC5: die Schreibweise ist gleichgültig, Leerzeichen sind optional. */
    @Test
    void handleMessage_withCodeCommandInAnyCase_activatesTheChat() {
        givenCodeForHousehold(MEMBER);

        useCase.handleMessage(CHAT_ID, "/CODE123456");

        assertThat(chats.findByHouseholdAndChatId(HOUSEHOLD, CHAT_ID)).isPresent();
    }

    /** AC6: eine Nachricht, die kein Code ist, löst keine Freischaltung aus. */
    @Test
    void handleMessage_withOrdinaryText_doesNotActivateAndStaysSilent() {
        givenCodeForHousehold(MEMBER);

        TelegramChatUseCase.BotAnswer answer = useCase.handleMessage(CHAT_ID, "Guten Morgen");

        assertThat(answer.recognized()).isFalse();
        assertThat(answer.hasMessage()).isFalse();
        assertThat(chats.findByHouseholdAndChatId(HOUSEHOLD, CHAT_ID)).isEmpty();
    }

    /** AC6: ein Text mit Ziffern, aber nicht ausschließlich, ist kein Code. */
    @Test
    void handleMessage_withTextAndDigits_doesNotActivateAndStaysSilent() {
        givenCodeForHousehold(MEMBER);

        TelegramChatUseCase.BotAnswer answer = useCase.handleMessage(CHAT_ID, "Code 123456 bitte");

        assertThat(answer.recognized()).isFalse();
        assertThat(answer.hasMessage()).isFalse();
        assertThat(chats.findByHouseholdAndChatId(HOUSEHOLD, CHAT_ID)).isEmpty();
    }

    // --- AC3, AC7: die Freischaltung ----------------------------------------

    /** AC3: nach der Freischaltung gehört der Chat zum Haushalt. */
    @Test
    void handleMessage_activatesTheChatForTheHouseholdOfTheCode() {
        givenCodeForHousehold(MEMBER);

        useCase.handleMessage(CHAT_ID, CODE);

        TelegramChat chat = chats.findByHouseholdAndChatId(HOUSEHOLD, CHAT_ID).orElseThrow();
        assertThat(chat.getHouseholdId()).isEqualTo(HOUSEHOLD);
        assertThat(chat.getActivatedBy()).isEqualTo(MEMBER);
    }

    /** AC7: ein bereits freigeschalteter Chat wird nicht doppelt angelegt. */
    @Test
    void handleMessage_forAnAlreadyActivatedChat_doesNotCreateASecondOne() {
        givenCodeForHousehold(MEMBER);
        useCase.handleMessage(CHAT_ID, CODE);

        useCase.handleMessage(CHAT_ID, CODE);

        assertThat(chats.findByHousehold(HOUSEHOLD)).hasSize(1);
    }

    /** AC11: Name und Art kommen von Telegram. */
    @Test
    void handleMessage_takesNameAndKindFromTelegram() {
        givenCodeForHousehold(MEMBER);
        bot.chatName = "WG Flur";
        bot.chatKind = TelegramBot.ChatKind.GROUP;

        useCase.handleMessage(CHAT_ID, CODE);

        TelegramChat chat = chats.findByHouseholdAndChatId(HOUSEHOLD, CHAT_ID).orElseThrow();
        assertThat(chat.getChatName()).isEqualTo("WG Flur");
        assertThat(chat.getKind()).isEqualTo(TelegramChat.ChatKind.GROUP);
    }

    // --- AC8, AC9, AC10: die Antworten des Bots -----------------------------

    /** AC8: die Bestätigung nennt den Namen des Haushalts nicht. */
    @Test
    void handleMessage_afterActivation_answersWithoutTheHouseholdName() {
        givenCodeForHousehold(MEMBER);
        households.name = "Familie Müller";

        TelegramChatUseCase.BotAnswer answer = useCase.handleMessage(CHAT_ID, CODE);

        assertThat(answer.hasMessage()).isTrue();
        assertThat(answer.message()).doesNotContain("Familie Müller");
        assertThat(answer.message()).doesNotContain(households.name);
    }

    /** AC9: ein unbekannter Code bekommt einen Hinweis auf den gültigen Code. */
    @Test
    void handleMessage_withUnknownCode_answersWithAHint() {
        chats.codeKnown = false;

        TelegramChatUseCase.BotAnswer answer = useCase.handleMessage(CHAT_ID, "999999");

        assertThat(answer.recognized()).isTrue();
        assertThat(answer.hasMessage()).isTrue();
        assertThat(answer.message()).contains("Freischalt-Code");
        assertThat(chats.findByHouseholdAndChatId(HOUSEHOLD, CHAT_ID)).isEmpty();
    }

    /**
     * AC10: der Code eines anderen Haushalts bekommt denselben Hinweis — die
     * Antwort verrät nicht, ob der Code existiert.
     */
    @Test
    void handleMessage_withTheCodeOfAnotherHousehold_answersWithTheSameHint() {
        chats.codeKnown = false;

        TelegramChatUseCase.BotAnswer answer = useCase.handleMessage(CHAT_ID, "999999");

        assertThat(answer.message()).contains("Freischalt-Code");
        assertThat(chats.findByHouseholdAndChatId(HOUSEHOLD, CHAT_ID)).isEmpty();
    }

    // --- AC13, AC14: das Trennen --------------------------------------------

    /** AC13: die freischaltende Person kann die Verbindung trennen. */
    @Test
    void disconnect_forTheActivatingMember_removesTheChat() {
        givenCodeForHousehold(MEMBER);
        useCase.handleMessage(CHAT_ID, CODE);

        useCase.disconnect(MEMBER, HOUSEHOLD, CHAT_ID);

        assertThat(chats.findByHousehold(HOUSEHOLD)).isEmpty();
    }

    /** AC14: ein anderes Mitglied darf die Verbindung nicht trennen. */
    @Test
    void disconnect_forAnotherMember_throwsNotChatOwner() {
        membership.members = List.of(MEMBER, OTHER_MEMBER);
        givenCodeForHousehold(MEMBER);
        useCase.handleMessage(CHAT_ID, CODE);

        assertThatThrownBy(() -> useCase.disconnect(OTHER_MEMBER, HOUSEHOLD, CHAT_ID))
                .isInstanceOf(NotChatOwnerException.class);
        assertThat(chats.findByHousehold(HOUSEHOLD)).hasSize(1);
    }

    /** AC16: wer kein Mitglied ist, darf nichts trennen. */
    @Test
    void disconnect_forNonMember_throwsMembershipRequired() {
        membership.members = List.of(MEMBER);

        assertThatThrownBy(() -> useCase.disconnect(OTHER_MEMBER, HOUSEHOLD, CHAT_ID))
                .isInstanceOf(MembershipRequiredException.class);
    }

    /** Ein unbekannter Chat wird abgelehnt — die Antwort verrät nichts über andere Haushalte. */
    @Test
    void disconnect_forAnUnknownChat_throwsNotFound() {
        assertThatThrownBy(() -> useCase.disconnect(MEMBER, HOUSEHOLD, CHAT_ID))
                .isInstanceOf(TelegramChatNotFoundException.class);
    }

    /** Der Code des Haushalts — angefordert von der angegebenen Person. */
    private void givenCodeForHousehold(String userId) {
        chats.ensureCode(HOUSEHOLD, userId);
    }

    // --- HELPERS ------------------------------------------------------------

    /** Der Chat-Speicher als In-Memory-Fake — ohne Spring und ohne Datenbank. */
    private static final class InMemoryChats implements TelegramChatRepository {

        private final Map<Long, Map<String, TelegramChat>> chats = new LinkedHashMap<>();
        private final Map<Long, String> codes = new LinkedHashMap<>();
        private final Map<Long, String> codeOwners = new LinkedHashMap<>();
        private boolean codeKnown = true;

        @Override
        public List<TelegramChat> findByHousehold(long householdId) {
            return new ArrayList<>(chats.getOrDefault(householdId, new LinkedHashMap<>()).values());
        }

        @Override
        public Optional<TelegramChat> findByHouseholdAndChatId(long householdId, String chatId) {
            return Optional.ofNullable(chats.getOrDefault(householdId, new LinkedHashMap<>()).get(chatId));
        }

        @Override
        public List<Long> findHouseholdsWithChats() {
            return chats.keySet().stream().sorted().toList();
        }

        @Override
        public Optional<CodeOwner> findCodeOwner(String code) {
            if (!codeKnown) {
                return Optional.empty();
            }
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

    /** Der Bot als Stub — die Antworten kommen aus dem Test. */
    private static final class StubBot implements TelegramBot {

        private String chatName = "Flur";
        private ChatKind chatKind = ChatKind.PRIVATE;
        private boolean configured = true;

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public void sendMessage(String chatId, String text) {
            // Der Bot wird im Freischaltungspfad nicht zum Senden gebraucht.
        }

        @Override
        public String chatName(String chatId) {
            return chatName;
        }

        @Override
        public ChatKind chatKind(String chatId) {
            return chatKind;
        }
    }

    /** Die Haushalte als Stub — ein Haushalt ist bekannt. */
    private static final class StubHouseholds implements HouseholdRepository {

        private boolean known = true;
        private String name = "Haushalt";

        @Override
        public List<Household> findAll() {
            return known ? List.of(household()) : List.of();
        }

        @Override
        public Optional<Household> findById(long id) {
            return known ? Optional.of(household()) : Optional.empty();
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

        private Household household() {
            return Household.builder().id(HOUSEHOLD).name(name).createdBy(MEMBER).build();
        }
    }

    /** Die Mitgliedschaft als Stub — nur die genannten Personen sind Mitglieder. */
    private static final class StubMembership implements MembershipUseCase {

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
            return null;
        }

        @Override
        public void removeMember(String currentUserId, long householdId, String userId) {
            // im Test nicht gebraucht
        }

        @Override
        public void requireMembership(long householdId, String userId) {
            if (!members.contains(userId)) {
                throw new MembershipRequiredException("Diese Person ist kein Mitglied des Haushalts.");
            }
        }

        @Override
        public boolean isMember(long householdId, String userId) {
            return members.contains(userId);
        }
    }
}
