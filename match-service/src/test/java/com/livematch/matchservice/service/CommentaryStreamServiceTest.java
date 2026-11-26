package com.livematch.matchservice.service;

import com.livematch.matchservice.dto.MatchCommentaryEvent;
import com.livematch.matchservice.entity.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CommentaryStreamServiceTest {

    private CommentaryStreamService service;

    @BeforeEach
    void setUp() {
        service = new CommentaryStreamService();
    }

    private MatchCommentaryEvent sampleCommentary(String matchId) {
        return new MatchCommentaryEvent(
                UUID.randomUUID(),
                matchId,
                "Sporting",
                "Trincão",
                EventType.SHOT,
                91.5,
                52.0,
                72,
                0.34,
                "What a strike!",
                Instant.now()
        );
    }

    @Test
    void subscribe_registersListenerForMatchOnly() {
        SseEmitter emitter = service.subscribe("sporting-vs-braga-2026");

        assertThat(emitter).isNotNull();
        assertThat(service.activeListenerCount("sporting-vs-braga-2026")).isEqualTo(1);
        assertThat(service.activeListenerCount("other-match")).isZero();
    }

    @Test
    void broadcast_noActiveListeners_isSafeNoOp() {
        assertThatCode(() -> service.broadcast(sampleCommentary("unwatched-match")))
                .doesNotThrowAnyException();

        assertThat(service.activeListenerCount("unwatched-match")).isZero();
    }

    @Test
    void broadcast_removesCompletedEmitterAndKeepsActiveOnes() {
        SseEmitter closedEmitter = service.subscribe("sporting-vs-braga-2026");
        service.subscribe("sporting-vs-braga-2026");
        service.subscribe("benfica-vs-porto-2026");
        closedEmitter.complete();

        service.broadcast(sampleCommentary("sporting-vs-braga-2026"));

        assertThat(service.activeListenerCount("sporting-vs-braga-2026")).isEqualTo(1);
        assertThat(service.activeListenerCount("benfica-vs-porto-2026")).isEqualTo(1);
    }

    @Test
    void sendHeartbeats_removesCompletedEmittersAndKeepsLiveOnes() {
        SseEmitter emitter = service.subscribe("sporting-vs-braga-2026");
        service.subscribe("benfica-vs-porto-2026");
        emitter.complete();

        service.sendHeartbeats();

        assertThat(service.activeListenerCount("sporting-vs-braga-2026")).isZero();
        assertThat(service.activeListenerCount("benfica-vs-porto-2026")).isEqualTo(1);
    }

    @Test
    void shutdown_completesAndClearsAllActiveListeners() {
        SseEmitter emitterA = service.subscribe("sporting-vs-braga-2026");
        SseEmitter emitterB = service.subscribe("benfica-vs-porto-2026");

        service.shutdown();

        assertThat(service.activeListenerCount("sporting-vs-braga-2026")).isZero();
        assertThat(service.activeListenerCount("benfica-vs-porto-2026")).isZero();
        // Attempting to send after shutdown confirms emitters were actually completed, not just removed from the map
        assertThatCode(() -> emitterA.send(SseEmitter.event().comment("after-shutdown")))
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> emitterB.send(SseEmitter.event().comment("after-shutdown")))
                .isInstanceOf(IllegalStateException.class);
    }
}
