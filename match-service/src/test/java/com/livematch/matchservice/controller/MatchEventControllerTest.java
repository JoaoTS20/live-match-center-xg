package com.livematch.matchservice.controller;

import com.livematch.matchservice.dto.MatchEventRequest;
import com.livematch.matchservice.dto.MatchEventResponse;
import com.livematch.matchservice.entity.EventType;
import com.livematch.matchservice.service.MatchEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchEventController.class)
class MatchEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchEventService service;

    @Test
    void createEvent_withValidPayload_returns201() throws Exception {
        MatchEventResponse mockResponse = new MatchEventResponse(
                UUID.randomUUID(), "match-1", "Benfica", "Pavlidis",
                EventType.SHOT, 88.5, 48.0, 64, Instant.now()
        );
        when(service.recordEvent(any(MatchEventRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matchId": "match-1",
                                  "team": "Benfica",
                                  "playerName": "Pavlidis",
                                  "eventType": "SHOT",
                                  "x": 88.5,
                                  "y": 48.0,
                                  "minute": 64
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerName").value("Pavlidis"))
                .andExpect(jsonPath("$.eventType").value("SHOT"));
    }

    @Test
    void createEvent_withOutOfBoundsCoordinates_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matchId": "match-1",
                                  "team": "Benfica",
                                  "playerName": "Pavlidis",
                                  "eventType": "SHOT",
                                  "x": 150.0,
                                  "y": 48.0,
                                  "minute": 64
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Match Event"))
                .andExpect(jsonPath("$.errors.x").exists());
    }
}

