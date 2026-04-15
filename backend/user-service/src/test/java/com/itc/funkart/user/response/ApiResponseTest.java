package com.itc.funkart.user.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the unified {@link ApiResponse} and {@link ErrorDetails} DTOs.
 * Hits remaining getters and setters to achieve 100% line coverage.
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
    @DisplayName("ErrorDetails: Test no-args constructor and setters")
    void testErrorDetailsSetters() {
        ErrorDetails details = new ErrorDetails();
        details.setCode("404");
        details.setMessage("Not Found");

        assertEquals("404", details.getCode());
        assertEquals("Not Found", details.getMessage());
    }

    @Test
    @DisplayName("ApiResponse: Test direct timestamp and success setters")
    void testRemainingSetters() {
        ApiResponse<Void> response = new ApiResponse<>();
        Instant now = Instant.now();

        response.setTimestamp(now);
        response.setSuccess(true);
        response.setMessage("Manual message");

        assertEquals(now, response.getTimestamp());
        assertTrue(response.getSuccess());
        assertEquals("Manual message", response.getMessage());
    }
}