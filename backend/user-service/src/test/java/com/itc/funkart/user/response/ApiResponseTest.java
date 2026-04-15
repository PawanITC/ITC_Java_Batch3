package com.itc.funkart.user.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the unified {@link ApiResponse} and {@link ErrorDetails} DTOs.
 * Ensures 100% line coverage for the response envelope.
 */
class ApiResponseTest {

    @Test
    @DisplayName("ApiResponse: Test successful data constructor")
    void testSuccessConstructor() {
        String data = "Test Data";
        ApiResponse<String> response = new ApiResponse<>(data, "Success Message");

        assertEquals(data, response.getData());
        assertEquals("Success Message", response.getMessage());
        assertNotNull(response.getTimestamp());
        assertNull(response.getError());
    }

    @Test
    @DisplayName("ErrorDetails: Test all-args constructor and specific fields")
    void testErrorDetails() {
        // Testing the new field-aware constructor
        ErrorDetails details = new ErrorDetails("BAD_REQUEST", "Invalid email", "email");

        assertEquals("BAD_REQUEST", details.getCode());
        assertEquals("Invalid email", details.getMessage());
        assertEquals("email", details.getField());
    }

    @Test
    @DisplayName("ErrorDetails: Test no-args constructor and setters")
    void testErrorDetailsSetters() {
        ErrorDetails details = new ErrorDetails();
        details.setCode("404");
        details.setMessage("Not Found");
        details.setField("userId");

        assertEquals("404", details.getCode());
        assertEquals("Not Found", details.getMessage());
        assertEquals("userId", details.getField());
    }

    @Test
    @DisplayName("ApiResponse: Test timestamp and error setters")
    void testRemainingSetters() {
        ApiResponse<Void> response = new ApiResponse<>();
        Instant now = Instant.now();
        ErrorDetails error = new ErrorDetails("INTERNAL_ERROR", "Oops", null);

        response.setTimestamp(now);
        response.setMessage("Manual message");
        response.setError(error);

        assertEquals(now, response.getTimestamp());
        assertEquals("Manual message", response.getMessage());
        assertEquals(error, response.getError());
        assertNull(response.getData());
    }
}