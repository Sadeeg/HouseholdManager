package de.householdmanager.domain.model;

/**
 * Fachfehler: Der Titel einer Aufgabe ist unzulässig (leer oder zu lang).
 *
 * <p>Der eingehende Adapter übersetzt ihn in 400. Eine Aufgabe ohne Titel
 * entsteht nicht.
 */
public class InvalidTaskTitleException extends RuntimeException {

    public InvalidTaskTitleException(String message) {
        super(message);
    }
}
