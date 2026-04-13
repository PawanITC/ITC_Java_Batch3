package com.itc.funkart.gateway.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Getter
@Setter
@NoArgsConstructor // For Jackson
@AllArgsConstructor // For you
public class ErrorDetails {
    private String code;
    private String message;
}

