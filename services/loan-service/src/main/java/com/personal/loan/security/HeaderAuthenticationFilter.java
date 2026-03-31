package com.personal.loan.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * A custom filter that save requests'authentication info
 * without verifying into SecurityContextHolder
 */
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-User-Roles");
        log.info("Received request: userId: {}, roles: {}", userId, rolesHeader);

        if (userId != null) {
            List<GrantedAuthority> authorities = parseRoles(rolesHeader);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);

            if (authorities.stream().anyMatch(a -> "ROLE_USER".equalsIgnoreCase(a.getAuthority()))) {
                log.info("Added ROLE_USER to SecurityContextHolder for userId={}", userId);
            }
            else if (authorities.stream().anyMatch(a -> "ROLE_LIBRARIAN".equalsIgnoreCase(a.getAuthority()))) {
                log.info("Added ROLE_LIBRARIAN to SecurityContextHolder for userId={}", userId);
            }
            if (authorities.stream().anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()))) {
                log.info("Added ROLE_ADMIN to SecurityContextHolder for userId={}", userId);
            }

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of();
        }

        String normalized = rolesHeader.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}

