package com.tms.backend.tms_backend.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long ticketId,
        String content,
        String author,
        Instant createdAt) {
}