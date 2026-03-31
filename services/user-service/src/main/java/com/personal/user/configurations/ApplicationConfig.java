package com.personal.user.configurations;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.personal.user.properties.JwtProperties;

/**
 * Enables binding values from application.yaml into <code>JwtProperties</code> record
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class ApplicationConfig {
}
