package org.example.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public final class KafkaUserEventNotifier implements UserEventNotifier {

    private static final Logger log = LoggerFactory.getLogger(KafkaUserEventNotifier.class);

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private final String topic;

    public KafkaUserEventNotifier(KafkaTemplate<String, UserEvent> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void notify(UserEvent event) {
        try {
            kafkaTemplate.send(topic, event.email(), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Kafka send failed for {}: {}", event, ex.toString());
                }
            });
        } catch (Exception e) {
            log.warn("Kafka publish error for {}: {}", event, e.toString());
        }
    }
}
