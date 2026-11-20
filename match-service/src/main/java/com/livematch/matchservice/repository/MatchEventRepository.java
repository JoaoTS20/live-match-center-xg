package com.livematch.matchservice.repository;

import com.livematch.matchservice.entity.MatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchEventRepository extends JpaRepository<MatchEvent, UUID> {
    List<MatchEvent> findByMatchIdOrderByMinuteAsc(String matchId);
}
