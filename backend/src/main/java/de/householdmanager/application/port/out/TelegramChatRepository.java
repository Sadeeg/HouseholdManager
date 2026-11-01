package de.householdmanager.application.port.out;

import de.householdmanager.domain.model.TelegramChat;
import java.util.List;
import java.util.Optional;

/**
 * Ausgehender Port: die freigeschalteten Telegram-Chats verwalten.
 *
 * <p>Implementierung: {@code adapters.outbound.persistence.TelegramChatRepositoryAdapter}.
 *
 * <p>Der Code eines Haushalts ist unveränderlich und dauerhaft gültig
 * (AC2): er wird beim ersten Anfordern erzeugt und danach nur noch gelesen.
 * Ein Chat gehört zu genau einem Haushalt; derselbe Chat wird für einen
 * Haushalt nur einmal angelegt (AC7).
 */
public interface TelegramChatRepository {

    /** Die Chats eines Haushalts, aufsteigend nach Anzeigename. */
    List<TelegramChat> findByHousehold(long householdId);

    /**
     * Die Kennungen der Haushalte, die mindestens einen freigeschalteten Chat
     * haben — die Grundlage des Benachrichtigungslaufs.
     *
     * <p>Der Lauf geht von den Verbindungen aus, nicht von den Haushalten:
     * ohne freigeschalteten Chat wird nichts berechnet und nichts versendet
     * (AC25).
     */
    List<Long> findHouseholdsWithChats();

    /** Der Chat eines Haushalts mit dieser Chat-ID; leer, wenn keiner existiert. */
    Optional<TelegramChat> findByHouseholdAndChatId(long householdId, String chatId);

    /** Der Freischalt-Code eines Haushalts; leer, wenn noch keiner erzeugt wurde. */
    Optional<String> findCode(long householdId);

    /**
     * Der Haushalt, zu dem ein Code gehört; leer, wenn der Code zu keinem
     * Haushalt passt (AC9, AC10).
     *
     * <p>Der Code ist nicht vorhersagbar und eindeutig — zu einem Code gehört
     * höchstens ein Haushalt.
     */
    Optional<Long> findHouseholdByCode(String code);

    /**
     * Der Haushalt, zu dem ein Code gehört, <strong>und</strong> die Person,
     * die den Code angefordert hat — die freischaltende Person der
     * Verbindung (AC13).
     *
     * <p>Der Code wird im Namen eines Mitglieds angefordert und im Chat an
     * den Bot geschickt; der Bot kennt keine Person. Deshalb trägt der Code
     * die anfordernde Person — sie ist diejenige, die die Verbindung später
     * trennen darf. Die Person ist ein Mitglied des Haushalts.
     */
    Optional<CodeOwner> findCodeOwner(String code);

    /**
     * Legt den Code an, wenn es ihn noch nicht gibt — sonst bleibt der
     * alte. Der Code gehört der anfordernden Person.
     */
    String ensureCode(long householdId, String userId);

    TelegramChat save(TelegramChat chat);

    /** Entfernt die Verbindung; die Nachrichten an den Chat enden damit (AC15). */
    void delete(long householdId, String chatId);

    /**
     * Der Code eines Haushalts und die Person, die ihn angefordert hat.
     *
     * @param householdId der Haushalt des Codes.
     * @param userId die Person, die den Code angefordert hat — die
     *     freischaltende Person der Verbindung.
     */
    record CodeOwner(long householdId, String userId) {
    }
}
