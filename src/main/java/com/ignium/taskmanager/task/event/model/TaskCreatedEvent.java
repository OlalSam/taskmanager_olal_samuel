package com.ignium.taskmanager.task.event.model;

import com.ignium.taskmanager.task.entity.Task;
import lombok.Getter;

@Getter
public class TaskCreatedEvent {

    private final Task task;

    public TaskCreatedEvent(Task task) {
        this.task = task;
    }
}
