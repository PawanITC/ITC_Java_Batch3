package com.itc.funkart.controller;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReview_returnsOk() throws Exception {
        ReviewRequest req = new ReviewRequest(4, "Nice");

        ReviewResponse resp = new ReviewResponse(
                1L, 1L, 10L, 4, "Nice", Instant.now(), Instant.now()
        );

        Mockito.when(reviewService.createOrUpdateReview(1L, 10L, req))
                .thenReturn(resp);

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
