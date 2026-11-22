package com.livematch.matchservice.controller;

import com.livematch.matchservice.service.CommentaryStreamService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchStreamController {

    private final CommentaryStreamService streamService;

    public MatchStreamController(CommentaryStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping(value = "/{matchId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMatchCommentary(
            @PathVariable String matchId,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no"); // Disables Nginx buffering for SSE
        return streamService.subscribe(matchId);
    }
}
