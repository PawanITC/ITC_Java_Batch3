// Unit test for GlobalExceptionHandler
// Verifies that exceptions are correctly mapped to ApiResponse
// Ensures proper HTTP status codes and messages are returned
// Uses ConstraintViolationException as a sample case
// Note: Full exception flow is also covered in controller tests
package com.itc.catalogueservice.exception;

import com.itc.catalogueservice.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void handleConstraintViolation_shouldReturnBadRequest() {

        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Page must be at least 1");

        ConstraintViolationException ex =
                new ConstraintViolationException(Set.of(violation));

        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<?>> response =
                handler.handleConstraintViolation(ex);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Page must be at least 1", response.getBody().getMessage());
    }
}