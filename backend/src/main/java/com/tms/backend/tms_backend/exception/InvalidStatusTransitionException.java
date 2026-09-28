package com.tms.backend.tms_backend.exception;

import com.tms.backend.tms_backend.entity.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    private final TicketStatus currentStatus;
    private final TicketStatus requestedStatus;

    public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus requestedStatus) {
        super("Ticket cannot transition from " + currentStatus + " to " + requestedStatus);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus requestedStatus,
            Throwable cause) {
        super("Ticket status transition could not be persisted", cause);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public TicketStatus getRequestedStatus() {
        return requestedStatus;
    }
}