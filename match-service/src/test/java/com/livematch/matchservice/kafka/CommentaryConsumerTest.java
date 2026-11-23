package com.livematch.matchservice.kafka;

import com.livematch.matchservice.dto.MatchCommentaryEvent;
import com.livematch.matchservice.entity.EventType;
import com.livematch.matchservice.service.CommentaryStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CommentaryConsumerTest {

    @Mock
    private CommentaryStreamService streamService;

    private CommentaryConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CommentaryConsumer(JsonMapper.builder().build(), streamService);
    }

    @Test
    void onCommentary_validPayload_deserializesAndBroadcasts() {
        String json = """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "matchId": "sporting-vs-braga-2026",
                  "team": "Sporting",
                  "playerName": "Trincão",
                  "eventType": "SHOT",
                  "x": 91.5,
                  "y": 52.0,
                  "minute": 72,
                  "xg": 0.3412,
                  "commentary": "Trincao fires just over the bar!",
                  "createdAt": "2026-11-22T20:15:30.123456Z"
                }
                """;

        consumer.onCommentary(json);

        ArgumentCaptor<MatchCommentaryEvent> captor = ArgumentCaptor.forClass(MatchCommentaryEvent.class);
        verify(streamService).broadcast(captor.capture());

        MatchCommentaryEvent event = captor.getValue();
        assertThat(event.matchId()).isEqualTo("sporting-vs-braga-2026");
        assertThat(event.playerName()).isEqualTo("Trincão");
        assertThat(event.eventType()).isEqualTo(EventType.SHOT);
        assertThat(event.xg()).isEqualTo(0.3412);
        assertThat(event.commentary()).isEqualTo("Trincao fires just over the bar!");
    }

    @Test
    void onCommentary_malformedJson_skipsWithoutBroadcasting() {
        consumer.onCommentary("{not-valid-json");

        verifyNoInteractions(streamService);
    }
}
