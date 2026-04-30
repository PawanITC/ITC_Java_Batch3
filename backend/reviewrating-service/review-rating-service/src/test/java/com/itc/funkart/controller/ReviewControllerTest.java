package com.itc.funkart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.dto.ReviewRequest;
import com.itc.funkart.dto.ReviewResponse;
import com.itc.funkart.entity.Review;
import com.itc.funkart.repository.ReviewRepository;
import com.itc.funkart.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private ReviewRepository reviewRepository; // ✅ important

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------- POST ----------------

    @Test
    @WithMockUser(username = "10")
    void createReview_success() throws Exception {

        ReviewRequest req = new ReviewRequest(5, "Great");

        ReviewResponse resp = new ReviewResponse(
                1L, 1L, 10L, 5,
                "Great",
                Instant.now()

        );

        Mockito.when(reviewService.createOrUpdateReview(eq(1L), eq(10L), any()))
                .thenReturn(resp);

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.userId").value(10));
    }

    @Test
    @WithMockUser(username = "10")
    void createReview_validationFail() throws Exception {

        ReviewRequest req = new ReviewRequest(0, "");

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(reviewService);
    }

    @Test
    void createReview_unauthorized() throws Exception {

        ReviewRequest req = new ReviewRequest(5, "Nice");

        mockMvc.perform(post("/api/v1/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ---------------- GET (single) ----------------

    @Test
    void getReview_success() throws Exception {

        Review review = new Review();
        review.setId(1L);
        review.setProductId(1L);
        review.setUserId(10L);
        review.setRating(5);
        review.setReviewText("Nice");
        review.setCreatedAt(LocalDateTime.now());

        Mockito.when(reviewRepository.findByProductIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(review));

        mockMvc.perform(get("/api/v1/reviews/1/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.userId").value(10));
    }

    @Test
    void getReview_notFound() throws Exception {

        Mockito.when(reviewRepository.findByProductIdAndUserId(1L, 10L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/reviews/1/10"))
                .andExpect(status().isInternalServerError()); // since you throw RuntimeException
    }

    // ---------------- GET (list) ----------------

    @Test
    void getReviews_success() throws Exception {

        ReviewResponse r1 = new ReviewResponse(
                1L, 1L, 10L, 5, "Good",
                Instant.now()
        );

        Page<ReviewResponse> page = new PageImpl<>(List.of(r1));

        Mockito.when(reviewService.getReviewsForProduct(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/reviews/1?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(1));
    }

    @Test
    void getReviews_empty() throws Exception {

        Page<ReviewResponse> emptyPage = Page.empty();

        Mockito.when(reviewService.getReviewsForProduct(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---------------- DELETE ----------------

    @Test
    @WithMockUser(username = "10")
    void deleteReview_success() throws Exception {

        mockMvc.perform(delete("/api/v1/reviews/1"))
                .andExpect(status().isOk());

        Mockito.verify(reviewService).deleteReview(1L, 10L);
    }

    @Test
    void deleteReview_unauthorized() throws Exception {

        mockMvc.perform(delete("/api/v1/reviews/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "10")
    void deleteReview_serviceThrows() throws Exception {

        Mockito.doThrow(new RuntimeException("fail"))
                .when(reviewService).deleteReview(1L, 10L);

        mockMvc.perform(delete("/api/v1/reviews/1"))
                .andExpect(status().isInternalServerError());
    }
}