package com.itc.funkart.repository;



import com.itc.funkart.model.RatingStats;
import com.itc.funkart.model.ReviewReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewReadModelRepository extends JpaRepository<ReviewReadModel, Long> {

    @Query("""
        SELECT new com.itc.funkart.model.RatingStats(
            AVG(r.rating), COUNT(r)
        )
        FROM ReviewReadModel r
        WHERE r.productId = :productId
    """)
    RatingStats getRatingStatsByProductId(Long productId);
}
