package com.ignium.taskmanager.task.dto;

import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.task.entity.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    Task.TaskStatus status,
    Task.TaskPriority priority,
    LocalDateTime dueDate,
    List<String> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        // Convert Set<Tag> to List<String> of tag names
        List<String> tagNames = task.getTags() != null 
            ? task.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList())
            : List.of();
            
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate(),
            tagNames,
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}