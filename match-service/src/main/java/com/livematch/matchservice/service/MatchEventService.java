package com.livematch.matchservice.service;

import com.livematch.matchservice.dto.MatchEventRequest;
import com.livematch.matchservice.dto.MatchEventResponse;
import com.livematch.matchservice.entity.MatchEvent;
import com.livematch.matchservice.kafka.MatchEventProducer;
import com.livematch.matchservice.repository.MatchEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchEventService {

    private final MatchEventRepository repository;
    private final MatchEventProducer producer;

    public MatchEventService(MatchEventRepository repository, MatchEventProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @Transactional
    public MatchEventResponse recordEvent(MatchEventRequest request) {
        MatchEvent event = MatchEvent.builder()
                .matchId(request.matchId())
                .team(request.team())
                .playerName(request.playerName())
                .eventType(request.eventType())
                .coordinates(request.x(), request.y())
                .minute(request.minute())
                .build();

        MatchEvent saved = repository.save(event);
        MatchEventResponse response = MatchEventResponse.from(saved);

        producer.publish(response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<MatchEventResponse> getMatchTimeline(String matchId) {
        return repository.findByMatchIdOrderByMinuteAsc(matchId)
                .stream()
                .map(MatchEventResponse::from)
                .toList();
    }
}
