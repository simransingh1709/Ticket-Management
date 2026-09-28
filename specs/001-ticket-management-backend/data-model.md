# Data Model: Ticket Management Backend

## Ticket

Persistence table: `tickets`.

| Field | Type | Rules |
| --- | --- | --- |
| `id` | `Long` | Generated numeric primary key. |
| `title` | `String` | Required; trim before storage; 5-150 characters inclusive. |
| `description` | `String` | Required and nonblank; trim before storage. |
| `status` | `TicketStatus` | `EnumType.STRING`; defaults to `OPEN`; indexed. |
| `assigneeId` | `String` | Optional opaque identifier; no directory lookup. |
| `createdAt` | `Instant` | Non-null; Hibernate `@CreationTimestamp`; indexed for ordering. |
| `updatedAt` | `Instant` | Non-null; Hibernate `@UpdateTimestamp`. |

The entity has no `priority` field. Ticket list and detail responses expose only the corresponding DTO fields.

## Comment

Persistence table: `comments`.

| Field | Type | Rules |
| --- | --- | --- |
| `id` | `Long` | Generated numeric primary key. |
| `ticket` | `Ticket` | Required lazy `@ManyToOne`; foreign key `ticket_id`; indexed. |
| `content` | `String` | Required; trim before storage; 1-1000 characters inclusive. |
| `author` | `String` | Optional request value; defaults to `system` when omitted or blank according to normalization policy. |
| `createdAt` | `Instant` | Non-null; Hibernate `@CreationTimestamp`. |

Deleting a ticket cascades to its comments and removes orphans. Comment retrieval is always an explicit repository query by ticket id; the relationship is not traversed for API serialization.

## TicketStatus and transitions

Allowed transitions are:

```text
OPEN        -> IN_PROGRESS, CANCELLED
IN_PROGRESS -> RESOLVED, CANCELLED
RESOLVED    -> CLOSED
CLOSED      -> none
CANCELLED   -> none
```

`TicketStatus.canTransitionTo(target)` is the single domain rule. The service validates it inside a transaction before mutation. Invalid transitions throw `InvalidStatusTransitionException`; stale optimistic-lock updates are also mapped to HTTP 409.

## API records

- `TicketCreateRequest(title, description, assigneeId)`
- `TicketUpdateRequest(title, description, assigneeId)`
- `TicketStatusUpdateRequest(status)`
- `CommentCreateRequest(content, author)`
- `TicketResponse(id, title, description, status, assigneeId, createdAt, updatedAt)`
- `CommentResponse(id, ticketId, content, author, createdAt)`
- `PageResponse<T>(content, page)` with page number, size, total elements, and total pages metadata
- `ProblemDetail` response with `type`, `title`, `status`, `detail`, `instance`, `timestamp`, and optional `fieldErrors`

All records are mapped manually in services or static factory methods. No JPA entity is returned from a controller.

## Indexes and query rules

Create indexes for `tickets.status`, `tickets.created_at`, and `comments.ticket_id`. Ticket search combines status equality with a grouped case-insensitive title-or-description predicate. Results are ordered by `created_at DESC, id DESC`; comments are ordered by `created_at ASC, id ASC` for chronological discussion display.
