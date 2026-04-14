package com.itc.funkart.kafka;




import com.itc.funkart.event.ReviewCreatedEvent;
import com.itc.funkart.service.RatingAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventListener.class);

    private final RatingAggregationService ratingAggregationService;

    public ReviewEventListener(RatingAggregationService ratingAggregationService) {
        this.ratingAggregationService = ratingAggregationService;
    }

    @KafkaListener(topics = "review-created-topic", groupId = "rating-aggregator-group")
    public void onReviewCreated(ReviewCreatedEvent event) {
        log.info("Received ReviewCreatedEvent: productId={}, rating={}",
                event.getProductId(), event.getRating());
        ratingAggregationService.handleNewReview(event);
    }
}