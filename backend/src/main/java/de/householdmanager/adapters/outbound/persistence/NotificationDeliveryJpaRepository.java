package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.domain.model.NotificationDelivery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring-Data-Repository des Zustellvermerk-Adapters — technisches Detail,
 * die Anwendung kennt diesen Typ nicht.
 */
interface NotificationDeliveryJpaRepository extends JpaRepository<NotificationDeliveryJpaEntity, Long> {

    /**
     * Die Vermerke, die einen weiteren Versuch brauchen: offen, fehlgeschlagen
     * und unter dem Limit von drei Versuchen (AC26, AC27).
     */
    List<NotificationDeliveryJpaEntity> findAllByCompletedFalseAndStatusAndAttemptCountLessThan(
            NotificationDelivery.Status status, int attemptCount);
}
