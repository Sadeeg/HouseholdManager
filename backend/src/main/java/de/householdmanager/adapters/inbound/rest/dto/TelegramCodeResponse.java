package de.householdmanager.adapters.inbound.rest.dto;

/**
 * Ausgehendes DTO: der Freischalt-Code eines Haushalts.
 *
 * <p>Der Code ist ein reiner Anzeigewert — er wird im Chat an den Bot
 * gesendet. Die DTO trägt deshalb nichts weiter als den Code.
 *
 * <p>{@code botConfigured} sagt der Oberfläche, ob der Bot überhaupt läuft:
 * ohne Bot-Token lassen sich keine Chats freischalten, und der Nutzer soll
 * das wissen, bevor er einen Code anfordert.
 *
 * <p>Ohne konfigurierten Bot wird kein Code erzeugt — {@code code} ist dann
 * {@code null}. Die Oberfläche zeigt in dem Fall nur den Hinweis auf den
 * fehlenden Bot-Token.
 */
public record TelegramCodeResponse(String code, boolean botConfigured) {
}
