package com.ignium.taskmanager.task.dto;

import com.ignium.taskmanager.task.entity.Tag;

import java.time.LocalDateTime;
import java.util.UUID;

public record TagResponse(
    UUID id,
    String name,
    String color,
    int taskCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(
            tag.getId(),
            tag.getName(),
            tag.getColor(),
            tag.getTaskCount(), // Uses @Transient method
            tag.getCreatedAt(),
            tag.getUpdatedAt()
        );
    }
}
