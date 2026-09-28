package com.tms.backend.tms_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OpenApiContractTest {

    @Test
    void contractContainsAllRoutesSchemasAndNoPriorityOrEmbeddedComments() throws Exception {
        Path contract = Path.of("../specs/001-ticket-management-backend/contracts/openapi.yaml");
        String openApi = Files.readString(contract);

        assertThat(openApi).contains(
                "/api/v1/tickets:",
                "/api/v1/tickets/{ticketId}:",
                "/api/v1/tickets/{ticketId}/status:",
                "/api/v1/tickets/{ticketId}/comments:",
                "TicketCreateRequest:",
                "TicketResponse:",
                "CommentCreateRequest:",
                "CommentResponse:",
                "ProblemDetail:");
        assertThat(openApi).doesNotContain("priority:");
        String ticketResponseSchema = openApi.substring(openApi.indexOf("    TicketResponse:"),
            openApi.indexOf("    CommentResponse:"));
        assertThat(ticketResponseSchema).doesNotContain("comments:");
        assertThat(openApi).contains("maxLength: 150", "maximum: 100", "'409':");
    }
}