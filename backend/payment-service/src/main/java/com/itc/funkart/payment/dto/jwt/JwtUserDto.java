package com.itc.funkart.payment.dto.jwt;

public record JwtUserDto(
        Long id,
        String name,
        String email
) {}
