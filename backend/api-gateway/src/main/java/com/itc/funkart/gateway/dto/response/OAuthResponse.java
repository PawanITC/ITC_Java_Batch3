package com.itc.funkart.gateway.dto.response;

import com.itc.funkart.gateway.dto.UserDto;
import lombok.Builder;

/**
 * <h2>OAuth Authentication Response</h2>
 *
 * <p>
 * Represents the response returned from the User-Service after
 * successfully completing the GitHub OAuth authentication flow.
 * </p>
 *
 * <p>
 * This DTO is consumed by the API Gateway to:
 * <ul>
 *   <li>Extract the authenticated user profile (if required)</li>
 *   <li>Retrieve the system-issued JWT</li>
 *   <li>Set secure HttpOnly cookies for session management</li>
 * </ul>
 * </p>
 *
 * <p>
 * It acts as a transport contract between the User-Service and the Gateway,
 * ensuring a consistent authentication response structure across services.
 * </p>
 *
 * <p>
 * <b>Security Note:</b>
 * The JWT contained in this response is never exposed directly to the
 * frontend in raw form; it is always wrapped into an HttpOnly cookie
 * by the Gateway.
 * </p>
 *
 * @param user
 *         The authenticated user profile returned by the User-Service.
 *         May be used for UI hydration or client-side display.
 *
 * @param token
 *         The signed JWT issued by the User-Service representing the
 *         authenticated session.
 */
@Builder
public record OAuthResponse(
        UserDto user,
        String token
) {}