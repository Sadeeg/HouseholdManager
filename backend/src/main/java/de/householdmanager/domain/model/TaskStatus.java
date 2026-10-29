package de.householdmanager.domain.model;

/**
 * Status einer Aufgabe auf der Tafel.
 *
 * <p>Genau drei Werte, alle Übergänge sind frei (auch DONE zurück nach
 * TODO). Die Bedeutung trägt die Position in der Bahn, nicht eine Farbe.
 */
public enum TaskStatus {
    TODO,
    DOING,
    DONE
}
