package de.householdmanager.domain.model;

/**
 * Fachfehler: die ICS-Adresse eines Haushalts ist unzulässig (R18, AC22).
 *
 * <p>Gehört zum Fachkern und ist bewusst framework-frei — der eingehende
 * Adapter übersetzt sie in einen HTTP-Status (400). Die Meldung nennt die
 * Grenze, damit der Nutzer sie sieht.
 */
public class InvalidWasteCalendarUrlException extends RuntimeException {

    public InvalidWasteCalendarUrlException(String message) {
        super(message);
    }
}
