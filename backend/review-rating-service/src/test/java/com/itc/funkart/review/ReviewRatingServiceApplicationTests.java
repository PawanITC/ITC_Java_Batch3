package com.itc.funkart.review;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ReviewRatingServiceApplicationTests {

    // Redis autoconfiguration is excluded via application-test.yaml
    // Kafka must be mocked to prevent connection to a real broker
    @MockitoBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
