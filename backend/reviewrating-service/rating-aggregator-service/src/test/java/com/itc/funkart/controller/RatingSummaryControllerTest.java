package com.itc.funkart.controller;

import com.itc.funkart.dto.ProductRatingSummaryResponse;
import com.itc.funkart.model.ProductRatingSummary;
import com.itc.funkart.repository.ProductRatingSummaryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RatingSummaryController.class)
class RatingSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRatingSummaryRepository summaryRepository;

    @Test
    void testGetSummary_WhenSummaryExists() throws Exception {
        Long productId = 10L;

        ProductRatingSummary summary = new ProductRatingSummary(productId);
        summary.setAverageRating(4.5);
        summary.setRatingCount(20L);

        when(summaryRepository.findById(productId))
                .thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/v1/rating-summary/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.ratingCount").value(20));
    }

    @Test
    void testGetSummary_WhenSummaryDoesNotExist() throws Exception {
        Long productId = 99L;

        when(summaryRepository.findById(productId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/rating-summary/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(99))
                .andExpect(jsonPath("$.averageRating").doesNotExist())
                .andExpect(jsonPath("$.ratingCount").doesNotExist());
    }
}
