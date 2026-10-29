package de.householdmanager.domain.model;

import java.util.Locale;

/**
 * Ein freies Label einer Aufgabe.
 *
 * <p>Identität ist die <em>normalisierte</em> Schreibweise (lower-case,
 * getrimmt): „Bad" und „bad" sind dasselbe Label. Gespeichert und
 * angezeigt wird die zuerst gespeizierte Schreibweise ({@link #name()}).
 * Neue Labels entstehen durch Gebrauch — eine Verwaltungsseite gibt es nicht.
 */
public record Label(Long id, String name) {

    /** Maximale Länge — Grenze der Spalten {@code label.name}/{@code normalized_name}. */
    public static final int MAX_NAME_LENGTH = 60;

    /** Normalisiert einen Label-Namen: trimmen und lower-case. */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
