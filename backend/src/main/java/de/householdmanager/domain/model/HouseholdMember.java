package de.householdmanager.domain.model;

import java.time.Instant;

/**
 * Ernannte Zugehörigkeit einer Person zu einem Haushalt.
 *
 * <p>Nur Mitglieder sehen und bearbeiten die Aufgaben-Tafel des Haushalts.
 * Die Haushaltsleitung ist immer Mitglied (Sie ist bei Anlage des Haushalts
 * eingetragen). Die Person selbst wird nicht kopiert — nur ihre Kennung;
 * Anzeigenamen löst die Anwendung live über das Nutzerverzeichnis auf.
 */
public record HouseholdMember(Long id, Long householdId, String userId, Instant createdAt) {
}
