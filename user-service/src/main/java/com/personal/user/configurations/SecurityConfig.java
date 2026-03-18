package com.personal.user.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String[] AUTH_REQUEST = { "/api/users/**"};
    private static final String[] PUBLIC_GET_REQUEST = {};
    private static final String[] PUBLIC_POST_REQUEST = {};
    private static final String[] PUBLIC_PUT_REQUEST = {};
    private static final String[] PUBLIC_DELETE_REQUEST = {};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(AUTH_REQUEST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_REQUEST).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_REQUEST).permitAll()
                        .requestMatchers(HttpMethod.PUT, PUBLIC_PUT_REQUEST).permitAll()
                        .requestMatchers(HttpMethod.DELETE, PUBLIC_DELETE_REQUEST).permitAll()
                        .anyRequest().authenticated())
                // Later: add JWT auth filter for API requests:
                // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
