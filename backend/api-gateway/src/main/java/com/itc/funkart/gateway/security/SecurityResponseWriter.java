package com.itc.funkart.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.common.dto.response.ErrorDetails;
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

        // Using the static factory method 'error' from your library
        ApiResponse<Void> body = ApiResponse.error(
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