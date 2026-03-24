package com.personal.book.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.personal.book.security.HeaderAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.http.HttpMethod.POST;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_REQUESTS = { "/auth/**", "/actuator/health" };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> {
                })
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_REQUESTS).permitAll()
                        .requestMatchers(POST, "/api/books").hasRole("LIBRARIAN")
                        .anyRequest().permitAll())
                .addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                    .accessDeniedHandler((request, response, e) -> {
                        Authentication auth = SecurityContextHolder
                                .getContext().getAuthentication();
                        String user = auth != null ? auth.getName() : "anonymous";

                        log.warn("ACCESS DENIED: user={}, path={} {}, roles={}, reason={}",
                                user,
                                request.getMethod(),
                                request.getRequestURI(),
                                auth != null ? auth.getAuthorities() : "[]",
                                e.getMessage());

                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("""
                            {"error":"forbidden","message":"Access is denied"}
                            """);
                    })
                )    
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
