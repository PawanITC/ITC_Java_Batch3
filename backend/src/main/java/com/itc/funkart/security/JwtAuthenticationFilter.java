package com.itc.funkart.security;

import com.itc.funkart.entity.User;
import com.itc.funkart.exceptions.JwtAuthenticationException;
import com.itc.funkart.repository.UserRepository;
import com.itc.funkart.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CookieUtil cookieUtil;
    private static final String COOKIE_NAME = "token";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // Refresh threshold in seconds (e.g., 5 min)
    private static final long REFRESH_THRESHOLD_SECONDS = 300;
    // Default cookie lifespan after refresh (e.g., 1 hour)
    private static final int COOKIE_MAX_AGE = 3600;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, CookieUtil cookieUtil) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.cookieUtil = cookieUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && !token.isBlank()) {
            try {
                Long userId = jwtService.getUserIdFromToken(token);
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new JwtAuthenticationException("User not found for token"));

                // Authenticate user in SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT Filter — valid token for user id {}", userId);

                // --- Auto-refresh logic ---
                Date expiry = jwtService.parseJwtToken(token).getExpiration();
                long remainingSeconds = (expiry.getTime() - System.currentTimeMillis()) / 1000;

                if (remainingSeconds < REFRESH_THRESHOLD_SECONDS) {
                    String newToken = jwtService.generateJwtToken(user);
                    cookieUtil.addTokenCookie(response, newToken, COOKIE_MAX_AGE);
                    log.debug("JWT Filter — token refreshed for user id {}", userId);
                }

            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                log.warn("JWT Filter — invalid token: {}", ex.getMessage());
                throw new JwtAuthenticationException("Invalid JWT token");
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Extract token from Authorization header or cookie */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            log.debug("JWT Filter — token found in Authorization header");
            return header.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_NAME.equals(c.getName())) {
                    log.debug("JWT Filter — token found in cookie");
                    return c.getValue();
                }
            }
        }

        log.debug("JWT Filter — no token found");
        return null;
    }
}