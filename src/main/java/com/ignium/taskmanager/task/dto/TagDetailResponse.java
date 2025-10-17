package com.ignium.taskmanager.task.dto;

import com.ignium.taskmanager.task.entity.Tag;
import com.ignium.taskmanager.task.entity.Task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record TagDetailResponse(
    UUID id,
    String name,
    String color,
    List<TaskSummary> tasks,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record TaskSummary(
        UUID id,
        String title,
        Task.TaskStatus status,
        Task.TaskPriority priority,
        LocalDateTime dueDate
    ) {}
    
    public static TagDetailResponse from(Tag tag) {
        List<TaskSummary> taskSummaries = tag.getTasks().stream()
            .map(task -> new TaskSummary(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate()
            ))
            .collect(Collectors.toList());
            
        return new TagDetailResponse(
            tag.getId(),
            tag.getName(),
            tag.getColor(),
            taskSummaries,
            tag.getCreatedAt(),
            tag.getUpdatedAt()
        );
    }
    
    public static TagDetailResponse from(Tag tag, UUID userId) {
        // Filter tasks to only include those belonging to the current user
        List<TaskSummary> taskSummaries = tag.getTasks().stream()
            .filter(task -> task.getUserId().equals(userId))
            .map(task -> new TaskSummary(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate()
            ))
            .collect(Collectors.toList());
            
        return new TagDetailResponse(
            tag.getId(),
            tag.getName(),
            tag.getColor(),
            taskSummaries,
            tag.getCreatedAt(),
            tag.getUpdatedAt()
        );
    }
}
