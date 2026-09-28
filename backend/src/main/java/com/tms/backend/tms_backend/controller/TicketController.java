package com.tms.backend.tms_backend.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.tms_backend.dto.PageResponse;
import com.tms.backend.tms_backend.dto.TicketCreateRequest;
import com.tms.backend.tms_backend.dto.TicketResponse;
import com.tms.backend.tms_backend.dto.TicketStatusUpdateRequest;
import com.tms.backend.tms_backend.dto.TicketUpdateRequest;
import com.tms.backend.tms_backend.entity.TicketStatus;
import com.tms.backend.tms_backend.service.TicketService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketCreateRequest request) {
        TicketResponse response = ticketService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + response.id())).body(response);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse get(@PathVariable @Min(1) Long ticketId) {
        return ticketService.getById(ticketId);
    }

    @PutMapping("/{ticketId}")
    public TicketResponse update(@PathVariable @Min(1) Long ticketId,
            @Valid @RequestBody TicketUpdateRequest request) {
        return ticketService.update(ticketId, request);
    }

    @PatchMapping("/{ticketId}/status")
    public TicketResponse updateStatus(@PathVariable @Min(1) Long ticketId,
            @Valid @RequestBody TicketStatusUpdateRequest request) {
        return ticketService.updateStatus(ticketId, request);
    }

    @GetMapping
    public PageResponse<TicketResponse> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) @Size(max = 150) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ticketService.list(status, keyword, page, size);
    }
}