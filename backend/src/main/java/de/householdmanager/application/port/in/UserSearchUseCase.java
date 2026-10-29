package de.householdmanager.application.port.in;

import java.util.List;

/**
 * Eingehender Port: Personensuche im Nutzerverzeichnis.
 *
 * <p>Das Frontend sucht damit Personen, bevor es einen Bearbeiter zuweist
 * oder ein Mitglied ernennt — Vorschläge kommen aus dem Verzeichnis, nicht
 * aus freier Eingabe.
 */
public interface UserSearchUseCase {

    List<UserRef> searchUsers(String query);

    /** Ein Treffer des Verzeichnisses: Kennung und Anzeigename, keine sensiblen Daten. */
    record UserRef(String id, String displayName) {
    }
}
