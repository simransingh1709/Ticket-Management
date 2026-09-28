package com.tms.backend.tms_backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class TicketDiscoveryIntegrationTest {

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
    void searchesFiltersPaginatesAndRetrievesCommentsIndependently() throws Exception {
        createTicket("Login failure", "Users cannot sign in");
        createTicket("Export report", "CSV export is slow");
        createTicket("Password reset", "Login recovery email is delayed");
        long loginTicketId = ticketRepository.findAll().stream()
                .filter(ticket -> ticket.getTitle().equals("Login failure")).findFirst().orElseThrow().getId();

        mockMvc.perform(get("/api/v1/tickets?keyword=LOGIN&page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(get("/api/v1/tickets?status=OPEN&keyword=login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(post("/api/v1/tickets/" + loginTicketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Investigating\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/tickets/" + loginTicketId + "/comments?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Investigating"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void combinesStatusAndKeywordAfterLifecycleChange() throws Exception {
        createTicket("Open login", "Login issue");
        createTicket("Open export", "Export issue");
        long loginId = ticketRepository.findAll().stream()
                .filter(ticket -> ticket.getTitle().equals("Open login")).findFirst().orElseThrow().getId();
        mockMvc.perform(patch("/api/v1/tickets/" + loginId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tickets?status=IN_PROGRESS&keyword=login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Open login"));
    }

    private void createTicket(String title, String description) throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"description\":\"" + description + "\"}"))
                .andExpect(status().isCreated());
    }
}