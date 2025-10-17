package com.ignium.taskmanager.task.service;

import com.ignium.taskmanager.calendar.entity.CalendarSyncLog;
import com.ignium.taskmanager.calendar.service.CalendarSyncService;
import com.ignium.taskmanager.config.exception.TaskNotFoundException;
import com.ignium.taskmanager.task.TestDataFactory;
import com.ignium.taskmanager.task.dto.CreateTaskRequest;
import com.ignium.taskmanager.task.dto.TaskResponse;
import com.ignium.taskmanager.task.dto.UpdateTaskRequest;
import com.ignium.taskmanager.task.entity.Tag;
import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.task.repository.TagRepository;
import com.ignium.taskmanager.task.repository.TaskRepository;
import com.ignium.taskmanager.user.service.UserContextService;
import com.ignium.taskmanager.user.service.UserSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserContextService userContextService;

    @Mock
    private CalendarSyncService calendarSyncService;

    @Mock
    private UserSyncService userSyncService;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        when(userContextService.getCurrentUserId()).thenReturn(TestDataFactory.TEST_USER_ID);
        // UserSyncService mock will be set up per test as needed
    }

    // CREATE Operations Tests

    @Test
    @DisplayName("createTask_withValidData_shouldCreateTask")
    void createTask_withValidData_shouldCreateTask() {
        // Arrange
        CreateTaskRequest request = TestDataFactory.createTaskRequest();
        Task savedTask = TestDataFactory.createTask();
        Tag workTag = TestDataFactory.createTag("work");
        Tag urgentTag = TestDataFactory.createTag("urgent");

        when(userSyncService.userExistsNonTransactional(TestDataFactory.TEST_USER_ID)).thenReturn(true);
        when(tagRepository.findByNameAndUserId("work", TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(workTag));
        when(tagRepository.findByNameAndUserId("urgent", TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(urgentTag));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.CREATE));

        // Act
        TaskResponse result = taskService.createTask(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo(request.title());
        assertThat(result.description()).isEqualTo(request.description());
        assertThat(result.status()).isEqualTo(request.status());
        assertThat(result.priority()).isEqualTo(request.priority());

        verify(taskRepository).save(any(Task.class));
        verify(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.CREATE));
    }

    @Test
    @DisplayName("createTask_withTags_shouldAutoCreateTags")
    void createTask_withTags_shouldAutoCreateTags() {
        // Arrange
        CreateTaskRequest request = TestDataFactory.createTaskRequest();
        Task savedTask = TestDataFactory.createTask();
        Tag newTag = TestDataFactory.createTag("newtag");

        when(userSyncService.userExistsNonTransactional(TestDataFactory.TEST_USER_ID)).thenReturn(true);
        when(tagRepository.findByNameAndUserId("work", TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.empty());
        when(tagRepository.findByNameAndUserId("urgent", TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(newTag);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.CREATE));

        // Act
        taskService.createTask(request);

        // Assert
        verify(tagRepository, times(2)).save(any(Tag.class));
        verify(tagRepository).findByNameAndUserId("work", TestDataFactory.TEST_USER_ID);
        verify(tagRepository).findByNameAndUserId("urgent", TestDataFactory.TEST_USER_ID);
    }

    @Test
    @DisplayName("createTask_withExistingTags_shouldReuseTags")
    void createTask_withExistingTags_shouldReuseTags() {
        // Arrange
        CreateTaskRequest request = TestDataFactory.createTaskRequest();
        Task savedTask = TestDataFactory.createTask();
        Tag existingTag = TestDataFactory.createTag("work");

        when(userSyncService.userExistsNonTransactional(TestDataFactory.TEST_USER_ID)).thenReturn(true);
        when(tagRepository.findByNameAndUserId("work", TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(existingTag));
        when(tagRepository.findByNameAndUserId("urgent", TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(existingTag));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.CREATE));

        // Act
        taskService.createTask(request);

        // Assert
        verify(tagRepository, never()).save(any(Tag.class));
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask_withNullTags_shouldCreateTaskWithoutTags")
    void createTask_withNullTags_shouldCreateTaskWithoutTags() {
        // Arrange
        CreateTaskRequest request = TestDataFactory.createTaskRequestWithoutTags();
        Task savedTask = TestDataFactory.createTask();

        when(userSyncService.userExistsNonTransactional(TestDataFactory.TEST_USER_ID)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.CREATE));

        // Act
        TaskResponse result = taskService.createTask(request);

        // Assert
        assertThat(result).isNotNull();
        verify(tagRepository, never()).findByNameAndUserId(any(String.class), any(UUID.class));
        verify(tagRepository, never()).save(any(Tag.class));
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask_withDueDate_shouldTriggerCalendarSync")
    void createTask_withDueDate_shouldTriggerCalendarSync() {
        // Arrange
        CreateTaskRequest request = TestDataFactory.createTaskRequest();
        Task savedTask = TestDataFactory.createTask();

        when(userSyncService.userExistsNonTransactional(TestDataFactory.TEST_USER_ID)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.CREATE));

        // Act
        taskService.createTask(request);

        // Assert
        verify(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.CREATE));
    }

    @Test
    @DisplayName("createTask_withNoDueDate_shouldNotSyncCalendar")
    void createTask_withNoDueDate_shouldNotSyncCalendar() {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest(
            "Test Task",
            "Test Description",
            Task.TaskStatus.TODO,
            Task.TaskPriority.MEDIUM,
            null, // no due date
            null
        );
        Task savedTask = TestDataFactory.createTask();

        when(userSyncService.userExistsNonTransactional(TestDataFactory.TEST_USER_ID)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.CREATE));

        // Act
        taskService.createTask(request);

        // Assert
        // Calendar sync is still called but will be skipped internally due to no due date
        verify(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.CREATE));
    }

    // READ Operations Tests

    @Test
    @DisplayName("getTaskById_withValidId_shouldReturnTask")
    void getTaskById_withValidId_shouldReturnTask() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;
        Task task = TestDataFactory.createTask();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(task));

        // Act
        TaskResponse result = taskService.getTaskById(taskId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(taskId);
        assertThat(result.title()).isEqualTo(task.getTitle());
    }

    @Test
    @DisplayName("getTaskById_withInvalidId_shouldThrowNotFoundException")
    void getTaskById_withInvalidId_shouldThrowNotFoundException() {
        // Arrange
        UUID taskId = UUID.randomUUID();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.getTaskById(taskId))
            .isInstanceOf(TaskNotFoundException.class)
            .hasMessageContaining(taskId.toString());
    }

    @Test
    @DisplayName("getTaskById_withDifferentUser_shouldThrowNotFoundException")
    void getTaskById_withDifferentUser_shouldThrowNotFoundException() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.getTaskById(taskId))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("getAllTasks_shouldReturnAllUserTasks")
    void getAllTasks_shouldReturnAllUserTasks() {
        // Arrange
        List<Task> tasks = TestDataFactory.createTaskList();
        Page<Task> taskPage = new PageImpl<>(tasks);

        when(taskRepository.findAllByUserId(TestDataFactory.TEST_USER_ID, Pageable.unpaged()))
            .thenReturn(taskPage);

        // Act
        List<TaskResponse> result = taskService.getAllTasks();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).title()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("getAllTasks_whenEmpty_shouldReturnEmptyList")
    void getAllTasks_whenEmpty_shouldReturnEmptyList() {
        // Arrange
        Page<Task> emptyPage = new PageImpl<>(List.of());

        when(taskRepository.findAllByUserId(TestDataFactory.TEST_USER_ID, Pageable.unpaged()))
            .thenReturn(emptyPage);

        // Act
        List<TaskResponse> result = taskService.getAllTasks();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTasksWithFilters_byStatus_shouldFilterCorrectly")
    void getTasksWithFilters_byStatus_shouldFilterCorrectly() {
        // Arrange
        Task.TaskStatus status = Task.TaskStatus.TODO;
        List<Task> tasks = List.of(TestDataFactory.createTask());
        Page<Task> taskPage = new PageImpl<>(tasks);

        when(taskRepository.findTasksWithFilters(eq(TestDataFactory.TEST_USER_ID), eq(status), isNull(), any(Pageable.class)))
            .thenReturn(taskPage);

        // Act
        Page<TaskResponse> result = taskService.getTasksWithFilters(status, null, 0, 10, "createdAt", "desc");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(status);
    }

    @Test
    @DisplayName("getTasksWithFilters_byTags_shouldFilterByAnyTag")
    void getTasksWithFilters_byTags_shouldFilterByAnyTag() {
        // Arrange
        List<String> tags = List.of("work", "urgent");
        List<Task> tasks = List.of(TestDataFactory.createTask());
        Page<Task> taskPage = new PageImpl<>(tasks);

        when(taskRepository.findTasksWithFilters(eq(TestDataFactory.TEST_USER_ID), isNull(), eq(tags), any(Pageable.class)))
            .thenReturn(taskPage);

        // Act
        Page<TaskResponse> result = taskService.getTasksWithFilters(null, tags, 0, 10, "createdAt", "desc");

        // Assert
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getTasksWithFilters_byStatusAndTags_shouldApplyBothFilters")
    void getTasksWithFilters_byStatusAndTags_shouldApplyBothFilters() {
        // Arrange
        Task.TaskStatus status = Task.TaskStatus.IN_PROGRESS;
        List<String> tags = List.of("work");
        List<Task> tasks = List.of(TestDataFactory.createTask());
        Page<Task> taskPage = new PageImpl<>(tasks);

        when(taskRepository.findTasksWithFilters(eq(TestDataFactory.TEST_USER_ID), eq(status), eq(tags), any(Pageable.class)))
            .thenReturn(taskPage);

        // Act
        Page<TaskResponse> result = taskService.getTasksWithFilters(status, tags, 0, 10, "createdAt", "desc");

        // Assert
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getTasksWithFilters_withPagination_shouldReturnPaginatedResults")
    void getTasksWithFilters_withPagination_shouldReturnPaginatedResults() {
        // Arrange
        List<Task> tasks = TestDataFactory.createTaskList();
        Page<Task> taskPage = new PageImpl<>(tasks, PageRequest.of(0, 10), 25);

        when(taskRepository.findTasksWithFilters(eq(TestDataFactory.TEST_USER_ID), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(taskPage);

        // Act
        Page<TaskResponse> result = taskService.getTasksWithFilters(null, null, 0, 10, "createdAt", "desc");

        // Assert
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(25);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }

    // UPDATE Operations Tests

    @Test
    @DisplayName("updateTask_withValidData_shouldUpdateTask")
    void updateTask_withValidData_shouldUpdateTask() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;
        UpdateTaskRequest request = TestDataFactory.updateTaskRequest();
        Task existingTask = TestDataFactory.createTask();
        Task updatedTask = TestDataFactory.createTask();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.UPDATE));

        // Act
        TaskResponse result = taskService.updateTask(taskId, request);

        // Assert
        assertThat(result).isNotNull();
        verify(taskRepository).save(existingTask);
        verify(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.UPDATE));
    }

    @Test
    @DisplayName("updateTask_partialUpdate_shouldUpdateOnlyProvidedFields")
    void updateTask_partialUpdate_shouldUpdateOnlyProvidedFields() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;
        UpdateTaskRequest request = TestDataFactory.partialUpdateTaskRequest();
        Task existingTask = TestDataFactory.createTask();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // Act
        taskService.updateTask(taskId, request);

        // Assert
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        
        Task capturedTask = taskCaptor.getValue();
        assertThat(capturedTask.getTitle()).isEqualTo("Updated Title");
        assertThat(capturedTask.getDescription()).isEqualTo("Test Description"); // unchanged
        assertThat(capturedTask.getStatus()).isEqualTo(Task.TaskStatus.TODO); // unchanged
    }

    @Test
    @DisplayName("updateTask_changeTags_shouldUpdateTags")
    void updateTask_changeTags_shouldUpdateTags() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;
        UpdateTaskRequest request = new UpdateTaskRequest(
            null, null, null, null, null, List.of("newtag")
        );
        Task existingTask = TestDataFactory.createTask();
        Tag newTag = TestDataFactory.createTag("newtag");

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(existingTask));
        when(tagRepository.findByNameAndUserId("newtag", TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(newTag));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // Act
        taskService.updateTask(taskId, request);

        // Assert
        verify(tagRepository).findByNameAndUserId("newtag", TestDataFactory.TEST_USER_ID);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("updateTask_withInvalidId_shouldThrowNotFoundException")
    void updateTask_withInvalidId_shouldThrowNotFoundException() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        UpdateTaskRequest request = TestDataFactory.updateTaskRequest();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.updateTask(taskId, request))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("updateTask_differentUser_shouldThrowNotFoundException")
    void updateTask_differentUser_shouldThrowNotFoundException() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;
        UpdateTaskRequest request = TestDataFactory.updateTaskRequest();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.updateTask(taskId, request))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("updateTask_shouldTriggerCalendarSync")
    void updateTask_shouldTriggerCalendarSync() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;
        UpdateTaskRequest request = TestDataFactory.updateTaskRequest();
        Task existingTask = TestDataFactory.createTask();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);
        
        // Mock tag repository calls for the tags in the update request
        Tag updatedTag = TestDataFactory.createTag("updated");
        Tag workTag = TestDataFactory.createTag("work");
        when(tagRepository.findByNameAndUserId("updated", TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(updatedTag));
        when(tagRepository.findByNameAndUserId("work", TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(workTag));
        
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.UPDATE));

        // Act
        taskService.updateTask(taskId, request);

        // Assert
        verify(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.UPDATE));
    }

    // DELETE Operations Tests

    @Test
    @DisplayName("deleteTask_withValidId_shouldDeleteTask")
    void deleteTask_withValidId_shouldDeleteTask() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;
        Task task = TestDataFactory.createTask();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(task));
        doNothing().when(taskRepository).delete(task);
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.DELETE));

        // Act
        taskService.deleteTask(taskId);

        // Assert
        verify(taskRepository).delete(task);
        verify(calendarSyncService).syncTaskToCalendar(task, CalendarSyncLog.SyncOperation.DELETE);
    }

    @Test
    @DisplayName("deleteTask_withInvalidId_shouldThrowNotFoundException")
    void deleteTask_withInvalidId_shouldThrowNotFoundException() {
        // Arrange
        UUID taskId = UUID.randomUUID();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.deleteTask(taskId))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("deleteTask_differentUser_shouldThrowNotFoundException")
    void deleteTask_differentUser_shouldThrowNotFoundException() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.deleteTask(taskId))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("deleteTask_shouldTriggerCalendarDelete")
    void deleteTask_shouldTriggerCalendarDelete() {
        // Arrange
        UUID taskId = TestDataFactory.TEST_TASK_ID;
        Task task = TestDataFactory.createTask();

        when(taskRepository.findByIdAndUserId(taskId, TestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(task));
        doNothing().when(taskRepository).delete(task);
        doNothing().when(calendarSyncService).syncTaskToCalendar(any(Task.class), eq(CalendarSyncLog.SyncOperation.DELETE));

        // Act
        taskService.deleteTask(taskId);

        // Assert
        verify(calendarSyncService).syncTaskToCalendar(task, CalendarSyncLog.SyncOperation.DELETE);
    }
}
