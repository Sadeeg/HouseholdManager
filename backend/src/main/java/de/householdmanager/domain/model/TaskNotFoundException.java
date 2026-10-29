package de.householdmanager.domain.model;

/**
 * Fachfehler: Die Aufgabe existiert nicht (in diesem Haushalt).
 *
 * <p>Der eingehende Adapter übersetzt den Fehler in 404.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(String message) {
        super(message);
    }
}
