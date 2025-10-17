package com.ignium.taskmanager.calendar.controller;

import com.ignium.taskmanager.calendar.service.GoogleAuthService;
import com.ignium.taskmanager.calendar.service.CalendarWebhookService;
import com.ignium.taskmanager.user.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarController {
    
    private final GoogleAuthService googleAuthService;
    private final CalendarWebhookService calendarWebhookService;
    private final UserContextService userContextService;
    
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> connectCalendar() {
        UUID userId = userContextService.getCurrentUserId();
        String authUrl = googleAuthService.getAuthorizationUrl(userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("authUrl", authUrl);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "state", required = false) String userId,
        @RequestParam(value = "scope", required = false) String scope,
        @RequestParam(value = "error", required = false) String error,
        @RequestParam(value = "error_description", required = false) String errorDescription
    ) {
        log.info("OAuth callback received - code: {}, state: {}, scope: {}, error: {}", 
                 code != null ? "present" : "null", 
                 userId != null ? "present" : "null", 
                 scope, error);
        
        // Handle OAuth errors first
        if (error != null) {
            log.error("OAuth error: {} - {}", error, errorDescription);
            return handleOAuthError(error, errorDescription);
        }
        
        // Validate required parameters
        if (code == null || code.trim().isEmpty()) {
            log.error("Authorization code is missing or empty");
            return ResponseEntity.badRequest().body("Authorization code is required");
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            log.error("User ID (state) is missing or empty");
            return ResponseEntity.badRequest().body("User ID is required");
        }
        
        // Validate scope if present
        if (scope != null && !scope.contains("https://www.googleapis.com/auth/calendar")) {
            log.warn("Unexpected scope received: {}", scope);
        }
        
        try {
            UUID userUuid = UUID.fromString(userId);
            googleAuthService.handleCallback(code, userUuid);
            
            // Subscribe to webhook notifications after successful OAuth
            calendarWebhookService.subscribeUserToCalendarChanges(userUuid);
            
            log.info("Calendar connected successfully for user: {}", userId);
            return ResponseEntity.ok("Calendar connected successfully!");
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID in callback: {}", userId);
            return ResponseEntity.badRequest().body("Invalid user ID format");
        } catch (Exception e) {
            log.error("Error handling calendar callback: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to connect calendar: " + e.getMessage());
        }
    }
    
    private ResponseEntity<String> handleOAuthError(String error, String errorDescription) {
        switch (error) {
            case "access_denied":
                log.info("User denied calendar access");
                return ResponseEntity.badRequest().body("Calendar access was denied by user");
            case "invalid_request":
                log.error("Invalid OAuth request: {}", errorDescription);
                return ResponseEntity.badRequest().body("Invalid OAuth request");
            case "invalid_scope":
                log.error("Invalid scope requested: {}", errorDescription);
                return ResponseEntity.badRequest().body("Invalid scope requested");
            case "server_error":
                log.error("Google OAuth server error: {}", errorDescription);
                return ResponseEntity.status(502).body("Google OAuth server error");
            default:
                log.error("Unknown OAuth error: {} - {}", error, errorDescription);
                return ResponseEntity.status(500).body("OAuth error: " + error);
        }
    }
}
