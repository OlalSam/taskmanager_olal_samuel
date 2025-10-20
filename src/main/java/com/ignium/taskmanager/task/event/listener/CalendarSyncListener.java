package com.ignium.taskmanager.task.event.listener;

import com.ignium.taskmanager.calendar.entity.CalendarSyncLog;
import com.ignium.taskmanager.calendar.service.CalendarSyncService;
import com.ignium.taskmanager.task.event.model.TaskCreatedEvent;
import com.ignium.taskmanager.task.event.model.TaskDeletedEvent;
import com.ignium.taskmanager.task.event.model.TaskUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarSyncListener {

    private final CalendarSyncService calendarSyncService;

    @Async
    @EventListener
    public void handleTaskCreatedEvent(TaskCreatedEvent event) throws IOException {
        log.info("Handling task created event for task ID: {}", event.getTask().getId());
        calendarSyncService.syncTaskToCalendar(event.getTask(), CalendarSyncLog.SyncOperation.CREATE);
    }

    @Async
    @EventListener
    public void handleTaskUpdatedEvent(TaskUpdatedEvent event) throws IOException {
        log.info("Handling task updated event for task ID: {}", event.getTask().getId());
        calendarSyncService.syncTaskToCalendar(event.getTask(), CalendarSyncLog.SyncOperation.UPDATE);
    }

    @Async
    @EventListener
    public void handleTaskDeletedEvent(TaskDeletedEvent event) throws IOException {
        log.info("Handling task deleted event for task ID: {}", event.getTask().getId());
        calendarSyncService.syncTaskToCalendar(event.getTask(), CalendarSyncLog.SyncOperation.DELETE);
    }
}
