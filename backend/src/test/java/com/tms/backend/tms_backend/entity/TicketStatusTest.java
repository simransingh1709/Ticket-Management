package com.tms.backend.tms_backend.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketStatusTest {

    @Test
    void allowsAllFiveDocumentedTransitions() {
        assertThat(TicketStatus.OPEN.canTransitionTo(TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(TicketStatus.OPEN.canTransitionTo(TicketStatus.CANCELLED)).isTrue();
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.RESOLVED)).isTrue();
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.CANCELLED)).isTrue();
        assertThat(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.CLOSED)).isTrue();
    }

    @Test
    void rejectsEveryUndocumentedTransitionAndSelfTransition() {
        for (TicketStatus source : TicketStatus.values()) {
            for (TicketStatus target : TicketStatus.values()) {
                boolean allowed = (source == TicketStatus.OPEN && (target == TicketStatus.IN_PROGRESS
                        || target == TicketStatus.CANCELLED))
                        || (source == TicketStatus.IN_PROGRESS && (target == TicketStatus.RESOLVED
                                || target == TicketStatus.CANCELLED))
                        || (source == TicketStatus.RESOLVED && target == TicketStatus.CLOSED);

                assertThat(source.canTransitionTo(target))
                        .as("transition from %s to %s", source, target)
                        .isEqualTo(allowed);
            }
        }
    }

    @Test
    void terminalStatusesHaveNoOutgoingTransitions() {
        for (TicketStatus target : TicketStatus.values()) {
            assertThat(TicketStatus.CLOSED.canTransitionTo(target)).isFalse();
            assertThat(TicketStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void nullTargetIsRejected() {
        assertThat(TicketStatus.OPEN.canTransitionTo(null)).isFalse();
    }
}