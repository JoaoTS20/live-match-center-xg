package com.livematch.matchservice.controller;

import com.livematch.matchservice.dto.MatchEventRequest;
import com.livematch.matchservice.dto.MatchEventResponse;
import com.livematch.matchservice.service.MatchEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class MatchEventController {
    private final MatchEventService service;

    public MatchEventController(MatchEventService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchEventResponse createEvent(@Valid @RequestBody MatchEventRequest request) {
        return service.recordEvent(request);
    }

    @GetMapping("/{matchId}")
    public List<MatchEventResponse> getTimeline(@PathVariable String matchId) {
        return service.getMatchTimeline(matchId);
    }

}
