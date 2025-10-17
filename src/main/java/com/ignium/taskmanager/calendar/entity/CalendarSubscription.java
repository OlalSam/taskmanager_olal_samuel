package com.ignium.taskmanager.calendar.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "resource_id", nullable = false)
    private String resourceId;
    
    @Column(name = "channel_id", nullable = false)
    private String channelId;
    
    @Column(name = "expiration", nullable = false)
    private LocalDateTime expiration;
    
    @Column(name = "sync_token", length = 1000)
    private String syncToken;
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
