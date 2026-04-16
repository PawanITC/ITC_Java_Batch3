package com.itc.funkart.exception;

public class OrderNotFound extends RuntimeException
{
    public OrderNotFound(String message)
    {
        super(message);
    }
}
