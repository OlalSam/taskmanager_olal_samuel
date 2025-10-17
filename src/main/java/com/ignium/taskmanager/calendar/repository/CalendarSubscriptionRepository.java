package com.ignium.taskmanager.calendar.repository;

import com.ignium.taskmanager.calendar.entity.CalendarSubscription;
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
public interface CalendarSubscriptionRepository extends JpaRepository<CalendarSubscription, UUID> {
    
    Optional<CalendarSubscription> findByUserId(UUID userId);
    
    @Modifying
    @Query("DELETE FROM CalendarSubscription cs WHERE cs.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT cs FROM CalendarSubscription cs WHERE cs.expiration < :now")
    List<CalendarSubscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);
}
