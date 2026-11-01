package de.householdmanager.application.usecase;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.TelegramChatUseCase;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.application.port.out.TelegramChatRepository;
import de.householdmanager.application.port.out.TelegramChatRepository.CodeOwner;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.NotChatOwnerException;
import de.householdmanager.domain.model.TelegramChat;
import de.householdmanager.domain.model.TelegramChatNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case „Telegram-Chats" — Freischaltung, Anzeige und Trennen.
 *
 * <p>Orchestriert: Mitgliedschaft prüfen, Code lesen oder erzeugen, Chat
 * anlegen oder entfernen. Die Fachgrenzen selbst wachen im Fachmodell und in
 * den Ports; die Telegram-Kenntnis bleibt im Adapter (ADR 0007).
 *
 * <p>Der Code ist dauerhaft gültig und unveränderlich (AC2): er entsteht beim
 * ersten Anfordern und wird danach nur noch gelesen. Er ist nicht
 * vorhersagbar — der Adapter erzeugt ihn aus einer kryptografischen
 * Zufallsquelle.
 *
 * <p>Getrennt werden darf nur die Verbindung, deren freischaltende Person
 * der aktuelle Benutzer ist (AC13, AC14): andere Mitglieder sehen die
 * Verbindung, erhalten aber weder Aktion noch Fehler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramChatUseCaseImpl implements TelegramChatUseCase {

    /**
     * Der Freischalt-Befehl: „/code" gefolgt vom Code (AC5). Die Schreibweise
     * ist gleichgültig, Leerzeichen sind optional.
     */
    private static final Pattern COMMAND_PATTERN = Pattern.compile("(?i)^/code\\s*(\\d+)$");

    /** Eine Nachricht, die ausschließlich aus Ziffern besteht (AC4). */
    private static final Pattern DIGITS_PATTERN = Pattern.compile("^\\d+$");

    /**
     * Die Bestätigung nach einer Freischaltung. Sie nennt <strong>keinen</strong>
     * Haushaltsnamen — die Antwort landet in einem fremden Chat (AC8).
     */
    private static final String ACTIVATION_CONFIRMATION =
            "Dieser Chat ist jetzt freigeschaltet. Du erhältst hier die Mülltermine der kommenden Woche "
                    + "und am Abend vor einem Termin eine Erinnerung.";

    /**
     * Der Hinweis bei einem ungültigen Code. Er verrät nicht, ob der Code
     * unbekannt ist oder zu einem anderen Haushalt gehört (AC9, AC10).
     */
    private static final String INVALID_CODE_HINT =
            "Dieser Code ist nicht gültig. Den Freischalt-Code siehst du in HouseholdManager unter "
                    + "„Haushalte“ im Abschnitt „Telegram-Chats“.";

    private final TelegramChatRepository chatRepository;
    private final TelegramBot telegramBot;
    private final HouseholdRepository householdRepository;
    private final MembershipUseCase membershipUseCase;

    // --- Anzeige: Code und Chats --------------------------------------------

    @Override
    public boolean isBotConfigured() {
        return telegramBot.isConfigured();
    }

    /**
     * Der Code des Haushalts — er entsteht beim ersten Anfordern und bleibt
     * danach unverändert (AC1, AC2).
     *
     * <p>Ohne konfigurierten Bot wird <strong>kein</strong> Code erzeugt: der
     * Code liefe ins Leere, weil kein Bot ihn annimmt. Die Oberfläche weist
     * stattdessen auf den fehlenden Bot-Token hin — eine wirkungslose Aktion
     * (ein Code ohne Bot) entfällt damit.
     */
    @Override
    @Transactional
    public Optional<String> code(String userId, long householdId) {
        membershipUseCase.requireMembership(householdId, userId);
        requireHousehold(householdId);
        if (!telegramBot.isConfigured()) {
            return Optional.empty();
        }
        // Der Code entsteht beim ersten Anfordern und bleibt danach unverändert.
        return Optional.of(chatRepository.ensureCode(householdId, userId));
    }

    /**
     * Die Chats des Haushalts als Anzeigewerte (AC11, AC12).
     *
     * <p>Die freischaltende Person wird mit ihrem Namen genannt, nicht mit
     * ihrer technischen Kennung. Der Name wird aus der Mitgliederliste des
     * Haushalts aufgelöst — er ist ein Snapshot der Anzeige, kein gespeicherter
     * Zustand. Lässt sich der Name nicht auflösen (die Person ist kein Mitglied
     * mehr), bleibt die Kennung stehen.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TelegramChatUseCase.ChatView> listChats(String userId, long householdId) {
        membershipUseCase.requireMembership(householdId, userId);
        requireHousehold(householdId);
        // Nur die Chats dieses Haushalts (AC12) — der Port filtert bereits.
        List<TelegramChat> chats = chatRepository.findByHousehold(householdId);

        Map<String, String> names = membershipUseCase.listMembers(userId, householdId).stream()
                .filter(member -> member.displayName() != null && !member.displayName().isBlank())
                .collect(Collectors.toMap(MembershipUseCase.MemberView::userId,
                        MembershipUseCase.MemberView::displayName, (first, second) -> first));

        return chats.stream()
                .map(chat -> new TelegramChatUseCase.ChatView(
                        chat.getChatId(),
                        chat.getChatName(),
                        chat.getKind(),
                        chat.getActivatedBy(),
                        names.getOrDefault(chat.getActivatedBy(), chat.getActivatedBy())))
                .toList();
    }

    // --- Freischaltung aus dem Chat -----------------------------------------

    /**
     * Eine Nachricht aus einem Telegram-Chat.
     *
     * <p>Erkannt wird eine Nachricht, die ausschließlich aus Ziffern besteht
     * (AC4), oder „/code" gefolgt vom Code (AC5). Alles andere wird
     * ignoriert — der Bot antwortet nicht (AC6).
     *
     * <p>Ein gültiger Code schaltet den Chat für seinen Haushalt frei (AC3).
     * Der Code bestimmt den Haushalt — der Bot kennt keine andere
     * Zuordnung.
     *
     * <p>Ein ungültiger Code bekommt einen Hinweis, wie ein gültiger Code
     * beschafft wird (AC9): entweder kennt niemanden den Code, oder der
     * Code gehört zu einem Haushalt, zu dem der Chat <em>nicht</em> gehört
     * (AC10). In beiden Fällen ist die Antwort gleich — sie verrät nicht,
     * ob der Code existiert. Die Antwort nennt <strong>nie</strong> den
     * Haushaltsnamen (AC8).
     */
    @Override
    @Transactional
    public BotAnswer handleMessage(String chatId, String text) {
        Optional<String> code = extractCode(text);
        if (code.isEmpty()) {
            // Kein Freischaltversuch — der Bot schweigt (AC6).
            return new BotAnswer(false, null);
        }

        Optional<CodeOwner> owner = chatRepository.findCodeOwner(code.get());
        if (owner.isEmpty()) {
            // Unbekannter Code: für den Bot ist beides dasselbe (AC9, AC10).
            return new BotAnswer(true, INVALID_CODE_HINT);
        }

        // Ein Chat gehört zu genau einem Haushalt. Ist er schon an einen
        // anderen gebunden, schaltet ein fremder Code ihn nicht um (AC10) —
        // die Antwort ist dieselbe wie bei einem unbekannten Code.
        Optional<Long> boundHousehold = chatRepository.findHouseholdByChatId(chatId);
        if (boundHousehold.isPresent() && boundHousehold.get() != owner.get().householdId()) {
            return new BotAnswer(true, INVALID_CODE_HINT);
        }

        activate(owner.get(), chatId);
        return new BotAnswer(true, ACTIVATION_CONFIRMATION);
    }

    /**
     * Der Chat wird für den Haushalt freigeschaltet: Name und Art kommen von
     * Telegram (AC11). Ein bereits freigeschalteter Chat wird nicht doppelt
     * angelegt (AC7) — die vorhandene Verbindung bleibt, auch wenn Telegram
     * später einen anderen Namen liefert.
     */
    private void activate(CodeOwner owner, String chatId) {
        Optional<TelegramChat> existing =
                chatRepository.findByHouseholdAndChatId(owner.householdId(), chatId);
        if (existing.isPresent()) {
            // Bereits freigeschaltet — keine zweite Verbindung (AC7).
            return;
        }

        // Name und Art kommen aus einem Aufruf — die Chat-Beschreibung bei
        // Telegram trägt beides (AC11).
        TelegramBot.ChatInfo info = telegramBot.chatInfo(chatId);
        chatRepository.save(TelegramChat.builder()
                .householdId(owner.householdId())
                .chatId(chatId)
                .chatName(info.name())
                .kind(toDomainKind(info.kind()))
                .activatedBy(owner.userId())
                .build());
    }

    /**
     * Der Code aus einer Nachricht: entweder die Nachricht besteht ganz aus
     * Ziffern (AC4), oder sie ist „/code" gefolgt vom Code (AC5). Sonst
     * liefert sie nichts — der Bot schweigt (AC6).
     */
    private Optional<String> extractCode(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        if (DIGITS_PATTERN.matcher(trimmed).matches()) {
            return Optional.of(trimmed);
        }
        var command = COMMAND_PATTERN.matcher(trimmed);
        if (command.matches()) {
            return Optional.of(command.group(1));
        }
        return Optional.empty();
    }

    // --- Trennen -------------------------------------------------------------

    /**
     * Trennt eine eigene Verbindung (AC13). Eine fremde Verbindung wird
     * abgelehnt (AC14) — die Antwort verrät nicht, ob es sie gibt.
     */
    @Override
    @Transactional
    public void disconnect(String userId, long householdId, String chatId) {
        membershipUseCase.requireMembership(householdId, userId);
        requireHousehold(householdId);

        TelegramChat chat = chatRepository.findByHouseholdAndChatId(householdId, chatId)
                .orElseThrow(() -> new TelegramChatNotFoundException(
                        "Dieser Telegram-Chat ist für den Haushalt nicht freigeschaltet."));
        if (!chat.getActivatedBy().equals(userId)) {
            // Nur die freischaltende Person darf trennen (AC13, AC14).
            throw new NotChatOwnerException(
                    "Diese Verbindung kann nur von der Person getrennt werden, die sie freigeschaltet hat.");
        }
        // Danach erreicht der Chat keine weiteren Benachrichtigungen (AC15).
        chatRepository.delete(householdId, chatId);
    }

    // --- Hilfen --------------------------------------------------------------

    private void requireHousehold(long householdId) {
        householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException("Der Haushalt existiert nicht."));
    }

    private TelegramChat.ChatKind toDomainKind(TelegramBot.ChatKind kind) {
        return switch (kind) {
            case GROUP -> TelegramChat.ChatKind.GROUP;
            case PRIVATE -> TelegramChat.ChatKind.PRIVATE;
        };
    }
}
