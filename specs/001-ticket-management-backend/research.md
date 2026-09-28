# Research: Ticket Management Backend

## Decision: Use Spring Data JPA Specifications for ticket search

**Rationale:** `JpaSpecificationExecutor<Ticket>` composes an optional status predicate and a grouped case-insensitive `LIKE` predicate for title or description. It keeps filtering parameterized, supports `Pageable`, and avoids a growing matrix of repository methods. Normalize and trim the keyword in the service; an empty value produces no keyword predicate. Use deterministic `createdAt DESC, id DESC` ordering.

**Alternatives considered:** A custom JPQL query is viable for the current two filters, but specifications better isolate optional criteria and future filters without changing the repository API. PostgreSQL full-text search is unnecessary for the initial expected volume and would add schema and ranking complexity.

## Decision: Protect status transitions with optimistic locking

**Rationale:** Add a non-API `@Version` field to `Ticket`. The transactional service loads the current entity, validates the enum transition, changes the status, and saves it. A stale concurrent update raises an optimistic-lock exception and is translated to HTTP 409, preserving the state-machine invariant.

**Alternatives considered:** A conditional `UPDATE` with both version and current status can reduce entity work under high contention, but optimistic locking follows standard JPA behavior and is simpler for the first increment. A pessimistic lock would increase contention and is not needed for this scope.

## Decision: Query comments separately

**Rationale:** Keep the ticket-to-comments association lazy and never serialize entities. Ticket detail maps ticket attributes only; the comments collection endpoint queries `CommentRepository` by `ticketId` with `Pageable`. This prevents accidental collection fetching, pagination duplication, and N+1 behavior.

**Alternatives considered:** A fetch join or entity graph could load comments for a detail response, but it conflicts with the requested independent endpoint design and makes the boundary less explicit.

## Decision: Use Java records and manual mapping

**Rationale:** Request and response records provide immutable API payloads and preserve the repository rule that entities never cross the controller boundary. Static factories or service-local constructors are sufficient for the small model and avoid a mapping dependency.

**Alternatives considered:** MapStruct would generate consistent mappings but is not needed for two entities and would add a build-time dependency and configuration surface.

## Decision: Use Spring `ProblemDetail` with stable extensions

**Rationale:** A single `@RestControllerAdvice` can emit RFC 7807-compatible fields (`type`, `title`, `status`, `detail`, `instance`) plus `timestamp` and `fieldErrors`. Stable application error types identify validation, not-found, invalid-transition, enum, and internal failures without exposing persistence details.

**Alternatives considered:** A custom error record gives full shape control, but Spring's native `ProblemDetail` is already aligned with the API standards and reduces custom serialization code.

## Decision: Use PostgreSQL for persistence tests and H2 only for MVC slicing support

**Rationale:** PostgreSQL is the production database and Testcontainers gives repository tests database-realistic behavior for enum storage, indexes, specifications, and timestamps. `@WebMvcTest` should not require a database; H2 may support lightweight test configuration where a slice requires it, but it is not treated as production-equivalent.

**Alternatives considered:** H2-only repository tests are faster but can hide PostgreSQL-specific behavior. A shared external PostgreSQL test database reduces setup isolation and determinism.

## Clarified scope decisions

- `priority` is excluded from entities, DTOs, persistence schema, routes, and OpenAPI because the supplied implementation decisions explicitly require no priority field.
- `GET /api/v1/tickets/{id}` returns ticket attributes only; comments are available from `GET /api/v1/tickets/{ticketId}/comments`.
- `assigneeId` is modeled as an optional opaque `String`, matching the assumption that the backend does not resolve an external user directory and accommodating both numeric-looking and agent-style identifiers.
- Hibernate `ddl-auto=update` is the requested initial schema strategy; no Flyway migration is introduced in this increment. Required indexes and cascade/orphan behavior are represented in the entity mapping and documented for later migration hardening.
