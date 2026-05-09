package com.itc.funkart.aggregator.controller;

import com.itc.funkart.aggregator.auth.JwtService;
import com.itc.funkart.aggregator.auth.PrincipalFactory;
import com.itc.funkart.aggregator.dto.RatingSummaryResponse;
import com.itc.funkart.aggregator.service.RatingSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RatingSummaryController.class)
class RatingSummaryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RatingSummaryService summaryService;
    // SecurityConfig wires these — must be mocked to prevent NoSuchBeanDefinitionException
    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    PrincipalFactory principalFactory;

    @Test
    void getSummary_existingProduct_returns200WithBody() throws Exception {
        RatingSummaryResponse response = RatingSummaryResponse.builder()
                .productId(5L).totalReviews(10).averageRating(4.2)
                .oneStar(0).twoStar(1).threeStar(2).fourStar(4).fiveStar(3)
                .build();

        when(summaryService.getSummary(5L)).thenReturn(response);

        mockMvc.perform(get("/rating-summary/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(5))
                .andExpect(jsonPath("$.totalReviews").value(10))
                .andExpect(jsonPath("$.averageRating").value(4.2))
                .andExpect(jsonPath("$.fiveStar").value(3));
    }

    @Test
    void getSummary_productWithNoReviews_returns200WithZeroedBody() throws Exception {
        RatingSummaryResponse empty = RatingSummaryResponse.empty(99L);
        when(summaryService.getSummary(99L)).thenReturn(empty);

        mockMvc.perform(get("/rating-summary/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(99))
                .andExpect(jsonPath("$.totalReviews").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0));
    }

    @Test
    void getSummary_isPublic_noAuthRequired() throws Exception {
        when(summaryService.getSummary(5L)).thenReturn(RatingSummaryResponse.empty(5L));

        // No authentication set — should still return 200 (public endpoint)
        mockMvc.perform(get("/rating-summary/5"))
                .andExpect(status().isOk());
    }
}
