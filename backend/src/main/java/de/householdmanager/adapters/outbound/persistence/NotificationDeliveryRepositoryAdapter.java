package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.application.port.out.NotificationDeliveryRepository;
import de.householdmanager.domain.model.NotificationDelivery;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ausgehender Adapter: setzt den ausgehenden Port auf JPA um.
 *
 * <p>Hier — und nur hier — wird zwischen Persistenzmodell und Fachmodell
 * übersetzt. Der Versuchszähler und der letzte Zustand sind persistent — sie
 * sind der Zustand der Wiederholung (AC26, AC27).
 */
@Repository
@RequiredArgsConstructor
public class NotificationDeliveryRepositoryAdapter implements NotificationDeliveryRepository {

    private final NotificationDeliveryJpaRepository deliveryJpaRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDelivery> findRetryable() {
        return deliveryJpaRepository
                .findAllByCompletedFalseAndStatusAndAttemptCountLessThan(
                        NotificationDelivery.Status.FAILED,
                        NotificationDelivery.MAX_ATTEMPTS)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDelivery> findByHousehold(long householdId) {
        return deliveryJpaRepository.findAllByHouseholdId(householdId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public NotificationDelivery save(NotificationDelivery delivery) {
        NotificationDeliveryJpaEntity entity = deliveryJpaRepository.save(NotificationDeliveryJpaEntity.builder()
                .id(delivery.getId())
                .householdId(delivery.getHouseholdId())
                .chatId(delivery.getChatId())
                .content(delivery.getContent())
                .attemptCount(delivery.getAttemptCount())
                .status(delivery.getStatus())
                .lastError(delivery.getLastError())
                .lastAttemptAt(delivery.getLastAttemptAt())
                .completed(delivery.isCompleted())
                .createdAt(Instant.now(clock))
                .build());
        return toDomain(entity);
    }

    private NotificationDelivery toDomain(NotificationDeliveryJpaEntity entity) {
        return NotificationDelivery.builder()
                .id(entity.getId())
                .householdId(entity.getHouseholdId())
                .chatId(entity.getChatId())
                .content(entity.getContent())
                .attemptCount(entity.getAttemptCount() == null ? 0 : entity.getAttemptCount())
                .status(entity.getStatus())
                .lastError(entity.getLastError())
                .lastAttemptAt(entity.getLastAttemptAt())
                .completed(Boolean.TRUE.equals(entity.getCompleted()))
                .build();
    }
}
