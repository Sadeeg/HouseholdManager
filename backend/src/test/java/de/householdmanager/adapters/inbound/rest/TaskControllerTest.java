package de.householdmanager.adapters.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.TaskUseCase;
import de.householdmanager.application.port.in.TaskUseCase.TaskView;
import de.householdmanager.domain.model.InvalidLabelNameException;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.Label;
import de.householdmanager.domain.model.TaskNotFoundException;
import de.householdmanager.domain.model.TaskStatus;
import de.householdmanager.domain.model.UnknownUserException;
import de.householdmanager.domain.model.UserAccount;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test des eingehenden Adapters: HTTP-Übersetzung des Aufgaben-Ports.
 * Fachlogik ist gemockt — geprüft werden Pfade, Statuscodes und DTOs.
 */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false) // Security wird separat getestet
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskUseCase taskUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @Test
    void listTasks_mapsViewsToResponses() throws Exception {
        givenSignedInAs("alice");
        when(taskUseCase.listTasks("alice", 1L)).thenReturn(List.of(new TaskView(
                10L, "Müll raus", "**tonnen raus**", TaskStatus.TODO,
                "bob", "Bob Mitglied", true,
                LocalDate.parse("2026-11-12"), List.of("Hausarbeit"), 0)));

        mockMvc.perform(get("/api/households/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Müll raus"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].assigneeUserId").value("bob"))
                .andExpect(jsonPath("$[0].assigneeDisplayName").value("Bob Mitglied"))
                .andExpect(jsonPath("$[0].assigneeInHousehold").value(true))
                .andExpect(jsonPath("$[0].dueDate").value("2026-11-12"))
                .andExpect(jsonPath("$[0].labels[0]").value("Hausarbeit"))
                .andExpect(jsonPath("$[0].position").value(0));
    }

    @Test
    void listTasks_withoutSignedInPerson_isUnauthorized() throws Exception {
        when(currentUserPort.currentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/households/1/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listTasks_byNonMember_isForbidden() throws Exception {
        givenSignedInAs("zoe");
        when(taskUseCase.listTasks("zoe", 1L))
                .thenThrow(new MembershipRequiredException("kein Mitglied"));

        mockMvc.perform(get("/api/households/1/tasks"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTask_returnsCreatedTask() throws Exception {
        givenSignedInAs("alice");
        when(taskUseCase.createTask(anyString(), anyLong(), any())).thenReturn(new TaskView(
                10L, "Neu", null, TaskStatus.TODO, null, null, false, null, List.of(), 2));

        mockMvc.perform(post("/api/households/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Neu","content":"**Text**","labels":["Bad"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createTask_withoutTitle_isBadRequest() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(post("/api/households/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ungültige Anfrage"));
    }

    @Test
    void createTask_withUnknownAssignee_isBadRequest() throws Exception {
        givenSignedInAs("alice");
        when(taskUseCase.createTask(anyString(), anyLong(), any()))
                .thenThrow(new UnknownUserException("Die Zuständige ist im Nutzerverzeichnis unbekannt."));

        mockMvc.perform(post("/api/households/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Neu\",\"assigneeUserId\":\"ghost\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_withLabelBeyondColumnLimit_isBadRequest() throws Exception {
        givenSignedInAs("alice");
        when(taskUseCase.createTask(anyString(), anyLong(), any()))
                .thenThrow(new InvalidLabelNameException("Ein Label darf höchstens 60 Zeichen lang sein."));

        mockMvc.perform(post("/api/households/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Neu\",\"labels\":[\"zu lang\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ungültiger Labelname"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("höchstens " + Label.MAX_NAME_LENGTH)));
    }

    /**
     * Auch das Bearbeiten meldet die zu langen Schreibweise — derselbe Fehler
     * am Label, nicht am Titel. Sonst wäre die Meldung am Task-Endpoint
     * unterschiedlich je nach Route.
     */
    @Test
    void updateTask_withLabelBeyondColumnLimit_isBadRequest() throws Exception {
        givenSignedInAs("alice");
        when(taskUseCase.updateTask(anyString(), anyLong(), anyLong(), any()))
                .thenThrow(new InvalidLabelNameException("Ein Label darf höchstens 60 Zeichen lang sein."));

        mockMvc.perform(patch("/api/households/1/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Neu\",\"labels\":[\"zu lang\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ungültiger Labelname"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("höchstens " + Label.MAX_NAME_LENGTH)));
    }

    @Test
    void updateTask_mapsAllFields() throws Exception {
        givenSignedInAs("alice");
        when(taskUseCase.updateTask(anyString(), anyLong(), anyLong(), any())).thenReturn(new TaskView(
                10L, "Geändert", "neu", TaskStatus.DOING, null, null, false, null, List.of(), 0));

        mockMvc.perform(patch("/api/households/1/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Geändert\",\"content\":\"neu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Geändert"));
    }

    @Test
    void moveTask_mapsStatusAndPosition() throws Exception {
        givenSignedInAs("alice");
        when(taskUseCase.moveTask("alice", 1L, 10L, TaskStatus.DOING, 1)).thenReturn(new TaskView(
                10L, "Verschoben", null, TaskStatus.DOING, null, null, false, null, List.of(), 1));

        mockMvc.perform(patch("/api/households/1/tasks/10/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DOING\",\"position\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOING"))
                .andExpect(jsonPath("$.position").value(1));
    }

    @Test
    void moveTask_withoutStatus_isBadRequest() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(patch("/api/households/1/tasks/10/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTask_returnsNoContent() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(delete("/api/households/1/tasks/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_unknownTask_isNotFound() throws Exception {
        givenSignedInAs("alice");
        doThrow(new TaskNotFoundException("nicht gefunden"))
                .when(taskUseCase).deleteTask("alice", 1L, 999L);

        mockMvc.perform(delete("/api/households/1/tasks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCompleted_onlyTheDoneColumnIsAllowed() throws Exception {
        givenSignedInAs("alice");

        mockMvc.perform(delete("/api/households/1/tasks").param("status", "DONE"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/households/1/tasks").param("status", "TODO"))
                .andExpect(status().isBadRequest());
    }

    private void givenSignedInAs(String userId) {
        when(currentUserPort.currentUser())
                .thenReturn(Optional.of(new UserAccount(userId, userId, null)));
    }
}
