package com.livematch.matchservice.service;

import com.livematch.matchservice.config.RedisCacheConfig;
import com.livematch.matchservice.dto.MatchEventRequest;
import com.livematch.matchservice.dto.MatchEventResponse;
import com.livematch.matchservice.entity.EventType;
import com.livematch.matchservice.entity.MatchEvent;
import com.livematch.matchservice.kafka.MatchEventProducer;
import com.livematch.matchservice.repository.MatchEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

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

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private MatchEventService service;

    @BeforeEach
    void setUp() {
        service = new MatchEventService(repository, producer, cacheManager);
    }

    private MatchEventRequest sampleRequest() {
        return new MatchEventRequest(
                "sporting-vs-braga-2026",
                "Sporting",
                "Trincao",
                EventType.SHOT,
                91.5,
                52.0,
                72
        );
    }

    private MatchEvent sampleEntity(int minute, EventType type) {
        return MatchEvent.builder()
                .matchId("sporting-vs-braga-2026")
                .team("Sporting")
                .playerName("Trincao")
                .eventType(type)
                .coordinates(91.5, 52.0)
                .minute(minute)
                .build();
    }

    @Test
    void recordEvent_persistsEvictsCacheAndPublishesInStrictOrder() {
        MatchEventRequest request = sampleRequest();
        when(repository.save(any(MatchEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache(RedisCacheConfig.MATCH_TIMELINE_CACHE)).thenReturn(cache);

        MatchEventResponse response = service.recordEvent(request);

        assertThat(response.matchId()).isEqualTo("sporting-vs-braga-2026");
        assertThat(response.team()).isEqualTo("Sporting");
        assertThat(response.playerName()).isEqualTo("Trincao");
        assertThat(response.eventType()).isEqualTo(EventType.SHOT);
        assertThat(response.x()).isEqualTo(91.5);
        assertThat(response.y()).isEqualTo(52.0);
        assertThat(response.minute()).isEqualTo(72);

        // Enforces the synchronous write path: DB commit -> Redis evict -> Kafka publish
        InOrder order = inOrder(repository, cache, producer);
        order.verify(repository).save(any(MatchEvent.class));
        order.verify(cache).evict("sporting-vs-braga-2026");
        order.verify(producer).publish(response);
    }

    @Test
    void recordEvent_redisEvictionFails_stillPublishesToKafka() {
        when(repository.save(any(MatchEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache(RedisCacheConfig.MATCH_TIMELINE_CACHE)).thenReturn(cache);
        doThrow(new RuntimeException("Redis unreachable"))
                .when(cache).evict("sporting-vs-braga-2026");

        MatchEventResponse response = service.recordEvent(sampleRequest());

        assertThat(response.matchId()).isEqualTo("sporting-vs-braga-2026");
        verify(producer).publish(response);
    }

    @Test
    void recordEvent_databaseSaveFails_doesNotEvictCacheOrPublish() {
        when(repository.save(any(MatchEvent.class)))
                .thenThrow(new RuntimeException("PostgreSQL connection lost"));

        assertThatThrownBy(() -> service.recordEvent(sampleRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("PostgreSQL connection lost");

        verifyNoInteractions(cacheManager, producer);
    }

    @Test
    void getMatchTimeline_returnsOrderedEventsFromRepository() {
        MatchEvent shot = sampleEntity(72, EventType.SHOT);
        MatchEvent goal = sampleEntity(72, EventType.GOAL);
        when(repository.findByMatchIdOrderByMinuteAscCreatedAtAsc("sporting-vs-braga-2026"))
                .thenReturn(List.of(shot, goal));

        List<MatchEventResponse> timeline = service.getMatchTimeline("sporting-vs-braga-2026");

        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).eventType()).isEqualTo(EventType.SHOT);
        assertThat(timeline.get(1).eventType()).isEqualTo(EventType.GOAL);
        verify(repository).findByMatchIdOrderByMinuteAscCreatedAtAsc("sporting-vs-braga-2026");
    }

}
