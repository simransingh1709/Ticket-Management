package com.tms.backend.tms_backend.dto;

import com.tms.backend.tms_backend.entity.TicketStatus;

import jakarta.validation.constraints.NotNull;

public record TicketStatusUpdateRequest(@NotNull TicketStatus status) {
}