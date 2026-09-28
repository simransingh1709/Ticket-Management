---

description: "Implementation tasks for the Ticket Management Backend"
---

# Tasks: Ticket Management Backend

**Input**: Design documents from `/specs/001-ticket-management-backend/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`, and `quickstart.md`

**Implementation root**: `TMS/backend/`

**Scope authority**: The supplied technical decisions override stale draft-spec references to priority and embedded ticket comments. No priority field is allowed. Ticket details and comments are separate operations.

## Phase 1: Setup & Configuration

**Purpose**: Establish the Maven dependencies, runtime configuration, package layout, and API documentation foundation.

- [x] T001 Update `TMS/backend/pom.xml` with PostgreSQL runtime, SpringDoc OpenAPI, H2 test, and Testcontainers PostgreSQL dependencies while retaining Java 21 and Spring Boot 4.1.1.
- [x] T002 [P] Create the package directories under `TMS/backend/src/main/java/com/tms/backend/tms_backend/` for `config`, `controller`, `dto`, `entity`, `exception`, `repository`, and `service`.
- [x] T003 [P] Create matching test package directories under `TMS/backend/src/test/java/com/tms/backend/tms_backend/` for `controller`, `repository`, `service`, and `entity`.
- [x] T004 Configure PostgreSQL datasource, UTC/JPA settings, `spring.jpa.hibernate.ddl-auto=update`, and environment-based credentials in `TMS/backend/src/main/resources/application.properties` without committing secrets.
- [x] T005 [P] Add SpringDoc metadata and Swagger UI configuration in `TMS/backend/src/main/java/com/tms/backend/tms_backend/config/OpenApiConfig.java` for the `/api/v1` contract.
- [x] T006 Verify the baseline Maven build and application context from `TMS/backend/` with `./mvnw test`, recording any dependency or Spring Boot 4.1.1 compatibility issue before feature implementation.

## Phase 2: Domain Model & State Machine

**Purpose**: Implement persistence entities, schema constraints, indexes, relationships, and the lifecycle rule that all stories depend on.

- [x] T007 [P] Create `TicketStatus` in `TMS/backend/src/main/java/com/tms/backend/tms_backend/entity/TicketStatus.java` with `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, and `CANCELLED`, plus the exact `canTransitionTo` relation and terminal-state behavior.
- [x] T008 [P] Create the `Ticket` JPA entity in `TMS/backend/src/main/java/com/tms/backend/tms_backend/entity/Ticket.java` with generated `Long id`, required title/description, `EnumType.STRING` status defaulting to `OPEN`, optional `String assigneeId`, UTC timestamps, and an internal optimistic-lock `@Version` field; do not add priority.
- [x] T009 [P] Create the `Comment` JPA entity in `TMS/backend/src/main/java/com/tms/backend/tms_backend/entity/Comment.java` with generated id, required lazy ticket association, content, optional author, and creation timestamp; configure the ticket foreign key and comment index.
- [x] T010 Add entity-level column constraints and indexes for `tickets.status`, `tickets.created_at`, and `comments.ticket_id` in `TMS/backend/src/main/java/com/tms/backend/tms_backend/entity/Ticket.java` and `TMS/backend/src/main/java/com/tms/backend/tms_backend/entity/Comment.java`.
- [x] T011 [US1] Add state-machine unit tests in `TMS/backend/src/test/java/com/tms/backend/tms_backend/entity/TicketStatusTest.java` covering all five allowed transitions, every illegal transition, self-transitions, and both terminal statuses.

## Phase 3: DTOs & Custom Exceptions

**Purpose**: Define immutable API records, request validation, pagination payloads, and stable error types before controllers and services are implemented.

- [x] T012 [P] [US1] Create `TicketCreateRequest` and `TicketUpdateRequest` records in `TMS/backend/src/main/java/com/tms/backend/tms_backend/dto/` with Jakarta validation for title length 5-150, nonblank description, and optional assigneeId; omit priority.
- [x] T013 [P] [US1] Create `TicketStatusUpdateRequest` and `TicketResponse` records in `TMS/backend/src/main/java/com/tms/backend/tms_backend/dto/`, exposing status and timestamps but not version, persistence fields, or comments.
- [x] T014 [P] [US2] Create `CommentCreateRequest` and `CommentResponse` records in `TMS/backend/src/main/java/com/tms/backend/tms_backend/dto/` with content validation of 1-1000 characters and optional author handling.
- [x] T015 [P] [US2] Create generic page metadata and response records in `TMS/backend/src/main/java/com/tms/backend/tms_backend/dto/PageResponse.java` with content, page number, page size, total elements, and total pages.
- [x] T016 [P] Create `ResourceNotFoundException`, `InvalidStatusTransitionException`, and a stable application error-code model in `TMS/backend/src/main/java/com/tms/backend/tms_backend/exception/`.
- [x] T017 Create `GlobalExceptionHandler` in `TMS/backend/src/main/java/com/tms/backend/tms_backend/exception/GlobalExceptionHandler.java` using Spring `ProblemDetail` plus timestamp and field-error extensions; map validation to 400, missing resources to 404, invalid/stale transitions to 409, and unexpected failures to a safe 500 response.
- [x] T018 [US3] Add DTO validation tests in `TMS/backend/src/test/java/com/tms/backend/tms_backend/controller/DtoValidationTest.java` for exact title/comment boundaries, blank values, null/malformed bodies, and absence of any priority property.

## Phase 4: Repositories & Search Specification

**Purpose**: Add persistence interfaces, reusable search predicates, pagination, ordering, and explicit independent comment queries.

- [x] T019 [P] [US1] Create `TicketRepository` in `TMS/backend/src/main/java/com/tms/backend/tms_backend/repository/TicketRepository.java` extending `JpaRepository<Ticket, Long>` and `JpaSpecificationExecutor<Ticket>`.
- [x] T020 [P] [US2] Create `CommentRepository` in `TMS/backend/src/main/java/com/tms/backend/tms_backend/repository/CommentRepository.java` with a pageable ticket-id query ordered by `createdAt ASC, id ASC`.
- [x] T021 [US2] Implement composable ticket specifications in `TMS/backend/src/main/java/com/tms/backend/tms_backend/repository/TicketSpecifications.java` for optional status equality and grouped case-insensitive title-or-description keyword matching.
- [x] T022 [US2] Add repository ordering and query behavior for `createdAt DESC, id DESC` ticket listings and pageable comment retrieval without collection fetch joins in `TMS/backend/src/main/java/com/tms/backend/tms_backend/repository/`.
- [x] T023 [US2] Add PostgreSQL/Testcontainers repository tests in `TMS/backend/src/test/java/com/tms/backend/tms_backend/repository/TicketRepositoryTest.java` covering unfiltered, keyword-only, status-only, combined filtering, case-insensitivity, empty keyword, pagination, and deterministic ordering.
- [x] T024 [US2] Add comment repository tests in `TMS/backend/src/test/java/com/tms/backend/tms_backend/repository/CommentRepositoryTest.java` proving ticket-id filtering, chronological ordering, pagination, and empty results for tickets without comments.

## Phase 5: Service Layer

**Purpose**: Implement transactions, normalization, domain enforcement, manual mapping, ticket lifecycle operations, and comment operations.

- [x] T025 [US1] Create `TicketService` in `TMS/backend/src/main/java/com/tms/backend/tms_backend/service/TicketService.java` with constructor injection and transactional ticket creation that trims required text, defaults status to `OPEN`, persists the entity, and maps it to `TicketResponse`.
- [x] T026 [US1] Implement ticket retrieval and full editable update methods in `TMS/backend/src/main/java/com/tms/backend/tms_backend/service/TicketService.java`, preserving status and `createdAt`, rejecting missing ids, and manually mapping only DTO fields.
- [x] T027 [US1] Implement transactional status updates in `TMS/backend/src/main/java/com/tms/backend/tms_backend/service/TicketService.java` using `TicketStatus.canTransitionTo`, throwing `InvalidStatusTransitionException` before mutation and translating optimistic-lock failures to conflict handling.
- [x] T028 [US2] Implement paginated ticket listing in `TMS/backend/src/main/java/com/tms/backend/tms_backend/service/TicketService.java`, trimming whitespace-only keywords, validating page/size bounds, combining specifications, and returning `PageResponse<TicketResponse>`.
- [x] T029 [US2] Create `CommentService` in `TMS/backend/src/main/java/com/tms/backend/tms_backend/service/CommentService.java` with transactional creation that verifies the ticket exists, trims content, defaults missing/blank author to `system`, persists, and maps to `CommentResponse`.
- [x] T030 [US2] Implement independent paginated comment retrieval by ticket id in `TMS/backend/src/main/java/com/tms/backend/tms_backend/service/CommentService.java`; return an empty page for an existing ticket with no comments and 404 for an unknown ticket.
- [x] T031 [P] [US1] Add `TicketServiceTest` in `TMS/backend/src/test/java/com/tms/backend/tms_backend/service/TicketServiceTest.java` using JUnit 5, Mockito, and AssertJ for create, get, update preservation, all valid transitions, invalid transitions, terminal states, and missing tickets.
- [x] T032 [P] [US2] Add `CommentServiceTest` in `TMS/backend/src/test/java/com/tms/backend/tms_backend/service/CommentServiceTest.java` using JUnit 5, Mockito, and AssertJ for author defaulting, normalization, missing tickets, creation, retrieval, and pagination mapping.
- [x] T033 [US3] Add service validation/error tests in `TMS/backend/src/test/java/com/tms/backend/tms_backend/service/ServiceValidationTest.java` for page boundaries, unknown statuses before querying, invalid input without mutation, and safe exception translation.

## Phase 6: REST Controllers

**Purpose**: Expose the OpenAPI contract with thin validated controllers, correct status codes, pagination parameters, and decoupled comment routes.

- [x] T034 [US1] Implement `TicketController` in `TMS/backend/src/main/java/com/tms/backend/tms_backend/controller/TicketController.java` for `POST /api/v1/tickets`, `GET /api/v1/tickets/{ticketId}`, `PUT /api/v1/tickets/{ticketId}`, and `PATCH /api/v1/tickets/{ticketId}/status` with `@Valid`, service delegation, 201/200 responses, and `Location` headers for creation.
- [x] T035 [US2] Add `GET /api/v1/tickets` to `TMS/backend/src/main/java/com/tms/backend/tms_backend/controller/TicketController.java` with default page 0/size 10, maximum size 100, validated status binding, keyword handling, and the documented page response.
- [x] T036 [US2] Implement `CommentController` in `TMS/backend/src/main/java/com/tms/backend/tms_backend/controller/CommentController.java` for `GET` and `POST /api/v1/tickets/{ticketId}/comments`, keeping both operations independent from ticket detail serialization.
- [x] T037 [US3] Add route-level path and query validation plus malformed enum/pagination handling in `TMS/backend/src/main/java/com/tms/backend/tms_backend/controller/` so invalid requests fail before service mutation or search.
- [x] T038 [US3] Add `@WebMvcTest` coverage in `TMS/backend/src/test/java/com/tms/backend/tms_backend/controller/TicketControllerTest.java` for successful ticket routes, request validation, 404 mapping, 409 transition mapping, page defaults, and response-field exclusion of priority/comments.
- [x] T039 [US2] Add `@WebMvcTest` coverage in `TMS/backend/src/test/java/com/tms/backend/tms_backend/controller/CommentControllerTest.java` for comment creation/listing, validation boundaries, missing tickets, empty collections/pages, and independent route handling.
- [x] T040 [US3] Add global error contract assertions in `TMS/backend/src/test/java/com/tms/backend/tms_backend/controller/GlobalExceptionHandlerTest.java` for `ProblemDetail` fields, field errors, stable codes/types, request path, timestamp, and no stack-trace or persistence-detail leakage.

## Phase 7: Testing Suite & Contract Verification

**Purpose**: Complete cross-layer verification for the requested acceptance criteria and the OpenAPI contract.

- [x] T041 [P] [US1] Add end-to-end lifecycle coverage in `TMS/backend/src/test/java/com/tms/backend/tms_backend/controller/TicketLifecycleIntegrationTest.java` for create, retrieve, update, all five valid transitions, invalid transitions, and unchanged state after conflicts.
- [x] T042 [P] [US2] Add search/filter/comment integration coverage in `TMS/backend/src/test/java/com/tms/backend/tms_backend/controller/TicketDiscoveryIntegrationTest.java` for seeded tickets, title/description matching, combined filters, pagination metadata, comment creation, and independent comment retrieval.
- [x] T043 [P] [US3] Add invalid-input integration coverage in `TMS/backend/src/test/java/com/tms/backend/tms_backend/controller/ValidationErrorIntegrationTest.java` for title/comment boundaries, unknown enums, pagination bounds, missing bodies, 404s, and safe unexpected errors.
- [x] T044 [US1] Add a concurrent optimistic-lock integration test in `TMS/backend/src/test/java/com/tms/backend/tms_backend/service/ConcurrentStatusTransitionTest.java` proving stale status updates cannot overwrite the winning state and are mapped to HTTP 409.
- [x] T045 [US2] Add OpenAPI contract verification in `TMS/backend/src/test/java/com/tms/backend/tms_backend/controller/OpenApiContractTest.java` or an equivalent Maven validation step against `specs/001-ticket-management-backend/contracts/openapi.yaml`, confirming all routes, schemas, status codes, and no priority/comment embedding in ticket details.
- [x] T046 [US3] Add a persistence restart/round-trip test in `TMS/backend/src/test/java/com/tms/backend/tms_backend/repository/PersistenceRoundTripTest.java` proving tickets and comments remain available after entity-manager/service restart against PostgreSQL.
- [x] T047 Run `cd TMS/backend && ./mvnw test` and fix only feature-related failures; retain deterministic AAA unit tests and isolated MVC/JPA slices.

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Align documentation, configuration, performance, and final acceptance evidence with the approved design.

- [x] T048 [P] Update `TMS/backend/src/main/resources/application.properties` and supporting configuration documentation to describe PostgreSQL environment variables, UTC timestamps, `ddl-auto=update`, and non-secret defaults.
- [x] T049 [P] Reconcile stale priority and embedded-comment statements in `specs/001-ticket-management-backend/spec.md` with the approved no-priority and decoupled-comment decisions, or record the explicit supersession in the feature documentation.
- [x] T050 [P] Update `specs/001-ticket-management-backend/quickstart.md` with the final Maven commands, Swagger URL, independent comment workflow, and actual test prerequisites.
- [x] T051 Review `specs/001-ticket-management-backend/contracts/openapi.yaml` against controller behavior and generated Swagger output; document any compatibility or rollback impact in `specs/001-ticket-management-backend/plan.md`.
- [x] T052 Run the quickstart scenarios from `specs/001-ticket-management-backend/quickstart.md` against PostgreSQL and record evidence for lifecycle, discovery, validation, persistence, and error-safety criteria.
- [x] T053 Run `git diff --check` and `cd TMS/backend && ./mvnw test` as the final quality gate; confirm no priority symbols, entity leakage, Flyway files, or untracked secrets were introduced.

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No feature dependencies; starts immediately.
- **Phase 2 (Domain)**: Depends on T001-T004; blocks all story implementation.
- **Phase 3 (DTOs/Exceptions)**: Depends on Phase 2 entity/status names; blocks service and controller work.
- **Phase 4 (Repositories)**: Depends on Phase 2 entities and Phase 1 JPA configuration; can run partly in parallel with Phase 3.
- **Phase 5 (Services)**: Depends on Phases 2-4.
- **Phase 6 (Controllers)**: Depends on Phase 5 and the OpenAPI contract.
- **Phase 7 (Testing)**: Unit tests can begin with their implementation slice; integration and controller contract tests depend on Phases 1-6.
- **Phase 8 (Polish)**: Depends on all required implementation and test tasks.

### User Story Dependencies

- **User Story 1 (P1)**: Starts after foundational setup/domain/DTO/repository work; delivers the MVP ticket lifecycle independently.
- **User Story 2 (P2)**: Uses the Ticket entity and service foundation from US1 but adds independent search, pagination, and comment operations; it must not reintroduce embedded comments.
- **User Story 3 (P3)**: Cross-cuts US1 and US2 through validation and error contracts; its focused tests can run after the corresponding endpoints exist.

### Parallel Opportunities

- T002-T005 can run in parallel after confirming the existing Maven project.
- T007-T009 and T012-T016 can run in parallel when their referenced package paths exist.
- T019-T021 can run in parallel after entities are defined.
- T031-T032 can run in parallel after the corresponding service APIs are stable.
- T038-T040 can run in parallel because they target separate test files and controller concerns.
- T041-T046 can run in parallel once the application layers and test database configuration are complete.
- T048-T050 can run in parallel during polish.

## Independent Test Criteria

- **US1/P1**: A test can create one ticket, retrieve only its ticket attributes, update editable fields without changing status/creation time, execute all five permitted transitions, and prove every illegal transition returns 409 without changing state.
- **US2/P2**: Seed tickets and comments, then independently verify unfiltered/status/keyword/combined/paginated ticket results plus comment creation and paginated retrieval from the dedicated comments endpoint.
- **US3/P3**: Submit each documented invalid boundary, missing resource, malformed body, enum, pagination, and unexpected-failure case and verify 400/404/409/500 structured errors with field details where applicable and no sensitive internals.

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phases 1-4 for the shared foundation.
2. Complete the US1 tasks in Phases 5-6.
3. Run T011, T031, T034, T038, and the lifecycle checks in T041.
4. Stop at the US1 checkpoint and validate the lifecycle before adding discovery features.

### Incremental Delivery

1. Deliver Setup, Domain, DTO/Exception, and Repository foundation.
2. Deliver US1 ticket lifecycle and validate independently.
3. Deliver US2 search, pagination, and independent comments and validate independently.
4. Deliver US3 complete error contract coverage.
5. Finish cross-cutting polish and the final Maven/quickstart gates.

### Technical Phase Mapping

- Phase 1: Setup & Configuration
- Phase 2: Domain Model & State Machine
- Phase 3: DTOs & Custom Exceptions
- Phase 4: Repositories & Search Specification
- Phase 5: Service Layer
- Phase 6: REST Controllers
- Phase 7: Testing Suite & Contract Verification
- Phase 8: Polish & Cross-Cutting Concerns

## Notes

- Every task has an ID, checkbox, and concrete file path.
- `[P]` marks only tasks that target independent files or concerns without unfinished prerequisites.
- `[US1]`, `[US2]`, and `[US3]` map implementation and test work to the prioritized stories in `spec.md`.
- Manual mapping is required; do not add MapStruct.
- Do not add priority to entities, DTOs, repositories, database configuration, or OpenAPI.
- Do not fetch or embed comments in `GET /api/v1/tickets/{ticketId}`; use the dedicated comment endpoints.
- Do not introduce Flyway; retain `spring.jpa.hibernate.ddl-auto=update`.
