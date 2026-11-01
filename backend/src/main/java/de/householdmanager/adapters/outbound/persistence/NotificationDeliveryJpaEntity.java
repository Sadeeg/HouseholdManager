package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.domain.model.NotificationDelivery;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistenzmodell eines Zustellvermerks.
 *
 * <p>Bleibt im ausgehenden Adapter: das Fachmodell
 * ({@code domain.model.NotificationDelivery}) kennt weder JPA noch Tabellen.
 *
 * <p>Der Versuchszähler und der letzte Zustand sind persistent — sie sind
 * der Zustand der Wiederholung (AC26, AC27). Das Modell passt zur Migration
 * {@code V6} ({@code ddl-auto: validate}).
 */
@Entity
@Table(name = "notification_delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "household_id", nullable = false)
    private Long householdId;

    @Column(name = "chat_id", nullable = false, length = NotificationDelivery.MAX_CHAT_ID_LENGTH)
    private String chatId;

    @Column(name = "content", nullable = false, length = NotificationDelivery.MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationDelivery.Status status;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "completed", nullable = false)
    private Boolean completed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
