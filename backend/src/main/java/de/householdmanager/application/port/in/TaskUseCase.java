package de.householdmanager.application.port.in;

import de.householdmanager.domain.model.Task;
import de.householdmanager.domain.model.TaskStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Eingehender Port: Aufgaben-Tafel eines Haushalts.
 *
 * <p>Alle Operationen setzen eine angemeldete Person voraus und prüfen ihre
 * Mitgliedschaft (über {@link MembershipUseCase}). Ohne Mitgliedschaft:
 * {@code MembershipRequiredException}.
 *
 * <p>Implementierung: {@code application.usecase.TaskUseCaseImpl}.
 */
public interface TaskUseCase {

    /** Eingabe einer Aufgabe: nur der Titel ist Pflicht, alles andere optional. */
    record TaskCommand(String title, String content, String assigneeUserId, LocalDate dueDate, List<String> labels) {
    }

    /**
     * Ausgabe einer Aufgabe zur Anzeige: Kennungen mit serverseitig
     * aufgelösten Namen ({@code null}, wenn nicht auflösbar — dann zeigt das
     * Frontend die Kennung). {@code assigneeInHousehold} kennzeichnet
     * Zuweisungen entfernter Mitglieder.
     */
    record TaskView(Long id, String title, String content, TaskStatus status,
                    String assigneeUserId, String assigneeDisplayName, boolean assigneeInHousehold,
                    LocalDate dueDate, List<String> labels, int position) {

        public static TaskView of(Task task, String assigneeDisplayName, boolean assigneeInHousehold) {
            return new TaskView(task.getId(), task.getTitle(), task.getContent(), task.getStatus(),
                    task.getAssigneeUserId(), assigneeDisplayName, assigneeInHousehold,
                    task.getDueDate(),
                    task.getLabels() == null ? List.of() : task.getLabels().stream().map(l -> l.name()).toList(),
                    task.getPosition());
        }
    }

    List<TaskView> listTasks(String userId, long householdId);

    TaskView createTask(String userId, long householdId, TaskCommand command);

    TaskView updateTask(String userId, long householdId, long taskId, TaskCommand command);

    /** Verschiebt eine Aufgabe in eine andere Spalte und/oder Position (null = ans Spaltenende). */
    TaskView moveTask(String userId, long householdId, long taskId, TaskStatus status, Integer position);

    void deleteTask(String userId, long householdId, long taskId);

    /** Löscht alle Aufgaben der DONE-Spalte; andere Spalten bleiben unverändert. */
    void deleteDoneTasks(String userId, long householdId);
}
