package com.itc.funkart.gateway.security;
import com.itc.funkart.gateway.dto.JwtUserDto;
import com.itc.funkart.gateway.service.JwtService;
import io.jsonwebtoken.Claims;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    // Refresh threshold in seconds (5 min)
    private static final long REFRESH_THRESHOLD_SECONDS = 300;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;

    public JwtAuthenticationFilter(JwtService jwtService, CookieUtil cookieUtil) {
        this.jwtService = jwtService;
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
                // Parse JWT claims
                Claims claims = jwtService.parseJwtToken(token);

                // Build user from JWT claims (no DB needed)
                Long userId = Long.parseLong(claims.getSubject());
                String name = (String) claims.get("name");
                String email = (String) claims.get("email");

                JwtUserDto user = new JwtUserDto(userId, name, email);

                // Authenticate user in SecurityContext
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Auto-refresh token if close to expiry
                long remainingSeconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
                if (remainingSeconds < REFRESH_THRESHOLD_SECONDS) {
                    String newToken = jwtService.generateJwtToken(user);
                    cookieUtil.addTokenCookie(response, newToken, null);  // null → default from config
                    log.debug("JWT token refreshed for user id {}", userId);
                }

            } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                // Token expired → clear context
                SecurityContextHolder.clearContext();
                log.warn("JWT token expired: {}", ex.getMessage());
            } catch (Exception ex) {
                // Any other parsing or validation error
                SecurityContextHolder.clearContext();
                log.warn("JWT token invalid: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract token from Authorization header or cookie
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            log.debug("JWT token found in Authorization header");
            return header.substring(7);
        }

        // Use cookie name from CookieUtil instead of hardcoded "token"
        String cookieName = cookieUtil.getCookieName();
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (cookieName.equals(c.getName())) {
                    log.debug("JWT token found in cookie");
                    return c.getValue();
                }
            }
        }

        log.debug("No JWT token found");
        return null;
    }
}
