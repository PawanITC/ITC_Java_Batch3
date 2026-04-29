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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    // ⭐ 1. Happy path
    @Test
    @WithMockUser(username = "10")
    void createReview_returnsOk_andResponseBody() throws Exception {

        ReviewRequest req = new ReviewRequest(4, "Nice");

        ReviewResponse resp = new ReviewResponse(
                UUID.randomUUID(),
                1L,
                10L,
                4,
                "Nice",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z")
        );

        Mockito.when(reviewService.createOrUpdateReview(eq(1L), eq(10L), any()))
                .thenReturn(resp);

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.rating").value(4));
    }

    // ⭐ 2. Validation failure
    @Test
    @WithMockUser(username = "10")
    void createReview_invalidRequest_returnsBadRequest() throws Exception {

        ReviewRequest invalidReq = new ReviewRequest(0, ""); // invalid rating/comment

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(reviewService);
    }

    // ⭐ 3. Unauthorized (no user)
    @Test
    void createReview_unauthorized_returns403() throws Exception {

        ReviewRequest req = new ReviewRequest(4, "Nice");

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ⭐ 4. Service throws exception → expect 500
    @Test
    @WithMockUser(username = "10")
    void createReview_serviceThrowsException_returns500() throws Exception {

        ReviewRequest req = new ReviewRequest(4, "Nice");

        Mockito.when(reviewService.createOrUpdateReview(eq(1L), eq(10L), any()))
                .thenThrow(new RuntimeException("Something went wrong"));

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }
}
