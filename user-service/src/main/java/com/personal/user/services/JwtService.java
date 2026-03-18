package com.personal.user.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import com.personal.user.properties.JwtProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * A small helper class responsible for creating app's JWT 
 * after a user successfully logs in.
 */
@Service
public class JwtService {
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    /**
     * Build a JWT using using <code>HMAC_SHA</code> (HS256) with secret from properties
     * @param subject standard fields
     * @param name custom claim
     * @param email custom claim
     * @return a JWT token that can be return to the client 
     */
    public String mintToken(String subject, String name, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.expirationSeconds());

        return Jwts.builder()
                .issuer(props.issuer())
                .subject(subject) // local user id or provider+id
                .claim("email", email)
                .claim("name", name)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
