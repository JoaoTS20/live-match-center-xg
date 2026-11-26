package com.livematch.matchservice.service;

import com.livematch.matchservice.dto.MatchCommentaryEvent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CommentaryStreamService {

    private static final Logger log = LoggerFactory.getLogger(CommentaryStreamService.class);

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByMatch = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String matchId) {
        SseEmitter emitter = new SseEmitter(0L); // No server-side timeout
        CopyOnWriteArrayList<SseEmitter> matchEmitters =
                emittersByMatch.computeIfAbsent(matchId, id -> new CopyOnWriteArrayList<>());
        matchEmitters.add(emitter);

        Runnable cleanup = () -> removeEmitter(matchId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());

        // Flush SSE headers immediately through dev proxy / Nginx without triggering onmessage
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException | IllegalStateException ex) {
            removeEmitter(matchId, emitter);
        }

        log.info("SSE client subscribed to match '{}' (active listeners: {})", matchId, matchEmitters.size());
        return emitter;
    }

    public void broadcast(MatchCommentaryEvent commentary) {
        List<SseEmitter> emitters = emittersByMatch.get(commentary.matchId());
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE listeners for match '{}'", commentary.matchId());
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                // Unnamed SSE data frame triggers Angular's eventSource.onmessage
                emitter.send(SseEmitter.event().data(commentary));
            } catch (IOException | IllegalStateException ex) {
                log.debug("Removing disconnected SSE client for match '{}'", commentary.matchId());
                removeEmitter(commentary.matchId(), emitter);
            }
        }
    }

    @Scheduled(fixedRate = 25_000)
    public void sendHeartbeats() {
        emittersByMatch.forEach((matchId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException ex) {
                    removeEmitter(matchId, emitter);
                }
            }
        });
    }

    public int activeListenerCount(String matchId) {
        List<SseEmitter> emitters = emittersByMatch.get(matchId);
        return emitters == null ? 0 : emitters.size();
    }

    private void removeEmitter(String matchId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByMatch.get(matchId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByMatch.remove(matchId, emitters);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SSE streams for {} active matches", emittersByMatch.size());
        emittersByMatch.forEach((matchId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                emitter.complete();
            }
        });
        emittersByMatch.clear();
    }
}
