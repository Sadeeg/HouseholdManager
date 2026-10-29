package de.householdmanager.domain.model;

/**
 * Fachfehler: Die Person ist kein ernanntes Mitglied des Haushalts.
 *
 * <p>Nur Mitglieder sehen und bearbeiten die Tafel. Der eingehende Adapter
 * übersetzt den Fehler in 403.
 */
public class MembershipRequiredException extends RuntimeException {

    public MembershipRequiredException(String message) {
        super(message);
    }
}
