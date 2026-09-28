package com.tms.backend.tms_backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.tms.backend.tms_backend.entity.Comment;
import com.tms.backend.tms_backend.entity.Ticket;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    private Ticket ticket;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        ticket = ticketRepository.save(new Ticket("Login failure", "Users cannot sign in", null));
        commentRepository.saveAll(List.of(
                new Comment(ticket, "First comment", "agent-1"),
                new Comment(ticket, "Second comment", "agent-2"),
                new Comment(ticket, "Third comment", "agent-3")));
    }

    @Test
    void filtersByTicketAndReturnsChronologicalComments() {
        Page<Comment> result = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(
                ticket.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Comment::getContent)
                .containsExactly("First comment", "Second comment", "Third comment");
    }

    @Test
    void paginatesCommentsInChronologicalOrder() {
        Page<Comment> result = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(
                ticket.getId(), PageRequest.of(1, 2));

        assertThat(result.getContent()).extracting(Comment::getContent).containsExactly("Third comment");
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void returnsEmptyPageForTicketWithoutComments() {
        Ticket otherTicket = ticketRepository.save(new Ticket("No comments", "Nothing posted", null));

        Page<Comment> result = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(
                otherTicket.getId(), PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyPageForUnknownTicketId() {
        Page<Comment> result = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(
                Long.MAX_VALUE, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }
}