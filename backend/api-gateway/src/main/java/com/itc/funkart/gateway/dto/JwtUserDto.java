package com.itc.funkart.gateway.dto;

public record JwtUserDto(
        Long id,
        String name,
        String email
) {}
