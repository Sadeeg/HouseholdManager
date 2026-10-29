package de.householdmanager.domain.model;

/**
 * Ein Nutzerkonto aus dem Verzeichnis (Nextcloud).
 *
 * <p>Fachmodell ohne Framework-Import — die Herkunft der Daten (Nextcloud,
 * OCS-API) kennt nur der ausgehende Adapter.
 */
public record UserAccount(String id, String displayName, String email) {
}
