package com.itc.funkart.gateway.exception;

import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.response.ErrorDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * Ensures that different exception types are mapped to the correct
 * HTTP status codes and consistent API response formats.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("IllegalArgument: Should return 400 Bad Request")
    void handleIllegalArgument_ShouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().getError().getCode());
        assertEquals("Invalid input", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("OAuthException: Should return 400 Bad Request and log error")
    void handleOAuthException_ShouldReturnBadRequest() {
        OAuthException ex = new OAuthException("GitHub error", new RuntimeException("root cause"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleOAuth(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("GitHub error", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("AccessDenied: Should return 403 Forbidden")
    void handleAccessDenied_ShouldReturnForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden access");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Access denied", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("Generic Exception: Should return 500 Internal Server Error")
    void handleGeneric_ShouldReturnInternalError() {
        Exception ex = new Exception("Something went wrong");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("OAuthException: Should return 400 with proper ApiResponse structure")
    void handleOAuthException_DetailedCheck() {
        OAuthException ex = new OAuthException("GitHub error");

        // 1. Get the ResponseEntity from the handler
        ResponseEntity<ApiResponse<Void>> response = handler.handleOAuth(ex);

        // 2. Extract your custom ApiResponse body
        ApiResponse<Void> body = response.getBody();

        // 3. Use your class getters to verify the "Contract"
        assertNotNull(body);
        assertNotNull(body.getTimestamp()); // Proves the ApiResponse() constructor ran
        assertNotNull(body.getError());     // Proves ApiResponse(ErrorDetails) constructor ran

        assertEquals("BAD_REQUEST", body.getError().getCode());
        assertEquals("GitHub error", body.getError().getMessage());
    }


    /**
     * Unit tests for {@link ApiResponse} & {@link ErrorDetails} in response folder since they are used in exception handler.
     * Ensures that different exception types are mapped to the correct
     * HTTP status codes and consistent API response formats.
     */
    @Nested
    @DisplayName("Exception Api-Responses / Error Api-Responses")
    class ExceptionApiResponses {
        @Test
        void testApiResponseGetters() {
            ApiResponse<String> response = new ApiResponse<>("data", "message");
            assertEquals("data", response.getData());
            assertEquals("message", response.getMessage());
            assertNotNull(response.getTimestamp());
        }

        @Test
        void testErrorDetailsNoArgs() {
            ErrorDetails details = new ErrorDetails();
            details.setCode("TEST");
            assertNotNull(details);
            assertEquals("TEST", details.getCode());
        }
    }

}