package de.householdmanager.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Fachmodell eines freigeschalteten Telegram-Chats.
 *
 * <p>Gehört zum Fachkern: keine Framework-Annotationen, keine Telegram-Typen.
 * Ein Chat verbindet einen Haushalt mit einem Chat bei Telegram — er ist
 * entweder eine Gruppe oder ein Einzelchat (AC11). Die Telegram-Kennungen
 * (Chat-ID, Bot-Token, API-Aufrufe) enden an der Adaptergrenze; hier stehen
 * nur die fachlichen Eigenschaften (ADR 0007).
 *
 * <p>Der Anzeigename und die Chat-Art sind ein **Snapshot**: Telegram liefert
 * sie bei der Freischaltung, und der Anzeigestand hält sie so lange, bis die
 * Verbindung getrennt wird (S12). Die Chat-ID selbst ist der Zugriffsweg auf
 * den Chat und wird dauerhaft gespeichert.
 *
 * <p>Wer den Chat freigeschaltet hat, steht in {@code activatedBy}: nur diese
 * Person darf die Verbindung trennen (AC13, AC14). Alle Mitglieder des
 * Haushalts sehen den Chat, aber nur die freischaltende Person trennt ihn.
 */
@Getter
@Builder(toBuilder = true)
public class TelegramChat {

    /** Grenze der Spalte {@code telegram_chat.chat_name}. */
    public static final int MAX_CHAT_NAME_LENGTH = 200;

    /** Grenze der Spalte {@code telegram_chat.chat_id}. */
    public static final int MAX_CHAT_ID_LENGTH = 100;

    /** Grenze der Spalte {@code telegram_chat.activated_by}. */
    public static final int MAX_ACTIVATED_BY_LENGTH = 200;

    private final Long id;
    private final long householdId;

    /** Kennung des Chats bei Telegram — der Zugriffsweg auf den Chat. */
    private final String chatId;

    /** Anzeigename des Chats, wie von Telegram geliefert. */
    private final String chatName;

    /** Die Chat-Art: {@code GROUP} oder {@code PRIVATE} (AC11). */
    private final ChatKind kind;

    /** Kennung der Person, die den Chat freigeschaltet hat. */
    private final String activatedBy;

    public TelegramChat withId(Long newId) {
        return toBuilder().id(newId).build();
    }

    /**
     * Die Art eines Chats bei Telegram.
     *
     * <p>Eine Eigenschaft, keine Bedeutung: der Anzeigename als Badge
     * „GRUPPE" oder „EINZELCHAT" (AC11). Das Modell kennt die Telegram-
     * Protokollwerte nicht — das Übersetzen liegt am Adapter.
     */
    public enum ChatKind {
        /** Eine Gruppe — der Bot ist Mitglied einer Telegram-Gruppe. */
        GROUP("GRUPPE"),
        /** Ein Einzelchat — der Bot hat eine private Unterhaltung. */
        PRIVATE("EINZELCHAT");

        private final String label;

        ChatKind(String label) {
            this.label = label;
        }

        /** Die Beschriftung für die Anzeige (deutsch, Versalien). */
        public String label() {
            return label;
        }
    }
}
