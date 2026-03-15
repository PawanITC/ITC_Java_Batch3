package com.itc.funkart.gateway.dto.response;

public record SuccessfulLoginResponse(
        Long id,
        String email,
        String name,
        String token
) {}
