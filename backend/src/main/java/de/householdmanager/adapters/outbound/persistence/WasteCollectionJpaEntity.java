package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.domain.model.WasteCollection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistenzmodell eines Mülltermins.
 *
 * <p>Bleibt im ausgehenden Adapter: das Fachmodell
 * ({@code domain.model.WasteCollection}) kennt weder JPA noch Tabellen.
 *
 * <p>Die Eindeutigkeit über (Haushalt, Datum, Tonnenart) ist die Grenze von
 * AC21: ein erneuter Lauf legt keinen zweiten Termin an. Sie ist hier
 * beschrieben, damit das Modell zur Migration {@code V5} passt
 * ({@code ddl-auto: validate}).
 */
@Entity
@Table(
        name = "waste_collection",
        uniqueConstraints = @UniqueConstraint(
                name = "waste_collection_household_date_type_key",
                columnNames = {"household_id", "collection_date", "waste_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasteCollectionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "household_id", nullable = false)
    private Long householdId;

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Column(name = "waste_type", nullable = false, length = WasteCollection.MAX_WASTE_TYPE_LENGTH)
    private String wasteType;

    @Column(name = "source_uid", length = WasteCollection.MAX_SOURCE_UID_LENGTH)
    private String sourceUid;
}
