package com.livematch.matchservice.service;

import com.livematch.matchservice.dto.MatchEventRequest;
import com.livematch.matchservice.dto.MatchEventResponse;
import com.livematch.matchservice.entity.EventType;
import com.livematch.matchservice.entity.MatchEvent;
import com.livematch.matchservice.kafka.MatchEventProducer;
import com.livematch.matchservice.repository.MatchEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchEventServiceTest {
    @Mock
    private MatchEventRepository repository;

    @Mock
    private MatchEventProducer producer;

    @InjectMocks
    private MatchEventService service;

    @Test
    void recordEvent_shouldSaveToDbAndPublishToKafka() {
        MatchEventRequest request = new MatchEventRequest(
                "benfica-vs-porto-2026", "Benfica", "Pavlidis",
                EventType.SHOT, 88.5, 48.0, 64
        );

        when(repository.save(any(MatchEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MatchEventResponse response = service.recordEvent(request);

        assertThat(response.matchId()).isEqualTo("benfica-vs-porto-2026");
        assertThat(response.playerName()).isEqualTo("Pavlidis");
        assertThat(response.eventType()).isEqualTo(EventType.SHOT);

        verify(repository, times(1)).save(any(MatchEvent.class));
        verify(producer, times(1)).publish(response);
    }

    @Test
    void getMatchTimeline_shouldReturnMappedEventsInOrder() {
        MatchEvent event = MatchEvent.builder()
                .matchId("match-1")
                .team("Benfica")
                .playerName("Pavlidis")
                .eventType(EventType.GOAL)
                .coordinates(92.0, 50.0)
                .minute(75)
                .build();

        when(repository.findByMatchIdOrderByMinuteAsc("match-1"))
                .thenReturn(List.of(event));

        List<MatchEventResponse> timeline = service.getMatchTimeline("match-1");

        assertThat(timeline).hasSize(1);
        assertThat(timeline.getFirst().eventType()).isEqualTo(EventType.GOAL);
    }

    @Test
    void matchEventBuilder_shouldFailFastWhenRequiredFieldIsMissing() {
        assertThatThrownBy(() -> MatchEvent.builder()
                .matchId("match-1")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }
}
