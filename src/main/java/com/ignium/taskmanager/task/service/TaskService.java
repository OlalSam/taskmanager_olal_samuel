package com.ignium.taskmanager.task.service;

import com.ignium.taskmanager.config.exception.TaskNotFoundException;
import com.ignium.taskmanager.task.dto.CreateTaskRequest;
import com.ignium.taskmanager.task.dto.TaskResponse;
import com.ignium.taskmanager.task.dto.UpdateTaskRequest;
import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.task.entity.Tag;
import com.ignium.taskmanager.task.event.model.TaskCreatedEvent;
import com.ignium.taskmanager.task.event.model.TaskDeletedEvent;
import com.ignium.taskmanager.task.event.model.TaskUpdatedEvent;
import com.ignium.taskmanager.task.repository.TaskRepository;
import com.ignium.taskmanager.task.repository.TagRepository;
import com.ignium.taskmanager.user.service.UserContextService;
import com.ignium.taskmanager.user.service.UserSyncService;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final UserContextService userContextService;
    private final UserSyncService userSyncService;
    private final ApplicationEventPublisher eventPublisher;

    private Set<Tag> findOrCreateTags(List<String> tagNames, UUID userId) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<Tag> tags = new HashSet<>();
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByNameAndUserId(tagName, userId)
                .orElseGet(() -> {
                    Tag newTag = Tag.builder()
                        .name(tagName)
                        .userId(userId)
                        .build();
                    return tagRepository.save(newTag);
                });
            tags.add(tag);
        }
        return tags;
    }
    
    /**
     * Ensures the user exists in the database. UserSyncFilter should have already created them,
     * but this provides a fallback mechanism.
     */
    private void ensureUserExists(UUID userId) {
        // Just check if user exists - UserSyncFilter with upsert should have created them
        if (!userSyncService.userExistsNonTransactional(userId)) {
            log.warn("User {} does not exist in database. UserSyncFilter may have failed.", userId);
        }
    }
    
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        
        UUID currentUserId = userContextService.getCurrentUserId();
        
        ensureUserExists(currentUserId);
        Task task = Task.builder()
            .userId(currentUserId)
            .title(request.title())
            .description(request.description())
            .status(request.status())
            .priority(request.priority())
            .dueDate(request.dueDate())
            .tags(findOrCreateTags(request.tags(), currentUserId))
            .build();
        
        task = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskCreatedEvent(task));
        
        log.info("Task created successfully with ID: {}", task.getId());
        return TaskResponse.from(task);
    }
    
    public TaskResponse getTaskById(UUID id) {
        log.info("Fetching task with ID: {}", id);
        
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Task task = taskRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        
        return TaskResponse.from(task);
    }
    
    public List<TaskResponse> getAllTasks() {
        log.info("Fetching all tasks for current user");
        
        UUID currentUserId = userContextService.getCurrentUserId();
        
        return taskRepository.findAllByUserId(currentUserId, Pageable.unpaged()).getContent().stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());
    }
    
    public Page<TaskResponse> getTasksPaginated(int page, int size, String sortBy, String sortDirection) {
        log.info("Fetching tasks with pagination - page: {}, size: {}, sortBy: {}, sortDirection: {}", 
                page, size, sortBy, sortDirection);
        
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Task> tasks = taskRepository.findAllByUserId(currentUserId, pageable);
        
        return tasks.map(TaskResponse::from);
    }
    
    public Page<TaskResponse> getTasksWithFilters(Task.TaskStatus status, List<String> tagNames, 
                                                    int page, int size, String sortBy, String sortDirection) {
        log.info("Fetching tasks with filters - status: {}, tags: {}, page: {}, size: {}, sortBy: {}, sortDirection: {}", 
                status, tagNames, page, size, sortBy, sortDirection);
        
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Handle empty tagNames list - if empty, pass null to query to avoid filtering by tags
        List<String> effectiveTagNames = (tagNames == null || tagNames.isEmpty()) ? null : tagNames;
        
        Page<Task> tasks = taskRepository.findTasksWithFilters(currentUserId, status, effectiveTagNames, pageable);
        
        return tasks.map(TaskResponse::from);
    }
    
    @Transactional
    public TaskResponse updateTask(UUID id, UpdateTaskRequest request) {
        log.info("Updating task with ID: {}", id);
        
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Task task = taskRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.tags() != null) {
            task.setTags(findOrCreateTags(request.tags(), currentUserId));
        }
        
        task = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskUpdatedEvent(task));
        
        log.info("Task updated successfully with ID: {}", task.getId());
        return TaskResponse.from(task);
    }
    
    @Transactional
    public void deleteTask(UUID id) {
        log.info("Deleting task with ID: {}", id);
        
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Task task = taskRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        
        eventPublisher.publishEvent(new TaskDeletedEvent(task));
        
        taskRepository.delete(task);
        
        log.info("Task deleted successfully with ID: {}", id);
    }
}