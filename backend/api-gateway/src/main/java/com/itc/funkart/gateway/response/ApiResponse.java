package com.itc.funkart.gateway.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

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
@Setter
@Getter
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

}