package com.ignium.taskmanager.task.dto;

import com.ignium.taskmanager.task.entity.Task;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateTaskRequest(
    String title,
    
    String description,
    
    Task.TaskStatus status,
    
    Task.TaskPriority priority,
    
    LocalDateTime dueDate,
    
    List<String> tags
) {
}