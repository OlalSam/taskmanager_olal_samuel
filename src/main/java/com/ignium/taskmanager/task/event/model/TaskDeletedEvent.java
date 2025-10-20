package com.ignium.taskmanager.task.event.model;

import com.ignium.taskmanager.task.entity.Task;
import lombok.Getter;

@Getter
public class TaskDeletedEvent {

    private final Task task;

    public TaskDeletedEvent(Task task) {
        this.task = task;
    }
}
