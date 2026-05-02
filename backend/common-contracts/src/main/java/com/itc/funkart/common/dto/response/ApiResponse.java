package com.itc.funkart.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * <h2>Unified Response Envelope</h2>
 * <p>
 * Acts as the standard container for all API responses across the Funkart ecosystem.
 * This ensures that whether a request hits the Gateway or a downstream service,
 * the JSON structure remains consistent for the frontend.
 * </p>
 *
 * @param <T> The type of the data payload contained in the response.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    /** The actual payload of the response. */
    private T data;
    /** Standardized error details if the request failed. */
    private ErrorDetails error;
    /** Human-readable message regarding the operation. */
    private String message;
    /** High-precision timestamp of when the response was generated. */
    private Instant timestamp;

    /**
     * Constructs a new ApiResponse with the current timestamp truncated to milliseconds.
     */
    public ApiResponse() {
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /**
     * Helper to create a successful response.
     *
     * @param <T>     The type of the data.
     * @param data    The payload to return.
     * @param message A descriptive message about the success.
     * @return A populated ApiResponse instance.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        response.setMessage(message);
        return response;
    }

    /**
     * Helper to create an error response.
     *
     * @param <T>   The expected data type (usually null for errors).
     * @param error The standardized error details.
     * @return A populated ApiResponse instance containing error details.
     */
    public static <T> ApiResponse<T> error(ErrorDetails error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setError(error);
        return response;
    }

    /**
     * Overloaded helper to create an error response directly from strings.
     *
     * @param code    Standardized error code (e.g., "NOT_FOUND")
     * @param message Human-readable error message
     * @param field   Specific field that caused the error (optional)
     * @return A populated ApiResponse instance.
     */
    public static <T> ApiResponse<T> error(String code, String message, String field) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setError(new ErrorDetails(code, message, field));
        return response;
    }
}