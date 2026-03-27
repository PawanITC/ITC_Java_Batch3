package com.itc.catalogueservice.controller;

import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import com.itc.catalogueservice.service.CatalogueService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogueController.class)
class CatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogueService catalogueService;

    @Test
    void getProducts_shouldReturnProducts() throws Exception {

        when(catalogueService.getProducts(1,10,null,null,null,null))
                .thenReturn(CompletableFuture.completedFuture(List.of(new ProductDTO())));

        MvcResult result = mockMvc.perform(get("/catalogue/products"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").value("Products retrieved"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getProducts_shouldReturnNotFound() throws Exception {

        when(catalogueService.getProducts(1,10,null,null,null,null))
                .thenReturn(CompletableFuture.failedFuture(new NoProductsException()));

        MvcResult result = mockMvc.perform(get("/catalogue/products"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").value("No products available"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getProducts_shouldAcceptPaginationParams() throws Exception {

        when(catalogueService.getProducts(2,5,null,null,null,null))
                .thenReturn(CompletableFuture.completedFuture(List.of(new ProductDTO())));

        MvcResult result = mockMvc.perform(get("/catalogue/products")
                        .param("page","2")
                        .param("size","5"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getProducts_shouldReturnBadRequest_whenInvalidParams() throws Exception {

        mockMvc.perform(get("/catalogue/products")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void getProducts_shouldReturnGatewayTimeout() throws Exception {

        when(catalogueService.getProducts(1,10,null,null,null,null))
                .thenReturn(CompletableFuture.failedFuture(
                        new TimeoutException()
                ));

        MvcResult result = mockMvc.perform(get("/catalogue/products"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(HttpStatus.GATEWAY_TIMEOUT.value()))
                .andExpect(jsonPath("$.message").value("External service timed out"));
    }

    @Test
    void getProducts_shouldReturnServiceUnavailable() throws Exception {

        CompletableFuture<List<ProductDTO>> future = new CompletableFuture<>();

        future.completeExceptionally(
                CallNotPermittedException.createCallNotPermittedException(
                        io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("test")
                )
        );

        when(catalogueService.getProducts(1,10,null,null,null,null))
                .thenReturn(future);

        MvcResult result = mockMvc.perform(get("/catalogue/products"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(HttpStatus.SERVICE_UNAVAILABLE.value()))
                .andExpect(jsonPath("$.message").value("Service temporarily unavailable"));
    }

    @Test
    void getProducts_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        mockMvc.perform(get("/catalogue/products")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("Page must be at least 1"));
    }

    @Test
    void getProducts_shouldReturnTooManyRequests_whenRateLimited() throws Exception {

        CompletableFuture<List<ProductDTO>> future = new CompletableFuture<>();
        future.completeExceptionally(RequestNotPermitted.createRequestNotPermitted(
                io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("test")
        ));

        when(catalogueService.getProducts(1,10,null,null,null,null))
                .thenReturn(future);

        MvcResult result = mockMvc.perform(get("/catalogue/products"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(HttpStatus.TOO_MANY_REQUESTS.value()))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded. Please try again later"));
    }

    /*@Test
    void getProducts_shouldReturnTooManyRequests_whenBulkheadFull() throws Exception {

        when(catalogueService.getProducts(1,10,null,null,null,null))
                .thenReturn(CompletableFuture.failedFuture(new RejectedExecutionException()));

        MvcResult result = mockMvc.perform(get("/catalogue/products"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(HttpStatus.TOO_MANY_REQUESTS.value()))
                .andExpect(jsonPath("$.message").value("Too many concurrent requests"));
    }*/
}