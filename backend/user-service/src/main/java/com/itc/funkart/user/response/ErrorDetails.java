package com.itc.funkart.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {
    // Getters and setters
    private String code;
    private String message;
    private String field;


    // No-args constructor (required for Jackson/Gson)
    public ErrorDetails() {}

    // Convenience constructor for common case
    public ErrorDetails(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // Constructor for validation errors
    public ErrorDetails(String code, String message, String field) {
        this.code = code;
        this.message = message;
        this.field = field;
    }
}

