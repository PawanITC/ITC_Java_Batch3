package com.itc.funkart.product_service.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A Generic API response wrapper for all endpoints.
 * Provides a consistent structure for success and error payloads.
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private ErrorDetails error;
    private String message;
    private Instant timestamp;

    /**
     * Base constructor to initialize the timestamp.
     */
    public ApiResponse() {
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /**
     * Standard success constructor.
     * Prevents type-mismatch errors in controllers by providing a
     * specific signature for the data payload.
     *
     * @param data The payload of type T
     */
    public ApiResponse(T data) {
        this();
        this.data = data;
        this.message = "Operation successful";
    }

    /**
     * Detailed success constructor.
     *
     * @param data    The payload of type T
     * @param message Custom success message
     */
    public ApiResponse(T data, String message) {
        this();
        this.message = message;
        this.data = data;
    }

    /**
     * Unsuccessful response constructor.
     *
     * @param error Error details containing code and description
     */
    public ApiResponse(ErrorDetails error) {
        this();
        this.error = error;
    }
}