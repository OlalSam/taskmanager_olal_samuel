package com.ignium.taskmanager.config.exception;

import java.util.UUID;

public class TagNotFoundException extends RuntimeException {
    
    private final UUID tagId;
    
    public TagNotFoundException(UUID tagId) {
        super("Tag not found: " + tagId);
        this.tagId = tagId;
    }
    
    public UUID getTagId() {
        return tagId;
    }
}
