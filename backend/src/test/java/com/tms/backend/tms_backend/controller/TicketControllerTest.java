package com.tms.backend.tms_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.tms.backend.tms_backend.dto.PageResponse;
import com.tms.backend.tms_backend.dto.TicketResponse;
import com.tms.backend.tms_backend.entity.TicketStatus;
import com.tms.backend.tms_backend.exception.InvalidStatusTransitionException;
import com.tms.backend.tms_backend.exception.ResourceNotFoundException;
import com.tms.backend.tms_backend.service.TicketService;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private TicketService ticketService;

    @Test
    void createsTicketAndReturnsLocation() throws Exception {
        when(ticketService.create(any())).thenReturn(ticketResponse());

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Login failure\",\"description\":\"Cannot sign in\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tickets/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").doesNotExist())
                .andExpect(jsonPath("$.comments").doesNotExist());
    }

    @Test
    void delegatesDetailUpdateAndStatusRoutes() throws Exception {
        when(ticketService.getById(7L)).thenReturn(ticketResponse());
        when(ticketService.update(eq(7L), any())).thenReturn(ticketResponse());
        when(ticketService.updateStatus(eq(7L), any())).thenReturn(ticketResponse());

        mockMvc.perform(get("/api/v1/tickets/7")).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/tickets/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New title\",\"description\":\"New details\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/tickets/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        verify(ticketService).getById(7L);
        verify(ticketService).update(eq(7L), any());
        verify(ticketService).updateStatus(eq(7L), any());
    }

    @Test
    void appliesListDefaultsAndReturnsPageMetadata() throws Exception {
        when(ticketService.list(null, null, 0, 10)).thenReturn(
                new PageResponse<>(List.of(ticketResponse()), new PageResponse.PageMetadata(0, 10, 1, 1)));

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10));

        verify(ticketService).list(null, null, 0, 10);
    }

    @Test
    void rejectsInvalidInputBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"bad\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        mockMvc.perform(get("/api/v1/tickets?page=-1&size=101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsNotFoundAndInvalidTransitionErrors() throws Exception {
        when(ticketService.getById(99L)).thenThrow(new ResourceNotFoundException("Ticket not found: 99"));
        when(ticketService.updateStatus(eq(7L), any()))
                .thenThrow(new InvalidStatusTransitionException(TicketStatus.OPEN, TicketStatus.CLOSED));

        mockMvc.perform(get("/api/v1/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/tickets/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    private TicketResponse ticketResponse() {
        return new TicketResponse(7L, "Login failure", "Cannot sign in", TicketStatus.OPEN, null,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }
}