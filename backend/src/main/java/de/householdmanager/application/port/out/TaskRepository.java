package de.householdmanager.application.port.out;

import de.householdmanager.domain.model.Label;
import de.householdmanager.domain.model.Task;
import de.householdmanager.domain.model.TaskStatus;
import java.util.List;
import java.util.Optional;

/**
 * Ausgehender Port: Aufgaben und Labels eines Haushalts.
 *
 * <p>Alle Zugriffe sind auf einen Haushalt bezogen — der Port kennt keine
 * haushaltübergreifenden Abfragen. Labels gehören zur Aufgabe: Sie werden
 * über denselben Port geladen und angelegt (Entstehung durch Gebrauch).
 *
 * <p>Implementierung: {@code adapters.outbound.persistence.TaskRepositoryAdapter}.
 */
public interface TaskRepository {

    /** Alle Aufgaben des Haushalts, sortiert nach Status und Position. */
    List<Task> findByHousehold(long householdId);

    Optional<Task> findById(long householdId, long taskId);

    /** Die Karten einer Spalte, sortiert nach Position. */
    List<Task> findByStatus(long householdId, TaskStatus status);

    Task save(Task task);

    void delete(long householdId, long taskId);

    /** Löscht alle Aufgaben einer Spalte; liefert die Anzahl. */
    int deleteByStatus(long householdId, TaskStatus status);

    /** Label eines Haushalts nach normalisiertem Namen. */
    Optional<Label> findLabel(long householdId, String normalizedName);

    Label saveLabel(long householdId, Label label);
}
