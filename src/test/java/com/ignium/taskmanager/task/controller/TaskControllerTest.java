package com.ignium.taskmanager.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ignium.taskmanager.config.TestSecurityConfig;
import com.ignium.taskmanager.config.exception.TaskNotFoundException;
import com.ignium.taskmanager.task.TestDataFactory;
import com.ignium.taskmanager.task.dto.CreateTaskRequest;
import com.ignium.taskmanager.task.dto.PaginatedResponse;
import com.ignium.taskmanager.task.dto.TaskResponse;
import com.ignium.taskmanager.task.dto.UpdateTaskRequest;
import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.task.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@WebMvcTest(TaskController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
@DisplayName("TaskController Integration Tests")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    // POST /api/v1/tasks - Create Task

    @Test
    @DisplayName("createTask_shouldReturnCreated")
    void createTask_shouldReturnCreated() throws Exception {
        // Arrange
        CreateTaskRequest request = TestDataFactory.createTaskRequest();
        TaskResponse response = TestDataFactory.createTaskResponse();

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/tasks/" + TestDataFactory.TEST_TASK_ID))
            .andExpect(jsonPath("$.id").value(TestDataFactory.TEST_TASK_ID.toString()))
            .andExpect(jsonPath("$.title").value("Test Task"))
            .andExpect(jsonPath("$.description").value("Test Description"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.priority").value("MEDIUM"))
            .andExpect(jsonPath("$.tags").isArray())
            .andExpect(jsonPath("$.tags[0]").value("work"))
            .andExpect(jsonPath("$.tags[1]").value("urgent"))
            .andDo(document("create-task",
                requestFields(
                    fieldWithPath("title").description("Title of the task").type("String"),
                    fieldWithPath("description").description("Description of the task").type("String").optional(),
                    fieldWithPath("status").description("Task status (TODO, IN_PROGRESS, COMPLETED, CANCELLED)").type("TaskStatus").optional(),
                    fieldWithPath("priority").description("Task priority (LOW, MEDIUM, HIGH, URGENT)").type("TaskPriority"),
                    fieldWithPath("dueDate").description("Task due date in ISO format").type("LocalDateTime").optional(),
                    fieldWithPath("tags").description("Array of tag names").type("List<String>").optional()
                ),
                responseFields(
                    fieldWithPath("id").description("Unique task identifier (UUID)").type("UUID"),
                    fieldWithPath("title").description("Title of the task").type("String"),
                    fieldWithPath("description").description("Description of the task").type("String"),
                    fieldWithPath("status").description("Current task status").type("TaskStatus"),
                    fieldWithPath("priority").description("Task priority").type("TaskPriority"),
                    fieldWithPath("dueDate").description("Task due date").type("LocalDateTime"),
                    fieldWithPath("tags").description("Array of associated tag names").type("List<String>"),
                    fieldWithPath("createdAt").description("Creation timestamp").type("LocalDateTime"),
                    fieldWithPath("updatedAt").description("Last update timestamp").type("LocalDateTime")
                )
            ));
    }

    @Test
    @DisplayName("createTask_withInvalidData_shouldReturnBadRequest")
    void createTask_withInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest(
            "", // empty title - should fail validation
            "Test Description",
            Task.TaskStatus.TODO,
            Task.TaskPriority.MEDIUM,
            null,
            null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andDo(document("create-task-validation-error",
                requestFields(
                    fieldWithPath("title").description("Title of the task - cannot be empty").type("String"),
                    fieldWithPath("description").description("Description of the task").type("String").optional(),
                    fieldWithPath("status").description("Task status").type("TaskStatus").optional(),
                    fieldWithPath("priority").description("Task priority").type("TaskPriority"),
                    fieldWithPath("dueDate").description("Task due date").type("LocalDateTime").optional(),
                    fieldWithPath("tags").description("Array of tag names").type("List<String>").optional()
                ),
                responseFields(
                    fieldWithPath("status").description("HTTP status code").type("int"),
                    fieldWithPath("error").description("Error type").type("String"),
                    fieldWithPath("message").description("Error message").type("String"),
                    fieldWithPath("timestamp").description("Error timestamp").type("LocalDateTime"),
                    fieldWithPath("fieldErrors").description("Field validation errors").type("Map<String, String>").optional(),
                    fieldWithPath("fieldErrors.title").description("Title field validation error").type("String").optional()
                )
            ));
    }

    // GET /api/v1/tasks/{id} - Get Task by ID

    @Test
    @DisplayName("getTaskById_shouldReturnTask")
    void getTaskById_shouldReturnTask() throws Exception {
        // Arrange
        TaskResponse response = TestDataFactory.createTaskResponse();

        when(taskService.getTaskById(TestDataFactory.TEST_TASK_ID)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks/{id}", TestDataFactory.TEST_TASK_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestDataFactory.TEST_TASK_ID.toString()))
            .andExpect(jsonPath("$.title").value("Test Task"))
            .andDo(document("get-task-by-id",
                pathParameters(
                    parameterWithName("id").description("Unique task identifier (UUID)")
                ),
                responseFields(
                    fieldWithPath("id").description("Unique task identifier (UUID)").type("UUID"),
                    fieldWithPath("title").description("Title of the task").type("String"),
                    fieldWithPath("description").description("Description of the task").type("String"),
                    fieldWithPath("status").description("Current task status").type("TaskStatus"),
                    fieldWithPath("priority").description("Task priority").type("TaskPriority"),
                    fieldWithPath("dueDate").description("Task due date").type("LocalDateTime"),
                    fieldWithPath("tags").description("Array of associated tag names").type("List<String>"),
                    fieldWithPath("createdAt").description("Creation timestamp").type("LocalDateTime"),
                    fieldWithPath("updatedAt").description("Last update timestamp").type("LocalDateTime")
                )
            ));
    }

    @Test
    @DisplayName("getTaskById_notFound_shouldReturn404")
    void getTaskById_notFound_shouldReturn404() throws Exception {
        // Arrange
        doThrow(new TaskNotFoundException(TestDataFactory.TEST_TASK_ID))
            .when(taskService).getTaskById(TestDataFactory.TEST_TASK_ID);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks/{id}", TestDataFactory.TEST_TASK_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Task Not Found"))
            .andDo(document("get-task-by-id-not-found",
                pathParameters(
                    parameterWithName("id").description("Unique task identifier (UUID)")
                ),
                responseFields(
                    fieldWithPath("status").description("HTTP status code").type("int"),
                    fieldWithPath("error").description("Error type").type("String"),
                    fieldWithPath("message").description("Error message").type("String"),
                    fieldWithPath("timestamp").description("Error timestamp").type("LocalDateTime"),
                    fieldWithPath("taskId").description("Requested task ID").type("String")
                )
            ));
    }

    // GET /api/v1/tasks - List Tasks with Filters

    @Test
    @DisplayName("getAllTasks_shouldReturnPaginatedList")
    void getAllTasks_shouldReturnPaginatedList() throws Exception {
        // Arrange
        List<TaskResponse> tasks = List.of(TestDataFactory.createTaskResponse());
        Page<TaskResponse> taskPage = new PageImpl<>(tasks, PageRequest.of(0, 10), 1);

        when(taskService.getTasksWithFilters(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
            .thenReturn(taskPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(TestDataFactory.TEST_TASK_ID.toString()))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true))
            .andExpect(jsonPath("$.empty").value(false))
            .andDo(document("get-all-tasks",
                queryParameters(
                    parameterWithName("status").description("Filter by task status").optional(),
                    parameterWithName("tags").description("Filter by tag names (OR logic)").optional(),
                    parameterWithName("page").description("Page number (0-based)").optional(),
                    parameterWithName("size").description("Page size").optional(),
                    parameterWithName("sortBy").description("Sort field").optional(),
                    parameterWithName("sortDirection").description("Sort direction (asc/desc)").optional()
                ),
                responseFields(
                    fieldWithPath("content").description("Array of task responses").type("List<TaskResponse>"),
                    fieldWithPath("content[].id").description("Unique task identifier").type("UUID"),
                    fieldWithPath("content[].title").description("Task title").type("String"),
                    fieldWithPath("content[].description").description("Task description").type("String"),
                    fieldWithPath("content[].status").description("Task status").type("TaskStatus"),
                    fieldWithPath("content[].priority").description("Task priority").type("TaskPriority"),
                    fieldWithPath("content[].dueDate").description("Task due date").type("LocalDateTime"),
                    fieldWithPath("content[].tags").description("Associated tag names").type("List<String>"),
                    fieldWithPath("content[].createdAt").description("Creation timestamp").type("LocalDateTime"),
                    fieldWithPath("content[].updatedAt").description("Last update timestamp").type("LocalDateTime"),
                    fieldWithPath("totalElements").description("Total number of tasks").type("long"),
                    fieldWithPath("totalPages").description("Total number of pages").type("int"),
                    fieldWithPath("size").description("Page size").type("int"),
                    fieldWithPath("number").description("Current page number").type("int"),
                    fieldWithPath("first").description("Is first page").type("boolean"),
                    fieldWithPath("last").description("Is last page").type("boolean"),
                    fieldWithPath("empty").description("Is result empty").type("boolean")
                )
            ));
    }

    @Test
    @DisplayName("getAllTasks_withStatusFilter_shouldFilterByStatus")
    void getAllTasks_withStatusFilter_shouldFilterByStatus() throws Exception {
        // Arrange
        List<TaskResponse> tasks = List.of(TestDataFactory.createTaskResponse());
        Page<TaskResponse> taskPage = new PageImpl<>(tasks);

        when(taskService.getTasksWithFilters(any(Task.TaskStatus.class), any(), anyInt(), anyInt(), anyString(), anyString()))
            .thenReturn(taskPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks")
                .param("status", "TODO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].status").value("TODO"))
            .andDo(document("get-tasks-with-status-filter",
                queryParameters(
                    parameterWithName("status").description("Filter by task status (TODO, IN_PROGRESS, COMPLETED, CANCELLED)"),
                    parameterWithName("tags").description("Filter by tag names").optional(),
                    parameterWithName("page").description("Page number").optional(),
                    parameterWithName("size").description("Page size").optional(),
                    parameterWithName("sortBy").description("Sort field").optional(),
                    parameterWithName("sortDirection").description("Sort direction").optional()
                )
            ));
    }

    @Test
    @DisplayName("getAllTasks_withTagsFilter_shouldFilterByTags")
    void getAllTasks_withTagsFilter_shouldFilterByTags() throws Exception {
        // Arrange
        List<TaskResponse> tasks = List.of(TestDataFactory.createTaskResponse());
        Page<TaskResponse> taskPage = new PageImpl<>(tasks);

        when(taskService.getTasksWithFilters(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
            .thenReturn(taskPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks")
                .param("tags", "work", "urgent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andDo(document("get-tasks-with-tags-filter",
                queryParameters(
                    parameterWithName("status").description("Filter by task status").optional(),
                    parameterWithName("tags").description("Filter by tag names (OR logic - tasks with ANY of these tags)"),
                    parameterWithName("page").description("Page number").optional(),
                    parameterWithName("size").description("Page size").optional(),
                    parameterWithName("sortBy").description("Sort field").optional(),
                    parameterWithName("sortDirection").description("Sort direction").optional()
                )
            ));
    }

    @Test
    @DisplayName("getAllTasks_withPagination_shouldReturnCorrectPage")
    void getAllTasks_withPagination_shouldReturnCorrectPage() throws Exception {
        // Arrange
        List<TaskResponse> tasks = List.of(TestDataFactory.createTaskResponse());
        Page<TaskResponse> taskPage = new PageImpl<>(tasks);

        when(taskService.getTasksWithFilters(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
            .thenReturn(taskPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks")
                .param("page", "1")
                .param("size", "5")
                .param("sortBy", "title")
                .param("sortDirection", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andDo(document("get-tasks-with-pagination",
                queryParameters(
                    parameterWithName("status").description("Filter by task status").optional(),
                    parameterWithName("tags").description("Filter by tag names").optional(),
                    parameterWithName("page").description("Page number (0-based)"),
                    parameterWithName("size").description("Page size"),
                    parameterWithName("sortBy").description("Sort field"),
                    parameterWithName("sortDirection").description("Sort direction (asc/desc)")
                )
            ));
    }

    // PUT /api/v1/tasks/{id} - Update Task

    @Test
    @DisplayName("updateTask_shouldReturnUpdated")
    void updateTask_shouldReturnUpdated() throws Exception {
        // Arrange
        UpdateTaskRequest request = TestDataFactory.updateTaskRequest();
        TaskResponse response = TestDataFactory.createTaskResponse();

        when(taskService.updateTask(TestDataFactory.TEST_TASK_ID, request)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/tasks/{id}", TestDataFactory.TEST_TASK_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestDataFactory.TEST_TASK_ID.toString()))
            .andExpect(jsonPath("$.title").value("Test Task"))
            .andDo(document("update-task",
                pathParameters(
                    parameterWithName("id").description("Unique task identifier (UUID)")
                ),
                requestFields(
                    fieldWithPath("title").description("Title of the task").type("String").optional(),
                    fieldWithPath("description").description("Description of the task").type("String").optional(),
                    fieldWithPath("status").description("Task status").type("TaskStatus").optional(),
                    fieldWithPath("priority").description("Task priority").type("TaskPriority").optional(),
                    fieldWithPath("dueDate").description("Task due date").type("LocalDateTime").optional(),
                    fieldWithPath("tags").description("Array of tag names").type("List<String>").optional()
                ),
                responseFields(
                    fieldWithPath("id").description("Unique task identifier (UUID)").type("UUID"),
                    fieldWithPath("title").description("Title of the task").type("String"),
                    fieldWithPath("description").description("Description of the task").type("String"),
                    fieldWithPath("status").description("Current task status").type("TaskStatus"),
                    fieldWithPath("priority").description("Task priority").type("TaskPriority"),
                    fieldWithPath("dueDate").description("Task due date").type("LocalDateTime"),
                    fieldWithPath("tags").description("Array of associated tag names").type("List<String>"),
                    fieldWithPath("createdAt").description("Creation timestamp").type("LocalDateTime"),
                    fieldWithPath("updatedAt").description("Last update timestamp").type("LocalDateTime")
                )
            ));
    }

    @Test
    @DisplayName("updateTask_partialUpdate_shouldUpdateFields")
    void updateTask_partialUpdate_shouldUpdateFields() throws Exception {
        // Arrange
        UpdateTaskRequest request = TestDataFactory.partialUpdateTaskRequest();
        TaskResponse response = TestDataFactory.createTaskResponse();

        when(taskService.updateTask(TestDataFactory.TEST_TASK_ID, request)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/tasks/{id}", TestDataFactory.TEST_TASK_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestDataFactory.TEST_TASK_ID.toString()))
            .andDo(document("update-task-partial",
                pathParameters(
                    parameterWithName("id").description("Unique task identifier (UUID)")
                ),
                requestFields(
                    fieldWithPath("title").description("Title of the task").type("String"),
                    fieldWithPath("description").description("Description of the task").type("String").optional(),
                    fieldWithPath("status").description("Task status").type("TaskStatus").optional(),
                    fieldWithPath("priority").description("Task priority").type("TaskPriority").optional(),
                    fieldWithPath("dueDate").description("Task due date").type("LocalDateTime").optional(),
                    fieldWithPath("tags").description("Array of tag names").type("List<String>").optional()
                )
            ));
    }

    @Test
    @DisplayName("updateTask_notFound_shouldReturn404")
    void updateTask_notFound_shouldReturn404() throws Exception {
        // Arrange
        UpdateTaskRequest request = TestDataFactory.updateTaskRequest();
        
        doThrow(new TaskNotFoundException(TestDataFactory.TEST_TASK_ID))
            .when(taskService).updateTask(TestDataFactory.TEST_TASK_ID, request);

        // Act & Assert
        mockMvc.perform(put("/api/v1/tasks/{id}", TestDataFactory.TEST_TASK_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Task Not Found"))
            .andDo(document("update-task-not-found",
                pathParameters(
                    parameterWithName("id").description("Unique task identifier (UUID)")
                ),
                requestFields(
                    fieldWithPath("title").description("Title of the task").type("String").optional(),
                    fieldWithPath("description").description("Description of the task").type("String").optional(),
                    fieldWithPath("status").description("Task status").type("TaskStatus").optional(),
                    fieldWithPath("priority").description("Task priority").type("TaskPriority").optional(),
                    fieldWithPath("dueDate").description("Task due date").type("LocalDateTime").optional(),
                    fieldWithPath("tags").description("Array of tag names").type("List<String>").optional()
                ),
                responseFields(
                    fieldWithPath("status").description("HTTP status code").type("int"),
                    fieldWithPath("error").description("Error type").type("String"),
                    fieldWithPath("message").description("Error message").type("String"),
                    fieldWithPath("timestamp").description("Error timestamp").type("LocalDateTime"),
                    fieldWithPath("taskId").description("Requested task ID").type("String")
                )
            ));
    }

    // DELETE /api/v1/tasks/{id} - Delete Task

    @Test
    @DisplayName("deleteTask_shouldReturnNoContent")
    void deleteTask_shouldReturnNoContent() throws Exception {
        // Arrange
        // No need to mock void method

        // Act & Assert
        mockMvc.perform(delete("/api/v1/tasks/{id}", TestDataFactory.TEST_TASK_ID))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""))
            .andDo(document("delete-task",
                pathParameters(
                    parameterWithName("id").description("Unique task identifier (UUID)")
                )
            ));
    }

    @Test
    @DisplayName("deleteTask_notFound_shouldReturn404")
    void deleteTask_notFound_shouldReturn404() throws Exception {
        // Arrange
        doThrow(new TaskNotFoundException(TestDataFactory.TEST_TASK_ID))
            .when(taskService).deleteTask(TestDataFactory.TEST_TASK_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/tasks/{id}", TestDataFactory.TEST_TASK_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Task Not Found"))
            .andDo(document("delete-task-not-found",
                pathParameters(
                    parameterWithName("id").description("Unique task identifier (UUID)")
                ),
                responseFields(
                    fieldWithPath("status").description("HTTP status code").type("int"),
                    fieldWithPath("error").description("Error type").type("String"),
                    fieldWithPath("message").description("Error message").type("String"),
                    fieldWithPath("timestamp").description("Error timestamp").type("LocalDateTime"),
                    fieldWithPath("taskId").description("Requested task ID").type("String")
                )
            ));
    }

    @Test
    @DisplayName("updateTask_concurrentUpdate_shouldReturn409Conflict")
    void updateTask_concurrentUpdate_shouldReturn409Conflict() throws Exception {
        // Arrange
        UpdateTaskRequest request = TestDataFactory.updateTaskRequest();
        
        doThrow(new ObjectOptimisticLockingFailureException("Task", TestDataFactory.TEST_TASK_ID))
            .when(taskService).updateTask(TestDataFactory.TEST_TASK_ID, request);

        // Act & Assert
        mockMvc.perform(put("/api/v1/tasks/{id}", TestDataFactory.TEST_TASK_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Concurrent Update Conflict"))
            .andExpect(jsonPath("$.message").value("The resource was modified by another user. Please refresh and try again."))
            .andExpect(jsonPath("$.details").value("This error occurs when multiple users try to update the same resource simultaneously. Please reload the data and try your update again."))
            .andDo(document("update-task-concurrent-conflict",
                pathParameters(
                    parameterWithName("id").description("Unique task identifier (UUID)")
                ),
                requestFields(
                    fieldWithPath("title").description("Updated task title").type("String").optional(),
                    fieldWithPath("description").description("Updated task description").type("String").optional(),
                    fieldWithPath("status").description("Updated task status").type("TaskStatus").optional(),
                    fieldWithPath("priority").description("Updated task priority").type("TaskPriority").optional(),
                    fieldWithPath("dueDate").description("Updated task due date").type("LocalDateTime").optional(),
                    fieldWithPath("tags").description("Updated list of tag names").type("List<String>").optional()
                ),
                responseFields(
                    fieldWithPath("status").description("HTTP status code").type("int"),
                    fieldWithPath("error").description("Error type").type("String"),
                    fieldWithPath("message").description("Error message").type("String"),
                    fieldWithPath("timestamp").description("Error timestamp").type("LocalDateTime"),
                    fieldWithPath("details").description("Detailed explanation of the conflict").type("String")
                )
            ));
    }
}
