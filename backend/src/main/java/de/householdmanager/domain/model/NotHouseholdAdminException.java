package de.householdmanager.domain.model;

/**
 * Fachfehler: Die Person ist nicht die Haushaltsleitung des Haushalts.
 *
 * <p>Nur die Haushaltsleitung ernennt und entfernt Mitglieder. Der
 * eingehende Adapter übersetzt den Fehler in 403.
 */
public class NotHouseholdAdminException extends RuntimeException {

    public NotHouseholdAdminException(String message) {
        super(message);
    }
}
