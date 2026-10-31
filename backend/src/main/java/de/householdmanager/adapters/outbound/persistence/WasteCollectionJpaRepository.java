package de.householdmanager.adapters.outbound.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring-Data-Repository des Persistenz-Adapters — technisches Detail,
 * die Anwendung kennt diesen Typ nicht.
 */
interface WasteCollectionJpaRepository extends JpaRepository<WasteCollectionJpaEntity, Long> {

    /** Liste der Anzeige: aufsteigend nach Datum, bei gleichem Datum nach Tonnenart (Annahme 8). */
    List<WasteCollectionJpaEntity> findAllByHouseholdIdOrderByCollectionDateAscWasteTypeAsc(long householdId);

    /**
     * Leert den Bestand des Haushalts. Als Modifying-Query ausgeführt, nicht
     * als abgeleitete Methode: der Löschen-vor-Insert- Ablauf ist die
     * Bedingung für den Unique-Index (AC21) — die Aktionsqueue von Hibernate
     * würde sonst zuerst einfügen.
     */
    @Modifying
    @Query("delete from WasteCollectionJpaEntity e where e.householdId = :householdId")
    int deleteAllByHouseholdId(long householdId);
}
