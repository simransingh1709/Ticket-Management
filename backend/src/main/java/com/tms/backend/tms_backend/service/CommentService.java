package com.tms.backend.tms_backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.tms_backend.dto.CommentCreateRequest;
import com.tms.backend.tms_backend.dto.CommentResponse;
import com.tms.backend.tms_backend.dto.PageResponse;
import com.tms.backend.tms_backend.entity.Comment;
import com.tms.backend.tms_backend.entity.Ticket;
import com.tms.backend.tms_backend.exception.ResourceNotFoundException;
import com.tms.backend.tms_backend.repository.CommentRepository;
import com.tms.backend.tms_backend.repository.TicketRepository;

@Service
public class CommentService {

    private static final String DEFAULT_AUTHOR = "system";

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public CommentResponse create(Long ticketId, CommentCreateRequest request) {
        Ticket ticket = findTicket(ticketId);
        Comment comment = new Comment(ticket, request.content().trim(), normalizeAuthor(request.author()));
        return toResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> listByTicketId(Long ticketId, int page, int size) {
        validatePage(page, size);
        findTicket(ticketId);
        Page<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(
                ticketId, PageRequest.of(page, size));
        return new PageResponse<>(
                comments.getContent().stream().map(this::toResponse).toList(),
                new PageResponse.PageMetadata(
                        comments.getNumber(), comments.getSize(), comments.getTotalElements(), comments.getTotalPages()));
    }

    private Ticket findTicket(Long ticketId) {
        if (ticketId == null) {
            throw new ResourceNotFoundException("Ticket not found: null");
        }
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getTicket().getId(), comment.getContent(),
                comment.getAuthor(), comment.getCreatedAt());
    }

    private String normalizeAuthor(String author) {
        if (author == null || author.isBlank()) {
            return DEFAULT_AUTHOR;
        }
        return author.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
    }
}