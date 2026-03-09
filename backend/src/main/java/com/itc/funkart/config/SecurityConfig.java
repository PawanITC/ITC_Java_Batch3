package com.itc.funkart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final ApiConfig apiConfig;


    public SecurityConfig(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        String usersBase = apiConfig.getVersion() + "/users";

        http.csrf(AbstractHttpConfigurer::disable) // disable CSRF for now
                // allow any request matching login/signup endpoint to attempt signup
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(usersBase + "/signup").permitAll()
                                .requestMatchers(usersBase + "/login").permitAll()
                                .anyRequest().authenticated() //everything else will require auth
                );

        return http.build();
    }
}
