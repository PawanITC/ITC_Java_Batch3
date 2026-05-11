package com.itc.funkart.aggregator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RatingAggregatorServiceApplicationTests {

    // Redis autoconfiguration is excluded via application-test.yaml
    // This service has no KafkaTemplate (consumer only) — no mock needed here.
    // The KafkaListenerContainerFactory will not start consumers in @SpringBootTest
    // unless @EmbeddedKafka is present.

    @Test
    void contextLoads() {
    }
}
