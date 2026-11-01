package de.householdmanager.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistenzmodell des Freischalt-Codes — genau eine Zeile je Haushalt.
 *
 * <p>Bleibt im ausgehenden Adapter: das Fachmodell kennt weder JPA noch
 * Tabellen. Der Code ist unveränderlich (AC2) — er entsteht beim ersten
 * Anfordern und wird danach nur noch gelesen.
 */
@Entity
@Table(name = "telegram_activation_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramActivationCodeJpaEntity {

    @Id
    @Column(name = "household_id")
    private Long householdId;

    /** Der Code: Ziffern, nicht vorhersagbar, eindeutig über alle Haushalte. */
    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    /** Die Person, die den Code angefordert hat — die freischaltende Person. */
    @Column(name = "activated_by", nullable = false, length = 200)
    private String activatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
