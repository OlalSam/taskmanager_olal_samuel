package com.ignium.taskmanager.task.dto;

import com.ignium.taskmanager.task.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateTaskRequest(
    @NotBlank(message = "Task title is required")
    String title,
    
    String description,
    
    Task.TaskStatus status,
    
    @NotNull(message = "Task priority is required")
    Task.TaskPriority priority,
    
    LocalDateTime dueDate,
    
    List<String> tags
) {
    public CreateTaskRequest {
        if (status == null) {
            status = Task.TaskStatus.TODO;
        }
    }
}