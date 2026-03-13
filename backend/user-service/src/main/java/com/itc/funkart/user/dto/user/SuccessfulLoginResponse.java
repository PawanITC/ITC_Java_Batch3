package com.itc.user.dto.user;

public record SuccessfulLoginResponse(
        Long id,
        String email,
        String name,
        String token
) {}
