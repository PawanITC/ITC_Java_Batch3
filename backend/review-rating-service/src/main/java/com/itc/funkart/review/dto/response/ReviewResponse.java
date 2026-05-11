package com.itc.funkart.review.dto.response;

import com.itc.funkart.review.entity.Review;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long productId;
    private Long userId;
    private String author;
    private String title;
    private String comment;
    private int rating;
    private int likes;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .userId(r.getUserId())
                .author(r.getAuthor())
                .title(r.getTitle())
                .comment(r.getComment())
                .rating(r.getRating())
                .likes(r.getLikes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
