package de.householdmanager.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Fachmodell der Aufgabe auf der Tafel eines Haushalts.
 *
 * <p>Gehört zum Fachkern: keine Framework-Annotationen, keine JPA-Importe.
 * Eine Aufgabe gehört zu genau einem Haushalt. Titel ist das einzige
 * Pflichtfeld; Inhalt, Zuständige, Frist und Labels sind optional.
 * Die Reihenfolge innerhalb einer Spalte trägt {@link #position()} —
 * lückenlos 0..n je (Haushalt, Status).
 */
@Getter
@Builder(toBuilder = true)
public class Task {

    /** Maximale Länge des Titels — Grenze der Spalte {@code task.title}. */
    public static final int MAX_TITLE_LENGTH = 200;

    private final Long id;
    private final Long householdId;
    private final String title;
    private final String content;
    private final TaskStatus status;
    private final String assigneeUserId;
    private final LocalDate dueDate;
    private final int position;
    private final Instant createdAt;
    private final List<Label> labels;

    public Task withId(Long newId) {
        return toBuilder().id(newId).build();
    }

    /** Aufgabe in eine andere Spalte (und/oder Position) stellen. */
    public Task atPosition(TaskStatus newStatus, int newPosition) {
        return toBuilder().status(newStatus).position(newPosition).build();
    }
}
