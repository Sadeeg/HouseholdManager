package de.householdmanager.adapters.inbound.scheduler;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.householdmanager.application.port.in.HouseholdUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.WasteFeedUnavailableException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test des Tageslaufs (AC2): der Adapter geht die Haushalte mit Quelle durch.
 * Ein fehlerhafter Haushalt stoppt die anderen nicht.
 */
@ExtendWith(MockitoExtension.class)
class WasteSyncJobTest {

    @Mock
    private HouseholdUseCase householdUseCase;

    @Mock
    private WasteCollectionUseCase wasteCollectionUseCase;

    private WasteSyncJob job;

    @BeforeEach
    void setUp() {
        job = new WasteSyncJob(householdUseCase, wasteCollectionUseCase);
    }

    @Test
    void syncDaily_runsEveryHouseholdWithSource() {
        when(householdUseCase.listHouseholdsWithSource()).thenReturn(List.of(
                household(1L), household(2L), household(3L)));

        job.syncDaily();

        verify(wasteCollectionUseCase).syncHousehold(1L);
        verify(wasteCollectionUseCase).syncHousehold(2L);
        verify(wasteCollectionUseCase).syncHousehold(3L);
    }

    @Test
    void syncDaily_continuesAfterAFailedHousehold() {
        when(householdUseCase.listHouseholdsWithSource()).thenReturn(List.of(household(1L), household(2L)));
        doThrow(new WasteFeedUnavailableException("Quelle nicht erreichbar")).when(wasteCollectionUseCase).syncHousehold(1L);

        job.syncDaily();

        verify(wasteCollectionUseCase).syncHousehold(2L);
    }

    /** R3: ohne Quelle läuft nichts — es gibt nichts zu lesen. */
    @Test
    void syncDaily_withoutSourceRunsDoesNothing() {
        when(householdUseCase.listHouseholdsWithSource()).thenReturn(List.of());

        job.syncDaily();

        verify(wasteCollectionUseCase, never()).syncHousehold(anyLong());
    }

    private static Household household(long id) {
        return Household.builder().id(id).name("Haus " + id)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).createdBy("alice")
                .wasteCalendarUrl("https://entsorger.example/ics").build();
    }
}
