package com.itc.funkart.user.dto.user;

public record JwtUserDto(
        Long id,
        String name,
        String email
) {}