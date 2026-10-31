package de.householdmanager.adapters.inbound.scheduler;

import de.householdmanager.application.port.in.HouseholdUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.domain.model.Household;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Eingehender Adapter: der geplante Tageslauf (R4, AC2).
 *
 * <p>Der Adapter löst nur aus. Er geht die Haushalte mit hinterlegter Quelle
 * durch und ruft die Synchronisation je Haushalt — damit ein fehlerhafter
 * Haushalt die anderen nicht stoppt und jeder Lauf seine eigene Transaktion
 * bekommt. Der Zeitpunkt kommt aus der Konfiguration
 * ({@code householdmanager.waste.sync-cron}, {@code sync-zone}); die
 * Freigabe der Planung steht im Composition Root.
 *
 * <p>Ein Haushalt ohne Quelle wird übersprungen: ohne Quelle gibt es nichts
 * zu lesen (R3).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WasteSyncJob {

    private final HouseholdUseCase householdUseCase;
    private final WasteCollectionUseCase wasteCollectionUseCase;

    /** Ein Lauf über alle Haushalte mit Quelle — der Zeitpunkt ist konfiguriert. */
    public void syncDaily() {
        List<Household> households = householdUseCase.listHouseholdsWithSource();
        if (households.isEmpty()) {
            log.debug("Tageslauf: kein Haushalt mit hinterlegter Quelle.");
            return;
        }

        for (Household household : households) {
            try {
                wasteCollectionUseCase.syncHousehold(household.getId());
            } catch (RuntimeException failure) {
                // Ein fehlerhafter Haushalt stoppt die anderen nicht (AC2).
                log.warn("Mülltermine von Haushalt {} nicht synchronisiert: {}",
                        household.getId(), failure.getMessage());
            }
        }
    }
}
