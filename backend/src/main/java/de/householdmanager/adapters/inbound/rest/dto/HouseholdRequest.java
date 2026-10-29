package de.householdmanager.adapters.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Eingehende DTOs des REST-Adapters. Gehören zum Transport, nicht zur
 * Anwendung — die Anwendung kennt keine HTTP-Begriffe.
 *
 * <p>Geprüft wird hier nur die <em>Anwesenheit</em> eines Namens. Die
 * Längengrenze ist eine Invariante der Anwendung und wird dort an
 * <em>getrimmtem</em> Wert gemessen — eine doppelte Prüfung an der Grenze
 * würde abweichende Werte zählen (Länge über der Grenze plus Leerzeichen).
 */
public record HouseholdRequest(@NotBlank String name) {
}
