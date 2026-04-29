package com.itc.funkart.controller;

import com.itc.funkart.model.ProductRatingSummary;
import com.itc.funkart.service.RatingAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RatingSummaryControllerTest {

    @Test
    void testGetRatingSummary_cacheHit() throws Exception {
        RatingAggregationService service = mock(RatingAggregationService.class);
        ProductRatingSummary summary = new ProductRatingSummary(1L);
        summary.setAverageRating(4.5);
        summary.setRatingCount(Long.valueOf(10));

        when(service.getFromCache(1L)).thenReturn(Optional.of(summary));

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new RatingSummaryController(service))
                .build();

        mvc.perform(get("/aggregator/products/1/rating-summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.ratingCount").value(10));

        verify(service, times(1)).getFromCache(1L);
        verify(service, never()).recomputeAndCache(anyLong());
    }

    @Test
    void testGetRatingSummary_cacheMiss_recompute() throws Exception {
        RatingAggregationService service = mock(RatingAggregationService.class);

        ProductRatingSummary summary = new ProductRatingSummary(1L);
        summary.setAverageRating(3.0);
        summary.setRatingCount(Long.valueOf(5));

        when(service.getFromCache(1L)).thenReturn(Optional.empty());
        when(service.recomputeAndCache(1L)).thenReturn(summary);

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new RatingSummaryController(service))
                .build();

        mvc.perform(get("/aggregator/products/1/rating-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(3.0))
                .andExpect(jsonPath("$.ratingCount").value(5));

        verify(service).getFromCache(1L);
        verify(service).recomputeAndCache(1L);
    }
}
