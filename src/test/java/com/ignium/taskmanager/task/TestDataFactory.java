package com.ignium.taskmanager.task;

import com.ignium.taskmanager.task.dto.CreateTaskRequest;
import com.ignium.taskmanager.task.dto.TagDetailResponse;
import com.ignium.taskmanager.task.dto.TagResponse;
import com.ignium.taskmanager.task.dto.TaskResponse;
import com.ignium.taskmanager.task.dto.UpdateTaskRequest;
import com.ignium.taskmanager.task.entity.Tag;
import com.ignium.taskmanager.task.entity.Task;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TestDataFactory {

    // Test UUIDs for consistent testing
    public static final UUID TEST_USER_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");
    public static final UUID TEST_TASK_ID = UUID.fromString("87654321-4321-4321-4321-210987654321");
    public static final UUID TEST_TAG_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    public static final UUID OTHER_USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    // Task builders
    public static Task.TaskBuilder defaultTaskBuilder() {
        return Task.builder()
            .id(TEST_TASK_ID)
            .userId(TEST_USER_ID)
            .title("Test Task")
            .description("Test Description")
            .status(Task.TaskStatus.TODO)
            .priority(Task.TaskPriority.MEDIUM)
            .dueDate(LocalDateTime.now().plusDays(1))
            .createdAt(LocalDateTime.now().minusDays(1))
            .updatedAt(LocalDateTime.now())
            .tags(Set.of());
    }

    public static Task createTask() {
        return defaultTaskBuilder().build();
    }

    public static Task createTaskWithTags() {
        Tag tag1 = createTag("work");
        Tag tag2 = createTag("urgent");
        return defaultTaskBuilder()
            .tags(Set.of(tag1, tag2))
            .build();
    }

    public static Task createTaskWithDifferentUser() {
        return defaultTaskBuilder()
            .id(UUID.fromString("99999999-9999-9999-9999-999999999999"))
            .userId(OTHER_USER_ID)
            .build();
    }

    // Tag builders
    public static Tag.TagBuilder defaultTagBuilder() {
        return Tag.builder()
            .id(TEST_TAG_ID)
            .userId(TEST_USER_ID)
            .name("test-tag")
            .color("#0088CC")
            .createdAt(LocalDateTime.now().minusDays(1))
            .updatedAt(LocalDateTime.now())
            .tasks(Set.of());
    }

    public static Tag createTag() {
        return defaultTagBuilder().build();
    }

    public static Tag createTag(String name) {
        return defaultTagBuilder()
            .name(name)
            .build();
    }

    public static Tag createTagWithTasks() {
        Task task1 = createTask();
        Task task2 = createTask();
        return defaultTagBuilder()
            .tasks(new HashSet<>(Set.of(task1, task2)))
            .build();
    }

    // Request builders
    public static CreateTaskRequest createTaskRequest() {
        return new CreateTaskRequest(
            "Test Task",
            "Test Description",
            Task.TaskStatus.TODO,
            Task.TaskPriority.MEDIUM,
            LocalDateTime.now().plusDays(1),
            List.of("work", "urgent")
        );
    }

    public static CreateTaskRequest createTaskRequestWithoutTags() {
        return new CreateTaskRequest(
            "Test Task",
            "Test Description",
            Task.TaskStatus.TODO,
            Task.TaskPriority.MEDIUM,
            LocalDateTime.now().plusDays(1),
            null
        );
    }

    public static UpdateTaskRequest updateTaskRequest() {
        return new UpdateTaskRequest(
            "Updated Task",
            "Updated Description",
            Task.TaskStatus.IN_PROGRESS,
            Task.TaskPriority.HIGH,
            LocalDateTime.now().plusDays(2),
            List.of("updated", "work")
        );
    }

    public static UpdateTaskRequest partialUpdateTaskRequest() {
        return new UpdateTaskRequest(
            "Updated Title",
            null,
            null,
            null,
            null,
            null
        );
    }

    // Response builders
    public static TaskResponse createTaskResponse() {
        return new TaskResponse(
            TEST_TASK_ID,
            "Test Task",
            "Test Description",
            Task.TaskStatus.TODO,
            Task.TaskPriority.MEDIUM,
            LocalDateTime.now().plusDays(1),
            List.of("work", "urgent"),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now()
        );
    }

    public static TagResponse createTagResponse() {
        return new TagResponse(
            TEST_TAG_ID,
            "test-tag",
            "#0088CC",
            2,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now()
        );
    }

    public static TagDetailResponse createTagDetailResponse() {
        return new TagDetailResponse(
            TEST_TAG_ID,
            "test-tag",
            "#0088CC",
            List.of(
                new TagDetailResponse.TaskSummary(
                    TEST_TASK_ID,
                    "Test Task",
                    Task.TaskStatus.TODO,
                    Task.TaskPriority.MEDIUM,
                    LocalDateTime.now().plusDays(1)
                )
            ),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now()
        );
    }

    // Test data lists
    public static List<Task> createTaskList() {
        return List.of(
            createTask(),
            defaultTaskBuilder().id(UUID.randomUUID()).title("Task 2").build(),
            defaultTaskBuilder().id(UUID.randomUUID()).title("Task 3").build()
        );
    }

    public static List<Tag> createTagList() {
        return List.of(
            createTag("work"),
            createTag("urgent"),
            createTag("personal")
        );
    }

    public static List<TagResponse> createTagResponseList() {
        return List.of(
            new TagResponse(UUID.randomUUID(), "work", "#0088CC", 3, LocalDateTime.now(), LocalDateTime.now()),
            new TagResponse(UUID.randomUUID(), "urgent", "#FF0000", 1, LocalDateTime.now(), LocalDateTime.now()),
            new TagResponse(UUID.randomUUID(), "personal", "#00FF00", 2, LocalDateTime.now(), LocalDateTime.now())
        );
    }
}
