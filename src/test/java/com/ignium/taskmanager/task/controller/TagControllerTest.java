package com.ignium.taskmanager.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ignium.taskmanager.config.TestSecurityConfig;
import com.ignium.taskmanager.config.exception.TagNotFoundException;
import com.ignium.taskmanager.task.TestDataFactory;
import com.ignium.taskmanager.task.dto.TagDetailResponse;
import com.ignium.taskmanager.task.dto.TagResponse;
import com.ignium.taskmanager.task.service.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
@DisplayName("TagController Integration Tests")
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    // GET /api/v1/tags - List All Tags

    @Test
    @DisplayName("getAllTags_shouldReturnTagList")
    void getAllTags_shouldReturnTagList() throws Exception {
        // Arrange
        List<TagResponse> tags = TestDataFactory.createTagResponseList();

        when(tagService.getAllTags()).thenReturn(tags);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("work"))
            .andExpect(jsonPath("$[0].taskCount").value(3))
            .andExpect(jsonPath("$[1].name").value("urgent"))
            .andExpect(jsonPath("$[1].taskCount").value(1))
            .andExpect(jsonPath("$[2].name").value("personal"))
            .andExpect(jsonPath("$[2].taskCount").value(2))
            .andDo(document("get-all-tags",
                responseFields(
                    fieldWithPath("[]").description("Array of tag responses").type("List<TagResponse>"),
                    fieldWithPath("[].id").description("Unique tag identifier (UUID)").type("UUID"),
                    fieldWithPath("[].name").description("Tag name").type("String"),
                    fieldWithPath("[].color").description("Tag color in hex format").type("String"),
                    fieldWithPath("[].taskCount").description("Number of tasks associated with tag").type("int"),
                    fieldWithPath("[].createdAt").description("Creation timestamp").type("LocalDateTime"),
                    fieldWithPath("[].updatedAt").description("Last update timestamp").type("LocalDateTime")
                )
            ));
    }

    @Test
    @DisplayName("getAllTags_withTaskCounts_shouldShowCounts")
    void getAllTags_withTaskCounts_shouldShowCounts() throws Exception {
        // Arrange
        List<TagResponse> tags = TestDataFactory.createTagResponseList();

        when(tagService.getAllTags()).thenReturn(tags);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].taskCount").value(3))
            .andExpect(jsonPath("$[1].taskCount").value(1))
            .andExpect(jsonPath("$[2].taskCount").value(2))
            .andDo(document("get-all-tags-with-counts",
                responseFields(
                    fieldWithPath("[]").description("Array of tag responses").type("List<TagResponse>"),
                    fieldWithPath("[].id").description("Unique tag identifier").type("UUID"),
                    fieldWithPath("[].name").description("Tag name").type("String"),
                    fieldWithPath("[].color").description("Tag color").type("String"),
                    fieldWithPath("[].taskCount").description("Number of tasks associated with tag").type("int"),
                    fieldWithPath("[].createdAt").description("Creation timestamp").type("LocalDateTime"),
                    fieldWithPath("[].updatedAt").description("Last update timestamp").type("LocalDateTime")
                )
            ));
    }

    // GET /api/v1/tags/{id} - Get Tag Details

    @Test
    @DisplayName("getTagById_shouldReturnTagWithTasks")
    void getTagById_shouldReturnTagWithTasks() throws Exception {
        // Arrange
        TagDetailResponse response = TestDataFactory.createTagDetailResponse();

        when(tagService.getTagById(TestDataFactory.TEST_TAG_ID)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tags/{id}", TestDataFactory.TEST_TAG_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestDataFactory.TEST_TAG_ID.toString()))
            .andExpect(jsonPath("$.name").value("test-tag"))
            .andExpect(jsonPath("$.color").value("#0088CC"))
            .andExpect(jsonPath("$.tasks").isArray())
            .andExpect(jsonPath("$.tasks[0].id").value(TestDataFactory.TEST_TASK_ID.toString()))
            .andExpect(jsonPath("$.tasks[0].title").value("Test Task"))
            .andExpect(jsonPath("$.tasks[0].status").value("TODO"))
            .andExpect(jsonPath("$.tasks[0].priority").value("MEDIUM"))
            .andDo(document("get-tag-by-id",
                pathParameters(
                    parameterWithName("id").description("Unique tag identifier (UUID)")
                ),
                responseFields(
                    fieldWithPath("id").description("Unique tag identifier (UUID)").type("UUID"),
                    fieldWithPath("name").description("Tag name").type("String"),
                    fieldWithPath("color").description("Tag color in hex format").type("String"),
                    fieldWithPath("tasks").description("Array of task summaries").type("List<TaskSummary>"),
                    fieldWithPath("tasks[].id").description("Task identifier").type("UUID"),
                    fieldWithPath("tasks[].title").description("Task title").type("String"),
                    fieldWithPath("tasks[].status").description("Task status").type("TaskStatus"),
                    fieldWithPath("tasks[].priority").description("Task priority").type("TaskPriority"),
                    fieldWithPath("tasks[].dueDate").description("Task due date").type("LocalDateTime"),
                    fieldWithPath("createdAt").description("Creation timestamp").type("LocalDateTime"),
                    fieldWithPath("updatedAt").description("Last update timestamp").type("LocalDateTime")
                )
            ));
    }

    @Test
    @DisplayName("getTagById_notFound_shouldReturn404")
    void getTagById_notFound_shouldReturn404() throws Exception {
        // Arrange
        doThrow(new TagNotFoundException(TestDataFactory.TEST_TAG_ID))
            .when(tagService).getTagById(TestDataFactory.TEST_TAG_ID);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tags/{id}", TestDataFactory.TEST_TAG_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Tag Not Found"))
            .andExpect(jsonPath("$.tagId").value(TestDataFactory.TEST_TAG_ID.toString()))
            .andDo(document("get-tag-by-id-not-found",
                pathParameters(
                    parameterWithName("id").description("Unique tag identifier (UUID)")
                ),
                responseFields(
                    fieldWithPath("status").description("HTTP status code").type("int"),
                    fieldWithPath("error").description("Error type").type("String"),
                    fieldWithPath("message").description("Error message").type("String"),
                    fieldWithPath("timestamp").description("Error timestamp").type("LocalDateTime"),
                    fieldWithPath("tagId").description("Requested tag ID").type("String")
                )
            ));
    }

    // DELETE /api/v1/tags/{id} - Delete Tag

    @Test
    @DisplayName("deleteTag_shouldReturnNoContent")
    void deleteTag_shouldReturnNoContent() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/tags/{id}", TestDataFactory.TEST_TAG_ID))
            .andExpect(status().isNoContent())
            .andDo(document("delete-tag",
                pathParameters(
                    parameterWithName("id").description("Unique tag identifier (UUID)")
                )
            ));
    }

    @Test
    @DisplayName("deleteTag_withTasks_shouldReturn400")
    void deleteTag_withTasks_shouldReturn400() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("Cannot delete tag with ID " + TestDataFactory.TEST_TAG_ID + " because it is associated with 2 tasks"))
            .when(tagService).deleteTag(TestDataFactory.TEST_TAG_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/tags/{id}", TestDataFactory.TEST_TAG_ID))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot delete tag")))
            .andDo(document("delete-tag-with-tasks",
                pathParameters(
                    parameterWithName("id").description("Unique tag identifier (UUID)")
                ),
                responseFields(
                    fieldWithPath("status").description("HTTP status code").type("int"),
                    fieldWithPath("error").description("Error type").type("String"),
                    fieldWithPath("message").description("Error message").type("String"),
                    fieldWithPath("timestamp").description("Error timestamp").type("LocalDateTime")
                )
            ));
    }

    @Test
    @DisplayName("deleteTag_notFound_shouldReturn404")
    void deleteTag_notFound_shouldReturn404() throws Exception {
        // Arrange
        doThrow(new TagNotFoundException(TestDataFactory.TEST_TAG_ID))
            .when(tagService).deleteTag(TestDataFactory.TEST_TAG_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/tags/{id}", TestDataFactory.TEST_TAG_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Tag Not Found"))
            .andExpect(jsonPath("$.tagId").value(TestDataFactory.TEST_TAG_ID.toString()))
            .andDo(document("delete-tag-not-found",
                pathParameters(
                    parameterWithName("id").description("Unique tag identifier (UUID)")
                ),
                responseFields(
                    fieldWithPath("status").description("HTTP status code").type("int"),
                    fieldWithPath("error").description("Error type").type("String"),
                    fieldWithPath("message").description("Error message").type("String"),
                    fieldWithPath("timestamp").description("Error timestamp").type("LocalDateTime"),
                    fieldWithPath("tagId").description("Requested tag ID").type("String")
                )
            ));
    }
}
