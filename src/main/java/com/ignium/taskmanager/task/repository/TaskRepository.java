package com.ignium.taskmanager.task.repository;


import com.ignium.taskmanager.task.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    Optional<Task> findByIdAndUserId(UUID id, UUID userId);
    
    Optional<Task> findByCalendarEventId(String calendarEventId);
    
    Page<Task> findAllByUserId(UUID userId, Pageable pageable);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status AND t.userId = :userId")
    long countByStatusAndUserId(@Param("status") Task.TaskStatus status, @Param("userId") UUID userId);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status IN :statuses AND t.userId = :userId")
    long countByStatusInAndUserId(@Param("statuses") Task.TaskStatus[] statuses, @Param("userId") UUID userId);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN t.tags tag WHERE t.userId = :userId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:tagNames IS NULL OR tag.name IN :tagNames)")
    Page<Task> findTasksWithFilters(@Param("userId") UUID userId,
                                     @Param("status") Task.TaskStatus status,
                                     @Param("tagNames") List<String> tagNames,
                                     Pageable pageable);
    
    /**
     * UPSERT method for updating calendar sync information without optimistic locking issues
     */
    @Modifying
    @Query(value = """
        UPDATE tasks 
        SET calendar_event_id = :eventId, 
            last_synced_at = :syncedAt,
            updated_at = NOW()
        WHERE id = :taskId
        """, nativeQuery = true)
    void upsertTaskCalendarInfo(@Param("taskId") UUID taskId, 
                               @Param("eventId") String eventId, 
                               @Param("syncedAt") LocalDateTime syncedAt);
    
}