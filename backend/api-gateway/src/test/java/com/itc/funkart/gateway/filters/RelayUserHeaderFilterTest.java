package com.itc.funkart.gateway.filters;

import com.itc.funkart.gateway.dto.JwtUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>RelayUserHeaderFilter — Unit Tests</h2>
 *
 * <p>Validates that the filter correctly injects (or omits) identity headers on
 * outbound requests to downstream microservices.
 *
 * <p><b>Headers injected when authenticated:</b>
 * <ul>
 *   <li>{@code X-User-Id}    — user's database ID</li>
 *   <li>{@code X-User-Email} — authenticated email</li>
 *   <li>{@code X-User-Role}  — RBAC role string</li>
 * </ul>
 *
 * <p>When there is no authenticated principal in the reactive context, the filter
 * must pass the request through unmodified — no headers added, no errors thrown.
 *
 * <p>The filter order ({@code -1}) is also validated to confirm it runs before
 * the gateway's routing layer.
 */
@ExtendWith(MockitoExtension.class)
class RelayUserHeaderFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private RelayUserHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RelayUserHeaderFilter();
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // -------------------------------------------------------------------------
    // Authenticated context
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Authenticated principal in context")
    class AuthenticatedTests {

        /**
         * Builds a reactive security context containing a {@link JwtUserDto} principal
         * and runs the filter within it so headers can be captured.
         */
        private ServerWebExchange runFilterWithUser(JwtUserDto user) {
            var auth = new UsernamePasswordAuthenticationToken(
                    user, null, List.of(new SimpleGrantedAuthority(user.role()))
            );

            AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

            when(chain.filter(any())).thenAnswer(inv -> {
                captured.set(inv.getArgument(0));
                return Mono.empty();
            });

            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders"));

            StepVerifier.create(
                    filter.filter(exchange, chain)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
            ).verifyComplete();

            return captured.get();
        }

        @Test
        @DisplayName("Injects X-User-Id header with user's ID as string")
        void injectsUserIdHeader() {
            JwtUserDto user = new JwtUserDto(42L, "Alice", "alice@example.com", "ROLE_USER");

            ServerWebExchange mutated = runFilterWithUser(user);

            HttpHeaders headers = mutated.getRequest().getHeaders();
            assertEquals("42", headers.getFirst("X-User-Id"));
        }

        @Test
        @DisplayName("Injects X-User-Email header")
        void injectsUserEmailHeader() {
            JwtUserDto user = new JwtUserDto(1L, "Alice", "alice@example.com", "ROLE_USER");

            ServerWebExchange mutated = runFilterWithUser(user);

            assertEquals("alice@example.com", mutated.getRequest().getHeaders().getFirst("X-User-Email"));
        }

        @Test
        @DisplayName("Injects X-User-Role header")
        void injectsUserRoleHeader() {
            JwtUserDto user = new JwtUserDto(1L, "Alice", "alice@example.com", "ROLE_ADMIN");

            ServerWebExchange mutated = runFilterWithUser(user);

            assertEquals("ROLE_ADMIN", mutated.getRequest().getHeaders().getFirst("X-User-Role"));
        }

        @Test
        @DisplayName("Filter chain is invoked with the mutated exchange")
        void filterChainInvokedWithMutatedExchange() {
            JwtUserDto user = new JwtUserDto(5L, "Bob", "bob@example.com", "ROLE_USER");

            runFilterWithUser(user);

            verify(chain, times(1)).filter(any());
        }
    }

    // -------------------------------------------------------------------------
    // No authentication in context
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("No authenticated principal in context")
    class UnauthenticatedTests {

        @Test
        @DisplayName("Passes request through without injecting any X-User headers")
        void noAuth_noHeadersAdded() {
            AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
            when(chain.filter(any())).thenAnswer(inv -> {
                captured.set(inv.getArgument(0));
                return Mono.empty();
            });

            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products"));

            // Run without populating the security context
            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertNotNull(captured.get());
            HttpHeaders headers = captured.get().getRequest().getHeaders();
            assertNull(headers.getFirst("X-User-Id"));
            assertNull(headers.getFirst("X-User-Email"));
            assertNull(headers.getFirst("X-User-Role"));
        }

        @Test
        @DisplayName("Filter chain is still invoked (request is not blocked)")
        void noAuth_filterChainStillInvoked() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products"));

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain, times(1)).filter(any());
        }
    }

    // -------------------------------------------------------------------------
    // Non-JwtUserDto principal (e.g. basic-auth string principal)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Principal is not a JwtUserDto")
    class NonJwtPrincipalTests {

        @Test
        @DisplayName("Does not inject X-User headers when principal type is unexpected")
        void wrongPrincipalType_noHeadersAdded() {
            // Simulate a non-JwtUserDto principal (e.g., a plain string)
            var auth = new UsernamePasswordAuthenticationToken(
                    "anonymous-string-principal", null, List.of()
            );

            AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
            when(chain.filter(any())).thenAnswer(inv -> {
                captured.set(inv.getArgument(0));
                return Mono.empty();
            });

            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders"));

            StepVerifier.create(
                    filter.filter(exchange, chain)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
            ).verifyComplete();

            assertNotNull(captured.get());
            assertNull(captured.get().getRequest().getHeaders().getFirst("X-User-Id"));
        }
    }

    // -------------------------------------------------------------------------
    // Filter ordering
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Filter order is -1 (runs before routing, after security)")
    void filterOrder_isMinusOne() {
        assertEquals(-1, filter.getOrder());
    }
}