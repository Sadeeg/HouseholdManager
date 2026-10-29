package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.application.port.out.HouseholdMemberRepository;
import de.householdmanager.domain.model.HouseholdMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Ausgehender Adapter: setzt den ausgehenden Port
 * {@link HouseholdMemberRepository} auf JPA um.
 */
@Repository
@RequiredArgsConstructor
public class HouseholdMemberRepositoryAdapter implements HouseholdMemberRepository {

    private final HouseholdMemberJpaRepository householdMemberJpaRepository;

    @Override
    public List<HouseholdMember> findByHousehold(long householdId) {
        return householdMemberJpaRepository.findAllByHouseholdIdOrderByIdAsc(householdId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<HouseholdMember> findByUser(String userId) {
        return householdMemberJpaRepository.findAllByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean exists(long householdId, String userId) {
        return userId != null && householdMemberJpaRepository.existsByHouseholdIdAndUserId(householdId, userId);
    }

    @Override
    public HouseholdMember save(HouseholdMember member) {
        HouseholdMemberJpaEntity entity = HouseholdMemberJpaEntity.builder()
                .id(member.id())
                .householdId(member.householdId())
                .userId(member.userId())
                .createdAt(member.createdAt())
                .build();
        return toDomain(householdMemberJpaRepository.save(entity));
    }

    @Override
    public void delete(long householdId, String userId) {
        if (userId != null) {
            householdMemberJpaRepository.deleteAllByHouseholdIdAndUserId(householdId, userId);
        }
    }

    private HouseholdMember toDomain(HouseholdMemberJpaEntity entity) {
        return new HouseholdMember(
                entity.getId(), entity.getHouseholdId(), entity.getUserId(), entity.getCreatedAt());
    }
}
