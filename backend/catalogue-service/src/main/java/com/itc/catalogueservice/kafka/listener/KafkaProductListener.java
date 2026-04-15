package com.itc.catalogueservice.kafka.listener;

import com.itc.catalogueservice.exception.kafka.InvalidProductEventException;
import com.itc.catalogueservice.kafka.listener.dto.ProductEventDTO;
import com.itc.catalogueservice.service.CatalogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaProductListener {

    private final CatalogueService catalogueService;
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 3000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )

    @KafkaListener(
            topics = "product-events",
            groupId = "catalogue-service"
    )
    public void listen(ProductEventDTO event) {

        switch (event.getEventType()) {

            case CREATED:
            case UPDATED:
                if (event.getProduct() == null ||
                        event.getProduct().getId() == null ||
                        event.getProduct().getName() == null ||
                        event.getProduct().getCategory() == null) {
                    throw new InvalidProductEventException("Product name is missing");
                }
                catalogueService.saveProductToCache(event.getProduct());
                break;

            case DELETED:
                catalogueService.deleteProductFromCache(
                        String.valueOf(event.getProduct().getId()),
                        event.getProduct().getCategory()
                );
                break;
        }
    }
}