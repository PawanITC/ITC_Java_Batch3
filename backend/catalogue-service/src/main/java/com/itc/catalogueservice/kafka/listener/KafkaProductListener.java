package com.itc.catalogueservice.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.itc.catalogueservice.exception.kafka.InvalidProductEventException;
import com.itc.catalogueservice.kafka.listener.dto.ProductEventDTO;
import com.itc.catalogueservice.service.CatalogueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProductListener {

    private final CatalogueService catalogueService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "product-events",
            groupId = "catalogue-service"
    )
    public void listen(String message) {
        log.info("RECEIVED EVENT MESSAGE: {}", message);

        ProductEventDTO event;
        try {
            event = objectMapper.readValue(message, ProductEventDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event message: {}", message, e);
            throw new InvalidProductEventException("Invalid JSON format");
        }


        switch (event.getEventType()) {

            case CREATED:
            case UPDATED:
                if (event.getProduct() == null ||
                        event.getProduct().getId() == null ||
                        event.getProduct().getName() == null ||
                        event.getProduct().getCategory() == null) {
                    throw new InvalidProductEventException("Invalid product data");
                }

                catalogueService.saveProductToCache(event.getProduct());
                break;

            case DELETED:
                if (event.getProduct() == null ||
                        event.getProduct().getId() == null ||
                        event.getProduct().getCategory() == null) {
                    throw new InvalidProductEventException("Invalid delete event");
                }

                catalogueService.deleteProductFromCache(
                        String.valueOf(event.getProduct().getId()),
                        event.getProduct().getCategory()
                );
                break;
        }

        log.info("Updated cache for product ID: {}",
                event.getProduct() != null ? event.getProduct().getId() : "null");
    }
}
