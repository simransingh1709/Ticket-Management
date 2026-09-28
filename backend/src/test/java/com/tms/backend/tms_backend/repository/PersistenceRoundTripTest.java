package com.tms.backend.tms_backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import jakarta.persistence.EntityManager;

import com.tms.backend.tms_backend.entity.Comment;
import com.tms.backend.tms_backend.entity.Ticket;

@DataJpaTest
class PersistenceRoundTripTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @Test
    void ticketAndCommentRemainAvailableAfterPersistenceContextRestart() {
        Ticket ticket = ticketRepository.saveAndFlush(new Ticket("Persistent ticket", "Round trip", null));
        commentRepository.saveAndFlush(new Comment(ticket, "Persistent comment", "system"));
        Long ticketId = ticket.getId();

        entityManager.clear();

        Ticket reloaded = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Persistent ticket");
        var comments = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId, PageRequest.of(0, 10));
        assertThat(comments.getContent()).hasSize(1);
        assertThat(comments.getContent().get(0).getContent()).isEqualTo("Persistent comment");
    }
}