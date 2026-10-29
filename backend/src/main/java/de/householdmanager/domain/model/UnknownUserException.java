package de.householdmanager.domain.model;

/**
 * Fachfehler: Die Kennung ist im Nutzerverzeichnis unbekannt.
 *
 * <p>Zuständige und zu ernennende Mitglieder sind immer Personen des
 * Nutzerverzeichnisses. Der eingehende Adapter übersetzt den Fehler in 400.
 *
 * <p>Er erbt von {@link IllegalArgumentException}, damit die im Issue #1
 * festgelegte API-Regel („IllegalArgumentException → 400") ihn ohne eigenen
 * Handler erfasst.
 */
public class UnknownUserException extends IllegalArgumentException {

    public UnknownUserException(String message) {
        super(message);
    }
}
