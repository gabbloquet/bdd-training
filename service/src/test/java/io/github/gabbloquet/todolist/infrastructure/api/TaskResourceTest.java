package io.github.gabbloquet.todolist.infrastructure.api;

import io.github.gabbloquet.todolist.domain.task.TaskService;
import io.github.gabbloquet.todolist.domain.task.addTask.AddTask;
import io.github.gabbloquet.todolist.domain.task.addTask.TaskCreated;
import io.github.gabbloquet.todolist.domain.task.completeTask.CompleteTask;
import io.github.gabbloquet.todolist.domain.task.deleteTask.DeleteTask;
import io.github.gabbloquet.todolist.domain.task.model.TaskId;
import io.github.gabbloquet.todolist.domain.task.model.TaskState;
import io.github.gabbloquet.todolist.domain.task.renameTask.RenameTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    private final UUID uuid = UUID.randomUUID();
    private final TaskId taskId = TaskId.from(uuid);
    private final String id = uuid.toString();
    private final LocalDateTime creationTime = LocalDateTime.now();

    private final TaskState taskState = new TaskState(
            List.of(TaskCreated.builder()
                    .taskId(taskId)
                    .description("Practice TDD")
                    .creationTime(creationTime)
                    .build())
    );

    private final TaskState modifiedTaskState = new TaskState(
            List.of(TaskCreated.builder()
                    .taskId(taskId)
                    .description("Always practice TDD!")
                    .creationTime(creationTime)
                    .build())
    );

    private final TaskState addedTaskState = new TaskState(
            List.of(TaskCreated.builder()
                    .taskId(taskId)
                    .description("Hey! Im a new task !")
                    .creationTime(creationTime)
                    .build())
    );

    @BeforeEach
    public void setUp() {
        when(taskService.getTask(uuid))
                .thenReturn(taskState);
        when(taskService.execute(AddTask.builder().description("Hey! Im a new task !").build()))
                .thenReturn(addedTaskState);
        when(taskService.execute(CompleteTask.builder().taskId(taskState.getId()).build()))
                .thenReturn(taskState);
        when(taskService.execute(RenameTask.builder().taskId(taskState.getId()).update("Always practice TDD!").build()))
                .thenReturn(modifiedTaskState);
    }

    @Test
    public void get_a_task() throws Exception {
        ResultActions requestResult = executeGetTaskOneRequest();

        assertTaskAffordance(requestResult, "Practice TDD");
    }

    @Test
    public void add_a_task() throws Exception {
        ResultActions requestResult = executeAddATaskRequest();

        assertTaskAffordance(requestResult, "Hey! Im a new task !");
    }

    @Test
    public void modify_a_task() throws Exception {
        ResultActions requestResult = executeModifyTaskTwoRequest();

        assertTaskAffordance(requestResult, "Always practice TDD!");
    }

    @Test
    public void delete_a_task_do_nothing() throws Exception {
        executeDeleteATaskRequest()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.addTask.href", is("http://localhost/tasks")))
                .andExpect(jsonPath("$._links.addTask.title", is("Add a task")))
                .andExpect(jsonPath("$._templates.default.method", is("POST")))
                .andExpect(jsonPath("$._templates.default.properties[0].name", is("description")))
                .andExpect(jsonPath("$._templates.default.properties[0].type", is("text")))
                .andExpect(jsonPath("$._templates.default.target", is("http://localhost/tasks")))

                .andExpect(jsonPath("$._links.todolist.href", is("http://localhost/todolist")));

        verify(taskService).execute(DeleteTask.builder().taskId(taskId).build());
    }

    private void assertTaskAffordance(ResultActions request, String description) throws Exception {
        request
            .andExpect(status().isOk())
            .andExpect(jsonPath("id").value(id))
            .andExpect(jsonPath("description").value(description))

            .andExpect(jsonPath("$._links.deleteOrModifyTask.href", is("http://localhost/tasks/" + id)))
            .andExpect(jsonPath("$._links.deleteOrModifyTask.title", is("Modify or delete a task")))

            .andExpect(jsonPath("$._templates.default.method", is("PUT")))
            .andExpect(jsonPath("$._templates.default.properties[0].name", is("description")))
            .andExpect(jsonPath("$._templates.default.properties[0].type", is("text")))

            .andExpect(jsonPath("$._templates.deleteTask.method", is("DELETE")))
            .andExpect(jsonPath("$._templates.deleteTask.target", is("http://localhost/tasks/" + id)))

            .andExpect(jsonPath("$._links.addTask.href", is("http://localhost/tasks")))
            .andExpect(jsonPath("$._links.addTask.title", is("Add a task")))
            .andExpect(jsonPath("$._templates.addTask.method", is("POST")))
            .andExpect(jsonPath("$._templates.addTask.properties[0].name", is("description")))
            .andExpect(jsonPath("$._templates.addTask.properties[0].type", is("text")))
            .andExpect(jsonPath("$._templates.addTask.target", is("http://localhost/tasks")))
            .andExpect(jsonPath("$._links.todolist.href", is("http://localhost/todolist")));
    }


    private ResultActions executeGetTaskOneRequest() throws Exception {
        return mockMvc.perform(get("/tasks/" + uuid)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_FORMS_JSON));
    }

    private ResultActions executeModifyTaskTwoRequest() throws Exception {
        return mockMvc.perform(put("/tasks/" + uuid)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_FORMS_JSON)
                .content("{\"description\": \"Always practice TDD!\"}"));
    }

    private ResultActions executeAddATaskRequest() throws Exception {
        return mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_FORMS_JSON)
                .content("{\"description\": \"Hey! Im a new task !\"}"));
    }

    private ResultActions executeDeleteATaskRequest() throws Exception {
        return mockMvc.perform(delete("/tasks/" + uuid)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_FORMS_JSON));
    }

}
