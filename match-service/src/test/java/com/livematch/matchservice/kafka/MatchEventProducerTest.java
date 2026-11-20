package com.livematch.matchservice.kafka;

import com.livematch.matchservice.dto.MatchEventResponse;
import com.livematch.matchservice.entity.EventType;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchEventProducerTest {

    @Mock
    private KafkaTemplate<String, MatchEventResponse> kafkaTemplate;

    private MatchEventProducer producer;

    private static final String TOPIC = "match-events";

    @BeforeEach
    void setUp() {
        producer = new MatchEventProducer(kafkaTemplate, TOPIC);
    }

    @Test
    void publish_shouldSendEventWithMatchIdAsKey() {
        MatchEventResponse event = new MatchEventResponse(
                UUID.randomUUID(), "benfica-vs-porto-2026", "Benfica", "Pavlidis",
                EventType.SHOT, 88.5, 48.0, 64, Instant.now()
        );

        SendResult<String, MatchEventResponse> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TOPIC, 0), 0L, 0, 0L, 0, 0
        );
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(TOPIC, event.matchId(), event))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        producer.publish(event);

        verify(kafkaTemplate, times(1))
                .send(eq(TOPIC), eq("benfica-vs-porto-2026"), eq(event));
    }

    @Test
    void publish_whenKafkaFails_shouldHandleErrorGracefully() {
        MatchEventResponse event = new MatchEventResponse(
                UUID.randomUUID(), "benfica-vs-porto-2026", "Benfica", "Pavlidis",
                EventType.SHOT, 88.5, 48.0, 64, Instant.now()
        );

        when(kafkaTemplate.send(TOPIC, event.matchId(), event))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Broker down")));

        // Should log the error via whenComplete without throwing an unhandled exception
        producer.publish(event);

        verify(kafkaTemplate, times(1))
                .send(eq(TOPIC), eq("benfica-vs-porto-2026"), eq(event));
    }
}

