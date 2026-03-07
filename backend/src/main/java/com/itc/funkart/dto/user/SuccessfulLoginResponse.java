package com.itc.funkart.dto.user;

public record SuccessfulLoginResponse(
        Long id,
        String email,
        String name
) {}
