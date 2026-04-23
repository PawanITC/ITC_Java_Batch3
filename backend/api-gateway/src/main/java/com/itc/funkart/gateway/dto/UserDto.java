package com.itc.funkart.gateway.dto;

import lombok.Builder;

/**
 * Standardized User profile information used across the Gateway.
 * This matches the "nested" user object sent by the User-Service.
 */
@Builder
public record UserDto(
        Long id,
        String name,
        String email,
        String role
) {}