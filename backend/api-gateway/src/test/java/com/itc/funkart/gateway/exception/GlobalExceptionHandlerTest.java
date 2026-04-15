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
import org.springframework.web.bind.support.WebExchangeBindException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the patched {@link GlobalExceptionHandler}.
 * Validates the transition to the Lean ApiResponse format.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("OAuthException: Should return 400 Bad Request")
    void handleOAuthException_ShouldReturnBadRequest() {
        OAuthException ex = new OAuthException("GitHub exchange failed");

        ResponseEntity<ApiResponse<Void>> response = handler.handleOAuth(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().getError().getCode());
        assertEquals("GitHub exchange failed", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("AccessDenied: Should return 403 Forbidden")
    void handleAccessDenied_ShouldReturnForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().getError().getCode());
        assertEquals("Access denied", response.getBody().getError().getMessage());
    }

    @Test
    @DisplayName("Generic Exception: Should return 500 with standardized 'Senior' message")
    void handleGeneric_ShouldReturnInternalError() {
        Exception ex = new Exception("DB Connection dropped");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        // Matches the string we put in the GlobalExceptionHandler catch-all
        assertEquals("An unexpected error occurred", response.getBody().getError().getMessage());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError().getCode());
    }

    @Test
    @DisplayName("Validation: Should handle WebExchangeBindException and extract field info")
    void handleValidation_ShouldCaptureFieldAndMessage() {
        // We mock the Reactive validation exception
        WebExchangeBindException ex = mock(WebExchangeBindException.class, invocation -> mock(org.springframework.validation.BindingResult.class));
        org.springframework.validation.FieldError fieldError = new org.springframework.validation.FieldError("signupRequest", "email", "Email must be valid");

        // Mocking the chain of calls internal to the exception
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("email", response.getBody().getError().getField());
        assertEquals("Email must be valid", response.getBody().getError().getMessage());
    }

    @Nested
    @DisplayName("Dojo: ApiResponse Structure Tests")
    class StructureTests {
        @Test
        @DisplayName("Success Response: Should not contain error object")
        void successResponse_ShouldBeLean() {
            ApiResponse<String> response = new ApiResponse<>("Success Data", "Operation OK");

            assertEquals("Success Data", response.getData());
            assertEquals("Operation OK", response.getMessage());
            assertNull(response.getError()); // Crucial: Error must be null on success
            assertNotNull(response.getTimestamp());
        }

        @Test
        @DisplayName("Error Details: Should support field-level reporting")
        void errorDetails_ShouldHoldFieldInfo() {
            ErrorDetails details = new ErrorDetails("VALIDATION_ERROR", "Password too short", "password");

            assertEquals("password", details.getField());
            assertEquals("VALIDATION_ERROR", details.getCode());
        }
    }
}