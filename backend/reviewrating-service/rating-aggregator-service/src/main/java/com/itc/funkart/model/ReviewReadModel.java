package com.itc.funkart.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reviews_read")
public class ReviewReadModel {
    @Id
    private Long id;
    private Long productId;
    private Long userId;
    private int rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}


