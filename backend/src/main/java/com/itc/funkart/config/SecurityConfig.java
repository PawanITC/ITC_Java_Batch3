package com.itc.funkart.config;

import com.itc.funkart.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final ApiConfig apiConfig;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    public SecurityConfig(ApiConfig apiConfig, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.apiConfig = apiConfig;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                                .requestMatchers(
                                        usersBase + "/signup",
                                        usersBase + "/login",
                                        "/oauth/github/login",
                                        "/oauth/github/callback"
                                ).permitAll()
                                .anyRequest().authenticated() //everything else will require auth
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }
}
