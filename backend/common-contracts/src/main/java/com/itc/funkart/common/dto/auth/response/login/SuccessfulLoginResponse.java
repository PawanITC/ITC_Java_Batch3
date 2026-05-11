package com.itc.funkart.common.dto.auth.response.login;

import com.itc.funkart.common.dto.user.UserDto;
import lombok.Builder;

/**
 * Response wrapper for successful authentication.
 *
 * <p>Returned by the login and OAuth endpoints after the JWT has been
 * issued and set as an HttpOnly cookie.</p>
 *
 * @param user  The authenticated user's public profile data.
 * @param token The signed JWT string (also available as the {@code token} HttpOnly cookie).
 */
@Builder
public record SuccessfulLoginResponse(
        UserDto user,
        String token
) {
}
