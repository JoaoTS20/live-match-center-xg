package com.livematch.matchservice.dto;

import com.livematch.matchservice.entity.EventType;

import java.time.Instant;
import java.util.UUID;

public record MatchCommentaryEvent(
        UUID id,
        String matchId,
        String team,
        String playerName,
        EventType eventType,
        Double x,
        Double y,
        Integer minute,
        Double xg,
        String commentary,
        Instant createdAt
) {}

