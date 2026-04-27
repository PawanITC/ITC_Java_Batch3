package com.itc.funkart.repository;
import com.itc.funkart.entity.Review;
import com.itc.funkart.projection.RatingStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    Page<Review> findByProductId(Long productId, Pageable pageable);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    @Query("select avg(r.rating), count(r) from Review r where r.productId = :productId")
    RatingStats getRatingStatsByProductId(@Param("productId") Long productId);
}
