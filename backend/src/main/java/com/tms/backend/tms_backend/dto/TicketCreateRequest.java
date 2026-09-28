package com.tms.backend.tms_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketCreateRequest(
        @NotBlank @Size(min = 5, max = 150) String title,
        @NotBlank String description,
        String assigneeId) {
}