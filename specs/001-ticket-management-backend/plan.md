# Implementation Plan: Ticket Management Backend

**Branch**: `001-ticket-management-backend` | **Date**: 2026-09-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-ticket-management-backend/spec.md`, clarified by the technical decisions supplied with this planning request.

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Deliver a layered Spring Boot REST API for ticket and comment management. The implementation uses Java 21, Spring Data JPA, PostgreSQL, immutable record DTOs with manual mapping, a domain-owned status state machine, repository specifications for combined filtering, and centralized RFC 7807-compatible errors. Ticket details and comments are queried independently so ticket reads never embed or eagerly fetch comments.

**Resolved authority:** The supplied technical decisions govern conflicts in the draft specification. This feature therefore has no `priority` field or priority API, and `GET /api/v1/tickets/{id}` returns ticket attributes only. Comments are exposed through the dedicated comments collection endpoint. The existing draft requirements mentioning priority and embedded comments must be updated or treated as superseded before implementation tasks are generated.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Hibernate, PostgreSQL driver, Lombok, SpringDoc OpenAPI 3, Jakarta Validation

**Storage**: PostgreSQL; Hibernate `ddl-auto=update`; H2 for web/test slices where needed; Testcontainers PostgreSQL for persistence tests

**Testing**: JUnit 5, Mockito, AssertJ, `@WebMvcTest`/MockMvc, `@DataJpaTest`, Testcontainers PostgreSQL

**Target Platform**: Linux server, Maven-built Spring Boot application

**Project Type**: API-first REST web service

**Performance Goals**: At least 95% of representative list, search, filter, and detail requests within 1 second on the agreed baseline dataset

**Constraints**: Page size defaults to 10 and is capped at 100; keyword matching is case-insensitive across title or description; comments must not be fetched by ticket listing/detail queries; API payloads must not expose entities

**Scale/Scope**: Initial internal support backend with ticket/comment CRUD, lifecycle transitions, search/filter/pagination, validation, persistence, OpenAPI documentation, and automated coverage; authentication is out of scope

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

All gates pass before research:

- **Backend scope and ownership:** Pass. Controllers delegate to services; lifecycle rules live in the domain/service layer.
- **API-first contracts:** Pass with a required OpenAPI contract artifact; the clarified no-priority and decoupled-comment decisions are recorded as compatibility decisions.
- **Testable quality:** Pass. Unit, MVC slice, and JPA/PostgreSQL integration coverage are planned for the documented risk.
- **Persistence, validation, and errors:** Pass. JPA/PostgreSQL, boundary validation, transaction boundaries, and centralized structured errors are explicit.
- **Maintainable simplicity:** Pass. Manual record mapping and Spring Data specifications avoid unnecessary mapping/query frameworks.
- **Technology/security constraints:** Pass. Java 21/Spring Boot/PostgreSQL are retained; secrets and internal failures remain outside API responses.

## Project Structure

### Documentation (this feature)

```text
specs/001-ticket-management-backend/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (`TMS/backend`)
```text
TMS/
└── backend/
	├── src/
	│   ├── main/java/com/tms/backend/tms_backend/
	│   │   ├── config/
	│   │   ├── controller/
	│   │   ├── dto/
	│   │   ├── entity/
	│   │   ├── exception/
	│   │   ├── repository/
	│   │   └── service/
	│   └── test/java/com/tms/backend/tms_backend/
	│       ├── controller/
	│       ├── repository/
	│       ├── service/
	│       └── entity/
	└── pom.xml
```

**Structure Decision**: Keep `TMS/backend` as the only implementation project in the monorepo. Use strict `controller -> service -> repository -> database` ownership under `com.tms.backend.tms_backend`; entities remain persistence-only, records define the API boundary, and tests mirror the production package ownership.

## Implementation Shape

- `TicketStatus` exposes the allowed transition relation: `OPEN -> IN_PROGRESS/CANCELLED`, `IN_PROGRESS -> RESOLVED/CANCELLED`, `RESOLVED -> CLOSED`; terminal states have no outgoing transitions.
- `Ticket` includes an internal optimistic-lock version so stale concurrent status updates become conflicts rather than overwrites.
- `TicketRepository` extends `JpaSpecificationExecutor`; status equality and grouped case-insensitive title/description `LIKE` predicates combine with `AND`, ordered by `createdAt DESC, id DESC`.
- `CommentRepository` queries by ticket id with pageable support and does not rely on serializing a lazy entity relationship.
- Services own transactions, existence checks, transitions, normalization, and manual entity/record mapping.
- `GlobalExceptionHandler` maps validation, malformed enum/pagination input, not-found, invalid transition, optimistic-lock conflict, and unexpected failures to the documented structured error shape.
- SpringDoc exposes the contract represented in [contracts/openapi.yaml](contracts/openapi.yaml); the implementation must keep generated documentation and endpoint behavior aligned.

## Post-Design Constitution Check

All gates continue to pass. The design keeps business rules in services/domain types, uses repository-backed persistence, exposes only DTO records, includes the required test layers, and documents the API and schema compatibility decisions. No complexity exception is required.

## Complexity Tracking

No constitution violations or complexity exceptions require tracking.

## Phase 8 Compatibility Review

The final controllers match the approved OpenAPI routes and status codes. Ticket responses omit
priority, persistence version, and embedded comments; comments use the independent collection
routes. Runtime configuration retains PostgreSQL with `ddl-auto=update`, while tests use H2 when
Docker is unavailable. No rollback or schema migration impact was introduced; Flyway remains out
of scope.
