package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.application.port.out.TaskRepository;
import de.householdmanager.domain.model.Label;
import de.householdmanager.domain.model.Task;
import de.householdmanager.domain.model.TaskStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Ausgehender Adapter: setzt den ausgehenden Port {@link TaskRepository}
 * auf JPA um — Aufgaben, Labels und ihre Zuordnung.
 *
 * <p>Hier — und nur hier — wird zwischen Persistenzmodell und Fachmodell
 * übersetzt. Labels werden über ihre Kennung angebunden; sie entstehen
 * durch Gebrauch und werden über {@link #saveLabel} angelegt.
 */
@Repository
@RequiredArgsConstructor
public class TaskRepositoryAdapter implements TaskRepository {

    private final TaskJpaRepository taskJpaRepository;
    private final LabelJpaRepository labelJpaRepository;

    @Override
    public List<Task> findByHousehold(long householdId) {
        // Die DB sortiert Status alphabetisch (DOING, DONE, TODO); die Tafel
        // folgt der Reihenfolge des Status-Enums — hier nachsortiert.
        return taskJpaRepository.findAllByHouseholdIdOrderByStatusAscPositionAsc(householdId).stream()
                .map(this::toDomain)
                .sorted(Comparator.comparingInt((Task t) -> t.getStatus().ordinal())
                        .thenComparingInt(Task::getPosition))
                .toList();
    }

    @Override
    public Optional<Task> findById(long householdId, long taskId) {
        return taskJpaRepository.findByHouseholdIdAndId(householdId, taskId).map(this::toDomain);
    }

    @Override
    public List<Task> findByStatus(long householdId, TaskStatus status) {
        return taskJpaRepository.findAllByHouseholdIdAndStatusOrderByPositionAsc(householdId, status).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Task save(Task task) {
        TaskJpaEntity entity = task.getId() == null ? toNewEntity(task) : updateExisting(task);
        return toDomain(taskJpaRepository.save(entity));
    }

    @Override
    public void delete(long householdId, long taskId) {
        taskJpaRepository.findByHouseholdIdAndId(householdId, taskId)
                .ifPresent(taskJpaRepository::delete);
    }

    @Override
    public int deleteByStatus(long householdId, TaskStatus status) {
        return taskJpaRepository.deleteAllByHouseholdIdAndStatus(householdId, status);
    }

    @Override
    public Optional<Label> findLabel(long householdId, String normalizedName) {
        return labelJpaRepository.findByHouseholdIdAndNormalizedName(householdId, normalizedName)
                .map(this::toDomain);
    }

    @Override
    public Label saveLabel(long householdId, Label label) {
        String name = label.name();
        LabelJpaEntity entity = LabelJpaEntity.builder()
                .householdId(householdId)
                .name(name)
                .normalizedName(Label.normalize(name))
                .build();
        return toDomain(labelJpaRepository.save(entity));
    }

    // --- Mapping ---------------------------------------------------------------------

    private TaskJpaEntity toNewEntity(Task task) {
        TaskJpaEntity entity = TaskJpaEntity.builder()
                .id(task.getId())
                .householdId(task.getHouseholdId())
                .title(task.getTitle())
                .content(task.getContent())
                .status(task.getStatus())
                .assigneeUserId(task.getAssigneeUserId())
                .dueDate(task.getDueDate())
                .position(task.getPosition())
                .createdAt(task.getCreatedAt())
                .build();
        entity.getLabels().addAll(labelEntities(task));
        return entity;
    }

    private TaskJpaEntity updateExisting(Task task) {
        TaskJpaEntity entity = taskJpaRepository.findById(task.getId())
                .orElseThrow(() -> new IllegalStateException("Aufgabe nicht gefunden: " + task.getId()));
        entity.setTitle(task.getTitle());
        entity.setContent(task.getContent());
        entity.setStatus(task.getStatus());
        entity.setAssigneeUserId(task.getAssigneeUserId());
        entity.setDueDate(task.getDueDate());
        entity.setPosition(task.getPosition());
        // createdAt bleibt unangetastet — der Zeitstempel entsteht beim Anlegen.
        entity.getLabels().clear();
        entity.getLabels().addAll(labelEntities(task));
        return entity;
    }

    private List<LabelJpaEntity> labelEntities(Task task) {
        if (task.getLabels() == null) {
            return List.of();
        }
        return task.getLabels().stream()
                .map(label -> labelJpaRepository.findById(label.id())
                        .orElseThrow(() -> new IllegalStateException("Label nicht gefunden: " + label.id())))
                .toList();
    }

    private Task toDomain(TaskJpaEntity entity) {
        return Task.builder()
                .id(entity.getId())
                .householdId(entity.getHouseholdId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .assigneeUserId(entity.getAssigneeUserId())
                .dueDate(entity.getDueDate())
                .position(entity.getPosition())
                .createdAt(entity.getCreatedAt())
                .labels(entity.getLabels().stream().map(this::toDomain).toList())
                .build();
    }

    private Label toDomain(LabelJpaEntity entity) {
        return new Label(entity.getId(), entity.getName());
    }
}
