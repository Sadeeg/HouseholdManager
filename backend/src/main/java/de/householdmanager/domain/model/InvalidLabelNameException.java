package de.householdmanager.domain.model;

/**
 * Fachfehler: Der Name eines Labels ist unzulässig (zu lang).
 *
 * <p>Der eingehende Adapter übersetzt ihn in 400. Ein Label, das die Grenze
 * überschreitet, entsteht nicht — es wird auch nicht stillschweigend
 * verworfen: der Nutzer muss den Namen korrigieren können (Issue #8).
 */
public class InvalidLabelNameException extends RuntimeException {

    public InvalidLabelNameException(String message) {
        super(message);
    }
}
