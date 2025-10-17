package com.ignium.taskmanager.calendar.service;

import com.ignium.taskmanager.calendar.CalendarTestDataFactory;
import com.ignium.taskmanager.user.entity.AppUser;
import com.ignium.taskmanager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleAuthService Unit Tests")
class GoogleAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        // Set up test configuration values
        ReflectionTestUtils.setField(googleAuthService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(googleAuthService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(googleAuthService, "redirectUri", "http://localhost:8080/callback");
        
        // Inject the mocked RestTemplate
        ReflectionTestUtils.setField(googleAuthService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("getAuthorizationUrl_shouldReturnValidUrl")
    void getAuthorizationUrl_shouldReturnValidUrl() {
        // Act
        String authUrl = googleAuthService.getAuthorizationUrl(CalendarTestDataFactory.TEST_USER_ID);

        // Assert
        assertThat(authUrl).isNotNull();
        assertThat(authUrl).contains("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(authUrl).contains("client_id=test-client-id");
        assertThat(authUrl).contains("redirect_uri=http://localhost:8080/callback");
        assertThat(authUrl).contains("response_type=code");
        assertThat(authUrl).contains("scope=https://www.googleapis.com/auth/calendar");
        assertThat(authUrl).contains("access_type=offline");
        assertThat(authUrl).contains("state=" + CalendarTestDataFactory.TEST_USER_ID);
    }

    @Test
    @DisplayName("getValidAccessToken_withValidToken_shouldReturnToken")
    void getValidAccessToken_withValidToken_shouldReturnToken() {
        // Arrange
        AppUser user = CalendarTestDataFactory.createAppUser();
        user.setGoogleTokenExpiry(LocalDateTime.now().plusHours(1)); // Token not expired

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));

        // Act
        String accessToken = googleAuthService.getValidAccessToken(CalendarTestDataFactory.TEST_USER_ID);

        // Assert
        assertThat(accessToken).isEqualTo("test-access-token");
        verify(userRepository).findById(CalendarTestDataFactory.TEST_USER_ID);
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    @DisplayName("getValidAccessToken_withExpiredToken_shouldRefreshToken")
    void getValidAccessToken_withExpiredToken_shouldRefreshToken() {
        // Arrange
        AppUser user = CalendarTestDataFactory.createAppUser();
        user.setGoogleTokenExpiry(LocalDateTime.now().minusHours(1)); // Token expired

        Map<String, Object> refreshResponse = new HashMap<>();
        refreshResponse.put("access_token", "new-access-token");
        refreshResponse.put("expires_in", 3600);

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));
        when(restTemplate.postForObject(anyString(), any(), any()))
            .thenReturn(refreshResponse);

        // Act
        String accessToken = googleAuthService.getValidAccessToken(CalendarTestDataFactory.TEST_USER_ID);

        // Assert
        assertThat(accessToken).isEqualTo("new-access-token");
        verify(userRepository).findById(CalendarTestDataFactory.TEST_USER_ID);
        verify(userRepository).save(user);
        verify(restTemplate).postForObject(
            eq("https://oauth2.googleapis.com/token"),
            any(Map.class),
            eq(Map.class)
        );
    }

    @Test
    @DisplayName("getValidAccessToken_withNullExpiry_shouldNotRefreshToken")
    void getValidAccessToken_withNullExpiry_shouldNotRefreshToken() {
        // Arrange
        AppUser user = CalendarTestDataFactory.createAppUser();
        user.setGoogleTokenExpiry(null); // No expiry set

        when(userRepository.findById(CalendarTestDataFactory.TEST_USER_ID))
            .thenReturn(Optional.of(user));

        // Act
        String accessToken = googleAuthService.getValidAccessToken(CalendarTestDataFactory.TEST_USER_ID);

        // Assert
        assertThat(accessToken).isEqualTo("test-access-token");
        verify(userRepository).findById(CalendarTestDataFactory.TEST_USER_ID);
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }
}
