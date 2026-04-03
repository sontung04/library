package com.personal.user.configurations;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.properties.JwtProperties;
import com.personal.user.services.JwtService;
import com.personal.user.services.TokenStateService;
import com.personal.user.services.UserAuthCacheService;
import com.personal.user.services.UserAuthCacheService.UserAuthSnapshot;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAuthCacheService userAuthCacheService;
    private final TokenStateService tokenStateService;
    private final JwtProperties jwtProperties;
    private final boolean trustGatewayValidation;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserAuthCacheService userAuthCacheService,
            TokenStateService tokenStateService,
            JwtProperties jwtProperties,
            @Value("${app.auth.trust-gateway-validation:false}") boolean trustGatewayValidation) {
        this.jwtService = jwtService;
        this.userAuthCacheService = userAuthCacheService;
        this.tokenStateService = tokenStateService;
        this.jwtProperties = jwtProperties;
        this.trustGatewayValidation = trustGatewayValidation;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (trustGatewayValidation && "true".equalsIgnoreCase(request.getHeader("X-Auth-Validated"))) {
                String userId = request.getHeader("X-User-Id");
                String rolesHeader = request.getHeader("X-User-Roles");

                if (userId == null || userId.isBlank()) {
                    throw new WebException(ErrorCode.UNAUTHORIZED);
                }

                List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UsernamePasswordAuthenticationToken gatewayAuth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities);
                SecurityContextHolder.getContext().setAuthentication(gatewayAuth);
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new WebException(ErrorCode.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Required order: user existence check first (redis, then db), then
            // cryptographic JWT validation.
            Long userId = jwtService.extractSubjectWithoutValidation(token);
            UserAuthSnapshot snapshot = userAuthCacheService.loadUserAuthSnapshot(
                    userId,
                    Duration.ofSeconds(jwtProperties.userCacheTtlSeconds()));

            Claims claims = jwtService.validateToken(token, JwtService.TOKEN_TYPE_ACCESS);

            String jti = claims.getId();
            if (jti != null && tokenStateService.isJtiRevoked(jti)) {
                throw new WebException(ErrorCode.TOKEN_REVOKED);
            }

            List<SimpleGrantedAuthority> authorities = snapshot.roles()
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    snapshot.userId().toString(),
                    null,
                    authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (WebException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, ex.getMessage());
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.warn("JWT authentication failed: {}", ex.getMessage());
            writeUnauthorized(response, ErrorCode.UNAUTHORIZED.getErrorMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":1005,\"message\":\"" + message + "\"}");
    }

    private List<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toList();
    }
}
