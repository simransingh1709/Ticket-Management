package com.tms.backend.tms_backend.service;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.tms_backend.dto.PageResponse;
import com.tms.backend.tms_backend.dto.TicketCreateRequest;
import com.tms.backend.tms_backend.dto.TicketResponse;
import com.tms.backend.tms_backend.dto.TicketStatusUpdateRequest;
import com.tms.backend.tms_backend.dto.TicketUpdateRequest;
import com.tms.backend.tms_backend.entity.Ticket;
import com.tms.backend.tms_backend.entity.TicketStatus;
import com.tms.backend.tms_backend.exception.InvalidStatusTransitionException;
import com.tms.backend.tms_backend.exception.ResourceNotFoundException;
import com.tms.backend.tms_backend.repository.TicketRepository;
import com.tms.backend.tms_backend.repository.TicketSpecifications;

@Service
public class TicketService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketResponse create(TicketCreateRequest request) {
        Ticket ticket = new Ticket(
                normalizeRequired(request.title()),
                normalizeRequired(request.description()),
                normalizeOptional(request.assigneeId()));
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long ticketId) {
        return toResponse(findTicket(ticketId));
    }

    @Transactional
    public TicketResponse update(Long ticketId, TicketUpdateRequest request) {
        Ticket ticket = findTicket(ticketId);
        ticket.setTitle(normalizeRequired(request.title()));
        ticket.setDescription(normalizeRequired(request.description()));
        ticket.setAssigneeId(normalizeOptional(request.assigneeId()));
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(Long ticketId, TicketStatusUpdateRequest request) {
        Ticket ticket = findTicket(ticketId);
        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus requestedStatus = request.status();
        if (!currentStatus.canTransitionTo(requestedStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, requestedStatus);
        }

        ticket.setStatus(requestedStatus);
        try {
            return toResponse(ticketRepository.save(ticket));
        } catch (OptimisticLockingFailureException exception) {
            throw new InvalidStatusTransitionException(currentStatus, requestedStatus, exception);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> list(TicketStatus status, String keyword, int page, int size) {
        validatePage(page, size);
        Specification<Ticket> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        Specification<Ticket> statusSpecification = TicketSpecifications.hasStatus(status);
        Specification<Ticket> keywordSpecification = TicketSpecifications.containsKeyword(keyword);
        if (statusSpecification != null) {
            specification = specification.and(statusSpecification);
        }
        if (keywordSpecification != null) {
            specification = specification.and(keywordSpecification);
        }

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Ticket> tickets = ticketRepository.findAll(specification, pageable);
        return new PageResponse<>(
                tickets.getContent().stream().map(this::toResponse).toList(),
                new PageResponse.PageMetadata(
                        tickets.getNumber(), tickets.getSize(), tickets.getTotalElements(), tickets.getTotalPages()));
    }

    private Ticket findTicket(Long ticketId) {
        if (ticketId == null) {
            throw new ResourceNotFoundException("Ticket not found: null");
        }
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(), ticket.getStatus(),
                ticket.getAssigneeId(), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}