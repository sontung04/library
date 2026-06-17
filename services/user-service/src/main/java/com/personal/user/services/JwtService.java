package com.personal.user.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.properties.JwtProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;

/**
 * A small helper class responsible for creating app's JWT
 * after a user successfully logs in.
 */
@Service
public class JwtService {
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_TOKEN_TYPE = "token_type";

    private final JwtProperties props;
    private final SecretKey key;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Build a JWT using using <code>HMAC_SHA</code> (HS256) with secret from
     * properties
     * 
     * @param subject standard fields
     * @return a JWT token that can be return to the client
     */
    public TokenBundle mintAccessToken(String subject, List<String> roles) {
        return mintToken(subject, TOKEN_TYPE_ACCESS, props.accessExpirationSeconds(), roles);
    }

    public TokenBundle mintRefreshToken(String subject) {
        return mintToken(subject, TOKEN_TYPE_REFRESH, props.refreshExpirationSeconds(), List.of());
    }

    public Claims validateToken(String token, String expectedTokenType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String actualType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!expectedTokenType.equals(actualType)) {
                throw new WebException(ErrorCode.INVALID_TOKEN);
            }

            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new WebException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Long extractSubjectWithoutValidation(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new WebException(ErrorCode.INVALID_TOKEN);
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode payloadNode = objectMapper.readTree(payload);
            String subject = payloadNode.path("sub").asText(null);
            if (subject == null || subject.isBlank()) {
                throw new WebException(ErrorCode.INVALID_TOKEN);
            }

            return Long.parseLong(subject);
        } catch (Exception ex) {
            throw new WebException(ErrorCode.INVALID_TOKEN);
        }
    }

    private TokenBundle mintToken(String subject, String tokenType, long expirationSeconds, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .issuer(props.issuer())
                .subject(subject)
                .id(jti)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();

        return new TokenBundle(token, jti, Duration.ofSeconds(expirationSeconds));
    }

    public record TokenBundle(String token, String jti, Duration ttl) {
    }
}
