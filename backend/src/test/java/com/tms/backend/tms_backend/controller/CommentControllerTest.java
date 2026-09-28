package com.tms.backend.tms_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.tms.backend.tms_backend.dto.CommentResponse;
import com.tms.backend.tms_backend.dto.PageResponse;
import com.tms.backend.tms_backend.exception.ResourceNotFoundException;
import com.tms.backend.tms_backend.service.CommentService;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private CommentService commentService;

    @Test
    void createsAndListsCommentsIndependently() throws Exception {
        when(commentService.create(eq(7L), any())).thenReturn(commentResponse());
        when(commentService.listByTicketId(7L, 0, 10)).thenReturn(
                new PageResponse<>(List.of(commentResponse()), new PageResponse.PageMetadata(0, 10, 1, 1)));

        mockMvc.perform(post("/api/v1/tickets/7/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"A comment\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tickets/7/comments/12"))
                .andExpect(jsonPath("$.ticketId").value(7));
        mockMvc.perform(get("/api/v1/tickets/7/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("A comment"));

        verify(commentService).create(eq(7L), any());
        verify(commentService).listByTicketId(7L, 0, 10);
    }

    @Test
    void rejectsInvalidCommentAndMapsMissingTicket() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/7/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        when(commentService.listByTicketId(99L, 0, 10))
                .thenThrow(new ResourceNotFoundException("Ticket not found: 99"));
        mockMvc.perform(get("/api/v1/tickets/99/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private CommentResponse commentResponse() {
        return new CommentResponse(12L, 7L, "A comment", "system",
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}