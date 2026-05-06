package com.itc.funkart.product.exceptions;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public String getCode() {
        return "UNAUTHORIZED_ACCESS";
    }
}
