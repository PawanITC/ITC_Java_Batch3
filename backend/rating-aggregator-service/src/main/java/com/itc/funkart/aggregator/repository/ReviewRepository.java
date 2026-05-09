package com.itc.funkart.aggregator.repository;

/**
 * @deprecated Old scaffold — the aggregator does not query raw reviews.
 *             Rating aggregation is driven by Kafka events consumed in
 *             {@link com.itc.funkart.aggregator.kafka.ReviewEventConsumer}
 *             which writes to {@link com.itc.funkart.aggregator.repository.RatingSummaryRepository}.
 *             Delete this file and {@code entity/Review.java}.
 */
@Deprecated
public interface ReviewRepository {
}
