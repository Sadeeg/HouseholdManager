package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.adapters.inbound.rest.dto.TaskMoveRequest;
import de.householdmanager.adapters.inbound.rest.dto.TaskRequest;
import de.householdmanager.adapters.inbound.rest.dto.TaskResponse;
import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.TaskUseCase;
import de.householdmanager.domain.model.TaskStatus;
import de.householdmanager.domain.model.UserAccount;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Eingehender Adapter: HTTP → eingehender Port {@link TaskUseCase}.
 *
 * <p>Der Adapter übersetzt nur. Fachlogik steht im Use Case, Persistenz im
 * ausgehenden Adapter. Jede Anfrage verlangt eine angemeldete Person;
 * ihre Mitgliedschaft prüft der Use Case (403 ohne sie).
 */
@RestController
@RequestMapping("api/households/{householdId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskUseCase taskUseCase;
    private final CurrentUserPort currentUserPort;

    @GetMapping
    public List<TaskResponse> listTasks(@PathVariable long householdId) {
        return taskUseCase.listTasks(currentUserId(), householdId).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@PathVariable long householdId, @Valid @RequestBody TaskRequest request) {
        return TaskResponse.from(taskUseCase.createTask(currentUserId(), householdId, toCommand(request)));
    }

    @PatchMapping("{taskId}")
    public TaskResponse updateTask(
            @PathVariable long householdId,
            @PathVariable long taskId,
            @Valid @RequestBody TaskRequest request) {
        return TaskResponse.from(taskUseCase.updateTask(currentUserId(), householdId, taskId, toCommand(request)));
    }

    @PatchMapping("{taskId}/move")
    public TaskResponse moveTask(
            @PathVariable long householdId,
            @PathVariable long taskId,
            @Valid @RequestBody TaskMoveRequest request) {
        return TaskResponse.from(
                taskUseCase.moveTask(currentUserId(), householdId, taskId, request.status(), request.position()));
    }

    @DeleteMapping("{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable long householdId, @PathVariable long taskId) {
        taskUseCase.deleteTask(currentUserId(), householdId, taskId);
    }

    /** „Erledigte löschen": löscht alle Aufgaben der DONE-Spalte. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompleted(@PathVariable long householdId, @RequestParam("status") TaskStatus status) {
        if (status != TaskStatus.DONE) {
            throw new IllegalArgumentException("Nur die DONE-Spalte kann gesammelt gelöscht werden.");
        }
        taskUseCase.deleteDoneTasks(currentUserId(), householdId);
    }

    private TaskUseCase.TaskCommand toCommand(TaskRequest request) {
        return new TaskUseCase.TaskCommand(
                request.title(), request.content(), request.assigneeUserId(), request.dueDate(), request.labels());
    }

    private String currentUserId() {
        return currentUserPort.currentUser().map(UserAccount::id)
                .orElseThrow(AuthController.NotAuthenticatedException::new);
    }
}
