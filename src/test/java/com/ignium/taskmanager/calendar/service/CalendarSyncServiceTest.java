package com.ignium.taskmanager.calendar.service;

import com.google.api.services.calendar.model.Event;
import com.ignium.taskmanager.calendar.CalendarTestDataFactory;
import com.ignium.taskmanager.calendar.entity.CalendarSyncLog;
import com.ignium.taskmanager.calendar.repository.CalendarSyncLogRepository;
import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.task.repository.TaskRepository;
import com.ignium.taskmanager.user.entity.AppUser;
import com.ignium.taskmanager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarSyncService Unit Tests")
class CalendarSyncServiceTest {

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CalendarSyncLogRepository syncLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CalendarSyncService calendarSyncService;

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    @DisplayName("syncTaskToCalendar_withNoCalendarConnected_shouldSkipSync")
    void syncTaskToCalendar_withNoCalendarConnected_shouldSkipSync() throws IOException {
        // Arrange
        Task task = CalendarTestDataFactory.createTask();
        AppUser user = CalendarTestDataFactory.createAppUserWithoutCalendar();

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));

        // Act
        calendarSyncService.syncTaskToCalendar(task, CalendarSyncLog.SyncOperation.CREATE);

        // Assert
        verify(googleCalendarService, never()).createEvent(any(), any(), any(), any());
        verify(syncLogRepository, never()).save(any(CalendarSyncLog.class));
    }

    @Test
    @DisplayName("syncTaskToCalendar_withNoDueDate_shouldSkipSync")
    void syncTaskToCalendar_withNoDueDate_shouldSkipSync() throws IOException {
        // Arrange
        Task task = CalendarTestDataFactory.createTaskWithoutDueDate();
        AppUser user = CalendarTestDataFactory.createAppUser();

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));

        // Act
        calendarSyncService.syncTaskToCalendar(task, CalendarSyncLog.SyncOperation.CREATE);

        // Assert
        verify(googleCalendarService, never()).createEvent(any(), any(), any(), any());
        verify(syncLogRepository, never()).save(any(CalendarSyncLog.class));
    }

    @Test
    @DisplayName("syncTaskToCalendar_createOperation_shouldCreateEvent")
    void syncTaskToCalendar_createOperation_shouldCreateEvent() throws IOException {
        // Arrange
        Task task = CalendarTestDataFactory.createTask();
        AppUser user = CalendarTestDataFactory.createAppUser();
        Event mockEvent = CalendarTestDataFactory.createCalendarEvent();

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));
        when(googleCalendarService.createEvent(
            eq(CalendarTestDataFactory.TEST_USER_ID),
            eq(task.getTitle()),
            eq(task.getDescription()),
            eq(task.getDueDate())
        )).thenReturn(mockEvent);

        // Act
        calendarSyncService.syncTaskToCalendar(task, CalendarSyncLog.SyncOperation.CREATE);

        // Assert
        verify(googleCalendarService).createEvent(
            CalendarTestDataFactory.TEST_USER_ID,
            task.getTitle(),
            task.getDescription(),
            task.getDueDate()
        );
        verify(taskRepository).upsertTaskCalendarInfo(
            eq(CalendarTestDataFactory.TEST_TASK_ID),
            eq(CalendarTestDataFactory.TEST_CALENDAR_EVENT_ID),
            any()
        );
        verify(syncLogRepository).save(any(CalendarSyncLog.class));
    }

    @Test
    @DisplayName("syncTaskToCalendar_updateOperation_shouldUpdateEvent")
    void syncTaskToCalendar_updateOperation_shouldUpdateEvent() throws IOException {
        // Arrange
        Task task = CalendarTestDataFactory.createTask();
        AppUser user = CalendarTestDataFactory.createAppUser();

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));

        // Act
        calendarSyncService.syncTaskToCalendar(task, CalendarSyncLog.SyncOperation.UPDATE);

        // Assert
        verify(googleCalendarService).updateEvent(
            CalendarTestDataFactory.TEST_USER_ID,
            CalendarTestDataFactory.TEST_CALENDAR_EVENT_ID,
            task.getTitle(),
            task.getDescription(),
            task.getDueDate()
        );
        verify(taskRepository).upsertTaskCalendarInfo(
            eq(CalendarTestDataFactory.TEST_TASK_ID),
            eq(CalendarTestDataFactory.TEST_CALENDAR_EVENT_ID),
            any()
        );
        verify(syncLogRepository).save(any(CalendarSyncLog.class));
    }

    @Test
    @DisplayName("syncTaskToCalendar_updateOperation_withoutCalendarEventId_shouldCreateEvent")
    void syncTaskToCalendar_updateOperation_withoutCalendarEventId_shouldCreateEvent() throws IOException {
        // Arrange
        Task task = CalendarTestDataFactory.createTaskWithoutCalendarEventId();
        AppUser user = CalendarTestDataFactory.createAppUser();
        Event mockEvent = CalendarTestDataFactory.createCalendarEvent();

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));
        when(googleCalendarService.createEvent(
            eq(CalendarTestDataFactory.TEST_USER_ID),
            eq(task.getTitle()),
            eq(task.getDescription()),
            eq(task.getDueDate())
        )).thenReturn(mockEvent);

        // Act
        calendarSyncService.syncTaskToCalendar(task, CalendarSyncLog.SyncOperation.UPDATE);

        // Assert
        verify(googleCalendarService).createEvent(
            CalendarTestDataFactory.TEST_USER_ID,
            task.getTitle(),
            task.getDescription(),
            task.getDueDate()
        );
        verify(googleCalendarService, never()).updateEvent(any(), any(), any(), any(), any());
        verify(taskRepository).upsertTaskCalendarInfo(
            eq(CalendarTestDataFactory.TEST_TASK_ID),
            eq(CalendarTestDataFactory.TEST_CALENDAR_EVENT_ID),
            any()
        );
    }

    @Test
    @DisplayName("syncTaskToCalendar_deleteOperation_shouldDeleteEvent")
    void syncTaskToCalendar_deleteOperation_shouldDeleteEvent() throws IOException {
        // Arrange
        Task task = CalendarTestDataFactory.createTask();
        AppUser user = CalendarTestDataFactory.createAppUser();

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));
        when(taskRepository.existsById(CalendarTestDataFactory.TEST_TASK_ID))
            .thenReturn(true);

        // Act
        calendarSyncService.syncTaskToCalendar(task, CalendarSyncLog.SyncOperation.DELETE);

        // Assert
        verify(googleCalendarService).deleteEvent(
            CalendarTestDataFactory.TEST_USER_ID,
            CalendarTestDataFactory.TEST_CALENDAR_EVENT_ID
        );
        verify(syncLogRepository).save(any(CalendarSyncLog.class));
    }

    @Test
    @DisplayName("syncTaskToCalendar_onFailure_shouldLogFailedSync")
    void syncTaskToCalendar_onFailure_shouldLogFailedSync() throws IOException {
        // Arrange
        Task task = CalendarTestDataFactory.createTask();
        AppUser user = CalendarTestDataFactory.createAppUser();

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));
        when(googleCalendarService.createEvent(any(), any(), any(), any()))
            .thenThrow(new IOException("Google API error"));

        // Act
        try {
            calendarSyncService.syncTaskToCalendar(task, CalendarSyncLog.SyncOperation.CREATE);
        } catch (RuntimeException e) {
            // Expected to throw RuntimeException wrapping IOException
        }

        // Assert
        ArgumentCaptor<CalendarSyncLog> logCaptor = ArgumentCaptor.forClass(CalendarSyncLog.class);
        verify(syncLogRepository).save(logCaptor.capture());
        
        CalendarSyncLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getTaskId()).isEqualTo(CalendarTestDataFactory.TEST_TASK_ID);
        assertThat(capturedLog.getOperation()).isEqualTo(CalendarSyncLog.SyncOperation.CREATE);
        assertThat(capturedLog.getStatus()).isEqualTo(CalendarSyncLog.SyncStatus.FAILED);
        assertThat(capturedLog.getErrorMessage()).contains("Google API error");
    }
}
