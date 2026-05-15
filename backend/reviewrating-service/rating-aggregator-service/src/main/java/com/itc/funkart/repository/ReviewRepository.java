package com.itc.funkart.repository;



import com.itc.funkart.dto.RatingStatsDto;
import com.itc.funkart.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("""
        select new com.itc.funkart.dto.RatingStatsDto(
            avg(r.rating),
            count(r)
        )
        from Review r
        where r.productId = :productId
    """)
    RatingStatsDto getRatingStatsByProductId(@Param("productId") Long productId);
}

