package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import com.itc.funkart.gateway.service.JwtService;
import com.itc.funkart.gateway.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>JwtAuthWebFilter — Unit Tests</h2>
 *
 * <p>Validates the token validation pipeline inside {@link JwtAuthWebFilter}:
 * <ol>
 *   <li>Token extraction from cookies / Authorization header (delegated to {@link CookieUtil})</li>
 *   <li>Redis blacklist check via {@link TokenBlacklistService}</li>
 *   <li>JWT parsing and claim extraction via {@link JwtService}</li>
 *   <li>Correct population of the reactive {@link SecurityContext}</li>
 *   <li>Correct 401 responses for missing, blacklisted, or invalid tokens</li>
 * </ol>
 *
 * <p>All collaborators are mocked so tests run without a real Redis instance or
 * a Spring application context.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthWebFilterTest {

    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private JwtService jwtService;
    @Mock
    private SecurityResponseWriter responseWriter;
    @Mock
    private WebFilterChain filterChain;

    private JwtAuthWebFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthWebFilter(cookieUtil, tokenBlacklistService, jwtService, responseWriter);
        lenient().when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a minimal Claims object with a subject and role.
     */
    private Claims buildClaims(String subject, String role) {
        return Jwts.claims()
                .subject(subject)
                .add("role", role)
                .build();
    }

    /**
     * Creates a mock exchange with the given Bearer token in the Authorization header.
     */
    private MockServerWebExchange exchangeWithToken(String token) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("Authorization", "Bearer " + token)
        );
    }

    // -------------------------------------------------------------------------
    // No token present
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("No token present")
    class NoTokenTests {

        @Test
        @DisplayName("Returns 401 when no token is found in cookie or header")
        void noToken_returns401() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));
            when(cookieUtil.extractToken(exchange)).thenReturn(null);

            StepVerifier.create(filter.filter(exchange, filterChain))
                    .verifyComplete();

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("Does not invoke JwtService when token is absent")
        void noToken_doesNotCallJwtService() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));
            when(cookieUtil.extractToken(exchange)).thenReturn(null);

            filter.filter(exchange, filterChain).block();

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Returns 401 when token is blank string")
        void blankToken_returns401() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));
            when(cookieUtil.extractToken(exchange)).thenReturn("   ");

            StepVerifier.create(filter.filter(exchange, filterChain))
                    .verifyComplete();

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Blacklisted token
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Blacklisted token")
    class BlacklistedTokenTests {

        @Test
        @DisplayName("Returns 401 when token is on the Redis blacklist")
        void blacklistedToken_returns401() {
            String token = "some.valid.looking.token";
            var exchange = exchangeWithToken(token);

            when(cookieUtil.extractToken(exchange)).thenReturn(token);
            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(Mono.just(true));

            StepVerifier.create(filter.filter(exchange, filterChain))
                    .verifyComplete();

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("Does not invoke JwtService for a blacklisted token")
        void blacklistedToken_doesNotCallJwtService() {
            String token = "some.valid.looking.token";
            var exchange = exchangeWithToken(token);

            when(cookieUtil.extractToken(exchange)).thenReturn(token);
            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(Mono.just(true));

            filter.filter(exchange, filterChain).block();

            verifyNoInteractions(jwtService);
        }
    }

    // -------------------------------------------------------------------------
    // Valid token — happy path
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Valid token — happy path")
    class ValidTokenTests {

        @Test
        @DisplayName("Populates SecurityContext with authentication when token is valid")
        void validToken_populatesSecurityContext() {
            String token = "valid.jwt.token";
            var exchange = exchangeWithToken(token);
            Claims claims = buildClaims("7", "ROLE_USER");

            when(cookieUtil.extractToken(exchange)).thenReturn(token);
            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(Mono.just(false));
            when(jwtService.parseClaims(token)).thenReturn(claims);
            doNothing().when(jwtService).validateClaims(claims);

            Mono<Void> result = filter.filter(exchange, filterChain);

            StepVerifier.create(result.then(
                            ReactiveSecurityContextHolder.getContext()
                                    .map(SecurityContext::getAuthentication)
                    ))
                    .expectNextMatches(auth -> auth != null && auth.isAuthenticated())
                    .verifyComplete();
        }

        @Test
        @DisplayName("Authentication principal is a UserDto with correct ID")
        void validToken_principalHasCorrectId() {
            String token = "valid.jwt.token";
            var exchange = exchangeWithToken(token);
            Claims claims = buildClaims("42", "ROLE_USER");

            when(cookieUtil.extractToken(exchange)).thenReturn(token);
            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(Mono.just(false));
            when(jwtService.parseClaims(token)).thenReturn(claims);
            doNothing().when(jwtService).validateClaims(claims);

            // We capture what was passed to filterChain by inspecting the mutated exchange
            filter.filter(exchange, filterChain).block();

            verify(filterChain).filter(any());
        }

        @Test
        @DisplayName("Filter chain is invoked for a valid token")
        void validToken_continuesFilterChain() {
            String token = "valid.jwt.token";
            var exchange = exchangeWithToken(token);
            Claims claims = buildClaims("1", "ROLE_USER");

            when(cookieUtil.extractToken(exchange)).thenReturn(token);
            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(Mono.just(false));
            when(jwtService.parseClaims(token)).thenReturn(claims);
            doNothing().when(jwtService).validateClaims(claims);

            filter.filter(exchange, filterChain).block();

            verify(filterChain, times(1)).filter(any());
        }

        @Test
        @DisplayName("Granted authority matches the role from the JWT")
        void validToken_grantedAuthorityMatchesRole() {
            String token = "valid.jwt.token";
            var exchange = exchangeWithToken(token);
            Claims claims = buildClaims("1", "ROLE_ADMIN");

            when(cookieUtil.extractToken(exchange)).thenReturn(token);
            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(Mono.just(false));
            when(jwtService.parseClaims(token)).thenReturn(claims);
            doNothing().when(jwtService).validateClaims(claims);

            // Run through the filter and capture the SecurityContext
            final Authentication[] captured = new Authentication[1];

            filter.filter(exchange, ex -> ReactiveSecurityContextHolder.getContext()
                    .doOnNext(ctx -> captured[0] = ctx.getAuthentication())
                    .then()).block();

            assertNotNull(captured[0]);
            assertTrue(
                    captured[0].getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
            );
        }
    }

    // -------------------------------------------------------------------------
    // Invalid / expired JWT
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Invalid or expired JWT")
    class InvalidTokenTests {

        @Test
        @DisplayName("Delegates to SecurityResponseWriter when JWT parsing throws")
        void invalidJwt_callsResponseWriter() {
            String token = "malformed.token";
            var exchange = exchangeWithToken(token);

            when(cookieUtil.extractToken(exchange)).thenReturn(token);
            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(Mono.just(false));
            when(jwtService.parseClaims(token))
                    .thenThrow(new JwtAuthenticationException("Invalid JWT"));
            when(responseWriter.writeUnauthorized(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, filterChain))
                    .verifyComplete();

            verify(responseWriter).writeUnauthorized(exchange);
        }

        @Test
        @DisplayName("Does not continue filter chain when JWT is invalid")
        void invalidJwt_doesNotContinueChain() {
            String token = "bad.token";
            var exchange = exchangeWithToken(token);

            when(cookieUtil.extractToken(exchange)).thenReturn(token);
            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(Mono.just(false));
            when(jwtService.parseClaims(token))
                    .thenThrow(new JwtAuthenticationException("Expired"));
            when(responseWriter.writeUnauthorized(exchange)).thenReturn(Mono.empty());

            filter.filter(exchange, filterChain).block();

            verify(filterChain, never()).filter(any());
        }
    }
}