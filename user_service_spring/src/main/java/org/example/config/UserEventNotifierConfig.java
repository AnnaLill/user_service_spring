package org.example.config;

import org.example.kafka.KafkaUserEventNotifier;
import org.example.kafka.NoOpUserEventNotifier;
import org.example.kafka.UserEvent;
import org.example.kafka.UserEventNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class UserEventNotifierConfig {

    @Bean
    public UserEventNotifier userEventNotifier(
            @Autowired(required = false) KafkaTemplate<String, UserEvent> kafkaTemplate,
            @Value("${app.kafka.user-events-topic:user-events}") String topic,
            Environment env) {
        boolean enabled = Boolean.parseBoolean(env.getProperty("app.kafka.enabled", "true"));
        if (enabled) {
            if (kafkaTemplate == null) {
                throw new IllegalStateException(
                        "app.kafka.enabled=true, но KafkaTemplate не создан. Проверь spring.kafka.bootstrap-servers и зависимость spring-kafka.");
            }
            return new KafkaUserEventNotifier(kafkaTemplate, topic);
        }
        return new NoOpUserEventNotifier();
    }
}
