package com.itc.funkart.gateway.filters;

import com.itc.funkart.gateway.dto.JwtUserDto;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <h2>Identity Propagation Filter (The Relay)</h2>
 * <p>
 * This filter bridges the gap between the Gateway's Security Context and the
 * downstream microservices. It extracts the authenticated user's details and
 * injects them into the outbound request headers.
 * </p>
 *
 * <h3>Why this is necessary:</h3>
 * <p>
 * Downstream services (like {@code order-service}) are protected behind the
 * internal network. They should not re-validate JWTs. Instead, they trust
 * these "X-User" headers provided by the Gateway as the source of truth.
 * </p>
 *
 * <h3>Headers Injected:</h3>
 * <ul>
 * <li><b>X-User-Id:</b> The unique database ID of the user.</li>
 * <li><b>X-User-Email:</b> The authenticated email address.</li>
 * <li><b>X-User-Role:</b> The user's permission level (e.g., ROLE_USER).</li>
 * </ul>
 */
@Component
public class RelayUserHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .filter(c -> c.getAuthentication() != null && c.getAuthentication().getPrincipal() instanceof JwtUserDto)
                .map(c -> (JwtUserDto) c.getAuthentication().getPrincipal())
                .flatMap(user -> {
                    // User is present, mutate and move on
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(builder -> builder
                                    .header("X-User-Id", String.valueOf(user.id()))
                                    .header("X-User-Email", user.email())
                                    .header("X-User-Role", user.role())
                            )
                            .build();
                    return chain.filter(mutatedExchange);
                })
                // If the map/flatMap was skipped because the context was empty,
                // this switchIfEmpty will catch it and proceed with the original exchange.
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    /**
     * Ensures this filter runs AFTER the {@code JwtWebFilter} has populated
     * the Security Context, but BEFORE the request is routed.
     */
    @Override
    public int getOrder() {
        // MUST run AFTER authentication filter, BEFORE routing
        // A value of 0 or 1 is usually safe to ensure it happens
        // after the SecurityWebFilterChain but before routing.
        return 1;
    }
}