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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exhaustive unit test suite for {@link GlobalExceptionHandler}.
 * <p>Directly invokes handler methods to bypass complex MockMvc setup for validation
 * and specific business exceptions, ensuring 100% line and branch coverage.</p>
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("Validation & Security Handlers")
    class ValidationAndSecurityTests {

        /**
         * Updated to reflect the switch from string concatenation to specific field extraction.
         */
        @Test
        @DisplayName("Validation: MethodArgumentNotValidException -> 400 with specific field")
        void handleValidation() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            // We pick the first error found
            FieldError error = new FieldError("user", "email", "invalid format");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldError()).thenReturn(error);

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("email", response.getBody().getError().getField());
            assertEquals("invalid format", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("Constraint: ConstraintViolationException -> 400")
        void handleConstraintViolation() {
            ConstraintViolationException ex = new ConstraintViolationException("Invalid param", Set.of());
            ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Invalid param", response.getBody().getError().getMessage());
            assertNull(response.getBody().getError().getField());
        }

        @Test
        @DisplayName("Auth: JwtAuthenticationException -> 401")
        void handleJwtAuthentication() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleJwtAuthentication(new JwtAuthenticationException("Token expired"));
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNull(response.getBody().getError().getField());
        }
    }

    @Nested
    @DisplayName("Standard Business Handlers")
    class BusinessExceptionTests {

        @Test
        @DisplayName("NotFound: NotFoundException -> 404")
        void handleNotFound() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleNotFound(new NotFoundException("Resource missing"));
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("BadRequest: BadRequestException -> 400")
        void handleBadRequest() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleBadRequest(new BadRequestException("Malformed request"));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Conflict: AlreadyExistsException -> 409 (Email Field check)")
        void handleConflict() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleConflict(new AlreadyExistsException("Duplicate entry"));

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            // Verifying our hardcoded "email" field logic for conflicts
            assertEquals("email", response.getBody().getError().getField());
        }

        @Test
        @DisplayName("Forbidden: ForbiddenException -> 403")
        void handleForbidden() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleForbidden(new ForbiddenException("Access denied"));
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Arguments: IllegalArgumentException -> 400")
        void handleIllegalArgument() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleIllegalArgument(new IllegalArgumentException("Illegal arg"));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Fallback & Security System Handlers")
    class SystemExceptionTests {

        @Test
        @DisplayName("Fallback: Generic Exception -> 500")
        void handleGeneric() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleGeneric(new RuntimeException("Oops"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Internal server error", response.getBody().getError().getMessage());
            assertNull(response.getBody().getError().getField());
        }

        @Test
        @DisplayName("Security: AccessDeniedException -> 403")
        void handleAccessDenied() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleAccessDenied(new org.springframework.security.access.AccessDeniedException("Denied"));
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Access denied", response.getBody().getError().getMessage());
        }

        @Test
        @DisplayName("Unauthorized: UnauthorizedException -> 401 (Log Warn Branch)")
        void handleUnauthorized() {
            UnauthorizedException ex = new UnauthorizedException("Session expired");
            ResponseEntity<ApiResponse<Void>> response = handler.handleUnauthorized(ex);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UNAUTHORIZED", response.getBody().getError().getCode());
            assertEquals("Session expired", response.getBody().getError().getMessage());
        }
    }

    @Test
    @DisplayName("OAuth: OAuthException -> 400 with StackTrace Logging")
    void handleOAuth() {
        OAuthException ex = new OAuthException("GitHub Handshake Failed");
        ResponseEntity<ApiResponse<Void>> response = handler.handleOAuth(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().getError().getCode());
        assertEquals("GitHub Handshake Failed", response.getBody().getError().getMessage());
    }
}