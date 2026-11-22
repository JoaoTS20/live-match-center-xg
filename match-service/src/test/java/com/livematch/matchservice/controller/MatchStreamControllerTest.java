package com.livematch.matchservice.controller;

import com.livematch.matchservice.service.CommentaryStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchStreamController.class)
class MatchStreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentaryStreamService streamService;

    @Test
    void streamMatchCommentary_opensSseStreamWithProxyHeaders() throws Exception {
        when(streamService.subscribe("sporting-vs-braga-2026")).thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/v1/matches/sporting-vs-braga-2026/stream"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(header().string("X-Accel-Buffering", "no"));

        verify(streamService).subscribe("sporting-vs-braga-2026");
    }
}
