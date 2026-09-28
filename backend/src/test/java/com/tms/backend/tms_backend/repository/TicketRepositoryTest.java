package com.tms.backend.tms_backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.tms.backend.tms_backend.entity.Ticket;
import com.tms.backend.tms_backend.entity.TicketStatus;

@DataJpaTest
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        ticketRepository.saveAll(List.of(
                new Ticket("Login failure", "Users cannot sign in", "agent-1"),
                new Ticket("Export report", "CSV export is slow", "agent-2"),
                new Ticket("Password reset", "Login recovery email is delayed", null),
                new Ticket("Update profile", "Change contact details", null)));

    }

    @Test
    void returnsUnfilteredTicketsInDeterministicDescendingOrder() {
        Page<Ticket> result = ticketRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent()).extracting(Ticket::getId)
                .isSortedAccordingTo((left, right) -> right.compareTo(left));
    }

    @Test
    void appliesKeywordAcrossTitleOrDescriptionCaseInsensitively() {
        Page<Ticket> result = ticketRepository.findAll(
                TicketSpecifications.containsKeyword("  LOGIN "), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Ticket::getTitle)
                .containsExactlyInAnyOrder("Login failure", "Password reset");
    }

    @Test
    void appliesStatusOnlyFilter() {
        Page<Ticket> result = ticketRepository.findAll(
                TicketSpecifications.hasStatus(TicketStatus.OPEN), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Ticket::getTitle)
                .containsExactlyInAnyOrder("Login failure", "Export report", "Password reset", "Update profile");
    }

    @Test
    void combinesStatusAndKeywordWithAnd() {
        Page<Ticket> result = ticketRepository.findAll(
                TicketSpecifications.hasStatus(TicketStatus.OPEN)
                        .and(TicketSpecifications.containsKeyword("csv")),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Ticket::getTitle).containsExactly("Export report");
    }

    @Test
    void ignoresEmptyKeyword() {
        Page<Ticket> result = ticketRepository.findAll(
                TicketSpecifications.containsKeyword("   "), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    void supportsPaginationAndExplicitDeterministicSort() {
        Page<Ticket> firstPage = ticketRepository.findAll(
                (Specification<Ticket>) null,
                PageRequest.of(0, 2, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        Page<Ticket> secondPage = ticketRepository.findAll(
                (Specification<Ticket>) null,
                PageRequest.of(1, 2, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Ticket::getId)
                .doesNotContainAnyElementsOf(secondPage.getContent().stream().map(Ticket::getId).toList());
    }
}