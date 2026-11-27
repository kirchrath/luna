package de.kirchrath.luna.task.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import de.kirchrath.luna.task.config.TestcontainersConfig;
import de.kirchrath.luna.task.data.entity.Task;
import de.kirchrath.luna.task.data.entity.TaskPriority;
import de.kirchrath.luna.task.data.entity.TaskSource;
import de.kirchrath.luna.task.data.entity.TaskStatus;
import de.kirchrath.luna.task.data.repository.CoreTaskProjectionRepository;
import de.kirchrath.luna.task.data.repository.TaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class TaskControllerTest {

    private static final OffsetDateTime DUE_DATE = OffsetDateTime.parse("2026-11-30T12:00:00+01:00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CoreTaskProjectionRepository projectionRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @Test
    void thatListingTasksShouldResultIntoDueTasksFirstAndTasksWithoutDueDateLast() throws Exception {
        // given
        var withoutDueDate = save("Without due date", null, TaskStatus.OPEN);
        var later = save("Later", DUE_DATE.plusDays(1), TaskStatus.DONE);
        var sooner = save("Sooner", DUE_DATE, TaskStatus.OPEN);

        // when / then
        mockMvc.perform(get("/tasks").param("page", "0").param("size", "10").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(
                        sooner.id().toString(), later.id().toString(), withoutDueDate.id().toString())))
                .andExpect(jsonPath("$.items[0].title").value("Sooner"))
                .andExpect(jsonPath("$.items[0].priority").value("NORMAL"))
                .andExpect(jsonPath("$.items[0].status").value("OPEN"))
                .andExpect(jsonPath("$.items[0].edited").value(false))
                .andExpect(jsonPath("$.items[2].dueDate").doesNotExist())
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalCount").value(3));
    }

    @Test
    void thatListingTasksByStatusShouldResultIntoTheMatchingTasksOnly() throws Exception {
        // given
        save("Open", DUE_DATE, TaskStatus.OPEN);
        var done = save("Done", DUE_DATE, TaskStatus.DONE);

        // when / then
        mockMvc.perform(get("/tasks").param("status", "DONE").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(done.id().toString())))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void thatListingTasksWithAnUnknownStatusShouldResultIntoABadRequest() throws Exception {
        // when / then
        mockMvc.perform(get("/tasks").param("status", "UNKNOWN").with(jwt())).andExpect(status().isBadRequest());
    }

    @Test
    void thatListingTasksWithoutTokenShouldResultIntoUnauthorized() throws Exception {
        // when / then
        mockMvc.perform(get("/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void thatGettingATaskShouldResultIntoItsDataAndAudit() throws Exception {
        // given
        var task = save("Pay the electricity bill", DUE_DATE, TaskStatus.OPEN);

        // when / then
        mockMvc.perform(get("/tasks/{id}", task.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.data.title").value("Pay the electricity bill"))
                .andExpect(jsonPath("$.data.description").value("Before the end of the month"))
                // Dates are returned in the time zone of the server, so the expected text depends on where the test runs.
                .andExpect(jsonPath("$.data.dueDate").value(DUE_DATE.atZoneSameInstant(ZoneId.systemDefault())
                        .toOffsetDateTime()
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$.data.priority").value("NORMAL"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.source").value("CORE"))
                .andExpect(jsonPath("$.data.sourceId").value(task.sourceId().toString()))
                .andExpect(jsonPath("$.data.edited").value(false))
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    @Test
    void thatGettingAnUnknownTaskShouldResultIntoNotFound() throws Exception {
        // when / then
        mockMvc.perform(get("/tasks/{id}", UUID.randomUUID()).with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void thatUpdatingATaskShouldResultIntoAnEditedTask() throws Exception {
        // given
        var task = save("Pay the electricity bill", DUE_DATE, TaskStatus.OPEN);

        // when / then
        mockMvc.perform(put("/tasks/{id}", task.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Pay the gas bill", "priority": "HIGH", "version": 0}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Pay the gas bill"))
                .andExpect(jsonPath("$.data.description").doesNotExist())
                .andExpect(jsonPath("$.data.dueDate").doesNotExist())
                .andExpect(content().string(not(containsString(":null"))))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.edited").value(true))
                .andExpect(jsonPath("$.audit.version").value(1));
        assertThat(taskRepository.findById(task.id())).get().extracting(Task::title).isEqualTo("Pay the gas bill");
    }

    @Test
    void thatUpdatingATaskInAnOutdatedVersionShouldResultIntoAConflict() throws Exception {
        // given
        var task = save("Pay the electricity bill", DUE_DATE, TaskStatus.OPEN);

        // when / then
        mockMvc.perform(put("/tasks/{id}", task.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Pay the gas bill", "priority": "HIGH", "version": 3}
                        """))
                .andExpect(status().isConflict());
        assertThat(taskRepository.findById(task.id())).get().extracting(Task::edited).isEqualTo(false);
    }

    @Test
    void thatUpdatingATaskWithoutTitleShouldResultIntoABadRequest() throws Exception {
        // given
        var task = save("Pay the electricity bill", DUE_DATE, TaskStatus.OPEN);

        // when / then
        mockMvc.perform(put("/tasks/{id}", task.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": " ", "priority": "HIGH", "version": 0}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thatCompletingAnOpenTaskShouldResultIntoADoneTaskWithCompletionDate() throws Exception {
        // given
        var task = save("Pay the electricity bill", DUE_DATE, TaskStatus.OPEN);

        // when / then
        mockMvc.perform(post("/tasks/{id}/complete", task.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.completedDate").isNotEmpty())
                .andExpect(jsonPath("$.data.edited").value(true))
                .andExpect(jsonPath("$.audit.version").value(1));
    }

    @Test
    void thatCompletingADoneTaskShouldResultIntoAnUnchangedTask() throws Exception {
        // given
        var task = save("Pay the electricity bill", DUE_DATE, TaskStatus.DONE);

        // when / then
        mockMvc.perform(post("/tasks/{id}/complete", task.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    @Test
    void thatReopeningADoneTaskShouldResultIntoAnOpenTaskWithoutCompletionDate() throws Exception {
        // given
        var task = save("Pay the electricity bill", DUE_DATE, TaskStatus.DONE);

        // when / then
        mockMvc.perform(post("/tasks/{id}/reopen", task.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.completedDate").doesNotExist())
                .andExpect(jsonPath("$.data.edited").value(true))
                .andExpect(jsonPath("$.audit.version").value(1));
    }

    @Test
    void thatReopeningAnOpenTaskShouldResultIntoAnUnchangedTask() throws Exception {
        // given
        var task = save("Pay the electricity bill", DUE_DATE, TaskStatus.OPEN);

        // when / then
        mockMvc.perform(post("/tasks/{id}/reopen", task.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    private Task save(String title, OffsetDateTime dueDate, TaskStatus status) {
        return taskRepository.save(Task.builder()
                .source(TaskSource.CORE)
                .sourceId(UUID.randomUUID())
                .title(title)
                .description("Before the end of the month")
                .dueDate(dueDate)
                .priority(TaskPriority.NORMAL)
                .status(status)
                .completedDate(status == TaskStatus.DONE ? DUE_DATE : null)
                .edited(false)
                .build());
    }
}
