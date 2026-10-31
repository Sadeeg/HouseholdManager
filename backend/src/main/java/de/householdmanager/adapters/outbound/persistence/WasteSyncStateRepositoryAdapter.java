package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.application.port.out.WasteSyncStateRepository;
import de.householdmanager.domain.model.WasteCalendar;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Ausgehender Adapter: setzt den ausgehenden Port für den Stand auf JPA um.
 *
 * <p>Das Persistenzmodell kennt nur die zwei Zeitstempel — die Quelle gehört
 * zum Haushalt. Das Fachmodell wird hier ohne Quelle aufgebaut; die Use Case
 * setzt sie aus dem Haushalt ein.
 */
@Repository
@RequiredArgsConstructor
public class WasteSyncStateRepositoryAdapter implements WasteSyncStateRepository {

    private final WasteSyncStateJpaRepository wasteSyncStateJpaRepository;

    @Override
    public Optional<WasteCalendar> findByHousehold(long householdId) {
        return wasteSyncStateJpaRepository.findById(householdId).map(this::toDomain);
    }

    @Override
    public WasteCalendar save(long householdId, Instant lastSyncedAt, Instant lastAttemptAt) {
        // Genau eine Zeile je Haushalt: anlegen oder fortschreiben.
        WasteSyncStateJpaEntity entity = wasteSyncStateJpaRepository.findById(householdId)
                .orElseGet(() -> WasteSyncStateJpaEntity.builder().householdId(householdId).build());
        entity.setLastSyncedAt(lastSyncedAt);
        entity.setLastAttemptAt(lastAttemptAt);
        return toDomain(wasteSyncStateJpaRepository.save(entity));
    }

    private WasteCalendar toDomain(WasteSyncStateJpaEntity entity) {
        return WasteCalendar.builder()
                .householdId(entity.getHouseholdId())
                .lastSyncedAt(entity.getLastSyncedAt())
                .lastAttemptAt(entity.getLastAttemptAt())
                .build();
    }
}
