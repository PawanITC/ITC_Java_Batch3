package com.itc.funkart.response;

import java.time.Instant;

/**
 * A Generic API response wrapper for all endpoints.
 * Contains:
 *   - success: whether the operation succeeded
 *   - message: descriptive text for the client
 *   - data: payload of type T
 *   - timestamp: when the response was generated
 *
 * @param <T> The type of the data included in the response (e.g., User, List<Product>, etc.)
 */
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final String timestamp;


    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toString();
    }
    //This ApiResponse will be used when success is false (exception use)
    public ApiResponse(String message, T data) {
        this.success = false;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toString();
    }

    public String getTimestamp() {
        return timestamp;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }
}
