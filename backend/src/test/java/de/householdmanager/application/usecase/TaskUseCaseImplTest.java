package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.TaskUseCase.TaskCommand;
import de.householdmanager.application.port.in.TaskUseCase.TaskView;
import de.householdmanager.application.port.out.TaskRepository;
import de.householdmanager.application.port.out.UserDirectory;
import de.householdmanager.domain.model.InvalidLabelNameException;
import de.householdmanager.domain.model.InvalidTaskTitleException;
import de.householdmanager.domain.model.Label;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.Task;
import de.householdmanager.domain.model.TaskNotFoundException;
import de.householdmanager.domain.model.TaskStatus;
import de.householdmanager.domain.model.UnknownUserException;
import de.householdmanager.domain.model.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test des Use Cases gegen ausgehende Ports — ohne Spring, ohne Datenbank.
 * Die Ports werden durch kleine In-Memory-Fakes ersetzt (die bessere Wahl
 * gegenüber Mocks, wenn Interaktionen zählen: Reihenfolge, Labels).
 */
class TaskUseCaseImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T10:15:30Z");
    private static final long HOUSEHOLD = 1L;
    private static final String MEMBER = "alice";

    private InMemoryTaskRepository tasks;
    private StubMembership membership;
    private StubUserDirectory directory;
    private TaskUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        tasks = new InMemoryTaskRepository();
        membership = new StubMembership(MEMBER, "bob");
        directory = new StubUserDirectory(
                new UserAccount("alice", "Alice Admin", "alice@example.org"),
                new UserAccount("bob", "Bob Mitglied", "bob@example.org"),
                new UserAccount("carl", "Carl Fremd", null));
        useCase = new TaskUseCaseImpl(
                tasks, membership, directory, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    // --- Anlegen (AC2, AC7, AC8, AC18, AC25) ---------------------------------------------

    @Test
    void createTask_withTitleAndMarkdown_landsInTodoAtEndOfColumn() {
        givenTask("Bestehende", TaskStatus.TODO, 0);
        givenTask("Noch eine", TaskStatus.TODO, 1);

        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("  Müller alphabetisieren  ", "**fett** und `Code`", null, null, null));

        assertThat(created.title()).isEqualTo("Müller alphabetisieren");
        assertThat(created.content()).isEqualTo("**fett** und `Code`");
        assertThat(created.status()).isEqualTo(TaskStatus.TODO);
        assertThat(created.position()).isEqualTo(2); // ans Spaltenende, lückenlos
        assertThat(created.id()).isNotNull();
    }

    @Test
    void createTask_withoutOptionalFields_isValid() {
        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Nur ein Titel", null, null, null, null));

        assertThat(created.content()).isNull();
        assertThat(created.assigneeUserId()).isNull();
        assertThat(created.dueDate()).isNull();
        assertThat(created.labels()).isEmpty();
    }

    @Test
    void createTask_rejectsMissingOrBlankTitle() {
        assertThatThrownBy(() -> useCase.createTask(MEMBER, HOUSEHOLD, new TaskCommand(null, null, null, null, null)))
                .isInstanceOf(InvalidTaskTitleException.class)
                .hasMessageContaining("Titel");
        assertThatThrownBy(() -> useCase.createTask(MEMBER, HOUSEHOLD, new TaskCommand("   ", null, null, null, null)))
                .isInstanceOf(InvalidTaskTitleException.class);
    }

    @Test
    void createTask_rejectsTitleBeyondColumnLimit() {
        String tooLong = "x".repeat(Task.MAX_TITLE_LENGTH + 1);

        assertThatThrownBy(() -> useCase.createTask(MEMBER, HOUSEHOLD, new TaskCommand(tooLong, null, null, null, null)))
                .isInstanceOf(InvalidTaskTitleException.class)
                .hasMessageContaining("höchstens " + Task.MAX_TITLE_LENGTH);
    }

    @Test
    void createTask_rejectsAssigneeOutsideTheUserDirectory() {
        assertThatThrownBy(() -> useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Titel", null, "niemand", null, null)))
                .isInstanceOf(UnknownUserException.class);
    }

    @Test
    void createTask_resolvesAssigneeDisplayNameFromDirectory() {
        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Titel", null, "bob", null, null));

        assertThat(created.assigneeUserId()).isEqualTo("bob");
        assertThat(created.assigneeDisplayName()).isEqualTo("Bob Mitglied");
        assertThat(created.assigneeInHousehold()).isTrue(); // bob ist ernanntes Mitglied
    }

    // --- Label-Normalisierung (AC16, AC20) ------------------------------------------------

    @Test
    void labels_differingOnlyInCaseAreTheSameLabel_shownInFirstStoredSpelling() {
        TaskView first = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of("Bad", "bad", " BAD ")));
        TaskView second = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Andere", null, null, null, List.of("BAD")));

        assertThat(first.labels()).containsExactly("Bad"); // zusammengeführt, erste Schreibweise
        assertThat(second.labels()).containsExactly("Bad"); // dasselbe Label, keine neue Schreibweise
    }

    @Test
    void labels_canBeAddedAndRemovedThroughUpdate() {
        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of("Einkaufen")));

        TaskView updated = useCase.updateTask(MEMBER, HOUSEHOLD, created.id(),
                new TaskCommand("Eine", null, null, null, List.of()));

        assertThat(created.labels()).containsExactly("Einkaufen");
        assertThat(updated.labels()).isEmpty();
    }

    // --- Grenze des Label-Namens (Issue #8) --------------------------------------------------

    @Test
    void createTask_rejectsLabelBeyondColumnLimit_insteadOfDroppingIt() {
        String tooLong = "x".repeat(Label.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of(tooLong))))
                .isInstanceOf(InvalidLabelNameException.class)
                .hasMessageContaining("höchstens " + Label.MAX_NAME_LENGTH);
    }

    @Test
    void createTask_acceptsLabelAtColumnLimit() {
        String atLimit = "x".repeat(Label.MAX_NAME_LENGTH);

        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of(atLimit)));

        assertThat(created.labels()).containsExactly(atLimit);
    }

    @Test
    void updateTask_rejectsLabelBeyondColumnLimit() {
        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of("Kurz")));
        String tooLong = "y".repeat(Label.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> useCase.updateTask(MEMBER, HOUSEHOLD, created.id(),
                new TaskCommand("Eine", null, null, null, List.of("Kurz", tooLong))))
                .isInstanceOf(InvalidLabelNameException.class)
                .hasMessageContaining("höchstens " + Label.MAX_NAME_LENGTH);
    }

    @Test
    void createTask_keepsDroppingEmptyAndDuplicateLabelsWithoutError() {
        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of("", "   ", "Bad", "bad")));

        assertThat(created.labels()).containsExactly("Bad");
    }

    /**
     * Die Grenze ist eine Obergrenze: ein Zeichen darunter ist noch gültig.
     * Die Prüfung darf nicht versehentlich bereits bei 59 Zeichen greifen.
     */
    @Test
    void createTask_acceptsLabelJustBelowTheColumnLimit() {
        String justBelow = "x".repeat(Label.MAX_NAME_LENGTH - 1);

        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of(justBelow)));

        assertThat(created.labels()).containsExactly(justBelow);
    }

    /**
     * Gemessen wird <em>nach</em> der Normalisierung (trimmen, kleinschreiben).
     * Leerzeichen drumherum zählen nicht, weil sie nicht gespeichert werden —
     * sonst wäre die Oberfläche strenger als der Fachkern (oder umgekehrt).
     */
    @Test
    void createTask_measuresTheLabelLengthAfterTrimming() {
        String padded = "  " + "x".repeat(Label.MAX_NAME_LENGTH) + "  ";

        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of(padded)));

        assertThat(padded).hasSize(Label.MAX_NAME_LENGTH + 4); // roh zu lang …
        assertThat(created.labels()).containsExactly("x".repeat(Label.MAX_NAME_LENGTH)); // … normalisiert gültig
    }

    /**
     * Ein Zeichen über der Grenze bleibt auch nach dem Trimmen zu lang und wird
     * abgelehnt — die Meldung nennt die Grenze.
     */
    @Test
    void createTask_rejectsLabelBeyondTheLimitAfterTrimming() {
        String padded = "  " + "x".repeat(Label.MAX_NAME_LENGTH + 1) + "  ";

        assertThatThrownBy(() -> useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of(padded))))
                .isInstanceOf(InvalidLabelNameException.class)
                .hasMessageContaining("höchstens " + Label.MAX_NAME_LENGTH);
    }

    /**
     * Gemessen wird die <em>normalisierte</em> Länge — und die normalisierte
     * Form landet als {@code normalized_name} in der Spalte. Manche Zeichen
     * werden beim Kleinschreiben länger (U+0130 wird zu zwei Zeichen), weshalb
     * der reine Rohtext nicht als Maß taugt: 31 solche Zeichen wären im
     * Rohtext 31 Zeichen lang, in der Spalte aber 62 — und passten nicht mehr
     * hinein.
     */
    @Test
    void createTask_measuresTheLabelLengthAfterLowerCasing() {
        String thirty = "İ".repeat(30); // roh 30 Zeichen, normalisiert 60
        String thirtyOne = "İ".repeat(31); // roh 31 Zeichen, normalisiert 62

        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of(thirty)));

        assertThat(created.labels()).containsExactly(thirty);

        assertThatThrownBy(() -> useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of(thirtyOne))))
                .isInstanceOf(InvalidLabelNameException.class);
    }

    /**
     * Ein abgelehntes Label darf die ganze Aufgabe nicht anlegen: der Nutzer
     * bekommt einen Fehler, keine halb gespeicherte Aufgabe.
     */
    @Test
    void createTask_withRejectedLabel_createsNoTask() {
        assertThatThrownBy(() -> useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null,
                        List.of("Kurz", "x".repeat(Label.MAX_NAME_LENGTH + 1)))))
                .isInstanceOf(InvalidLabelNameException.class);

        assertThat(tasks.findByHousehold(HOUSEHOLD)).isEmpty();
    }

    /**
     * Eine abgelehnte Änderung lässt die gespeicherte Aufgabe unberührt: Titel
     * und Labels bleiben, wie sie waren. Sonst wäre die Aufgabe halb geändert.
     */
    @Test
    void updateTask_withRejectedLabel_leavesTheStoredTaskUnchanged() {
        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Eine", null, null, null, List.of("Kurz")));

        assertThatThrownBy(() -> useCase.updateTask(MEMBER, HOUSEHOLD, created.id(),
                new TaskCommand("Geändert", null, null, null,
                        List.of("Kurz", "x".repeat(Label.MAX_NAME_LENGTH + 1)))))
                .isInstanceOf(InvalidLabelNameException.class);

        Task stored = tasks.findById(HOUSEHOLD, created.id()).orElseThrow();
        assertThat(stored.getTitle()).isEqualTo("Eine");
        assertThat(stored.getLabels()).extracting(Label::name).containsExactly("Kurz");
    }

    // --- Bearbeiten (AC5) -------------------------------------------------------------------

    @Test
    void updateTask_changesAllFieldsAndKeepsPositionAndTimestamp() {
        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Alt", "alter Inhalt", null, null, List.of("Alt")));
        Task before = tasks.findById(HOUSEHOLD, created.id()).orElseThrow();

        TaskView updated = useCase.updateTask(MEMBER, HOUSEHOLD, created.id(),
                new TaskCommand("Neu", "**neu**", "carl", LocalDate.parse("2026-11-12"), List.of("Neu")));

        assertThat(updated.title()).isEqualTo("Neu");
        assertThat(updated.content()).isEqualTo("**neu**");
        assertThat(updated.assigneeUserId()).isEqualTo("carl");
        assertThat(updated.dueDate()).isEqualTo(LocalDate.parse("2026-11-12"));
        assertThat(updated.labels()).containsExactly("Neu");
        assertThat(updated.position()).isEqualTo(before.getPosition());
        assertThat(updated.status()).isEqualTo(TaskStatus.TODO);

        Task after = tasks.findById(HOUSEHOLD, created.id()).orElseThrow();
        assertThat(after.getCreatedAt()).isEqualTo(FIXED_NOW);
        assertThat(after.getTitle()).isEqualTo("Neu");
    }

    @Test
    void updateTask_removingDueDateAndAssignee_clearsThemPersistently() {
        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Frist läuft ab", null, "bob", LocalDate.parse("2020-01-01"), null));

        TaskView updated = useCase.updateTask(MEMBER, HOUSEHOLD, created.id(),
                new TaskCommand("Frist läuft ab", null, null, null, null));

        assertThat(updated.dueDate()).isNull();
        assertThat(updated.assigneeUserId()).isNull();
        Task stored = tasks.findById(HOUSEHOLD, created.id()).orElseThrow();
        assertThat(stored.getDueDate()).isNull();
        assertThat(stored.getAssigneeUserId()).isNull();
    }

    @Test
    void updateTask_unknownTask_isNotFound() {
        assertThatThrownBy(() -> useCase.updateTask(MEMBER, HOUSEHOLD, 999L,
                new TaskCommand("Titel", null, null, null, null)))
                .isInstanceOf(TaskNotFoundException.class);
    }

    // --- Verschieben (AC4, AC13, AC24) ------------------------------------------------------

    @Test
    void moveTask_fromTodoToDoing_staysMovedAfterwards() {
        Task created = givenTask("Müll raus", TaskStatus.TODO, 0);

        TaskView moved = useCase.moveTask(MEMBER, HOUSEHOLD, created.getId(), TaskStatus.DOING, null);

        assertThat(moved.status()).isEqualTo(TaskStatus.DOING);
        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.TODO)).noneMatch(t -> t.getId().equals(created.getId()));
        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.DOING))
                .anyMatch(t -> t.getId().equals(created.getId()) && t.getStatus() == TaskStatus.DOING);
    }

    @Test
    void moveTask_fromDoneBackToTodo_isAllowed() {
        Task done = givenTask("Fertig", TaskStatus.DONE, 0);

        TaskView moved = useCase.moveTask(MEMBER, HOUSEHOLD, done.getId(), TaskStatus.TODO, null);

        assertThat(moved.status()).isEqualTo(TaskStatus.TODO);
        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.TODO)).hasSize(1);
    }

    @Test
    void moveTask_insertsAtWishedPositionAndClosesTheGapInBothColumns() {
        Task doing1 = givenTask("Doing 1", TaskStatus.DOING, 0);
        givenTask("Doing 2", TaskStatus.DOING, 1);
        Task todo = givenTask("Todo", TaskStatus.TODO, 0);

        useCase.moveTask(MEMBER, HOUSEHOLD, todo.getId(), TaskStatus.DOING, 0);

        // Zielspalte: lückenlos 0..n, umgesetzte Karte vorn.
        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.DOING))
                .extracting(Task::getPosition)
                .containsExactly(0, 1, 2);
        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.DOING).getFirst().getId())
                .isEqualTo(todo.getId());
        // Bestehende Karten behalten ihren relativen Erhalt.
        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.DOING).get(1).getId()).isEqualTo(doing1.getId());
        // Quellspalte wird entleert.
        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.TODO)).isEmpty();
    }

    @Test
    void moveTask_withinTheSameColumn_reordersPersistently() {
        Task a = givenTask("A", TaskStatus.TODO, 0);
        Task b = givenTask("B", TaskStatus.TODO, 1);
        Task c = givenTask("C", TaskStatus.TODO, 2);

        useCase.moveTask(MEMBER, HOUSEHOLD, a.getId(), TaskStatus.TODO, 2);

        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.TODO))
                .extracting(Task::getId)
                .containsExactly(b.getId(), c.getId(), a.getId());
        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.TODO))
                .extracting(Task::getPosition)
                .containsExactly(0, 1, 2);
    }

    @Test
    void moveTask_withoutPositionAppendsToEndOfTargetColumn() {
        Task a = givenTask("A", TaskStatus.TODO, 0);
        Task b = givenTask("B", TaskStatus.TODO, 1);

        useCase.moveTask(MEMBER, HOUSEHOLD, a.getId(), TaskStatus.TODO, null);

        assertThat(tasks.findByStatus(HOUSEHOLD, TaskStatus.TODO).getLast().getId()).isEqualTo(a.getId());
    }

    // --- Löschen (AC6, AC15) ----------------------------------------------------------------

    @Test
    void deleteTask_removesTheTaskFromEveryColumn() {
        Task created = givenTask("Weg damit", TaskStatus.TODO, 0);

        useCase.deleteTask(MEMBER, HOUSEHOLD, created.getId());

        assertThat(tasks.findByHousehold(HOUSEHOLD)).isEmpty();
    }

    @Test
    void deleteTask_unknownTask_isNotFound() {
        assertThatThrownBy(() -> useCase.deleteTask(MEMBER, HOUSEHOLD, 999L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void deleteDoneTasks_removesOnlyTheDoneColumn() {
        givenTask("Offen", TaskStatus.TODO, 0);
        givenTask("Laufend", TaskStatus.DOING, 0);
        givenTask("Erledigt 1", TaskStatus.DONE, 0);
        givenTask("Erledigt 2", TaskStatus.DONE, 1);

        useCase.deleteDoneTasks(MEMBER, HOUSEHOLD);

        assertThat(tasks.findByHousehold(HOUSEHOLD))
                .extracting(Task::getStatus)
                .containsExactlyInAnyOrder(TaskStatus.TODO, TaskStatus.DOING);
    }

    // --- Berechtigung (AC9, AC12, AC14) ----------------------------------------------------

    @Test
    void allMembersMayDoEverything_noRoleDifference() {
        useCase.createTask(MEMBER, HOUSEHOLD, new TaskCommand("Alice", null, null, null, null));
        useCase.createTask("bob", HOUSEHOLD, new TaskCommand("Bob", null, null, null, null));

        assertThat(tasks.findByHousehold(HOUSEHOLD)).hasSize(2);
    }

    @Test
    void everyOperation_requiresMembership_aNonMemberSeesNothing() {
        String outsider = "zoe"; // nicht ernannt

        assertThatThrownBy(() -> useCase.listTasks(outsider, HOUSEHOLD))
                .isInstanceOf(MembershipRequiredException.class);
        assertThatThrownBy(() -> useCase.createTask(outsider, HOUSEHOLD, new TaskCommand("X", null, null, null, null)))
                .isInstanceOf(MembershipRequiredException.class);
        assertThatThrownBy(() -> useCase.deleteDoneTasks(outsider, HOUSEHOLD))
                .isInstanceOf(MembershipRequiredException.class);
    }

    @Test
    void assigneeOfRemovedMember_staysAssignedAndIsMarkedNotInHousehold() {
        TaskView created = useCase.createTask(MEMBER, HOUSEHOLD,
                new TaskCommand("Für Bob", null, "bob", null, null));
        membership.removeMember(HOUSEHOLD, "bob");

        TaskView listed = useCase.listTasks(MEMBER, HOUSEHOLD).getFirst();

        assertThat(listed.id()).isEqualTo(created.id()); // Zuweisung bleibt erhalten
        assertThat(listed.assigneeUserId()).isEqualTo("bob");
        assertThat(listed.assigneeDisplayName()).isEqualTo("Bob Mitglied"); // Name bleibt auflösbar
        assertThat(listed.assigneeInHousehold()).isFalse(); // Kennzeichnung „nicht mehr im Haushalt"
    }

    @Test
    void listTasks_unresolvableAssignee_showsNoName_soFrontendShowsTheIdentifier() {
        useCase.createTask(MEMBER, HOUSEHOLD, new TaskCommand("Für Bob", null, "bob", null, null));
        directory.accounts.remove("bob"); // die Person verschwindet aus dem Verzeichnis

        TaskView listed = useCase.listTasks(MEMBER, HOUSEHOLD).getFirst();

        assertThat(listed.assigneeUserId()).isEqualTo("bob");
        assertThat(listed.assigneeDisplayName()).isNull();
    }

    // --- Testgestell ------------------------------------------------------------------------

    private Task givenTask(String title, TaskStatus status, int position) {
        return tasks.given(title, status, position);
    }

    static final class InMemoryTaskRepository implements TaskRepository {

        private final Map<Long, Task> store = new LinkedHashMap<>();
        private final Map<String, Label> labelStore = new HashMap<>();
        private long ids = 0;
        private long labelIds = 0;

        Task given(String title, TaskStatus status, int position) {
            return save(Task.builder()
                    .householdId(HOUSEHOLD)
                    .title(title)
                    .status(status)
                    .position(position)
                    .createdAt(FIXED_NOW)
                    .labels(List.of())
                    .build());
        }

        @Override
        public List<Task> findByHousehold(long householdId) {
            return store.values().stream()
                    .filter(t -> t.getHouseholdId().equals(householdId))
                    .sorted(Comparator.comparing((Task t) -> t.getStatus().ordinal())
                            .thenComparingInt(Task::getPosition))
                    .toList();
        }

        @Override
        public Optional<Task> findById(long householdId, long taskId) {
            return Optional.ofNullable(store.get(taskId))
                    .filter(t -> t.getHouseholdId().equals(householdId));
        }

        @Override
        public List<Task> findByStatus(long householdId, TaskStatus status) {
            return store.values().stream()
                    .filter(t -> t.getHouseholdId().equals(householdId) && t.getStatus() == status)
                    .sorted(Comparator.comparingInt(Task::getPosition))
                    .toList();
        }

        @Override
        public Task save(Task task) {
            Task saved = task.getId() == null ? task.withId(++ids) : task;
            store.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public void delete(long householdId, long taskId) {
            store.remove(taskId);
        }

        @Override
        public int deleteByStatus(long householdId, TaskStatus status) {
            List<Long> idsToRemove = store.values().stream()
                    .filter(t -> t.getHouseholdId().equals(householdId) && t.getStatus() == status)
                    .map(Task::getId)
                    .toList();
            idsToRemove.forEach(store::remove);
            return idsToRemove.size();
        }

        @Override
        public Optional<Label> findLabel(long householdId, String normalizedName) {
            return Optional.ofNullable(labelStore.get(householdId + "|" + normalizedName));
        }

        @Override
        public Label saveLabel(long householdId, Label label) {
            String key = householdId + "|" + Label.normalize(label.name());
            return labelStore.computeIfAbsent(key, ignored -> new Label(++labelIds, label.name()));
        }
    }

    static final class StubMembership implements MembershipUseCase {

        private final java.util.Set<String> members;

        StubMembership(String... members) {
            this.members = new java.util.HashSet<>(java.util.Arrays.asList(members));
        }

        void removeMember(long householdId, String userId) {
            members.remove(userId);
        }

        @Override
        public List<MemberView> listMembers(String currentUserId, long householdId) {
            return List.of();
        }

        @Override
        public Optional<String> displayNameOf(String userId) {
            return Optional.empty();
        }

        @Override
        public MemberView addMember(String currentUserId, long householdId, String userId) {
            members.add(userId);
            return new MemberView(userId, userId, false);
        }

        @Override
        public void removeMember(String currentUserId, long householdId, String userId) {
            members.remove(userId);
        }

        @Override
        public void requireMembership(long householdId, String userId) {
            if (!members.contains(userId)) {
                throw new MembershipRequiredException("kein Mitglied");
            }
        }

        @Override
        public boolean isMember(long householdId, String userId) {
            return members.contains(userId);
        }
    }

    static final class StubUserDirectory implements UserDirectory {

        private final Map<String, UserAccount> accounts = new HashMap<>();

        StubUserDirectory(UserAccount... accounts) {
            for (UserAccount account : accounts) {
                this.accounts.put(account.id(), account);
            }
        }

        @Override
        public Optional<UserAccount> findById(String id) {
            return Optional.ofNullable(accounts.get(id));
        }

        @Override
        public List<UserAccount> search(String query, int limit) {
            return List.of();
        }
    }
}
