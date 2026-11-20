package com.livematch.matchservice.dto;

import com.livematch.matchservice.entity.EventType;
import jakarta.validation.constraints.*;

// Inbound API payload
public record MatchEventRequest(
        @NotBlank String matchId,
        @NotBlank String team,
        @NotBlank String playerName,
        @NotNull EventType eventType,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double x,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double y,
        @NotNull @Min(0) @Max(130) Integer minute
) {}
