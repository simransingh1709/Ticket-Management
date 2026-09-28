package com.tms.backend.tms_backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.tms.backend.tms_backend.repository.CommentRepository;
import com.tms.backend.tms_backend.repository.TicketRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TicketLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void cleanDatabase() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @Test
    void createsRetrievesUpdatesAndCompletesAllLifecyclePaths() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Login failure\",\"description\":\"Cannot sign in\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        long ticketId = ticketRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/v1/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.comments").doesNotExist());
        mockMvc.perform(put("/api/v1/tickets/" + ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated login failure\",\"description\":\"Updated details\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated login failure"));

        for (String statusName : new String[] {"IN_PROGRESS", "RESOLVED", "CLOSED"}) {
            mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + statusName + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(statusName));
        }
        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        org.assertj.core.api.Assertions.assertThat(createResponse).contains("Login failure");
    }

    @Test
    void supportsTheTwoCancellationPaths() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Cancel open\",\"description\":\"Cancel me\"}"))
                .andExpect(status().isCreated());
        long openTicketId = ticketRepository.findAll().get(0).getId();
        mockMvc.perform(patch("/api/v1/tickets/" + openTicketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Cancel progress\",\"description\":\"Cancel me\"}"))
                .andExpect(status().isCreated());
        long progressTicketId = ticketRepository.findAll().stream()
                .filter(ticket -> ticket.getId() != openTicketId).findFirst().orElseThrow().getId();
        mockMvc.perform(patch("/api/v1/tickets/" + progressTicketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/tickets/" + progressTicketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
    }
}