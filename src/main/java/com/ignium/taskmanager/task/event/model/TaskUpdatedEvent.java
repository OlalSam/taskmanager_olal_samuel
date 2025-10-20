package com.ignium.taskmanager.task.event.model;

import com.ignium.taskmanager.task.entity.Task;
import lombok.Getter;

@Getter
public class TaskUpdatedEvent {

    private final Task task;

    public TaskUpdatedEvent(Task task) {
        this.task = task;
    }
}
