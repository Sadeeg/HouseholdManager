package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.application.port.out.TelegramChatRepository;
import de.householdmanager.domain.model.TelegramChat;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Ausgehender Adapter: setzt den ausgehenden Port auf JPA um.
 *
 * <p>Hier — und nur hier — wird zwischen Persistenzmodell und Fachmodell
 * übersetzt.
 *
 * <p>Der Code wird hier erzeugt, weil die Erzeugung eine technische
 * Zufallsquelle braucht: der Code besteht aus Ziffern und ist nicht
 * vorhersagbar. Er entsteht beim ersten Anfordern und bleibt danach
 * unverändert (AC2) — ein späteres Anfordern liefert denselben Code.
 */
@Repository
public class TelegramChatRepositoryAdapter implements TelegramChatRepository {

    /** Länge des Codes: sechs Ziffern — genug für einen Haushalt, kurz genug zum Abtippen. */
    private static final int CODE_LENGTH = 6;

    /** Die Zufallsquelle des Codes — kryptografisch, nicht vorhersagbar. */
    private final SecureRandom random = new SecureRandom();

    private final TelegramChatJpaRepository chatJpaRepository;
    private final TelegramActivationCodeJpaRepository codeJpaRepository;
    private final Clock clock;

    public TelegramChatRepositoryAdapter(
            TelegramChatJpaRepository chatJpaRepository,
            TelegramActivationCodeJpaRepository codeJpaRepository,
            Clock clock) {
        this.chatJpaRepository = chatJpaRepository;
        this.codeJpaRepository = codeJpaRepository;
        this.clock = clock;
    }

    @Override
    public List<TelegramChat> findByHousehold(long householdId) {
        return chatJpaRepository.findAllByHouseholdIdOrderByChatNameAsc(householdId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<TelegramChat> findByHouseholdAndChatId(long householdId, String chatId) {
        return chatJpaRepository.findByHouseholdIdAndChatId(householdId, chatId).map(this::toDomain);
    }

    @Override
    public Optional<Long> findHouseholdByChatId(String chatId) {
        return chatJpaRepository.findHouseholdIdByChatId(chatId);
    }

    @Override
    public List<Long> findHouseholdsWithChats() {
        return chatJpaRepository.findHouseholdIds();
    }

    @Override
    public Optional<CodeOwner> findCodeOwner(String code) {
        return codeJpaRepository.findByCode(code)
                .map(entity -> new CodeOwner(entity.getHouseholdId(), entity.getActivatedBy()));
    }

    /**
     * Der Code entsteht beim ersten Anfordern und bleibt danach unverändert
     * (AC2). Ein vorhandener Code wird <em>nicht</em> ersetzt — auch nicht,
     * wenn ein anderes Mitglied ihn anfordert.
     */
    @Override
    public String ensureCode(long householdId, String userId) {
        return codeJpaRepository.findByHouseholdId(householdId)
                .map(TelegramActivationCodeJpaEntity::getCode)
                .orElseGet(() -> createCode(householdId, userId));
    }

    private String createCode(long householdId, String userId) {
        String code = newCode();
        codeJpaRepository.save(TelegramActivationCodeJpaEntity.builder()
                .householdId(householdId)
                .code(code)
                .activatedBy(userId)
                .createdAt(Instant.now(clock))
                .build());
        return code;
    }

    /** Ein Code aus Ziffern — nicht vorhersagbar (SecureRandom). */
    private String newCode() {
        StringBuilder digits = new StringBuilder();
        for (int position = 0; position < CODE_LENGTH; position++) {
            digits.append(random.nextInt(10));
        }
        return digits.toString();
    }

    /**
     * Der Chat wird angelegt.
 *
     * <p>Ein bereits vorhandener Chat für denselben Haushalt wird <strong>nicht</strong>
     * doppelt angelegt (AC7): die Eindeutigkeit sichert der Use Case (er
     * kehrt bei einem vorhandenen Chat zurück) und der Unique-Index über
     * (Haushalt, Chat-ID) in der Migration {@code V6}.
 */
    @Override
    public TelegramChat save(TelegramChat chat) {
        TelegramChatJpaEntity entity = chatJpaRepository.save(TelegramChatJpaEntity.builder()
                .id(chat.getId())
                .householdId(chat.getHouseholdId())
                .chatId(chat.getChatId())
                .chatName(chat.getChatName())
                .kind(chat.getKind())
                .activatedBy(chat.getActivatedBy())
                .createdAt(Instant.now(clock))
                .build());
        return toDomain(entity);
    }

    @Override
    public void delete(long householdId, String chatId) {
        chatJpaRepository.findByHouseholdIdAndChatId(householdId, chatId)
                .ifPresent(chatJpaRepository::delete);
    }

    private TelegramChat toDomain(TelegramChatJpaEntity entity) {
        return TelegramChat.builder()
                .id(entity.getId())
                .householdId(entity.getHouseholdId())
                .chatId(entity.getChatId())
                .chatName(entity.getChatName())
                .kind(entity.getKind())
                .activatedBy(entity.getActivatedBy())
                .build();
    }
}
