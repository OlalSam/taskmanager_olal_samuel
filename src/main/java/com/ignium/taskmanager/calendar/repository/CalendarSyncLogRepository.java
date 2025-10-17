package com.ignium.taskmanager.calendar.repository;

import com.ignium.taskmanager.calendar.entity.CalendarSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CalendarSyncLogRepository extends JpaRepository<CalendarSyncLog, UUID> {
    List<CalendarSyncLog> findByTaskIdOrderBySyncedAtDesc(UUID taskId);
}
