package com.itc.funkart.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <h2>Standardized Error Structure</h2>
 * <p>
 * Provides a machine-readable code and a human-readable message for failures.
 * The 'field' attribute is specifically used for JSR-303 validation errors.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetails {
    /**
     * Machine-friendly error code (e.g., "VALIDATION_FAILED").
     */
    private String code;
    /**
     * Human-friendly explanation.
     */
    private String message;
    /**
     * The request field that caused the error, if applicable.
     */
    private String field;

    /**
     * Convenience constructor for general errors that don't target a specific field.
     *
     * @param code    The machine-readable error code.
     * @param message The human-readable error message.
     */
    public ErrorDetails(String code, String message) {
        this.code = code;
        this.message = message;
    }
}