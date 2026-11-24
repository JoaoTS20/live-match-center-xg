package com.livematch.matchservice.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "match_events",
        indexes = @Index(
                name = "idx_match_events_timeline",
                columnList = "match_id, minute, created_at"
        )
)
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String matchId;

    @Column(nullable = false)
    private String team;

    @Column(nullable = false)
    private String playerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    @Column(nullable = false)
    private Integer minute;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected MatchEvent() {
    }

    private MatchEvent(MatchEventBuilder builder) {
        this.matchId = builder.matchId;
        this.team = builder.team;
        this.playerName = builder.playerName;
        this.eventType = builder.eventType;
        this.x = builder.x;
        this.y = builder.y;
        this.minute = builder.minute;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public static MatchEventBuilder builder() {
        return new MatchEventBuilder();
    }

    public UUID getId() { return id; }
    public String getMatchId() { return matchId; }
    public String getTeam() { return team; }
    public String getPlayerName() { return playerName; }
    public EventType getEventType() { return eventType; }
    public Double getX() { return x; }
    public Double getY() { return y; }
    public Integer getMinute() { return minute; }
    public Instant getCreatedAt() { return createdAt; }

    public static class MatchEventBuilder {
        private String matchId;
        private String team;
        private String playerName;
        private EventType eventType;
        private Double x;
        private Double y;
        private Integer minute;

        private MatchEventBuilder() {
        }

        public MatchEventBuilder matchId(String matchId) {
            this.matchId = matchId;
            return this;
        }

        public MatchEventBuilder team(String team) {
            this.team = team;
            return this;
        }

        public MatchEventBuilder playerName(String playerName) {
            this.playerName = playerName;
            return this;
        }

        public MatchEventBuilder eventType(EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public MatchEventBuilder coordinates(Double x, Double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public MatchEventBuilder minute(Integer minute) {
            this.minute = minute;
            return this;
        }

        public MatchEvent build() {
            Objects.requireNonNull(matchId, "matchId must not be null");
            Objects.requireNonNull(team, "team must not be null");
            Objects.requireNonNull(playerName, "playerName must not be null");
            Objects.requireNonNull(eventType, "eventType must not be null");
            Objects.requireNonNull(x, "x must not be null");
            Objects.requireNonNull(y, "y must not be null");
            Objects.requireNonNull(minute, "minute must not be null");
            return new MatchEvent(this);
        }
    }
}
