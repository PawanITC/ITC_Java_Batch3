package com.itc.funkart.user.exceptions;

import com.itc.funkart.user.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <h2>GlobalExceptionHandler — Unit Tests</h2>
 *
 * <p>Directly invokes each handler method to verify:
 * <ul>
 *   <li>Correct HTTP status code</li>
 *   <li>Correct error code string (mirrors {@code HttpStatus.name()} in this service)</li>
 *   <li>Correct message forwarding</li>
 *   <li>Field attribution where applicable</li>
 * </ul>
 *
 * <p><b>Error code convention in this service:</b> the {@code buildErrorResponse} helper
 * uses {@code HttpStatus.name()} as the code (e.g. {@code "BAD_REQUEST"}, {@code "CONFLICT"}).
 * This differs from the api-gateway which uses custom strings like {@code "OAUTH_ERROR"}.
 *
 * <p><b>Handlers added in this revision:</b>
 * <ul>
 *   <li>{@link MessagingException} → 503 SERVICE_UNAVAILABLE</li>
 *   <li>{@link InvalidEventException} → 422 UNPROCESSABLE_ENTITY</li>
 * </ul>
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Generic Exception → 500 with generic user-safe message")
    void handleGeneric_returns500() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGeneric(new RuntimeException("DB connection dropped"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().getError().getMessage());
        assertNull(response.getBody().getError().getField());
    }

    // -------------------------------------------------------------------------
    // Business exceptions
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Validation handlers")
    class ValidationTests {

        @Test
        @DisplayName("MethodArgumentNotValidException → 400 with field and message")
        void handleValidation_returns400() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("user", "email", "invalid format");
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldError()).thenReturn(fieldError);

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("email", response.getBody().getError().getField());
            assertEquals("invalid format", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("MethodArgumentNotValidException → falls back to 'Validation failed' when no field error")
        void handleValidation_fallsBackOnNoFieldError() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldError()).thenReturn(null);

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNull(response.getBody().getError().getField());
            assertEquals("Validation failed", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("ConstraintViolationException → 400 with exception message")
        void handleConstraintViolation_returns400() {
            ConstraintViolationException ex =
                    new ConstraintViolationException("Invalid param", Set.of());

            ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Invalid param", response.getBody().getError().getMessage());
            assertNull(response.getBody().getError().getField());
        }
    }

    // -------------------------------------------------------------------------
    // JWT / Security handlers
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Business exception handlers")
    class BusinessExceptionTests {

        @Test
        @DisplayName("BadRequestException → 400")
        void handleBadRequest_returns400() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleBadRequest(new BadRequestException("Malformed request"));

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Malformed request", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("UnauthorizedException → 401 with UNAUTHORIZED code")
        void handleUnauthorized_returns401() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleUnauthorized(new UnauthorizedException("Session expired"));

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UNAUTHORIZED", response.getBody().getError().getCode());
            assertEquals("Session expired", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("NotFoundException → 404")
        void handleNotFound_returns404() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleNotFound(new NotFoundException("Resource missing"));

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Resource missing", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("AlreadyExistsException → 409 with hardcoded 'email' field")
        void handleConflict_returns409WithEmailField() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleConflict(new AlreadyExistsException("Duplicate entry"));

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("CONFLICT", response.getBody().getError().getCode());
            assertEquals("email", response.getBody().getError().getField());
        }

        @Test
        @DisplayName("ForbiddenException → 403")
        void handleForbidden_returns403() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleForbidden(new ForbiddenException("No access"));

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("No access", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("IllegalArgumentException → 400")
        void handleIllegalArgument_returns400() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleIllegalArgument(new IllegalArgumentException("Bad arg"));

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Bad arg", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("OAuthException → 400 BAD_REQUEST (external OAuth failures are client-side in this service)")
        void handleOAuth_returns400() {
            OAuthException ex = new OAuthException("GitHub handshake failed");

            ResponseEntity<ApiResponse<Void>> response = handler.handleOAuth(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("BAD_REQUEST", response.getBody().getError().getCode());
            assertEquals("GitHub handshake failed", response.getBody().getError().getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Infrastructure / messaging handlers
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("JWT and security handlers")
    class SecurityHandlerTests {

        @Test
        @DisplayName("JwtAuthenticationException → 401")
        void handleJwtAuthentication_returns401() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleJwtAuthentication(new JwtAuthenticationException("Token expired"));

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNull(response.getBody().getError().getField());
        }

        @Test
        @DisplayName("AccessDeniedException → 403 with 'Access denied' message")
        void handleAccessDenied_returns403() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleAccessDenied(
                            new org.springframework.security.access.AccessDeniedException("Denied"));

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Access denied", response.getBody().getError().getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Fallback
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Infrastructure exception handlers")
    class InfrastructureHandlerTests {

        @Test
        @DisplayName("MessagingException → 503 SERVICE_UNAVAILABLE")
        void handleMessaging_returns503() {
            MessagingException ex = new MessagingException("Kafka broker unreachable",
                    new RuntimeException("connection refused"));

            ResponseEntity<ApiResponse<Void>> response = handler.handleMessaging(ex);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("SERVICE_UNAVAILABLE", response.getBody().getError().getCode());
            assertEquals("Messaging system is temporarily unavailable",
                    response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("InvalidEventException → 422 UNPROCESSABLE_ENTITY")
        void handleInvalidEvent_returns422() {
            InvalidEventException ex = new InvalidEventException("User ID is required");

            ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidEvent(ex);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UNPROCESSABLE_ENTITY", response.getBody().getError().getCode());
            assertEquals("User ID is required", response.getBody().getError().getMessage());
        }
    }
}