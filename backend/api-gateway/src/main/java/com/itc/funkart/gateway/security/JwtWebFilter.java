package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.dto.JwtUserDto;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <h2>JWT Identity Re-hydration Filter</h2>
 * Extracts identity data from Cookies or Headers. Converts the "Dehydrated" JWT
 * back into a living {@link JwtUserDto} and populates the Security Context.
 */
@Component
public class JwtWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtWebFilter.class);

    private final CookieUtil cookieUtil;
    private final JwtTokenValidator jwtTokenValidator;

    public JwtWebFilter(CookieUtil cookieUtil, JwtTokenValidator jwtTokenValidator) {
        this.cookieUtil = cookieUtil;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String token = extractToken(exchange);

        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        try {
            Claims claims = jwtTokenValidator.validateAndParseClaims(token);

            // 1. Re-hydrate the User DTO from claims
            Long userId = Long.parseLong(claims.getSubject());
            String name = (String) claims.get("name");
            String email = (String) claims.get("email");
            String role = (String) claims.get("role"); // Extracting the role

            JwtUserDto user = JwtUserDto.builder()
                    .id(userId)
                    .name(name)
                    .email(email)
                    .role(role)
                    .build();

            // 2. Convert role to Spring Authority (e.g., "ROLE_USER")
            // Note: Spring expects "ROLE_" prefix for hasRole() checks
            List<SimpleGrantedAuthority> authorities = (role != null)
                    ? List.of(new SimpleGrantedAuthority(role))
                    : List.of();

            // 3. Create the Auth object with authorities
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            log.debug("Authenticated user {} with role {}", email, role);

            // 4. Inject into the Reactive Security Context
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        } catch (Exception ex) {
            // If the token is junk, we log it and proceed as anonymous.
            // Downstream 'anyExchange().authenticated()' will catch them.
            log.warn("Security Context cleared: JWT validation failed ({})", ex.getMessage());
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.clearContext());
        }
    }

    private String extractToken(ServerWebExchange exchange) {
        // 1. Priority: Check Authorization header
        List<String> authHeaders = exchange.getRequest().getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }

        // 2. Fallback: Check secure HttpOnly cookie
        String cookieName = cookieUtil.getCookieName();
        var cookie = exchange.getRequest().getCookies().getFirst(cookieName);
        if (cookie != null) {
            return cookie.getValue();
        }

        return null;
    }
}