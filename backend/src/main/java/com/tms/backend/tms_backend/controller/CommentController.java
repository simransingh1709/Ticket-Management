package com.tms.backend.tms_backend.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.tms_backend.dto.CommentCreateRequest;
import com.tms.backend.tms_backend.dto.CommentResponse;
import com.tms.backend.tms_backend.dto.PageResponse;
import com.tms.backend.tms_backend.service.CommentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(@PathVariable @Min(1) Long ticketId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.create(ticketId, request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + ticketId + "/comments/" + response.id()))
                .body(response);
    }

    @GetMapping
    public PageResponse<CommentResponse> list(
            @PathVariable @Min(1) Long ticketId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return commentService.listByTicketId(ticketId, page, size);
    }
}