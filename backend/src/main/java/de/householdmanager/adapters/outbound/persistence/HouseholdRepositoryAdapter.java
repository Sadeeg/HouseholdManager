package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.domain.model.Household;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Ausgehender Adapter: setzt den ausgehenden Port auf JPA um.
 *
 * <p>Hier — und nur hier — wird zwischen Persistenzmodell und Fachmodell
 * übersetzt.
 */
@Repository
@RequiredArgsConstructor
public class HouseholdRepositoryAdapter implements HouseholdRepository {

    private final HouseholdJpaRepository householdJpaRepository;

    @Override
    public List<Household> findAll() {
        return householdJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Household> findById(long id) {
        return householdJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Household> findByCreatedBy(String userId) {
        return householdJpaRepository.findByCreatedBy(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Household> findWithSource() {
        return householdJpaRepository.findByWasteCalendarUrlIsNotNull().stream().map(this::toDomain).toList();
    }

    @Override
    public Household save(Household household) {
        HouseholdJpaEntity entity = household.getId() == null
                ? toEntity(household)
                : householdJpaRepository.findById(household.getId())
                        .map(existing -> {
                            existing.setName(household.getName());
                            existing.setWasteCalendarUrl(household.getWasteCalendarUrl());
                            // createdAt bleibt unangetastet — der Zeitstempel
                            // entsteht beim Anlegen und wird nie überschrieben.
                            return existing;
                        })
                        .orElseGet(() -> toEntity(household));
        return toDomain(householdJpaRepository.save(entity));
    }

    private Household toDomain(HouseholdJpaEntity entity) {
        return Household.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .wasteCalendarUrl(entity.getWasteCalendarUrl())
                .build();
    }

    private HouseholdJpaEntity toEntity(Household household) {
        return HouseholdJpaEntity.builder()
                .id(household.getId())
                .name(household.getName())
                .createdAt(household.getCreatedAt())
                .createdBy(household.getCreatedBy())
                .wasteCalendarUrl(household.getWasteCalendarUrl())
                .build();
    }
}
