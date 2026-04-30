package com.itc.funkart.kafka;

import com.itc.funkart.avro.ReviewCreatedEvent;

import com.itc.funkart.avro.ReviewDeletedEvent;
import com.itc.funkart.event.ReviewUpdatedEvent;
import com.itc.funkart.service.RatingAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

    private final RatingAggregationService ratingAggregationService;

    @KafkaListener(
            topics = "review.events.v1",
            groupId = "rating-aggregator-service"
    )
    public void handle(SpecificRecordBase event) {

        log.info("Received event: {}", event.getClass().getSimpleName());

        if (event instanceof ReviewCreatedEvent created) {
            ratingAggregationService.handleCreated(
                    created.getPayload().getProductId(),
                    created.getPayload().getRating()
            );
        }

        else if (event instanceof ReviewUpdatedEvent updated) {
            ratingAggregationService.handleUpdated(
                    updated.getPayload().getProductId()
            );
        }

        else if (event instanceof ReviewDeletedEvent deleted) {
            ratingAggregationService.handleDeleted(
                    deleted.getPayload().getProductId()
            );
        }

        else {
            log.warn("Unknown event type: {}", event.getClass().getName());
        }
    }
}
