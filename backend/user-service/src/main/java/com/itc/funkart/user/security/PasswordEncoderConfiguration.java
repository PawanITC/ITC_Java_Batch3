package com.itc.funkart.user.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for password encryption beans.
 * Defines the {@link BCryptPasswordEncoder} as the primary strategy for
 * hashing user credentials.
 */
@Configuration
public class PasswordEncoderConfiguration {

    /**
     * Provides the hashing algorithm used across the service (Signup/Login).
     * * @return A {@link BCryptPasswordEncoder} instance with default strength (10 rounds).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}