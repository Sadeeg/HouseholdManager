package de.householdmanager.application.usecase;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.TaskUseCase;
import de.householdmanager.application.port.out.TaskRepository;
import de.householdmanager.application.port.out.UserDirectory;
import de.householdmanager.domain.model.InvalidTaskTitleException;
import de.householdmanager.domain.model.Label;
import de.householdmanager.domain.model.Task;
import de.householdmanager.domain.model.TaskNotFoundException;
import de.householdmanager.domain.model.TaskStatus;
import de.householdmanager.domain.model.UnknownUserException;
import de.householdmanager.domain.model.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case „Aufgaben verwalten" — Transaktionsgrenze der Tafel.
 *
 * <p>Orchestriert: Mitgliedschaft prüfen (über {@link MembershipUseCase},
 * nicht über Tabellen), Titel und Zuständige validieren, Labels auflösen
 * (Entstehung durch Gebrauch), Reihenfolge normalisieren. Die Fachgrenzen
 * selbst wachen im Fachmodell ({@link Task}, {@link Label}).
 *
 * <p>Reihenfolge-Semantik: {@code position} ist lückenlos 0..n je
 * (Haushalt, Status). Nach jedem Move werden Quell- und Zielspalte
 * neu durchnummeriert; Neuanlage erhält max+1.
 */
@Service
@RequiredArgsConstructor
public class TaskUseCaseImpl implements TaskUseCase {

    private final TaskRepository taskRepository;
    private final MembershipUseCase membershipUseCase;
    private final UserDirectory userDirectory;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<TaskView> listTasks(String userId, long householdId) {
        membershipUseCase.requireMembership(householdId, userId);
        return taskRepository.findByHousehold(householdId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    public TaskView createTask(String userId, long householdId, TaskCommand command) {
        membershipUseCase.requireMembership(householdId, userId);
        int position = nextPosition(householdId, TaskStatus.TODO);
        Task task = Task.builder()
                .householdId(householdId)
                .title(validatedTitle(command.title()))
                .content(normalizedContent(command))
                .status(TaskStatus.TODO)
                .assigneeUserId(validatedAssignee(command.assigneeUserId()))
                .dueDate(command.dueDate())
                .position(position)
                .createdAt(Instant.now(clock))
                .labels(resolveLabels(householdId, command.labels()))
                .build();
        return toView(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskView updateTask(String userId, long householdId, long taskId, TaskCommand command) {
        membershipUseCase.requireMembership(householdId, userId);
        Task existing = requireTask(householdId, taskId);
        Task updated = existing.toBuilder()
                .title(validatedTitle(command.title()))
                .content(normalizedContent(command))
                .assigneeUserId(validatedAssignee(command.assigneeUserId()))
                .dueDate(command.dueDate())
                .labels(resolveLabels(householdId, command.labels()))
                .build();
        return toView(taskRepository.save(updated));
    }

    @Override
    @Transactional
    public TaskView moveTask(String userId, long householdId, long taskId, TaskStatus status, Integer position) {
        if (status == null) {
            throw new IllegalArgumentException("Ein Zielstatus ist erforderlich.");
        }
        membershipUseCase.requireMembership(householdId, userId);
        Task task = requireTask(householdId, taskId);
        TaskStatus sourceStatus = task.getStatus();

        // Zielspalte ohne die verschobene Karte, Karte an Wunschposition einfügen.
        List<Task> target = new ArrayList<>(taskRepository.findByStatus(householdId, status).stream()
                .filter(t -> !t.getId().equals(taskId))
                .toList());
        int targetPosition = position == null ? target.size() : Math.max(0, Math.min(position, target.size()));
        target.add(targetPosition, task);
        List<Task> renumberedTarget = IntStream.range(0, target.size())
                .mapToObj(i -> target.get(i).atPosition(status, i))
                .toList();

        // Quellspalte neu durchnummerieren, wenn der Status wechselt.
        List<Task> renumberedSource = sourceStatus == status
                ? List.of()
                : enumerate(taskRepository.findByStatus(householdId, sourceStatus).stream()
                        .filter(t -> !t.getId().equals(taskId)).toList(), sourceStatus);

        Task moved = null;
        for (Task t : renumberedTarget) {
            Task saved = taskRepository.save(t);
            if (saved.getId().equals(taskId)) {
                moved = saved;
            }
        }
        renumberedSource.forEach(taskRepository::save);
        return toView(moved);
    }

    @Override
    @Transactional
    public void deleteTask(String userId, long householdId, long taskId) {
        membershipUseCase.requireMembership(householdId, userId);
        requireTask(householdId, taskId);
        taskRepository.delete(householdId, taskId);
    }

    @Override
    @Transactional
    public void deleteDoneTasks(String userId, long householdId) {
        membershipUseCase.requireMembership(householdId, userId);
        taskRepository.deleteByStatus(householdId, TaskStatus.DONE);
    }

    // --- Fachregeln der Orchestrierung -------------------------------------------------

    private Task requireTask(long householdId, long taskId) {
        return taskRepository.findById(householdId, taskId)
                .orElseThrow(() -> new TaskNotFoundException("Die Aufgabe existiert nicht."));
    }

    private String validatedTitle(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidTaskTitleException("Ein Titel ist erforderlich.");
        }
        if (trimmed.length() > Task.MAX_TITLE_LENGTH) {
            throw new InvalidTaskTitleException(
                    "Der Titel darf höchstens " + Task.MAX_TITLE_LENGTH + " Zeichen lang sein.");
        }
        return trimmed;
    }

    private String normalizedContent(TaskCommand command) {
        return command.content() == null || command.content().isBlank() ? null : command.content();
    }

    private String validatedAssignee(String assigneeUserId) {
        if (assigneeUserId == null || assigneeUserId.isBlank()) {
            return null;
        }
        userDirectory.findById(assigneeUserId)
                .orElseThrow(() -> new UnknownUserException("Die Zuständige ist im Nutzerverzeichnis unbekannt."));
        return assigneeUserId;
    }

    /**
     * Labels entstehen durch Gebrauch: gleicher normalisierter Name = gleiches
     * Label (AC20); die zuerst gespeicherte Schreibweise gewinnt. Doppelte
     * normalisierte Namen werden zusammengeführt, leere verworfen.
     */
    private List<Label> resolveLabels(long householdId, List<String> names) {
        List<Label> labels = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (names == null) {
            return labels;
        }
        for (String raw : names) {
            String normalized = Label.normalize(raw);
            if (normalized.isEmpty() || normalized.length() > Label.MAX_NAME_LENGTH || !seen.add(normalized)) {
                continue;
            }
            Label existing = taskRepository.findLabel(householdId, normalized).orElse(null);
            if (existing != null) {
                labels.add(existing);
            } else {
                labels.add(taskRepository.saveLabel(householdId, new Label(null, raw.trim())));
            }
        }
        return labels;
    }

    private int nextPosition(long householdId, TaskStatus status) {
        return taskRepository.findByStatus(householdId, status).stream()
                .mapToInt(Task::getPosition)
                .max()
                .orElse(-1) + 1;
    }

    private List<Task> enumerate(List<Task> tasks, TaskStatus status) {
        return IntStream.range(0, tasks.size())
                .mapToObj(i -> tasks.get(i).atPosition(status, i))
                .toList();
    }

    private TaskView toView(Task task) {
        String assigneeId = task.getAssigneeUserId();
        if (assigneeId == null) {
            return TaskView.of(task, null, false);
        }
        String displayName = userDirectory.findById(assigneeId).map(UserAccount::displayName).orElse(null);
        boolean inHousehold = membershipUseCase.isMember(task.getHouseholdId(), assigneeId);
        return TaskView.of(task, displayName, inHousehold);
    }
}
