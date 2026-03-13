package com.itc.funkart.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A Generic API response wrapper for all endpoints.
 * Contains:
 * - success: whether the operation succeeded
 * - message: descriptive text for the client
 * - data: payload of type T
 * - timestamp: when the response was generated
 *
 * @param <T> The type of the data included in the response (e.g., User, List<Product>, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;              // success payload
    private ErrorDetails error;     // error info
    private String message;      // optional human-friendly message
    private Boolean success;     // optional, derived from HTTP status
    private Instant timestamp;   // ISO 8601 timestamp

    // Constructors
    //The this() keyword in the other two constructors makes sure that it calls the timestamp constructor first before chaining with the correct error/success api response
    public ApiResponse() {
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // Successful response
    public ApiResponse(T data, String message) {
        this();
        this.message = message;
        this.data = data;
    }

    // Unsuccessful response constructor
    public ApiResponse(ErrorDetails error) {
        this();
        this.error = error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}