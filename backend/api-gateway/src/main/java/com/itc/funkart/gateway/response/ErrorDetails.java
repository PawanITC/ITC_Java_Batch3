package com.itc.funkart.gateway.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <h2>Standardized Error Structure</h2>
 * Matches the User-Service signature to ensure the Frontend can use
 * a single logic for error parsing.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetails {
    private String code;    // e.g., "BAD_REQUEST", "CONFLICT"
    private String message; // Human-readable explanation
    private String field;   // Optional: Which field caused the error (e.g., "email")

    // Convenience constructor for general errors
    public ErrorDetails(String code, String message) {
        this.code = code;
        this.message = message;
    }
}