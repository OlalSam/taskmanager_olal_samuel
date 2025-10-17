package com.ignium.taskmanager.calendar.service;

import com.google.api.services.calendar.model.Event;
import com.ignium.taskmanager.calendar.entity.CalendarSyncLog;
import com.ignium.taskmanager.calendar.repository.CalendarSyncLogRepository;
import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.task.repository.TaskRepository;
import com.ignium.taskmanager.user.entity.AppUser;
import com.ignium.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalendarSyncService {
    
    private final GoogleCalendarService googleCalendarService;
    private final TaskRepository taskRepository;
    private final CalendarSyncLogRepository syncLogRepository;
    private final UserRepository userRepository;
    
    @Async
    @Transactional
    @Retryable(
        retryFor = {IOException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void syncTaskToCalendar(Task task, CalendarSyncLog.SyncOperation operation) {
        
        // Check if user has calendar connected
        AppUser user = userRepository.findById(task.getUserId()).orElse(null);
        if (user == null || !user.isCalendarConnected()) {
            log.info("User {} doesn't have calendar connected, skipping sync", task.getUserId());
            return;
        }
        
        // Skip tasks without due dates
        if (task.getDueDate() == null) {
            log.info("Task {} has no due date, skipping calendar sync", task.getId());
            return;
        }
        
        // For DELETE operations, check if task still exists
        if (operation == CalendarSyncLog.SyncOperation.DELETE) {
            if (!taskRepository.existsById(task.getId())) {
                log.info("Task {} already deleted, skipping calendar sync", task.getId());
                return;
            }
        }
        
        try {
            switch (operation) {
                case CREATE -> createCalendarEvent(task);
                case UPDATE -> updateCalendarEvent(task);
                case DELETE -> deleteCalendarEvent(task);
                case CALENDAR_UPDATE -> {
                    log.info("CALENDAR_UPDATE operation should not be processed by CalendarSyncService");
                }
            }
            logSync(task.getId(), operation, CalendarSyncLog.SyncStatus.SUCCESS, null);
        } catch (Exception e) {
            log.error("Sync failed for task {}: {}", task.getId(), e.getMessage());
            logSync(task.getId(), operation, CalendarSyncLog.SyncStatus.FAILED, e.getMessage());
            throw new RuntimeException(e); // Trigger retry
        }
    }
    
    private void createCalendarEvent(Task task) throws IOException {
        Event event = googleCalendarService.createEvent(
            task.getUserId(),
            task.getTitle(),
            task.getDescription(),
            task.getDueDate()
        );
        
        taskRepository.upsertTaskCalendarInfo(task.getId(), event.getId(), LocalDateTime.now());
    }
    
    private void updateCalendarEvent(Task task) throws IOException {
        if (task.getCalendarEventId() == null) {
            createCalendarEvent(task);
            return;
        }
        
        googleCalendarService.updateEvent(
            task.getUserId(),
            task.getCalendarEventId(),
            task.getTitle(),
            task.getDescription(),
            task.getDueDate()
        );
        
        taskRepository.upsertTaskCalendarInfo(task.getId(), task.getCalendarEventId(), LocalDateTime.now());
    }
    
    private void deleteCalendarEvent(Task task) throws IOException {
        if (task.getCalendarEventId() != null) {
            googleCalendarService.deleteEvent(task.getUserId(), task.getCalendarEventId());
        }
    }
    
    private void logSync(UUID taskId, CalendarSyncLog.SyncOperation operation, 
                        CalendarSyncLog.SyncStatus status, String error) {
        CalendarSyncLog log = CalendarSyncLog.builder()
            .taskId(taskId)
            .operation(operation)
            .status(status)
            .errorMessage(error)
            .syncedAt(LocalDateTime.now())
            .build();
        syncLogRepository.save(log);
    }
}
