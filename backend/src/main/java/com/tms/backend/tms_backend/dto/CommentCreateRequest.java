package com.tms.backend.tms_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank @Size(min = 1, max = 1000) String content,
        String author) {
}