package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.adapters.inbound.rest.dto.WasteOverviewResponse;
import de.householdmanager.adapters.inbound.rest.dto.WasteSourceRequest;
import de.householdmanager.adapters.inbound.rest.dto.WasteSourceResponse;
import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.HouseholdUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.UserAccount;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Eingehender Adapter: HTTP → eingehender Port {@link WasteCollectionUseCase}.
 *
 * <p>Der Adapter übersetzt nur. Fachlogik steht im Use Case, Mitgliedschaft
 * und Haushalt werden dort geprüft (403, 404). Das Pfadmuster folgt
 * {@code MemberController} und {@code TaskController}.
 *
 * <p>Das Speichern der Quelle ist ein zweistufiger Vorgang: erst der Use Case
 * zum Setzen (Transaktion 1), danach der Use Case zur Synchronisation
 * (Transaktion 2, eigene Zeitgrenze). Die Antwort wartet auf den Lauf, damit
 * der Nutzer die Termine sofort sieht (AC11). Ein Fehler beim Abruf ist kein
 * HTTP-Fehler: die Übersicht meldet den Stand als veraltet (AC6).
 */
@RestController
@RequestMapping("api/households/{householdId}/waste")
@RequiredArgsConstructor
public class WasteCollectionController {

    private final WasteCollectionUseCase wasteCollectionUseCase;
    private final HouseholdUseCase householdUseCase;
    private final CurrentUserPort currentUserPort;

    @GetMapping
    public WasteOverviewResponse overview(@PathVariable long householdId) {
        return WasteOverviewResponse.from(wasteCollectionUseCase.overview(currentUserId(), householdId));
    }

    @PutMapping("source")
    public WasteSourceResponse saveSource(
            @PathVariable long householdId,
            @Valid @RequestBody WasteSourceRequest request) {
        Household household = householdUseCase.setWasteSource(currentUserId(), householdId, request.url());
        wasteCollectionUseCase.syncHousehold(household.getId());
        return WasteSourceResponse.from(household);
    }

    /** Entfernt die Quelle; die gespeicherten Termine bleiben (Annahme 5). */
    @DeleteMapping("source")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSource(@PathVariable long householdId) {
        householdUseCase.removeWasteSource(currentUserId(), householdId);
    }

    private String currentUserId() {
        return currentUserPort.currentUser().map(UserAccount::id)
                .orElseThrow(AuthController.NotAuthenticatedException::new);
    }
}
