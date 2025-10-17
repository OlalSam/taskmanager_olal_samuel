package com.ignium.taskmanager.calendar.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Slf4j
public class CalendarConfigController {
    
    @Value("${google.client-id}")
    private String clientId;
    
    @Value("${google.redirect-uri}")
    private String redirectUri;
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getCalendarConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Check if credentials are properly configured
        boolean isConfigured = !clientId.equals("YOUR_GOOGLE_CLIENT_ID_HERE") && 
                              !clientId.equals("YOUR_GOOGLE_CLIENT_ID");
        
        config.put("configured", isConfigured);
        config.put("clientId", isConfigured ? clientId.substring(0, 20) + "..." : "NOT_CONFIGURED");
        config.put("redirectUri", redirectUri);
        config.put("message", isConfigured ? 
            "Google Calendar API is properly configured" : 
            "Please configure Google OAuth2 credentials in application.properties");
        
        return ResponseEntity.ok(config);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getCalendarHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Basic configuration check
            boolean isConfigured = !clientId.equals("YOUR_GOOGLE_CLIENT_ID_HERE") && 
                                  !clientId.equals("YOUR_GOOGLE_CLIENT_ID");
            
            health.put("status", isConfigured ? "HEALTHY" : "CONFIGURATION_REQUIRED");
            health.put("configured", isConfigured);
            health.put("redirectUri", redirectUri);
            
            if (!isConfigured) {
                health.put("instructions", "Update application.properties with your Google OAuth2 credentials");
            }
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Calendar health check failed", e);
            health.put("status", "ERROR");
            health.put("error", e.getMessage());
            return ResponseEntity.status(500).body(health);
        }
    }
}
