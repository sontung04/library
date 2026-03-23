package com.personal.book.security;

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

/**
 * A custom filter that save requests'authentication info 
 * without verifying into SecurityContextHolder
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-User-Roles");
        if (userId != null) {
            List<GrantedAuthority> authorities = parseRoles(rolesHeader);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                userId, null, authorities
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return List.of();
        return Arrays.stream(rolesHeader.split(","))
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
