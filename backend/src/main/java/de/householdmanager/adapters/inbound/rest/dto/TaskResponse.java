package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.application.port.in.TaskUseCase.TaskView;
import de.householdmanager.domain.model.TaskStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Ausgehendes DTO: eine Aufgabe der Tafel.
 *
 * <p>{@code assigneeDisplayName} ist serverseitig aufgelöst (null, wenn
 * nicht auflösbar — dann zeigt das Frontend die Kennung).
 * {@code assigneeInHousehold} kennzeichnet Zuweisungen entfernter Mitglieder.
 */
public record TaskResponse(
        Long id,
        String title,
        String content,
        TaskStatus status,
        String assigneeUserId,
        String assigneeDisplayName,
        boolean assigneeInHousehold,
        LocalDate dueDate,
        List<String> labels,
        int position) {

    public static TaskResponse from(TaskView view) {
        return new TaskResponse(view.id(), view.title(), view.content(), view.status(),
                view.assigneeUserId(), view.assigneeDisplayName(), view.assigneeInHousehold(),
                view.dueDate(), view.labels(), view.position());
    }
}
