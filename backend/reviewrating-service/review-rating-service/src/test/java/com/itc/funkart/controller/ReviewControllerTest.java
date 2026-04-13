





/*
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
*/

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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
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

    // ✅ 1. Happy path
    @Test
    @WithMockUser(username = "10")
    void createReview_returnsOk_andResponseBody() throws Exception {

        ReviewRequest req = new ReviewRequest(4, "Nice");

        ReviewResponse resp = new ReviewResponse(
                1L, 1L, 10L, 4, "Nice",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z")
        );

        Mockito.when(reviewService.createOrUpdateReview(eq(1L), eq(10L), any()))
                .thenReturn(resp);

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ✅ 2. Validation failure (e.g., rating out of range or missing fields)
    @Test
    @WithMockUser
    void createReview_invalidRequest_returnsBadRequest() throws Exception {
        ReviewRequest invalidReq = new ReviewRequest(0, ""); // assume invalid

        mockMvc.perform(post("/api/v1/reviews/1")
                        .param("productId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(reviewService);
    }

    // ✅ 3. Unauthorized (no user)
    @Test
    void createReview_unauthorized_returns401() throws Exception {
        ReviewRequest req = new ReviewRequest(4, "Nice");

        mockMvc.perform(post("/api/v1/reviews/1")
                        .param("productId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ✅ 4. Service throws exception → expect 500 (or mapped exception)
    @Test
    @WithMockUser
    void createReview_serviceThrowsException_returnsInternalServerError() throws Exception {
        ReviewRequest req = new ReviewRequest(4, "Nice");

        Mockito.when(reviewService.createOrUpdateReview(eq(1L), eq(10L), any()))
                .thenThrow(new RuntimeException("Something went wrong"));

        mockMvc.perform(post("/api/v1/reviews/1")
                        .param("productId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    // ✅ 5. Missing required param (productId)
    @Test
    @WithMockUser
    void createReview_missingProductId_returnsBadRequest() throws Exception {
        ReviewRequest req = new ReviewRequest(4, "Nice");

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
