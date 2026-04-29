package com.itc.funkart.product_service.config;

import com.itc.funkart.product_service.constants.JwtClaims;
import com.itc.funkart.product_service.dto.jwt.JwtUserDto;
import com.itc.funkart.product_service.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Primary security interceptor for the Product Service.
 * <p>
 * This filter extends {@link org.springframework.web.filter.OncePerRequestFilter} to ensure
 * single execution per request dispatch. It extracts JWT tokens from either the
 * 'Authorization' header or 'token' cookie.
 * </p>
 * * <h2>Responsibilities:</h2>
 * <ul>
 * <li>Extract raw JWT strings from incoming HTTP requests.</li>
 * <li>Invoke {@link com.itc.funkart.product_service.service.JwtService} for cryptographic validation.</li>
 * <li>Convert JWT claims into a {@link com.itc.funkart.product_service.dto.jwt.JwtUserDto}.</li>
 * <li>Populate the {@link org.springframework.security.core.context.SecurityContextHolder}
 * with an authenticated token for downstream role-based access control.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtWebFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    public void doFilterInternal(@Nonnull HttpServletRequest request,
                                 @Nonnull HttpServletResponse response,
                                 @Nonnull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && !token.isBlank()) {
            try {
                Claims claims = jwtService.parseJwtToken(token);

                String subject = claims.getSubject();
                String role = claims.get(JwtClaims.ROLE, String.class);


                if (subject != null && role != null) {
                    // Build the user DTO from claims
                    JwtUserDto user = JwtUserDto.builder()
                            .id(Long.parseLong(subject))
                            .name(claims.get(JwtClaims.NAME, String.class))
                            .email(claims.get(JwtClaims.EMAIL, String.class))
                            .role(role)
                            .build();

                    // Create Authentication object
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority(role))
                    );

                    // Set the context
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Authenticated user {} with role {}", subject, role);
                }

            } catch (Exception ex) {
                // Log and continue - the SecurityFilterChain will block if the endpoint requires auth
                log.error("JWT validation failed: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the token from the Authorization header or a 'token' cookie.
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
