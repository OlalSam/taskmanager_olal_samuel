package com.ignium.taskmanager.calendar.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import com.google.api.services.calendar.model.Event;
import com.ignium.taskmanager.calendar.entity.CalendarSubscription;
import com.ignium.taskmanager.calendar.repository.CalendarSubscriptionRepository;
import com.ignium.taskmanager.task.entity.Task;
import com.ignium.taskmanager.task.repository.TaskRepository;
import com.ignium.taskmanager.user.entity.AppUser;
import com.ignium.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarWebhookService {
    
    private final CalendarSubscriptionRepository subscriptionRepository;
    private final GoogleCalendarService googleCalendarService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    
    @Value("${app.webhook.base-url:${app.base-url}}")
    private String webhookBaseUrl;
    
    @Transactional
    public void subscribeUserToCalendarChanges(UUID userId) {
        log.info("Subscribing user {} to calendar webhook notifications", userId);
        
        try {
            // Remove existing subscription if any
            unsubscribeUserFromCalendarChanges(userId);
            
            // Create new subscription
            Calendar calendarService = googleCalendarService.getCalendarService(userId);
            if (calendarService == null) {
                log.error("Could not get calendar service for user: {}", userId);
                return;
            }
            
            // Generate unique channel ID
            String channelId = UUID.randomUUID().toString();
            
            // Create webhook URL
            String webhookUrl = webhookBaseUrl + "/api/v1/calendar/webhook/notifications";
            
            // Create channel token (Base64 encoded userId:channelId)
            String tokenData = userId.toString() + ":" + channelId;
            String channelToken = Base64.getEncoder().encodeToString(tokenData.getBytes());
            
            // Create watch request
            Calendar.Events.Watch watchRequest = calendarService.events().watch("primary", 
                new Channel()
                    .setId(channelId)
                    .setType("web_hook")
                    .setAddress(webhookUrl)
                    .setToken(channelToken)
                    .setExpiration(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)) // 7 days
            );
            
            // Execute watch request
            var response = watchRequest.execute();
            
            // Initialize sync token by fetching current events
            String initialSyncToken = null;
            try {
                var events = calendarService.events().list("primary").execute();
                initialSyncToken = events.getNextSyncToken();
                log.info("Initialized sync token for user: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to initialize sync token for user {}: {}", userId, e.getMessage());
            }
            
            // Store subscription in database
            CalendarSubscription subscription = CalendarSubscription.builder()
                    .userId(userId)
                    .resourceId(response.getResourceId())
                    .channelId(channelId)
                    .expiration(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(response.getExpiration()), 
                        ZoneOffset.UTC))
                    .syncToken(initialSyncToken)
                    .lastSyncAt(LocalDateTime.now())
                    .build();
            
            subscriptionRepository.save(subscription);
            
            log.info("Successfully subscribed user {} to calendar webhooks. Resource ID: {}, Expiration: {}", 
                    userId, response.getResourceId(), subscription.getExpiration());
            
        } catch (Exception e) {
            log.error("Failed to subscribe user {} to calendar webhooks: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to subscribe to calendar webhooks", e);
        }
    }
    
    @Transactional
    public void unsubscribeUserFromCalendarChanges(UUID userId) {
        log.info("Unsubscribing user {} from calendar webhook notifications", userId);
        
        try {
            CalendarSubscription existingSubscription = subscriptionRepository.findByUserId(userId).orElse(null);
            if (existingSubscription == null) {
                log.debug("No existing subscription found for user: {}", userId);
                return;
            }
            
            // Stop the watch
            Calendar calendarService = googleCalendarService.getCalendarService(userId);
            if (calendarService != null) {
                try {
                    calendarService.channels().stop(
                        new Channel()
                            .setId(existingSubscription.getChannelId())
                            .setResourceId(existingSubscription.getResourceId())
                    ).execute();
                    
                    log.info("Successfully stopped watch for user: {}", userId);
                } catch (Exception e) {
                    log.warn("Failed to stop watch for user {}: {}", userId, e.getMessage());
                    // Continue with cleanup even if stop fails
                }
            }
            
            // Remove from database
            subscriptionRepository.deleteByUserId(userId);
            
            log.info("Successfully unsubscribed user {} from calendar webhooks", userId);
            
        } catch (Exception e) {
            log.error("Failed to unsubscribe user {} from calendar webhooks: {}", userId, e.getMessage(), e);
            // Don't throw exception - cleanup should be best effort
        }
    }
    
    @Transactional
    public void renewExpiredSubscriptions() {
        log.info("Checking for expired calendar webhook subscriptions");
        
        LocalDateTime now = LocalDateTime.now();
        var expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(now);
        
        for (CalendarSubscription subscription : expiredSubscriptions) {
            log.info("Renewing expired subscription for user: {}", subscription.getUserId());
            try {
                // Unsubscribe old subscription
                unsubscribeUserFromCalendarChanges(subscription.getUserId());
                
                // Subscribe again
                subscribeUserToCalendarChanges(subscription.getUserId());
                
                log.info("Successfully renewed subscription for user: {}", subscription.getUserId());
            } catch (Exception e) {
                log.error("Failed to renew subscription for user {}: {}", 
                        subscription.getUserId(), e.getMessage(), e);
            }
        }
        
        log.info("Completed renewal check for {} expired subscriptions", expiredSubscriptions.size());
    }
    
    /**
     * Process webhook event update notification.
     */
    @Async
    @Transactional
    public void processEventUpdate(UUID userId, String resourceUri) {
        try {
            AppUser user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isCalendarConnected()) {
                log.debug("User {} doesn't have calendar connected", userId);
                return;
            }
            
            // For calendar-level changes, fetch all events with sync token
            if (resourceUri.contains("/events?") || resourceUri.contains("/events")) {
                log.info("Processing calendar-level changes for user {}", userId);
                processIncrementalCalendarSync(userId);
                return;
            }
            
            // For specific event changes, extract event ID
            String eventId = extractEventIdFromResourceUri(resourceUri);
            if (eventId == null) {
                log.warn("Could not extract event ID from resource URI: {}", resourceUri);
                return;
            }
            
            Optional<Task> taskOpt = taskRepository.findByCalendarEventId(eventId);
            if (taskOpt.isEmpty()) {
                log.debug("No task found for calendar event ID: {}", eventId);
                return;
            }
            
            Task task = taskOpt.get();
            Event updatedEvent = googleCalendarService.getEvent(userId, eventId);
            if (updatedEvent == null) {
                log.warn("Could not fetch event from Google Calendar: {}", eventId);
                return;
            }
            
            if (shouldUpdateFromCalendar(task, updatedEvent)) {
                updateTaskFromCalendarEvent(task, updatedEvent);
                log.info("Updated task {} from calendar event {}", task.getId(), eventId);
            }
            
        } catch (Exception e) {
            log.error("Error processing event update for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Process incremental calendar sync using sync tokens.
     */
    @Async
    @Transactional
    public void processIncrementalCalendarSync(UUID userId) {
        try {
            AppUser user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isCalendarConnected()) {
                log.debug("User {} doesn't have calendar connected", userId);
                return;
            }
            
            Optional<CalendarSubscription> subscriptionOpt = subscriptionRepository.findByUserId(userId);
            if (subscriptionOpt.isEmpty()) {
                log.warn("No calendar subscription found for user {}", userId);
                return;
            }
            
            CalendarSubscription subscription = subscriptionOpt.get();
            String syncToken = subscription.getSyncToken();
            
            if (syncToken == null) {
                log.info("No sync token available for user {}, performing full sync", userId);
                // Perform full sync if no sync token available
                return;
            }
            
            // Fetch events using sync token
            Calendar calendarService = googleCalendarService.getCalendarService(userId);
            if (calendarService == null) {
                log.warn("Could not get calendar service for user {}", userId);
                return;
            }
            
            // Use the existing list method with sync token
            var events = calendarService.events().list("primary")
                .setSyncToken(syncToken)
                .execute();
            
            if (events.getItems() != null && !events.getItems().isEmpty()) {
                log.info("Processing {} changed events for user {}", events.getItems().size(), userId);
                
                for (Event event : events.getItems()) {
                    if ("cancelled".equals(event.getStatus())) {
                        // Handle deleted event
                        handleDeletedEvent(event);
                    } else {
                        // Handle updated/new event
                        handleUpdatedEvent(event, userId);
                    }
                }
            } else {
                log.info("No changed events found for user {}", userId);
            }
            
            // Update sync token for next incremental sync
            if (events.getNextSyncToken() != null) {
                subscription.setSyncToken(events.getNextSyncToken());
                subscription.setLastSyncAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
                log.info("Updated sync token for user {}", userId);
            }
            
        } catch (Exception e) {
            log.error("Error processing incremental calendar sync for user {}: {}", userId, e.getMessage());
        }
    }
    
    private void handleDeletedEvent(Event event) {
        try {
            Optional<Task> taskOpt = taskRepository.findByCalendarEventId(event.getId());
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.setCalendarEventId(null);
                taskRepository.save(task);
                log.info("Removed calendar event ID from task {} (event deleted)", task.getId());
            }
        } catch (Exception e) {
            log.error("Error handling deleted event {}: {}", event.getId(), e.getMessage());
        }
    }
    
    private void handleUpdatedEvent(Event event, UUID userId) {
        try {
            Optional<Task> taskOpt = taskRepository.findByCalendarEventId(event.getId());
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                if (shouldUpdateFromCalendar(task, event)) {
                    updateTaskFromCalendarEvent(task, event);
                    log.info("Updated task {} from calendar event {}", task.getId(), event.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error handling updated event {}: {}", event.getId(), e.getMessage());
        }
    }
    
    @Scheduled(fixedRate = 6 * 24 * 60 * 60 * 1000, initialDelay = 60 * 1000)
    public void renewExpiredWebhooks() {
        log.info("Starting scheduled webhook renewal");
        renewExpiredSubscriptions();
    }
    
    /**
     * Daily health check for webhook subscriptions.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyWebhookHealthCheck() {
        log.info("Starting daily webhook health check");
        renewExpiredSubscriptions();
    }
    
    // Helper methods
    private String extractEventIdFromResourceUri(String resourceUri) {
        if (resourceUri == null) return null;
        String[] parts = resourceUri.split("/events/");
        return parts.length > 1 ? parts[1] : null;
    }
    
    private boolean shouldUpdateFromCalendar(Task task, Event calendarEvent) {
        if (calendarEvent.getUpdated() == null) return false;
        
        LocalDateTime taskModified = task.getUpdatedAt();
        com.google.api.client.util.DateTime calendarUpdated = calendarEvent.getUpdated();
        Instant calendarModifiedInstant = Instant.ofEpochMilli(calendarUpdated.getValue());
        LocalDateTime calendarModified = LocalDateTime.ofInstant(calendarModifiedInstant, ZoneOffset.UTC);
        
        return calendarModified.isAfter(taskModified);
    }
    
    private void updateTaskFromCalendarEvent(Task task, Event calendarEvent) {
        if (calendarEvent.getSummary() != null) {
            task.setTitle(calendarEvent.getSummary());
        }
        
        if (calendarEvent.getDescription() != null) {
            task.setDescription(calendarEvent.getDescription());
        }
        
        if (calendarEvent.getStart() != null && calendarEvent.getStart().getDateTime() != null) {
            com.google.api.client.util.DateTime startDateTime = calendarEvent.getStart().getDateTime();
            Instant startInstant = Instant.ofEpochMilli(startDateTime.getValue());
            LocalDateTime dueDate = LocalDateTime.ofInstant(startInstant, ZoneOffset.UTC);
            task.setDueDate(dueDate);
        }
        
        taskRepository.save(task);
    }
}
