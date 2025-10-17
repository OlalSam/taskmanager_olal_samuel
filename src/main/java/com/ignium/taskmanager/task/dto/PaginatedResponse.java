package com.ignium.taskmanager.task.dto;

import java.util.List;

public record PaginatedResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int size,
    int number,
    boolean first,
    boolean last,
    boolean empty
) {
}