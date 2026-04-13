package com.itc.funkart.payment.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A Generic API response wrapper for all endpoints.
 * Contains:
 * - data: payload of type T
 * - error: error details if operation failed
 * - message: descriptive text for the client
 * - timestamp: when the response was generated
 *
 * @param <T> The type of the data included in the response (e.g., PaymentIntentResponse, PaymentResponse)
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private ErrorDetails error;
    private String message;
    private Instant timestamp;

    // Successful response
    public ApiResponse(T data, String message) {
        this.data = data;
        this.message = message;
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // Unsuccessful response constructor
    public ApiResponse(ErrorDetails error) {
        this.error = error;
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}