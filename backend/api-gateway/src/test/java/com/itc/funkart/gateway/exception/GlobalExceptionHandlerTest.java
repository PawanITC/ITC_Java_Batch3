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
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <h2>Global Exception Handler — Unit Tests</h2>
 *
 * <p>Validates that every exception handler in {@link GlobalExceptionHandler} produces
 * the correct HTTP status and a well-formed {@link ApiResponse} envelope.
 *
 * <p>Each nested class groups tests by exception type so junior developers can
 * locate the relevant test for any handler at a glance.
 *
 * <p><b>Important codes to remember:</b>
 * <ul>
 *   <li>{@code OAUTH_ERROR} — GitHub / OAuth failures (401)</li>
 *   <li>{@code VALIDATION_ERROR} — bean-validation failures (400)</li>
 *   <li>{@code DOWNSTREAM_ERROR_<status>} — WebClient relay errors</li>
 *   <li>{@code ACCESS_DENIED} — Spring Security 403s</li>
 *   <li>{@code INTERNAL_ERROR} — catch-all 500s</li>
 * </ul>
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // -------------------------------------------------------------------------
    // OAuthException  →  401
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("OAuthException handler")
    class OAuthExceptionTests {

        @Test
        @DisplayName("Returns 401 Unauthorized")
        void shouldReturn401() {
            OAuthException ex = new OAuthException("GitHub exchange failed");

            ResponseEntity<ApiResponse<Void>> response = handler.handleOAuth(ex);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Error code is OAUTH_ERROR")
        void shouldUseOauthErrorCode() {
            OAuthException ex = new OAuthException("GitHub exchange failed");

            ResponseEntity<ApiResponse<Void>> response = handler.handleOAuth(ex);

            assertNotNull(response.getBody());
            assertEquals("OAUTH_ERROR", response.getBody().getError().getCode());
        }

        @Test
        @DisplayName("Error message mirrors exception message")
        void shouldPreserveMessage() {
            OAuthException ex = new OAuthException("Bad OAuth state");

            ResponseEntity<ApiResponse<Void>> response = handler.handleOAuth(ex);

            assertNotNull(response.getBody());
            assertEquals("Bad OAuth state", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("Response body is non-null and data field is absent")
        void shouldHaveNoData() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleOAuth(new OAuthException("err"));

            assertNotNull(response.getBody());
            assertNull(response.getBody().getData());
        }
    }

    // -------------------------------------------------------------------------
    // WebExchangeBindException  →  400
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Validation exception handler")
    class ValidationExceptionTests {

        /**
         * Builds a mocked {@link WebExchangeBindException} wired to the supplied field error.
         */
        private WebExchangeBindException mockBindException(String field, String message) {
            WebExchangeBindException ex = mock(WebExchangeBindException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("obj", field, message);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldError()).thenReturn(fieldError);
            return ex;
        }

        @Test
        @DisplayName("Returns 400 Bad Request")
        void shouldReturn400() {
            WebExchangeBindException ex = mockBindException("email", "Email must be valid");

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Error code is VALIDATION_ERROR")
        void shouldUseValidationErrorCode() {
            WebExchangeBindException ex = mockBindException("email", "Email must be valid");

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertNotNull(response.getBody());
            assertEquals("VALIDATION_ERROR", response.getBody().getError().getCode());
        }

        @Test
        @DisplayName("Captures failing field name")
        void shouldCaptureFieldName() {
            WebExchangeBindException ex = mockBindException("password", "Password too short");

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertNotNull(response.getBody());
            assertEquals("password", response.getBody().getError().getField());
        }

        @Test
        @DisplayName("Captures field-level message")
        void shouldCaptureFieldMessage() {
            WebExchangeBindException ex = mockBindException("email", "Email must be valid");

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertNotNull(response.getBody());
            assertEquals("Email must be valid", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("Falls back gracefully when no field error is present")
        void shouldFallBackWhenNoFieldError() {
            WebExchangeBindException ex = mock(WebExchangeBindException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldError()).thenReturn(null);

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            // Handler uses "unknown" / "Validation failed" as fallbacks
            assertEquals("unknown", response.getBody().getError().getField());
            assertEquals("Validation failed", response.getBody().getError().getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // WebClientResponseException  →  mirrors downstream status
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("WebClient downstream error handler")
    class WebClientErrorTests {

        @Test
        @DisplayName("Mirrors downstream 404 status")
        void shouldMirror404() {
            WebClientResponseException ex =
                    WebClientResponseException.create(404, "Not Found", null, null, null);

            ResponseEntity<ApiResponse<Void>> response = handler.handleWebClientError(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Mirrors downstream 503 status")
        void shouldMirror503() {
            WebClientResponseException ex =
                    WebClientResponseException.create(503, "Service Unavailable", null, null, null);

            ResponseEntity<ApiResponse<Void>> response = handler.handleWebClientError(ex);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        }

        @Test
        @DisplayName("Error code encodes downstream status (DOWNSTREAM_ERROR_<status>)")
        void shouldEncodeStatusInErrorCode() {
            WebClientResponseException ex =
                    WebClientResponseException.create(404, "Not Found", null, null, null);

            ResponseEntity<ApiResponse<Void>> response = handler.handleWebClientError(ex);

            assertNotNull(response.getBody());
            assertEquals("DOWNSTREAM_ERROR_404", response.getBody().getError().getCode());
        }
    }

    // -------------------------------------------------------------------------
    // AccessDeniedException  →  403
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("AccessDenied handler")
    class AccessDeniedTests {

        @Test
        @DisplayName("Returns 403 Forbidden")
        void shouldReturn403() {
            AccessDeniedException ex = new AccessDeniedException("nope");

            ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Error code is ACCESS_DENIED")
        void shouldUseAccessDeniedCode() {
            AccessDeniedException ex = new AccessDeniedException("nope");

            ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

            assertNotNull(response.getBody());
            assertEquals("ACCESS_DENIED", response.getBody().getError().getCode());
        }

        @Test
        @DisplayName("Returns standard permission message")
        void shouldReturnStandardMessage() {
            AccessDeniedException ex = new AccessDeniedException("nope");

            ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

            assertNotNull(response.getBody());
            assertEquals(
                    "Access denied: Insufficient permissions",
                    response.getBody().getError().getMessage()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Generic Exception  →  500
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Generic exception handler")
    class GenericExceptionTests {

        @Test
        @DisplayName("Returns 500 Internal Server Error")
        void shouldReturn500() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleGeneric(new RuntimeException("boom"));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        @DisplayName("Error code is INTERNAL_ERROR")
        void shouldUseInternalErrorCode() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleGeneric(new RuntimeException("boom"));

            assertNotNull(response.getBody());
            assertEquals("INTERNAL_ERROR", response.getBody().getError().getCode());
        }

        @Test
        @DisplayName("Returns generic user-safe message (no leak of internal detail)")
        void shouldReturnGenericMessage() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleGeneric(new RuntimeException("DB connection dropped"));

            assertNotNull(response.getBody());
            assertEquals(
                    "An unexpected system error occurred",
                    response.getBody().getError().getMessage()
            );
        }
    }

    // -------------------------------------------------------------------------
    // ApiResponse / ErrorDetails structure contracts
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ApiResponse and ErrorDetails structure contracts")
    class StructureTests {

        @Test
        @DisplayName("Success ApiResponse: data and message present, error absent")
        void successResponse_leanEnvelope() {
            ApiResponse<String> response = new ApiResponse<>("payload", "Operation OK");

            assertEquals("payload", response.getData());
            assertEquals("Operation OK", response.getMessage());
            assertNull(response.getError());
            assertNotNull(response.getTimestamp());
        }

        @Test
        @DisplayName("Error ApiResponse: error present, data absent")
        void errorResponse_errorPresent() {
            ErrorDetails details = new ErrorDetails("SOME_CODE", "Something went wrong");
            ApiResponse<Void> response = new ApiResponse<>(details);

            assertNotNull(response.getError());
            assertNull(response.getData());
            assertEquals("SOME_CODE", response.getError().getCode());
        }

        @Test
        @DisplayName("ErrorDetails: three-arg constructor sets all fields")
        void errorDetails_threeArgConstructor() {
            ErrorDetails details = new ErrorDetails("VALIDATION_ERROR", "Password too short", "password");

            assertEquals("VALIDATION_ERROR", details.getCode());
            assertEquals("Password too short", details.getMessage());
            assertEquals("password", details.getField());
        }

        @Test
        @DisplayName("ErrorDetails: two-arg convenience constructor leaves field null")
        void errorDetails_twoArgConstructorLeavesFieldNull() {
            ErrorDetails details = new ErrorDetails("INTERNAL_ERROR", "Unexpected error");

            assertEquals("INTERNAL_ERROR", details.getCode());
            assertEquals("Unexpected error", details.getMessage());
            assertNull(details.getField());
        }

        @Test
        @DisplayName("ApiResponse timestamp is always populated on construction")
        void timestamp_alwaysPresent() {
            ApiResponse<Void> error = new ApiResponse<>(new ErrorDetails("X", "y"));
            ApiResponse<String> success = new ApiResponse<>("data", "msg");

            assertNotNull(error.getTimestamp());
            assertNotNull(success.getTimestamp());
        }
    }
}