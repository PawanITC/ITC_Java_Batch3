package com.itc.funkart.gateway.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * <h2>Unified Response Envelope</h2>
 * Acts as the standard container for all Gateway responses.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private ErrorDetails error;
    private String message;
    private Instant timestamp;

    public ApiResponse() {
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** Success constructor */
    public ApiResponse(T data, String message) {
        this();
        this.data = data;
        this.message = message;
    }

    /** Error constructor */
    public ApiResponse(ErrorDetails error) {
        this();
        this.error = error;
    }
}