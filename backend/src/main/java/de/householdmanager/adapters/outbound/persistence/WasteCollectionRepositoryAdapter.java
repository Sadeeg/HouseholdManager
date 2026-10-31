package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.application.port.out.WasteCollectionRepository;
import de.householdmanager.domain.model.WasteCollection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Ausgehender Adapter: setzt den ausgehenden Port auf JPA um.
 *
 * <p>Hier — und nur hier — wird zwischen Persistenzmodell und Fachmodell
 * übersetzt. Der Bestand eines Haushalts ist der Anzeigestand des letzten
 * Laufs: er wird vollständig ersetzt (R5), auch mit einer leeren Liste (R8).
 */
@Repository
@RequiredArgsConstructor
public class WasteCollectionRepositoryAdapter implements WasteCollectionRepository {

    private final WasteCollectionJpaRepository wasteCollectionJpaRepository;

    @Override
    public List<WasteCollection> findByHousehold(long householdId) {
        return wasteCollectionJpaRepository
                .findAllByHouseholdIdOrderByCollectionDateAscWasteTypeAsc(householdId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void replace(long householdId, List<WasteCollection> collections) {
        // Löschen zuerst: sonst würde die Aktionsqueue vor dem Löschen einfügen
        // und die Unique-Grenze (AC21) verletzen.
        wasteCollectionJpaRepository.deleteAllByHouseholdId(householdId);
        if (collections.isEmpty()) {
            return;
        }
        wasteCollectionJpaRepository.saveAll(collections.stream()
                .map(collection -> toEntity(householdId, collection))
                .toList());
    }

    private WasteCollection toDomain(WasteCollectionJpaEntity entity) {
        return WasteCollection.builder()
                .id(entity.getId())
                .householdId(entity.getHouseholdId())
                .collectionDate(entity.getCollectionDate())
                .wasteType(entity.getWasteType())
                .sourceUid(entity.getSourceUid())
                .build();
    }

    private WasteCollectionJpaEntity toEntity(long householdId, WasteCollection collection) {
        return WasteCollectionJpaEntity.builder()
                .id(collection.getId())
                .householdId(householdId)
                .collectionDate(collection.getCollectionDate())
                .wasteType(collection.getWasteType())
                .sourceUid(collection.getSourceUid())
                .build();
    }
}
