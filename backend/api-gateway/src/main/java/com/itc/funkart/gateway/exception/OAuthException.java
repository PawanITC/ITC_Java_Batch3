package com.itc.funkart.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <h2>OAuth and Security Exception</h2>
 * <p>
 * Thrown during JWT validation failures, expired sessions, or unauthorized
 * access attempts within the Gateway.
 * </p>
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class OAuthException extends RuntimeException {
    public OAuthException(String message) {
        super(message);
    }

    public OAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}