package com.itc.funkart.review.repository;

import com.itc.funkart.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductId(Long productId, Pageable pageable);

    Optional<Review> findByIdAndUserId(Long id, Long userId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);
}
