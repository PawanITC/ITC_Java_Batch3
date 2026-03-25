package com.itc.funkart.payment.exception;

import com.itc.funkart.payment.response.ApiResponse;
import com.itc.funkart.payment.response.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle PaymentException
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<?>> handlePaymentException(
            PaymentException ex, WebRequest request) {

        logger.error("✗ Payment error: {}", ex.getMessage());

        ErrorDetails errorDetails = new ErrorDetails(
                "PAYMENT_ERROR",
                ex.getMessage()
        );

        return new ResponseEntity<>(
                new ApiResponse<>(errorDetails),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Handle IllegalArgumentException (validation errors)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        logger.error("✗ Illegal argument: {}", ex.getMessage());

        ErrorDetails errorDetails = new ErrorDetails(
                "INVALID_ARGUMENT",
                ex.getMessage()
        );

        return new ResponseEntity<>(
                new ApiResponse<>(errorDetails),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(
            Exception ex, WebRequest request) {

        logger.error("✗ Unexpected error: {}", ex.getMessage(), ex);

        ErrorDetails errorDetails = new ErrorDetails(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred"
        );

        return new ResponseEntity<>(
                new ApiResponse<>(errorDetails),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}