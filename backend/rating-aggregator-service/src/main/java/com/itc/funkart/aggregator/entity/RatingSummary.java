package com.itc.funkart.aggregator.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rating_summary")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingSummary {

    @Id
    private Long productId;

    @Column(nullable = false)
    private int totalReviews;

    @Column(nullable = false)
    private int sumRatings;

    @Column(nullable = false)
    private double averageRating;

    @Column(nullable = false)
    private int oneStar;

    @Column(nullable = false)
    private int twoStar;

    @Column(nullable = false)
    private int threeStar;

    @Column(nullable = false)
    private int fourStar;

    @Column(nullable = false)
    private int fiveStar;
}
