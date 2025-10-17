package com.ignium.taskmanager.task.controller;

import com.ignium.taskmanager.task.dto.CreateTaskRequest;
import com.ignium.taskmanager.task.dto.PaginatedResponse;
import com.ignium.taskmanager.task.dto.TaskResponse;
import com.ignium.taskmanager.task.dto.UpdateTaskRequest;
import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + response.id())).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable UUID id) {
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<PaginatedResponse<TaskResponse>> getAllTasks(
            @RequestParam(required = false) Task.TaskStatus status,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Page<TaskResponse> taskPage = taskService.getTasksWithFilters(status, tags, page, size, sortBy, sortDirection);
        
        PaginatedResponse<TaskResponse> response = new PaginatedResponse<>(
                taskPage.getContent(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                taskPage.getSize(),
                taskPage.getNumber(),
                taskPage.isFirst(),
                taskPage.isLast(),
                taskPage.isEmpty()
        );
        
        return ResponseEntity.ok(response);
    }
    
    // @Deprecated - Use GET /api/v1/tasks with query parameters instead
    // @GetMapping("/paginated")
    // public ResponseEntity<PaginatedResponse<TaskResponse>> getTasksPaginated(
    //         @RequestParam(defaultValue = "0") int page,
    //         @RequestParam(defaultValue = "10") int size,
    //         @RequestParam(defaultValue = "createdAt") String sortBy,
    //         @RequestParam(defaultValue = "desc") String sortDirection) {
    //     Page<TaskResponse> taskPage = taskService.getTasksPaginated(page, size, sortBy, sortDirection);
    //     
    //     PaginatedResponse<TaskResponse> response = new PaginatedResponse<>(
    //             taskPage.getContent(),
    //             taskPage.getTotalElements(),
    //             taskPage.getTotalPages(),
    //             taskPage.getSize(),
    //             taskPage.getNumber(),
    //             taskPage.isFirst(),
    //             taskPage.isLast(),
    //             taskPage.isEmpty()
    //     );
    //     
    //     return ResponseEntity.ok(response);
    // }
    
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}