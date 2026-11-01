package de.householdmanager.adapters.outbound.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring-Data-Repository des Telegram-Chat-Adapters — technisches Detail,
 * die Anwendung kennt diesen Typ nicht.
 */
interface TelegramChatJpaRepository extends JpaRepository<TelegramChatJpaEntity, Long> {

    /** Liste der Anzeige: je Haushalt, aufsteigend nach Anzeigename. */
    List<TelegramChatJpaEntity> findAllByHouseholdIdOrderByChatNameAsc(long householdId);

    /** Der Chat eines Haushalts mit dieser Chat-ID. */
    Optional<TelegramChatJpaEntity> findByHouseholdIdAndChatId(long householdId, String chatId);

    /** Die Haushalte, die mindestens einen freigeschalteten Chat haben. */
    @Query("select distinct c.householdId from TelegramChatJpaEntity c order by c.householdId")
    List<Long> findHouseholdIds();
}
