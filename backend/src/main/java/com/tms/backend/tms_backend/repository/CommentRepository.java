package com.tms.backend.tms_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tms.backend.tms_backend.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId, Pageable pageable);
}