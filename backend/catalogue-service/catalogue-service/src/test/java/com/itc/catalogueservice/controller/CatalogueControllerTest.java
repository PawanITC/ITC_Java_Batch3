package com.itc.catalogueservice.controller;

import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import com.itc.catalogueservice.exception.external.ExternalServiceFailureException;
import com.itc.catalogueservice.exception.external.ExternalServiceTimeoutException;
import com.itc.catalogueservice.service.CatalogueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
                .andExpect(jsonPath("$.status").value(200))
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
                .andExpect(jsonPath("$.status").value(404))
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
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getProducts_shouldReturnBadRequest_whenInvalidParams() throws Exception {

        mockMvc.perform(get("/catalogue/products")
                        .param("page", "0")) // invalid if you have @Min(1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getProducts_shouldReturnGatewayTimeout() throws Exception {

        when(catalogueService.getProducts(1,10,null,null,null,null))
                .thenReturn(CompletableFuture.failedFuture(
                        new ExternalServiceTimeoutException()
                ));

        MvcResult result = mockMvc.perform(get("/catalogue/products"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.message").value("External service timed out"));
    }

    @Test
    void getProducts_shouldReturnServiceUnavailable() throws Exception {

        when(catalogueService.getProducts(1,10,null,null,null,null))
                .thenReturn(CompletableFuture.failedFuture(
                        new ExternalServiceFailureException()
                ));

        MvcResult result = mockMvc.perform(get("/catalogue/products"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("External service failed"));
    }

    @Test
    void getProducts_shouldReturnBadRequest_whenInvalidPage() throws Exception {

        mockMvc.perform(get("/catalogue/products")
                        .param("page", "0")) // invalid
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page must be at least 1"));
    }


}