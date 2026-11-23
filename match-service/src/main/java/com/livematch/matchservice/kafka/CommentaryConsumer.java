package com.livematch.matchservice.kafka;

import com.livematch.matchservice.dto.MatchCommentaryEvent;
import com.livematch.matchservice.service.CommentaryStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CommentaryConsumer {
    private static final Logger log = LoggerFactory.getLogger(CommentaryConsumer.class);

    private final JsonMapper jsonMapper;
    private final CommentaryStreamService streamService;

    public CommentaryConsumer(JsonMapper jsonMapper, CommentaryStreamService streamService) {
        this.jsonMapper = jsonMapper;
        this.streamService = streamService;
    }

    @KafkaListener(topics = "${app.kafka.topics.match-commentary}")
    public void onCommentary(String payload) {
        try {
            MatchCommentaryEvent commentary = jsonMapper.readValue(payload, MatchCommentaryEvent.class);
            log.info("Streaming commentary for [{}] {}' {} (xG={})",
                    commentary.matchId(), commentary.minute(), commentary.playerName(), commentary.xg());
            streamService.broadcast(commentary);
        } catch (JacksonException ex) {
            log.warn("Skipping invalid commentary payload: {}", ex.getMessage());
        }
    }


}
