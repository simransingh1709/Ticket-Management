package com.tms.backend.tms_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.tms.backend.tms_backend.dto.CommentCreateRequest;
import com.tms.backend.tms_backend.dto.CommentResponse;
import com.tms.backend.tms_backend.dto.PageResponse;
import com.tms.backend.tms_backend.entity.Comment;
import com.tms.backend.tms_backend.entity.Ticket;
import com.tms.backend.tms_backend.exception.ResourceNotFoundException;
import com.tms.backend.tms_backend.repository.CommentRepository;
import com.tms.backend.tms_backend.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, ticketRepository);
    }

    @Test
    void createsCommentWithTrimmedContentAndDefaultAuthor() {
        Ticket ticket = new Ticket("Login failure", "Cannot sign in", null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.create(1L,
                new CommentCreateRequest("  The issue is fixed  ", "   "));

        assertThat(response.content()).isEqualTo("The issue is fixed");
        assertThat(response.author()).isEqualTo("system");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void rejectsCommentCreationForUnknownTicket() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(99L,
                new CommentCreateRequest("A comment", "agent")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void retrievesCommentsWithPageMetadata() {
        Ticket ticket = new Ticket("Login failure", "Cannot sign in", null);
        Comment firstComment = new Comment(ticket, "First", "agent-1");
        Comment secondComment = new Comment(ticket, "Second", "agent-2");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(1L, PageRequest.of(0, 2)))
                .thenReturn(new PageImpl<>(List.of(firstComment, secondComment), PageRequest.of(0, 2), 3));

        PageResponse<CommentResponse> response = commentService.listByTicketId(1L, 0, 2);

        assertThat(response.content()).extracting(CommentResponse::content).containsExactly("First", "Second");
        assertThat(response.page().totalElements()).isEqualTo(3);
        assertThat(response.page().totalPages()).isEqualTo(2);
    }

    @Test
    void rejectsUnknownTicketDuringCommentRetrieval() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.listByTicketId(99L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}