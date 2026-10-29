package de.householdmanager.domain.model;

/**
 * Fachfehler: Der Name eines Haushalts ist unzulässig.
 *
 * <p>Gehört zum Fachkern und ist bewusst **framework-frei** — der
 * eingehende Adapter übersetzt ihn in einen HTTP-Status (400). Andere
 * {@link IllegalArgumentException}s bleiben Programmierfehler und werden
 * nicht als 400 getarnt.
 */
public class InvalidHouseholdNameException extends RuntimeException {

    public InvalidHouseholdNameException(String message) {
        super(message);
    }
}
