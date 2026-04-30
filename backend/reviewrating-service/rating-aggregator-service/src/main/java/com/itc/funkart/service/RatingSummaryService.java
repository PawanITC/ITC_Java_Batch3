package com.itc.funkart.service;



public interface RatingSummaryService {

    /**
     * Recalculates the rating summary (average rating + rating count)
     * for the given productId by querying the ReviewRepository.
     *
     * This method is called by:
     *  - The rating-aggregator-service Kafka consumer
     *  - Any manual recomputation triggers (if needed)
     */
    void recalculateSummary(Long productId);
}
