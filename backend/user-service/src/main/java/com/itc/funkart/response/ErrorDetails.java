package com.itc.funkart.response;

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
public class ErrorDetails {
    private String code;
    private String message;


    // No-args constructor (required for Jackson/Gson)
    public ErrorDetails() {}

    // Convenience constructor for common case
    public ErrorDetails(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // Getters and setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}

