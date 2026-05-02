package com.itc.funkart.common.dto.user;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * General user profile data used for UI display and data transfer.
 */
@Builder
@Jacksonized
public record UserDto(
    /* Database primary key. */
    Long id,
    /* User's full name. */
    String name,
    /* User's email. */
    String email,
    /* Assigned security role (e.g., ROLE_USER). */
    String role
) {}