package com.tms.backend.tms_backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.tms.backend.tms_backend.dto.TicketStatusUpdateRequest;
import com.tms.backend.tms_backend.entity.Ticket;
import com.tms.backend.tms_backend.entity.TicketStatus;
import com.tms.backend.tms_backend.exception.InvalidStatusTransitionException;
import com.tms.backend.tms_backend.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class ServiceValidationTest {

    @Mock
    private TicketRepository ticketRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository);
    }

    @Test
    void rejectsInvalidPageBoundsBeforeQuerying() {
        assertThatThrownBy(() -> ticketService.list(null, null, -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ticketService.list(null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ticketService.list(null, null, 0, 101))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ticketRepository, never()).findAll(
            org.mockito.ArgumentMatchers.<Specification<Ticket>>any(),
            org.mockito.ArgumentMatchers.<Pageable>any());
    }

    @Test
    void propagatesOptimisticLockFailureAfterValidTransition() {
        Ticket ticket = new Ticket("Title", "Description", null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenThrow(new OptimisticLockingFailureException("stale"));

        assertThatThrownBy(() -> ticketService.updateStatus(1L,
                new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS)))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);
    }

}