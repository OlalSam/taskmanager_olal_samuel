package com.ignium.taskmanager.calendar.service;

import com.ignium.taskmanager.user.entity.AppUser;
import com.ignium.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleAuthService {
    
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${google.client-id}")
    private String clientId;
    
    @Value("${google.client-secret}")
    private String clientSecret;
    
    @Value("${google.redirect-uri}")
    private String redirectUri;
    
    public String getAuthorizationUrl(UUID userId) {
        // Generate Google OAuth2 URL
        // Include state parameter with userId for security
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
        + "?client_id=" + clientId
        + "&redirect_uri=" + redirectUri
        + "&response_type=code"
        + "&scope=https://www.googleapis.com/auth/calendar"
        + "&access_type=offline"
        + "&state=" + userId;
        return authUrl;
    }
    
    public void handleCallback(String code, UUID userId) {
        // Exchange authorization code for tokens
        // Store tokens in AppUser entity
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Make HTTP request to token endpoint
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("client_id", clientId);
        tokenRequest.put("client_secret", clientSecret);
        tokenRequest.put("code", code);
        tokenRequest.put("grant_type", "authorization_code");
        tokenRequest.put("redirect_uri", redirectUri);
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                "https://oauth2.googleapis.com/token", 
                tokenRequest, 
                Map.class
            );
            
            String accessToken = (String) response.get("access_token");
            String refreshToken = (String) response.get("refresh_token");
            Integer expiresIn = (Integer) response.get("expires_in");
            
            // Save tokens
            user.setGoogleAccessToken(accessToken);
            user.setGoogleRefreshToken(refreshToken);
            user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            user.setCalendarConnected(true);
            
            userRepository.save(user);
            log.info("Google Calendar connected for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to exchange authorization code for tokens: {}", e.getMessage());
            throw new RuntimeException("Failed to connect Google Calendar", e);
        }
    }
    
    public String getValidAccessToken(UUID userId) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        if (user.getGoogleTokenExpiry() != null && 
            user.getGoogleTokenExpiry().isBefore(LocalDateTime.now())) {
            // Token expired, refresh it
            refreshAccessToken(user);
        }
        
        return user.getGoogleAccessToken();
    }
    
    private void refreshAccessToken(AppUser user) {
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("client_id", clientId);
        refreshRequest.put("client_secret", clientSecret);
        refreshRequest.put("refresh_token", user.getGoogleRefreshToken());
        refreshRequest.put("grant_type", "refresh_token");
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                "https://oauth2.googleapis.com/token", 
                refreshRequest, 
                Map.class
            );
            
            String accessToken = (String) response.get("access_token");
            Integer expiresIn = (Integer) response.get("expires_in");
            
            user.setGoogleAccessToken(accessToken);
            user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            
            userRepository.save(user);
            log.info("Refreshed access token for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to refresh access token: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh access token", e);
        }
    }
}
