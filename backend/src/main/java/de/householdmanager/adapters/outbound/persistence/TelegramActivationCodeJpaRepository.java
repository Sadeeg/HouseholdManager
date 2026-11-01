package de.householdmanager.adapters.outbound.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring-Data-Repository des Freischalt-Codes — genau eine Zeile je
 * Haushalt.
 */
interface TelegramActivationCodeJpaRepository extends JpaRepository<TelegramActivationCodeJpaEntity, Long> {

    /** Der Code eines Haushalts. */
    Optional<TelegramActivationCodeJpaEntity> findByHouseholdId(long householdId);

    /** Der Code selbst — eindeutig über alle Haushalte. */
    Optional<TelegramActivationCodeJpaEntity> findByCode(String code);
}
