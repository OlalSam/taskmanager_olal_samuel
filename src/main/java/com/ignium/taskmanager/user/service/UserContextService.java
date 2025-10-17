package com.ignium.taskmanager.user.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserContextService {

    public UUID getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        String subject = jwt.getSubject();
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalStateException("JWT subject is empty");
        }
        
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid UUID in JWT subject: " + subject, e);
        }
    }

    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getCurrentUserId(auth);
    }
}

