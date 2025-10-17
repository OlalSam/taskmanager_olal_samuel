package com.ignium.taskmanager.calendar.controller;

import com.ignium.taskmanager.calendar.service.CalendarWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Enumeration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar/webhook")
@RequiredArgsConstructor
@Slf4j
public class CalendarWebhookController {
    
    private final CalendarWebhookService calendarWebhookService;
    
    @PostMapping("/notifications")
    public ResponseEntity<String> handleNotification(
            @RequestHeader(value = "X-Goog-Channel-ID", required = false) String channelId,
            @RequestHeader(value = "X-Goog-Channel-Token", required = false) String channelToken,
            @RequestHeader(value = "X-Goog-Resource-ID", required = false) String resourceId,
            @RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState,
            @RequestHeader(value = "X-Goog-Resource-URI", required = false) String resourceUri,
            @RequestHeader(value = "X-Goog-Message-Number", required = false) String messageNumber,
            @RequestHeader(value = "X-Goog-Channel-Expiration", required = false) String expiration,
            HttpServletRequest request) {
        
        // Log all headers for debugging
        log.info("========== WEBHOOK NOTIFICATION RECEIVED ==========");
        log.info("Method: {} {}", request.getMethod(), request.getRequestURI());
        log.info("Channel ID: {}", channelId);
        log.info("Channel Token: {}", channelToken);
        log.info("Resource ID: {}", resourceId);
        log.info("Resource State: {}", resourceState);
        log.info("Resource URI: {}", resourceUri);
        log.info("Message Number: {}", messageNumber);
        log.info("Expiration: {}", expiration);
        log.info("===================================================");
        
        try {
            // Handle different resource states
            if ("sync".equals(resourceState)) {
                log.info("Initial sync notification received - acknowledging");
                return ResponseEntity.ok("OK");
            }
            
            if ("exists".equals(resourceState)) {
                log.info("Calendar events changed - processing updates");
                
                // Extract user ID from channel token if available
                UUID userId = null;
                if (channelToken != null) {
                    userId = extractUserIdFromToken(channelToken);
                    if (userId == null) {
                        log.warn("Could not extract user ID from channel token: {}", channelToken);
                    }
                }
                
                if (userId != null) {
                    // Process the calendar changes asynchronously
                    calendarWebhookService.processEventUpdate(userId, resourceUri);
                } else {
                    log.warn("No user ID available - skipping calendar processing");
                }
                
                return ResponseEntity.ok("OK");
            }
            
            if ("not_exists".equals(resourceState)) {
                log.info("Calendar was deleted - cleaning up subscriptions");
                return ResponseEntity.ok("OK");
            }
            
            log.warn("Unknown resource state: {}", resourceState);
            return ResponseEntity.ok("OK"); // Always return OK to prevent retries
            
        } catch (Exception e) {
            log.error("Error processing webhook notification: {}", e.getMessage(), e);
            // Always return OK even on errors to prevent Google from retrying excessively
            return ResponseEntity.ok("OK");
        }
    }
    
    private UUID extractUserIdFromToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            String[] parts = decoded.split(":");
            if (parts.length >= 1) {
                return UUID.fromString(parts[0]);
            }
        } catch (Exception e) {
            log.debug("Failed to decode token: {}", e.getMessage());
        }
        return null;
    }
}
