package com.itc.funkart.repository;



import com.itc.funkart.entity.Review;
import com.itc.funkart.model.ReviewProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewReadRepository extends JpaRepository<Review, Long> {

    @Query("""
           select r.productId as productId, r.rating as rating
           from Review r
           where r.productId = :productId
           """)
    List<ReviewProjection> findByProductId(String productId);
}
