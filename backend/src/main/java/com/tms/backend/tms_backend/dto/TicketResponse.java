package com.tms.backend.tms_backend.dto;

import java.time.Instant;

import com.tms.backend.tms_backend.entity.TicketStatus;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        String assigneeId,
        Instant createdAt,
        Instant updatedAt) {
}