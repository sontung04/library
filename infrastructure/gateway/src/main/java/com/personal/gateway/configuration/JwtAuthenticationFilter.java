package com.personal.gateway.configuration;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.personal.gateway.service.GatewayJwtService;
import com.personal.gateway.service.GatewayUserAuthCacheService;
import com.personal.gateway.service.GatewayUserAuthClient;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * A <code>GlobalFilter</code> that validates every request except
 * <code>/auth/**</code> before other filters, routing
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final GatewayJwtService gatewayJwtService;
    private final GatewayUserAuthCacheService gatewayUserAuthCacheService;
    private final GatewayUserAuthClient gatewayUserAuthClient;

    public JwtAuthenticationFilter(
            GatewayJwtService gatewayJwtService,
            GatewayUserAuthCacheService gatewayUserAuthCacheService,
            GatewayUserAuthClient gatewayUserAuthClient) {
        this.gatewayJwtService = gatewayJwtService;
        this.gatewayUserAuthCacheService = gatewayUserAuthCacheService;
        this.gatewayUserAuthClient = gatewayUserAuthClient;
    }

    /**
     * Configures behavior of the filter
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // Always allow preflight requests so browser CORS negotiation can complete.
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            logger.debug("CORS preflight request allowed: {} {}", exchange.getRequest().getMethod(), path);
            return chain.filter(exchange);
        }

        // Allow unauthenticated access to auth endpoints
        if (path.startsWith("/auth/")) {
            logger.debug("Auth endpoint accessed without authentication: {} {}", exchange.getRequest().getMethod(),
                    path);
            return chain.filter(exchange);
        }

        // For everything else, require Authorization: Bearer ...
        List<String> authHeaders = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            logger.warn("REQUEST BLOCKED: Missing Authorization header - {} {}", exchange.getRequest().getMethod(),
                    path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            logger.warn("REQUEST BLOCKED: Invalid Authorization header format (expected 'Bearer ...') - {} {}",
                    exchange.getRequest().getMethod(), path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract info from the token
        String token = authHeader.substring(7);

        try {
            // Validate signature and token type FIRST — never trust Base64-only extraction
            Claims claims = gatewayJwtService.validateAccessToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String jti = claims.getId();

            return gatewayUserAuthCacheService.readSnapshot(userId)
                    .switchIfEmpty(gatewayUserAuthClient.fetchAndCache(userId))
                    .flatMap(snapshot -> {
                        Mono<Boolean> revokedCheck = jti == null
                                ? Mono.just(false)
                                : gatewayUserAuthCacheService.isJtiRevoked(jti);

                        return revokedCheck.flatMap(isRevoked -> {
                            if (isRevoked) {
                                logger.warn("REQUEST BLOCKED: revoked access token jti={} path={}", jti, path);
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                            }

                            logger.info("REQUEST ALLOWED: user-exists and JWT valid - {} {} - User: {} - Roles: {}",
                                    exchange.getRequest().getMethod(),
                                    path,
                                    snapshot.userId(),
                                    snapshot.roles());

                            ServerWebExchange mutated = exchange.mutate()
                                    .request(builder -> builder
                                            .header("X-User-Id", snapshot.userId().toString())
                                            .header("X-User-Roles", String.join(",", snapshot.roles()))
                                            .header("X-Auth-Validated", "true"))
                                    .build();

                            return chain.filter(mutated);
                        });
                    })
                    .onErrorResume(e -> {
                        logger.warn("REQUEST BLOCKED: auth/user-check failed - {} {} - Error: {}",
                                exchange.getRequest().getMethod(),
                                path,
                                e.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        } catch (Exception e) {
            logger.warn("REQUEST BLOCKED: JWT validation failed - {} {} - Error: {}", exchange.getRequest().getMethod(),
                    path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * Set this filter to be the first executed
     */
    @Override
    public int getOrder() {
        // Run early
        return -1;
    }
}
