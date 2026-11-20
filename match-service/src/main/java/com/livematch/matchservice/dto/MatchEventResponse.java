package com.livematch.matchservice.dto;

import com.livematch.matchservice.entity.EventType;
import com.livematch.matchservice.entity.MatchEvent;

import java.time.Instant;
import java.util.UUID;

// API response & Kafka JSON payload
public record MatchEventResponse(
        UUID id,
        String matchId,
        String team,
        String playerName,
        EventType eventType,
        Double x,
        Double y,
        Integer minute,
        Instant createdAt
) {
    public static MatchEventResponse from(MatchEvent event) {
        return new MatchEventResponse(
                event.getId(),
                event.getMatchId(),
                event.getTeam(),
                event.getPlayerName(),
                event.getEventType(),
                event.getX(),
                event.getY(),
                event.getMinute(),
                event.getCreatedAt()
        );
    }
}
