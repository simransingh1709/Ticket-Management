package com.tms.backend.tms_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.tms.backend.tms_backend.dto.TicketCreateRequest;
import com.tms.backend.tms_backend.dto.TicketResponse;
import com.tms.backend.tms_backend.dto.TicketStatusUpdateRequest;
import com.tms.backend.tms_backend.dto.TicketUpdateRequest;
import com.tms.backend.tms_backend.entity.Ticket;
import com.tms.backend.tms_backend.entity.TicketStatus;
import com.tms.backend.tms_backend.exception.InvalidStatusTransitionException;
import com.tms.backend.tms_backend.exception.ResourceNotFoundException;
import com.tms.backend.tms_backend.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository);
    }

    @Test
    void createsTicketWithTrimmedFieldsAndOpenStatus() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.create(
                new TicketCreateRequest("  Login failure  ", "  Cannot sign in  ", " agent-1 "));

        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.title()).isEqualTo("Login failure");
        assertThat(response.description()).isEqualTo("Cannot sign in");
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void retrievesExistingTicketAndRejectsMissingTicket() {
        Ticket existingTicket = ticket("Login failure", "Cannot sign in", null);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(existingTicket));

        assertThat(ticketService.getById(7L).title()).isEqualTo("Login failure");

        when(ticketRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ticketService.getById(8L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("8");
    }

    @Test
    void updatesEditableFieldsWhilePreservingStatusAndCreatedAt() {
        Ticket ticket = ticket("Old title", "Old description", "agent-1");
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.update(1L,
                new TicketUpdateRequest(" New title ", " New description ", " agent-2 "));

        assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getTitle()).isEqualTo("New title");
        assertThat(ticket.getDescription()).isEqualTo("New description");
        assertThat(ticket.getAssigneeId()).isEqualTo("agent-2");
        verify(ticketRepository).save(ticket);
    }

    @Test
    void appliesEveryAllowedTransition() {
        assertTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        assertTransition(TicketStatus.OPEN, TicketStatus.CANCELLED);
        assertTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);
        assertTransition(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
        assertTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED);
    }

    @Test
    void rejectsInvalidTransitionBeforeMutation() {
        Ticket ticket = ticket("Title", "Description", null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateStatus(1L,
                new TicketStatusUpdateRequest(TicketStatus.CLOSED)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void rejectsTransitionsFromBothTerminalStates() {
        for (TicketStatus terminalStatus : new TicketStatus[] {TicketStatus.CLOSED, TicketStatus.CANCELLED}) {
            Ticket ticket = ticket("Title", "Description", null);
            ticket.setStatus(terminalStatus);
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> ticketService.updateStatus(1L,
                    new TicketStatusUpdateRequest(TicketStatus.OPEN)))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            verify(ticketRepository, never()).save(any(Ticket.class));
        }
    }

    @Test
    void listsTicketsWithCombinedFiltersAndMetadata() {
        Ticket matchingTicket = ticket("Login failure", "Users cannot sign in", null);
        when(ticketRepository.findAll(
            org.mockito.ArgumentMatchers.<Specification<Ticket>>any(),
            org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(matchingTicket), PageRequest.of(0, 10), 1));

        var response = ticketService.list(TicketStatus.OPEN, " login ", 0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("Login failure");
        assertThat(response.page().number()).isZero();
        assertThat(response.page().size()).isEqualTo(10);
        assertThat(response.page().totalElements()).isEqualTo(1);
    }

    private void assertTransition(TicketStatus currentStatus, TicketStatus requestedStatus) {
        Ticket ticket = ticket("Title", "Description", null);
        ticket.setStatus(currentStatus);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        ticketService.updateStatus(1L, new TicketStatusUpdateRequest(requestedStatus));

        assertThat(ticket.getStatus()).isEqualTo(requestedStatus);
    }

    private Ticket ticket(String title, String description, String assigneeId) {
        Ticket ticket = new Ticket(title, description, assigneeId);
        return ticket;
    }
}