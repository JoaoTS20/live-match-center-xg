package com.livematch.matchservice.service;

import com.livematch.matchservice.config.RedisCacheConfig;
import com.livematch.matchservice.dto.MatchEventRequest;
import com.livematch.matchservice.dto.MatchEventResponse;
import com.livematch.matchservice.entity.MatchEvent;
import com.livematch.matchservice.kafka.MatchEventProducer;
import com.livematch.matchservice.repository.MatchEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchEventService {

    private static final Logger log = LoggerFactory.getLogger(MatchEventService.class);

    private final MatchEventRepository repository;
    private final MatchEventProducer producer;
    private final CacheManager cacheManager;

    public MatchEventService(MatchEventRepository repository, MatchEventProducer producer, CacheManager cacheManager) {
        this.repository = repository;
        this.producer = producer;
        this.cacheManager = cacheManager;
    }

    public MatchEventResponse recordEvent(MatchEventRequest request) {
        MatchEvent event = MatchEvent.builder()
                .matchId(request.matchId())
                .team(request.team())
                .playerName(request.playerName())
                .eventType(request.eventType())
                .coordinates(request.x(), request.y())
                .minute(request.minute())
                .build();

        // 1. Persist & commit to PostgreSQL first
        MatchEvent saved = repository.save(event);
        MatchEventResponse response = MatchEventResponse.from(saved);

        // 2. Evict stale timeline in Redis BEFORE Kafka triggers narrator-worker
        evictMatchTimelineCache(saved.getMatchId());

        // 3. Publish to Kafka only after DB & cache are consistent
        producer.publish(response);
        return response;
    }

    @Cacheable(cacheNames = RedisCacheConfig.MATCH_TIMELINE_CACHE, key = "#matchId")
    @Transactional(readOnly = true)
    public List<MatchEventResponse> getMatchTimeline(String matchId) {
        return repository.findByMatchIdOrderByMinuteAscCreatedAtAsc(matchId)
                .stream()
                .map(MatchEventResponse::from)
                .toList();
    }

    private void evictMatchTimelineCache(String matchId) {
        try {
            Cache cache = cacheManager.getCache(RedisCacheConfig.MATCH_TIMELINE_CACHE);
            if (cache != null) {
                cache.evict(matchId);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to evict timeline cache for match '{}': {}", matchId, ex.getMessage());
        }
    }
}
