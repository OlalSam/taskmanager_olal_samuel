package com.ignium.taskmanager.calendar;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.user.entity.AppUser;

import java.time.LocalDateTime;
import java.util.UUID;

public class CalendarTestDataFactory {
    
    public static final UUID TEST_USER_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");
    public static final UUID TEST_TASK_ID = UUID.fromString("87654321-4321-4321-4321-210987654321");
    public static final String TEST_CALENDAR_EVENT_ID = "test-event-123";
    
    public static AppUser createAppUser() {
        return AppUser.builder()
                .id(TEST_USER_ID)
                .email("test@example.com")
                .fullName("Test User")
                .calendarConnected(true)
                .googleAccessToken("test-access-token")
                .googleRefreshToken("test-refresh-token")
                .googleTokenExpiry(LocalDateTime.now().plusHours(1))
                .build();
    }
    
    public static AppUser createAppUserWithoutCalendar() {
        return AppUser.builder()
                .id(TEST_USER_ID)
                .email("test@example.com")
                .fullName("Test User")
                .calendarConnected(false)
                .build();
    }
    
    public static Task createTask() {
        return Task.builder()
                .id(TEST_TASK_ID)
                .userId(TEST_USER_ID)
                .title("Test Task")
                .description("Test Description")
                .status(Task.TaskStatus.TODO)
                .priority(Task.TaskPriority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(1))
                .calendarEventId(TEST_CALENDAR_EVENT_ID)
                .build();
    }
    
    public static Task createTaskWithoutDueDate() {
        return Task.builder()
                .id(TEST_TASK_ID)
                .userId(TEST_USER_ID)
                .title("Test Task")
                .description("Test Description")
                .status(Task.TaskStatus.TODO)
                .priority(Task.TaskPriority.MEDIUM)
                .dueDate(null)
                .build();
    }
    
    public static Task createTaskWithoutCalendarEventId() {
        return Task.builder()
                .id(TEST_TASK_ID)
                .userId(TEST_USER_ID)
                .title("Test Task")
                .description("Test Description")
                .status(Task.TaskStatus.TODO)
                .priority(Task.TaskPriority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(1))
                .calendarEventId(null)
                .build();
    }
    
    public static Event createCalendarEvent() {
        Event event = new Event();
        event.setId(TEST_CALENDAR_EVENT_ID);
        event.setSummary("Test Task");
        event.setDescription("Test Description");
        
        // Set start time
        EventDateTime start = new EventDateTime();
        start.setDateTime(new com.google.api.client.util.DateTime(System.currentTimeMillis()));
        event.setStart(start);
        
        // Set end time (1 hour later)
        EventDateTime end = new EventDateTime();
        end.setDateTime(new com.google.api.client.util.DateTime(System.currentTimeMillis() + 3600000));
        event.setEnd(end);
        
        return event;
    }
}
