package de.householdmanager.domain.model;

/**
 * Fachfehler: Der Haushalt existiert nicht.
 *
 * <p>Der eingehende Adapter übersetzt den Fehler in 404.
 */
public class HouseholdNotFoundException extends RuntimeException {

    public HouseholdNotFoundException(String message) {
        super(message);
    }
}
