package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.response.ErrorDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> writeUnauthorized(ServerWebExchange exchange) {

        var response = exchange.getResponse();
        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Void> body = new ApiResponse<>(
                new ErrorDetails(
                        "UNAUTHORIZED",
                        "Invalid or expired token",
                        null
                )
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            var buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }
}