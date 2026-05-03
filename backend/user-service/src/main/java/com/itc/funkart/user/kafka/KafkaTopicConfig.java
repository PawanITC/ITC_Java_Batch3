package com.itc.funkart.user.kafka;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * <h2>Kafka Infrastructure Provisioning</h2>
 *
 * <p>Responsible for the automatic creation and management of Kafka topics required
 * by the User Domain. This configuration uses the AdminClient to ensure topics
 * exist on the cluster before any publishing occurs.</p>
 *
 * <p><b>Topic Ownership:</b> The user-service owns the lifecycle of user-related
 * topics. Consumers (like email-service) only reference the names, they do not
 * define the infrastructure.</p>
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Provisions the User Signup topic.
     *
     * <p><b>Configuration:</b>
     * <ul>
     *   <li>3 Partitions: Allows for horizontal scaling of consumers.</li>
     *   <li>1 Replica: Standard for development (increase to 3 for production).</li>
     * </ul>
     * </p>
     *
     * @return A {@link NewTopic} definition for the signup event stream.
     */
    @Bean
    public NewTopic signupTopic() {
        return TopicBuilder.name(KafkaTopics.USER_SIGNUP)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Provisions the Auth Login topic for tracking security events.
     *
     * @return A {@link NewTopic} definition for the login event stream.
     */
    @Bean
    public NewTopic loginTopic() {
        return TopicBuilder.name(KafkaTopics.AUTH_LOGIN)
                .partitions(3)
                .replicas(1)
                .build();
    }
}