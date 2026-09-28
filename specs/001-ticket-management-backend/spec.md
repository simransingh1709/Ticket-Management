# Feature Specification: Ticket Management Backend

**Feature Branch**: `001-ticket-management-backend`

**Created**: 2026-09-08

**Status**: Draft

**Input**: User description: "Build a Ticket Management System backend with ticket and comment management, status transitions, search, filtering, persistence, validation, REST contracts, relational schema, and automated test coverage."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manage Ticket Lifecycle (Priority: P1)

A support user can create a ticket, inspect its details, update editable information, and move it through the defined lifecycle so work can be tracked from opening to resolution or cancellation.

**Why this priority**: Ticket lifecycle management is the core value of the system and provides a usable MVP without secondary discovery features.

**Independent Test**: Create one ticket, retrieve it, update its editable fields, apply every valid status transition, and verify that invalid transitions are rejected without changing the ticket.

**Acceptance Scenarios**:

1. **Given** valid title, description, and optional assignee, **When** a user creates a ticket, **Then** the system returns the new ticket with a generated identifier, status `OPEN`, and managed creation and update timestamps.
2. **Given** an existing ticket, **When** a user requests its details, **Then** the response includes ticket attributes only; comments are retrieved through the dedicated comments endpoint.
3. **Given** an existing ticket and valid replacement title, description, or assignee, **When** the user updates ticket details, **Then** only those editable fields change and status and creation time remain unchanged.
4. **Given** a ticket in `OPEN`, **When** the user requests `IN_PROGRESS` or `CANCELLED`, **Then** the status changes successfully.
5. **Given** a ticket in `IN_PROGRESS`, **When** the user requests `RESOLVED` or `CANCELLED`, **Then** the status changes successfully.
6. **Given** a ticket in `RESOLVED`, **When** the user requests `CLOSED`, **Then** the status changes successfully.
7. **Given** a ticket in `CLOSED` or `CANCELLED`, **When** the user requests any new status, **Then** the system rejects the request with HTTP 409 and leaves the terminal status unchanged.
8. **Given** a ticket in `RESOLVED`, **When** the user requests `OPEN`, **Then** the system rejects the request with HTTP 409 and leaves the status unchanged.

---

### User Story 2 - Discover and Discuss Tickets (Priority: P2)

A support user can list tickets, search by a keyword, filter by status, paginate results, and add comments so they can find relevant work and preserve its discussion history.

**Why this priority**: Discovery and collaboration make the lifecycle useful at more than trivial scale, while remaining independent from the status state machine.

**Independent Test**: Seed tickets with varied titles, descriptions, statuses, and comments; verify unfiltered, keyword, status, combined, and paginated results, then add and retrieve a comment.

**Acceptance Scenarios**:

1. **Given** persisted tickets, **When** the user lists tickets without filters, **Then** the response returns a paginated result with total count and page metadata.
2. **Given** tickets whose title or description contains a keyword, **When** the user searches with that keyword, **Then** only matching tickets are returned regardless of which of those two fields matched.
3. **Given** tickets with different statuses, **When** the user filters by a valid status, **Then** only tickets with that status are returned.
4. **Given** a keyword and a status filter, **When** the user lists tickets with both, **Then** every result matches both constraints.
5. **Given** an existing ticket and valid comment content, **When** the user adds a comment, **Then** the system returns the comment with a generated identifier and creation timestamp, associated with that ticket.
6. **Given** a missing ticket identifier, **When** the user adds a comment, **Then** the system returns HTTP 404 with a structured error and does not persist the comment.

---

### User Story 3 - Receive Predictable Validation Errors (Priority: P3)

A client receives clear, consistent feedback when it submits invalid ticket, comment, query, or status data, allowing the client to correct the request without exposing internal failure details.

**Why this priority**: Predictable errors protect data quality and make all other workflows usable for clients and operators.

**Independent Test**: Submit each documented invalid boundary case and verify the HTTP status, field-level details where applicable, stable error shape, and unchanged persisted data.

**Acceptance Scenarios**:

1. **Given** a title shorter than 5 characters, longer than 150 characters, or blank, **When** a ticket is created or updated, **Then** the system returns HTTP 400 with a title validation detail.
2. **Given** blank ticket description or comment content, **When** the request is submitted, **Then** the system returns HTTP 400 with the relevant validation detail.
3. **Given** comment content longer than 1000 characters, **When** the comment is submitted, **Then** the system returns HTTP 400 and does not persist it.
4. **Given** an unknown ticket identifier, **When** details, update, status, or comment operations are requested, **Then** the system returns HTTP 404 with a structured not-found error.
5. **Given** an invalid status value, **When** the request is submitted, **Then** the system returns HTTP 400 with an allowed-values error.
6. **Given** an unexpected server failure, **When** the request is processed, **Then** the response contains a safe generic message and does not expose stack traces or sensitive persistence details.

### Edge Cases

- A title of exactly 5 characters and exactly 150 characters is accepted; lengths outside that inclusive range are rejected.
- A comment of exactly 1 character and exactly 1000 characters is accepted; blank or longer content is rejected.
- Leading and trailing whitespace is ignored for required text validation, and stored values follow the documented normalization rule.
- An empty keyword returns the normal unfiltered listing; whitespace-only keyword is treated as empty.
- A negative page, zero or negative size, or size above the documented maximum returns HTTP 400.
- An unknown status filter returns HTTP 400 rather than an empty result.
- A missing, null, or malformed request body returns HTTP 400 with a structured error.
- A request to update a nonexistent ticket returns HTTP 404 and makes no database change.
- A comment referencing a nonexistent ticket returns HTTP 404 and makes no database change.
- Concurrent valid status updates MUST preserve the state-machine rule; an update that no longer applies to the current state is rejected.
- `CLOSED` and `CANCELLED` are terminal and cannot transition to themselves or any other status.
- A ticket with no comments returns an empty page from the dedicated comments endpoint.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST create tickets with a generated Long identifier, required title, required description, status `OPEN`, and managed `createdAt` and `updatedAt` timestamps.
- **FR-002**: The system MUST accept ticket titles from 5 through 150 characters inclusive and reject blank or out-of-range titles.
- **FR-004**: The system MUST support statuses `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, and `CANCELLED`.
- **FR-005**: The system MUST allow updates to title, description, and optional assigneeId without changing status or createdAt.
- **FR-006**: The system MUST expose ticket details without embedded comments and return HTTP 404 for an unknown ticket identifier; comments MUST be available through the dedicated comments endpoint.
- **FR-007**: The system MUST enforce exactly these status transitions: `OPEN -> IN_PROGRESS`, `IN_PROGRESS -> RESOLVED`, `RESOLVED -> CLOSED`, `OPEN -> CANCELLED`, and `IN_PROGRESS -> CANCELLED`.
- **FR-008**: The system MUST reject every transition not listed in FR-007, including transitions from terminal states, with HTTP 409 and no state change.
- **FR-009**: The system MUST list tickets with page, size, total count, and page metadata, using page 0 and size 10 as defaults and rejecting invalid or excessive page sizes.
- **FR-010**: The system MUST filter ticket listings by one valid status when `status` is provided.
- **FR-011**: The system MUST search ticket title and description by keyword when `keyword` is provided, with case-insensitive matching and no partial-field omission.
- **FR-012**: The system MUST allow status filtering and keyword search in the same listing request, applying both constraints.
- **FR-013**: The system MUST create comments only for an existing ticket and assign each comment a generated Long identifier, ticket association, required content, optional author, and managed createdAt timestamp.
- **FR-014**: The system MUST accept comment content from 1 through 1000 characters inclusive and reject blank or longer content.
- **FR-015**: The system MUST default an omitted comment author to `system` and preserve an explicitly supplied nonblank author within the documented response.
- **FR-016**: The system MUST persist tickets and comments durably and maintain the ticket-comment relationship with referential integrity.
- **FR-017**: The system MUST validate request bodies, path parameters, query parameters, enum values, and pagination boundaries before applying mutations or searches.
- **FR-018**: The system MUST return a consistent structured error payload containing at least an HTTP status, machine-readable error type or code, human-readable message, request path, timestamp, and field violations when applicable.
- **FR-019**: The system MUST return HTTP 400 for malformed or invalid input, HTTP 404 for missing ticket references, and HTTP 409 for invalid state transitions.
- **FR-020**: The system MUST expose only request and response DTOs at the API boundary; persistence records are not API payloads.
- **FR-021**: The system MUST provide unit coverage for service CRUD behavior, keyword search, comment addition, and every valid and invalid status transition.
- **FR-022**: The system MUST provide controller-slice coverage for validation failures, successful responses, not-found responses, conflict responses, and structured error payloads.

### REST API Contract

#### Ticket endpoints

| Method | Path | Request | Success response |
| --- | --- | --- | --- |
| POST | `/api/v1/tickets` | `{ "title": "Login fails", "description": "Users cannot sign in", "assigneeId": "agent-7" }` | `201` with Ticket response; status is `OPEN` |
| GET | `/api/v1/tickets` | Query: `status`, `keyword`, `page`, `size` | `200` with `{ "content": [TicketSummary], "page": { "number": 0, "size": 10, "totalElements": 1, "totalPages": 1 } }` |
| GET | `/api/v1/tickets/{id}` | Path ticket id | `200` with Ticket detail and `comments` collection |
| PUT | `/api/v1/tickets/{id}` | `{ "title": "Login fails for SSO", "description": "...", "assigneeId": "agent-7" }` | `200` with updated Ticket response |
| PATCH | `/api/v1/tickets/{id}/status` | `{ "status": "IN_PROGRESS" }` | `200` with updated Ticket response |
| POST | `/api/v1/tickets/{id}/comments` | `{ "content": "Investigating logs", "author": "agent-7" }` | `201` with Comment response |

Ticket response fields are `id`, `title`, `description`, `status`, `assigneeId`, `createdAt`,
and `updatedAt`. Comments are returned only by the dedicated comments collection endpoint.

#### Error response

All documented errors use the same shape: `{ "type": "VALIDATION_ERROR", "status": 400,
"message": "Request validation failed", "path": "/api/v1/tickets", "timestamp":
"2026-09-08T12:00:00Z", "fieldErrors": [{ "field": "title", "message": "must be
between 5 and 150 characters" }] }`. `fieldErrors` may be empty when the error is not
field-specific. Error types include `VALIDATION_ERROR`, `NOT_FOUND`, `INVALID_STATUS_TRANSITION`,
`INVALID_ENUM_VALUE`, and `INTERNAL_ERROR`.

### Relational Schema Requirements

- **tickets** MUST contain `id` as a generated numeric primary key; `title` as required text;
  `description` as required text and `status` as a constrained enum-compatible value;
  nullable `assignee_id`; and non-null `created_at` and `updated_at` timestamps.
- **comments** MUST contain `id` as a generated numeric primary key; required `ticket_id` as a
  foreign key to `tickets.id`; required `content`; optional `author`; and non-null `created_at`.
- Deleting a ticket MUST follow an explicit comment-retention rule; the default for this feature is
  to delete associated comments with the ticket so no orphan comments remain.
- The schema MUST index `tickets.status` for status filtering, `tickets.created_at` for stable
  listing order, and `comments.ticket_id` for detail retrieval.
- The schema MUST provide a database-supported strategy for keyword matching across `tickets.title`
  and `tickets.description`; the selected strategy MUST support the expected search volume and be
  documented during planning.

### Key Entities *(include if feature involves data)*

- **Ticket**: A trackable unit of support work with required title and description, lifecycle
  status, optional assignee, timestamps, and separately retrievable comments.
- **Comment**: A chronological discussion entry owned by one ticket, with required content,
  optional author, and creation timestamp.
- **Status Transition**: A permitted change from one ticket status to another, constrained by the
  lifecycle state machine and rejected when not explicitly allowed.
- **Page**: A listing result containing ticket summaries, requested page number and size, total
  elements, and total pages.
- **Problem Detail**: A stable error result describing validation, missing resources, conflicts, or
  unexpected failures without exposing internal details.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client can create, retrieve, update, and comment on a valid ticket through the
  documented workflow with at least 99% successful completion in acceptance testing.
- **SC-002**: At least 95% of valid ticket listings, searches, filters, and detail requests return a
  usable response within 1 second under the agreed baseline dataset and load.
- **SC-003**: 100% of the five permitted transitions succeed and 100% of attempted unlisted
  transitions are rejected without changing ticket status in automated acceptance tests.
- **SC-004**: 100% of requests violating documented title, comment, enum, pagination, or required
  field boundaries receive a structured client-correctable error.
- **SC-005**: 100% of successfully created tickets and comments remain available after the service
  is restarted and reconnects to its configured data store.
- **SC-006**: At least 90% of representative users can find a target ticket using status filtering,
  keyword search, or both on their first attempt during usability acceptance testing.
- **SC-007**: No client-facing error response exposes a stack trace, database detail, credential, or
  other internal secret in security and error-contract tests.

## Assumptions

- The first release serves authenticated or trusted internal support users; authentication and
  authorization policy are outside this feature unless added by a later specification.
- `assigneeId` is an opaque optional identifier; resolving it to a user directory is out of scope.
- The default comment author is `system` when the caller omits author.
- List results are ordered by `createdAt` descending for stable newest-first discovery.
- Page numbering is zero-based, the default page size is 10, and the maximum page size is 100.
- Keyword matching is case-insensitive and searches title and description; stemming, fuzzy matching,
  highlighting, and relevance ranking are out of scope.
- Timestamps are represented in UTC and are generated and maintained by the service.
- The initial persistence approach uses the project-approved relational database configuration and
  schema management policy; authentication, audit history, attachments, notifications, and deletion
  APIs are out of scope.
- The implementation must follow the project constitution and its specified Spring Boot, Java 21,
  Spring Data JPA, PostgreSQL, Lombok, DTO, validation, error-handling, and testing constraints.
