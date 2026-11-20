package com.livematch.matchservice.kafka;

import com.livematch.matchservice.dto.MatchEventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
public class MatchEventProducer {
    private static final Logger log = LoggerFactory.getLogger(MatchEventProducer.class);

    private final KafkaTemplate<String, MatchEventResponse> kafkaTemplate;
    private final String topic;

    public MatchEventProducer(
            KafkaTemplate<String, MatchEventResponse> kafkaTemplate,
            @Value("${app.kafka.topics.match-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(MatchEventResponse event) {
        kafkaTemplate.send(topic, event.matchId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to topic {}", event.id(), topic, ex);
                    } else {
                        log.info("Published event {} to topic {} [partition={}, offset={}]",
                                event.id(),
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

}
