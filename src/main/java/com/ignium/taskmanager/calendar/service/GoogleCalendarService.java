package com.ignium.taskmanager.calendar.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleCalendarService {
    
    private final GoogleAuthService googleAuthService;
    
    @Value("${google.client-id}")
    private String clientId;
    
    @Value("${google.client-secret}")
    private String clientSecret;
    
    private static final String APPLICATION_NAME = "Task Manager";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final NetHttpTransport HTTP_TRANSPORT;
    
    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HTTP transport", e);
        }
    }
    
    public Event createEvent(UUID userId, String title, String description, LocalDateTime dueDate) 
            throws IOException {
        
        Calendar calendar = getCalendarService(userId);
        
        Event event = new Event()
            .setSummary(title)
            .setDescription(description);
        
        Date startDate = Date.from(dueDate.atZone(ZoneId.systemDefault()).toInstant());
        EventDateTime start = new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startDate));
        event.setStart(start);
        
        Date endDate = Date.from(dueDate.plusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        EventDateTime end = new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endDate));
        event.setEnd(end);
        
        event = calendar.events().insert("primary", event).execute();
        
        log.info("Created calendar event: {}", event.getId());
        return event;
    }
    
    public Event updateEvent(UUID userId, String eventId, String title, String description, 
                            LocalDateTime dueDate) throws IOException {
        
        Calendar calendar = getCalendarService(userId);
        
        Event event = calendar.events().get("primary", eventId).execute();
        
        event.setSummary(title);
        event.setDescription(description);
        
        // Update times
        Date startDate = Date.from(dueDate.atZone(ZoneId.systemDefault()).toInstant());
        event.getStart().setDateTime(new com.google.api.client.util.DateTime(startDate));
        
        Date endDate = Date.from(dueDate.plusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        event.getEnd().setDateTime(new com.google.api.client.util.DateTime(endDate));
        
        event = calendar.events().update("primary", eventId, event).execute();
        
        log.info("Updated calendar event: {}", event.getId());
        return event;
    }
    
    public void deleteEvent(UUID userId, String eventId) throws IOException {
        Calendar calendar = getCalendarService(userId);
        calendar.events().delete("primary", eventId).execute();
        log.info("Deleted calendar event: {}", eventId);
    }
    
    public Event getEvent(UUID userId, String eventId) throws IOException {
        Calendar calendar = getCalendarService(userId);
        Event event = calendar.events().get("primary", eventId).execute();
        log.debug("Retrieved calendar event: {}", eventId);
        return event;
    }
    
    public Calendar getCalendarService(UUID userId) throws IOException {
        String accessToken = googleAuthService.getValidAccessToken(userId);
        
        Credential credential = new Credential.Builder(
            new Credential.AccessMethod() {
                @Override
                public void intercept(HttpRequest request, String accessToken) throws IOException {
                    request.getHeaders().setAuthorization("Bearer " + accessToken);
                }
                
                @Override
                public String getAccessTokenFromRequest(HttpRequest request) {
                    List<String> authorizationAsList = request.getHeaders().getAuthorizationAsList();
                    if (authorizationAsList != null) {
                        for (String header : authorizationAsList) {
                            if (header.startsWith("Bearer ")) {
                                return header.substring("Bearer ".length());
                            }
                        }
                    }
                    return null;
                }
            }
        ).build();
        credential.setAccessToken(accessToken);
        
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }
}
