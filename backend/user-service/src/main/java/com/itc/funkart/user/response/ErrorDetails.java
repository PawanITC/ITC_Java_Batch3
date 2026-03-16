package com.itc.funkart.user.response;

import lombok.Getter;
import lombok.Setter;

/**
 * A Generic Error Detail setup for unsuccessful API responses for all endpoints.
 * Contains:
 * - code: machine-readable code
 * - message: human-readable message
 * - field: optional, e.g., "email"
 * - details: optional, extra info
 * - requestId: optional, for tracing/logging
 *
 */
@Setter
@Getter
public class ErrorDetails {
    // Getters and setters
    private String code;
    private String message;


    // No-args constructor (required for Jackson/Gson)
    public ErrorDetails() {}

    // Convenience constructor for common case
    public ErrorDetails(String code, String message) {
        this.code = code;
        this.message = message;
    }

}

