package com.ignium.taskmanager.user.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class UserSyncFilter extends OncePerRequestFilter {

    private final UserSyncService userSyncService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/calendar/webhook/**",
            "/api/v1/calendar/callback",
            "/docs/**",
            "/actuator/health",
            "/actuator/info"
    };

    public UserSyncFilter(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        
        // Skip user sync filter for public endpoints
        for (String pattern : PUBLIC_ENDPOINTS) {
            if (pathMatcher.match(pattern, requestUri)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        
        // Check if user is authenticated before running user sync
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() 
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getPrincipal() instanceof Jwt jwt) {
            try {
                // Sync user data from JWT claims in a separate transaction
                userSyncService.syncFromJwt(jwt);
            } catch (Exception e) {
                // Log error but don't break the request - this is just user sync
                log.warn("Failed to sync user from JWT: {}", e.getMessage());
                // Don't rethrow - let the request continue
                // The business logic will handle missing users appropriately
            }
        }

        filterChain.doFilter(request, response);
    }
}
