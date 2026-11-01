package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.domain.model.TelegramChat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistenzmodell eines freigeschalteten Telegram-Chats.
 *
 * <p>Bleibt im ausgehenden Adapter: das Fachmodell
 * ({@code domain.model.TelegramChat}) kennt weder JPA noch Tabellen.
 *
 * <p>Die Eindeutigkeit über (Haushalt, Chat-ID) ist die Grenze von AC7:
 * eine erneute Freischaltung legt keinen zweiten Chat an. Sie ist hier
 * beschrieben, damit das Modell zur Migration {@code V6} passt
 * ({@code ddl-auto: validate}).
 */
@Entity
@Table(
        name = "telegram_chat",
        uniqueConstraints = @UniqueConstraint(
                name = "telegram_chat_household_chat_key",
                columnNames = {"household_id", "chat_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramChatJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "household_id", nullable = false)
    private Long householdId;

    @Column(name = "chat_id", nullable = false, length = TelegramChat.MAX_CHAT_ID_LENGTH)
    private String chatId;

    @Column(name = "chat_name", nullable = false, length = TelegramChat.MAX_CHAT_NAME_LENGTH)
    private String chatName;

    /** Die Chat-Art als Enum — der Adapter übersetzt sie ins Fachmodell. */
    @Column(name = "chat_kind", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TelegramChat.ChatKind kind;

    @Column(name = "activated_by", nullable = false, length = TelegramChat.MAX_ACTIVATED_BY_LENGTH)
    private String activatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
