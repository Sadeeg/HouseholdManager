package de.householdmanager.domain.model;

/**
 * Fachfehler: das Nutzerverzeichnis ist nicht erreichbar.
 *
 * <p>Ein technischer Ausfall des Verzeichnisses (Nextcloud) — nicht
 * erreichbar oder nicht lesbar. Er ist kein Eingabefehler des Nutzers und
 * wird nicht als „niemand gefunden" behandelt: die Suche liefert keine
 * Treffer, weil es keine gibt, sondern weil niemand gefragt werden konnte
 * (Issue #21).
 */
public class UserDirectoryUnavailableException extends RuntimeException {

    public UserDirectoryUnavailableException(String message) {
        super(message);
    }

    public UserDirectoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
