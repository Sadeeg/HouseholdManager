package de.householdmanager.application.usecase;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.WasteCalendarFeed;
import de.householdmanager.application.port.out.WasteCollectionRepository;
import de.householdmanager.application.port.out.WasteSyncStateRepository;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.WasteCalendar;
import de.householdmanager.domain.model.WasteCollection;
import de.householdmanager.domain.model.WasteFeedUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case „Mülltermine" — Transaktionsgrenze der Synchronisation.
 *
 * <p>Orchestriert: Mitgliedschaft prüfen, Fenster bestimmen (R5), Quelle
 * lesen, Bestand ersetzen, Stand schreiben. Die Fachgrenzen selbst wachen im
 * Fachmodell ({@link WasteCollection}, {@link WasteCalendar}).
 *
 * <p>Ein Lauf ersetzt den Bestand vollständig — das ist die Semantik des
 * Anzeigestands (R5). Zwei Fälle sind bewusst getrennt (Runde 2, F1):
 * eine Quelle ohne Termine im Fenster leert den Bestand (R8); eine Quelle,
 * die nicht lesbar ist, lässt den Bestand stehen und der Stand wird
 * veraltet (R7, R17, AC6).
 *
 * <p>Ohne Mitgliedschaft und ohne Haushalt wird über die bestehenden
 * Fachfehler entschieden (403, 404); die Fachregeln greifen unabhängig von
 * der Anmeldung.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WasteCollectionUseCaseImpl implements WasteCollectionUseCase {

    private final WasteCalendarFeed feed;
    private final WasteCollectionRepository collectionRepository;
    private final WasteSyncStateRepository syncStateRepository;
    private final HouseholdRepository householdRepository;
    private final MembershipUseCase membershipUseCase;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public WasteOverview overview(String userId, long householdId) {
        membershipUseCase.requireMembership(householdId, userId);
        Household household = requireHousehold(householdId);

        // Der Stand kommt aus der Anwendung, nicht aus dem Alter des Zeitstempels (R13).
        WasteCalendar state = syncStateRepository.findByHousehold(householdId)
                .orElseGet(() -> WasteCalendar.withoutRun(householdId));
        WasteCalendar calendar = state.toBuilder().sourceUrl(household.getWasteCalendarUrl()).build();

        // R9 und Annahme 8: alles, aufsteigend nach Datum, bei gleichem Datum
        // nach Tonnenart — ohne Filter, ohne Auswahl.
        List<WasteCollection> collections = collectionRepository.findByHousehold(householdId).stream()
                .sorted(Comparator.comparing(WasteCollection::getCollectionDate)
                        .thenComparing(WasteCollection::getWasteType))
                .toList();

        return new WasteOverview(calendar, collections);
    }

    /**
     * Ein Lauf für einen Haushalt: Fenster ab heute (R5), Bestand ersetzen,
     * Stand schreiben. Der Stand wird auch bei einem fehlgeschlagenen Lauf
     * geschrieben — sonst wäre der Fehler nicht sichtbar (AC6).
     */
    @Override
    @Transactional
    public void syncHousehold(long householdId) {
        Household household = requireHousehold(householdId);
        if (household.getWasteCalendarUrl() == null || household.getWasteCalendarUrl().isBlank()) {
            return; // ohne Quelle kein Lauf (R3)
        }

        Instant now = Instant.now(clock);
        LocalDate from = LocalDate.now(clock);
        LocalDate to = from.plusDays(WasteCalendar.SYNC_WINDOW_DAYS);
        WasteCalendar state = syncStateRepository.findByHousehold(householdId)
                .orElseGet(() -> WasteCalendar.withoutRun(householdId));

        try {
            List<WasteCollection> terms = termsInWindow(householdId, household.getWasteCalendarUrl(), from, to);
            collectionRepository.replace(householdId, terms);
            syncStateRepository.save(householdId, now, now);
        } catch (WasteFeedUnavailableException failure) {
            log.warn("Mülltermine von Haushalt {} konnten nicht gelesen werden: {}", householdId, failure.getMessage());
            syncStateRepository.save(householdId, state.getLastSyncedAt(), now);
        }
    }

    /**
     * Termine im Fenster, unverändert aus der Quelle (AC8). Doppelte
     * (Haus, Datum, Tonnenart) werden vor dem Speichern zusammengeführt —
     * dieselbe Grenze wie der Unique-Index (AC21). Ereignisse ohne
     * Startdatum oder ohne Tonnenart werden ignoriert: das Modell erfindet
     * keine Bezeichnung (Runde 3, F6).
     */
    private List<WasteCollection> termsInWindow(long householdId, String url, LocalDate from, LocalDate to) {
        List<WasteCalendarFeed.FeedTerm> fetched = feed.fetch(url, from, to);
        Set<String> seen = new HashSet<>();
        List<WasteCollection> terms = new ArrayList<>();
        for (WasteCalendarFeed.FeedTerm term : fetched) {
            if (term.date() == null || term.wasteType() == null || term.wasteType().isBlank()) {
                continue;
            }
            if (!seen.add(term.date() + "|" + term.wasteType())) {
                continue;
            }
            terms.add(WasteCollection.builder()
                    .householdId(householdId)
                    .collectionDate(term.date())
                    .wasteType(term.wasteType())
                    .sourceUid(term.sourceUid())
                    .build());
        }
        return terms;
    }

    private Household requireHousehold(long householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException("Der Haushalt existiert nicht."));
    }
}
