package com.ignium.taskmanager.user.service;

import com.ignium.taskmanager.user.entity.AppUser;
import com.ignium.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncService {

    private final UserRepository appUserRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppUser syncFromJwt(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        
        AppUser user = AppUser.builder()
                .id(keycloakId)
                .email(jwt.getClaimAsString("email"))
                .fullName(jwt.getClaimAsString("name"))
                .preferredUsername(jwt.getClaimAsString("preferred_username"))
                .lastLogin(Instant.now())
                .calendarConnected(false)
                .build();
        
        appUserRepository.upsertUser(user);
        return appUserRepository.findById(keycloakId)
                .orElseThrow(() -> new RuntimeException("Failed to upsert user: " + keycloakId));
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> getCurrentUser(UUID userId) {
        return appUserRepository.findById(userId);
    }
    
    @Transactional(readOnly = true)
    public boolean userExists(UUID userId) {
        return appUserRepository.existsById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> getCurrentUserByJwt(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        return appUserRepository.findById(keycloakId);
    }
    
    /**
     * Non-transactional method to check if user exists.
     * Use this when you're already in a transaction context.
     */
    public boolean userExistsNonTransactional(UUID userId) {
        return appUserRepository.existsById(userId);
    }
}

