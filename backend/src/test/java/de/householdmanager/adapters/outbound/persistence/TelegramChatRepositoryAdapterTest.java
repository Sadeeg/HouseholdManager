package de.householdmanager.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.config.ClockConfig;
import de.householdmanager.domain.model.TelegramChat;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Test des ausgehenden Adapters gegen eine echte PostgreSQL-Instanz
 * (Testcontainers). Prüft das Mapping, die Unveränderlichkeit des Codes
 * (AC2), die Eindeutigkeit über Haushalt und Chat (AC7) und das Finden des
 * Haushalts über den Code (AC3).
 */
@DataJpaTest
@Import({TelegramChatRepositoryAdapter.class, ClockConfig.class})
@ImportTestcontainers
class TelegramChatRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private TelegramChatRepositoryAdapter adapter;

    @Autowired
    private HouseholdJpaRepository householdJpaRepository;

    private long householdA;
    private long householdB;

    @BeforeEach
    void createHouseholds() {
        householdA = householdJpaRepository.save(HouseholdJpaEntity.builder()
                .name("Haus A").createdAt(Instant.now()).createdBy("alice").build()).getId();
        householdB = householdJpaRepository.save(HouseholdJpaEntity.builder()
                .name("Haus B").createdAt(Instant.now()).createdBy("alice").build()).getId();
    }

    /** AC2: der Code entsteht beim ersten Anfordern und bleibt danach unverändert. */
    @Test
    void ensureCode_returnsTheSameCodeOnASecondRequest() {
        String first = adapter.ensureCode(householdA, "alice");
        String second = adapter.ensureCode(householdA, "bob");

        assertThat(second).isEqualTo(first);
        assertThat(adapter.findCode(householdA)).contains(first);
    }

    /** Der Code besteht aus Ziffern — er wird im Chat abgetippt. */
    @Test
    void ensureCode_producesDigitsOnly() {
        assertThat(adapter.ensureCode(householdA, "alice")).matches("\\d+");
    }

    /** Zwei Haushalte bekommen verschiedene Codes. */
    @Test
    void ensureCode_givesEachHouseholdItsOwnCode() {
        String codeA = adapter.ensureCode(householdA, "alice");
        String codeB = adapter.ensureCode(householdB, "bob");

        assertThat(codeB).isNotEqualTo(codeA);
    }

    /** AC3: über den Code lässt sich der Haushalt finden — mit der anfordernden Person. */
    @Test
    void findCodeOwner_returnsTheHouseholdAndTheRequestingMember() {
        String code = adapter.ensureCode(householdA, "alice");

        assertThat(adapter.findCodeOwner(code))
                .isPresent()
                .get()
                .satisfies(owner -> {
                    assertThat(owner.householdId()).isEqualTo(householdA);
                    assertThat(owner.userId()).isEqualTo("alice");
                });
    }

    /** Ein unbekannter Code gehört zu keinem Haushalt (AC9, AC10). */
    @Test
    void findCodeOwner_forAnUnknownCode_isEmpty() {
        assertThat(adapter.findCodeOwner("000000")).isEmpty();
    }

    /** AC11: Name und Art werden gespeichert und zurückgegeben. */
    @Test
    void save_storesNameAndKindOfTheChat() {
        adapter.save(chat(householdA, "-100", "WG Flur", TelegramChat.ChatKind.GROUP, "alice"));

        TelegramChat stored = adapter.findByHouseholdAndChatId(householdA, "-100").orElseThrow();
        assertThat(stored.getChatName()).isEqualTo("WG Flur");
        assertThat(stored.getKind()).isEqualTo(TelegramChat.ChatKind.GROUP);
        assertThat(stored.getActivatedBy()).isEqualTo("alice");
    }

    /** AC7: derselbe Chat wird für einen Haushalt nicht doppelt angelegt. */
    @Test
    void save_doesNotCreateASecondChatForTheSameHouseholdAndChatId() {
        adapter.save(chat(householdA, "-100", "WG Flur", TelegramChat.ChatKind.GROUP, "alice"));
        adapter.save(chat(householdA, "-100", "WG Flur neu", TelegramChat.ChatKind.GROUP, "alice"));

        assertThat(adapter.findByHousehold(householdA)).hasSize(1);
    }

    /** AC12: die Chats eines Haushalts gehören nur diesem Haushalt. */
    @Test
    void findByHousehold_returnsOnlyTheChatsOfThatHousehold() {
        adapter.save(chat(householdA, "-100", "Gruppe A", TelegramChat.ChatKind.GROUP, "alice"));
        adapter.save(chat(householdB, "-200", "Gruppe B", TelegramChat.ChatKind.GROUP, "bob"));

        assertThat(adapter.findByHousehold(householdA))
                .extracting(TelegramChat::getChatName)
                .containsExactly("Gruppe A");
    }

    /** Die Liste der Anzeige ist aufsteigend nach Anzeigename. */
    @Test
    void findByHousehold_sortsByChatName() {
        adapter.save(chat(householdA, "-2", "Zebra", TelegramChat.ChatKind.GROUP, "alice"));
        adapter.save(chat(householdA, "-1", "Alpha", TelegramChat.ChatKind.GROUP, "alice"));

        assertThat(adapter.findByHousehold(householdA))
                .extracting(TelegramChat::getChatName)
                .containsExactly("Alpha", "Zebra");
    }

    /** Der Lauf geht von den Verbindungen aus — ohne Chat kein Haushalt (AC25). */
    @Test
    void findHouseholdsWithChats_returnsOnlyHouseholdsWithAChat() {
        adapter.save(chat(householdA, "-100", "Gruppe A", TelegramChat.ChatKind.GROUP, "alice"));

        assertThat(adapter.findHouseholdsWithChats()).containsExactly(householdA);
    }

    /** AC15: nach dem Trennen ist der Chat weg — er bekommt keine weiteren Nachrichten. */
    @Test
    void delete_removesTheChatOfThatHousehold() {
        adapter.save(chat(householdA, "-100", "Gruppe A", TelegramChat.ChatKind.GROUP, "alice"));

        adapter.delete(householdA, "-100");

        assertThat(adapter.findByHousehold(householdA)).isEmpty();
        assertThat(adapter.findByHouseholdAndChatId(householdA, "-100")).isEmpty();
    }

    private TelegramChat chat(long householdId, String chatId, String name,
            TelegramChat.ChatKind kind, String activatedBy) {
        return TelegramChat.builder()
                .householdId(householdId)
                .chatId(chatId)
                .chatName(name)
                .kind(kind)
                .activatedBy(activatedBy)
                .build();
    }
}
