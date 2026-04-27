package com.itc.funkart.payment.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Error detail wrapper for unsuccessful API responses.
 * Contains:
 * - code: machine-readable error code (e.g., "PAYMENT_ERROR", "VALIDATION_ERROR")
 * - message: human-readable error message
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetails {
    private String code;
    private String message;
}