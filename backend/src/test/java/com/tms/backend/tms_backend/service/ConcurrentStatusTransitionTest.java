package com.tms.backend.tms_backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.tms_backend.entity.Ticket;
import com.tms.backend.tms_backend.entity.TicketStatus;
import com.tms.backend.tms_backend.repository.TicketRepository;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcurrentStatusTransitionTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAll();
    }

    @Test
    void staleEntityCannotOverwriteWinningStatus() {
        Ticket persisted = ticketRepository.saveAndFlush(new Ticket("Concurrent ticket", "Race", null));
        Long ticketId = persisted.getId();
        entityManager.clear();
        Ticket winner = ticketRepository.findById(ticketId).orElseThrow();
        entityManager.clear();
        Ticket stale = ticketRepository.findById(ticketId).orElseThrow();

        winner.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.saveAndFlush(winner);
        stale.setStatus(TicketStatus.CANCELLED);

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(stale))
                .isInstanceOfAny(OptimisticLockException.class, OptimisticLockingFailureException.class);
        entityManager.clear();
        assertThat(ticketRepository.findById(ticketId).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.IN_PROGRESS);
    }
}