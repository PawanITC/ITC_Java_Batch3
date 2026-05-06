package com.itc.funkart.repository;
import com.itc.funkart.dto.RatingStatsDto;
import com.itc.funkart.entity.Review;
import com.itc.funkart.projection.RatingStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductId(Long productId);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);
    Page<Review> findByProductId(Long productId, Pageable pageable);

}
