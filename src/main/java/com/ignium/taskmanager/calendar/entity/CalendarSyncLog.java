package com.ignium.taskmanager.calendar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_sync_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarSyncLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "task_id", nullable = false)
    private UUID taskId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncOperation operation;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;
    
    public enum SyncOperation {
        CREATE, UPDATE, DELETE, CALENDAR_UPDATE
    }
    
    public enum SyncStatus {
        SUCCESS, FAILED, SKIPPED
    }
}
