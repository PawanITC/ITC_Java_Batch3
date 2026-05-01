package com.itc.funkart.product_service.exceptions;

public class UnauthorizedException extends RuntimeException {
    public String getCode() {
        return "UNAUTHORIZED_ACCESS";
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
