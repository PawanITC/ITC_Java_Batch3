package com.itc.funkart.payment.security;

import com.itc.funkart.payment.auth.claims.JwtClaims;
import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * <h2>JwtWebFilter</h2>
 * <p>
 * The primary security interceptor for all web requests.
 * </p>
 * <p>
 * This filter executes once per request to determine if the requester is authenticated.
 * It is responsible for bridging the gap between an external token (Header or Cookie)
 * and Spring Security's internal <b>SecurityContext</b>.
 * </p>
 */
@Component
public class JwtWebFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * @param jwtService Service used for cryptographic validation of incoming tokens.
     */
    public JwtWebFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Intercepts the request and attempts to authenticate the user based on a JWT.
     * <p>
     * If authentication fails, the filter simply passes the request along. The
     * final decision on whether to block the request is made by the
     * {@code SecurityFilterChain} based on the endpoint's requirements.
     * </p>
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. FAST PASS: Bypass security logic for Stripe webhooks
        if (request.getServletPath().contains("/payments/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token != null && !token.isBlank()) {
            try {
                Claims claims = jwtService.parseJwtToken(token);

                String subject = claims.getSubject();
                String role = claims.get(JwtClaims.ROLE, String.class);

                if (subject != null && role != null) {
                    JwtUserDto user = JwtUserDto.builder()
                            .id(Long.parseLong(subject))
                            .name(claims.get(JwtClaims.NAME, String.class))
                            .email(claims.get(JwtClaims.EMAIL, String.class))
                            .role(role)
                            .build();

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority(role))
                    );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    // Downgraded to debug for production cleanliness
                    logger.debug("Successfully authenticated user: " + subject);
                }

            } catch (Exception ex) {
                // Log at debug level to avoid log-spamming from malformed bot requests
                logger.debug("JWT validation failed: " + ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Strategy for locating the token in the incoming request.
     * 1. Authorization Header (Bearer scheme)
     * 2. 'token' Cookie (HttpOnly fallback)
     *
     * @return The raw token string or null if not found.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null && header.toLowerCase().startsWith("bearer ")) {
            return header.substring(7).trim();
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue().trim();
                }
            }
        }
        return null;
    }
}