package com.itc.catalogueservice.client;

import com.itc.catalogueservice.exception.external.ExternalServiceFailureException;
import com.itc.catalogueservice.exception.external.ExternalServiceTimeoutException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class ProductApiClientTest {

    private final ProductApiClient client = new ProductApiClient();

    //Test timeout fallback
    @Test
    void getProductsFallback_shouldReturnTimeoutException() {

        CompletableFuture<?> result =
                client.getProductsFallback(new TimeoutException());

        CompletionException ex = assertThrows(
                CompletionException.class,
                result::join
        );

        assertTrue(ex.getCause() instanceof ExternalServiceTimeoutException);
    }

    //Test other failure fallback
    @Test
    void getProductsFallback_shouldReturnFailureException() {

        CompletableFuture<?> result =
                client.getProductsFallback(new RuntimeException());

        CompletionException ex = assertThrows(
                CompletionException.class,
                result::join
        );

        assertTrue(ex.getCause() instanceof ExternalServiceFailureException);
    }
}