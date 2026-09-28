# Quickstart Validation Guide

## Prerequisites

- Java 21
- Maven Wrapper from `backend/mvnw`
- PostgreSQL available locally for runtime validation, or Docker for Testcontainers
- `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` supplied through the environment; do not commit credentials

## Start the backend

From the repository root:

```bash
cd backend
./mvnw spring-boot:run
```

Open Swagger UI at `http://localhost:8080/swagger-ui/index.html` and compare the generated endpoints with [contracts/openapi.yaml](contracts/openapi.yaml).

The default runtime datasource is `jdbc:postgresql://localhost:5432/tms` with username `postgres` and an empty password placeholder. Set all three `DB_*` variables for a real deployment.

## Validate the lifecycle

1. `POST /api/v1/tickets` with a valid title and description. Expect `201`, generated `id`, status `OPEN`, and timestamps.
2. `GET /api/v1/tickets/{id}`. Expect ticket attributes only; comments are not embedded and persistence fields are not exposed.
3. `PUT /api/v1/tickets/{id}` with valid title, description, and optional assignee. Expect editable fields updated while status and `createdAt` remain unchanged.
4. `PATCH /api/v1/tickets/{id}/status` through `IN_PROGRESS`, `RESOLVED`, and `CLOSED`; verify the two allowed cancellation paths from `OPEN` and `IN_PROGRESS`.
5. Attempt a terminal or otherwise unlisted transition. Expect `409` with `INVALID_STATUS_TRANSITION` and unchanged status.

## Validate search and comments

1. Create tickets with different titles, descriptions, and statuses.
2. Call `GET /api/v1/tickets?page=0&size=10&keyword=login&status=OPEN`; verify both filters apply and metadata is present.
3. Call `POST /api/v1/tickets/{id}/comments` with valid content, then `GET /api/v1/tickets/{id}/comments?page=0&size=10`; expect a created comment and a paginated chronological collection.
4. Use an unknown ticket id for details, updates, status, and comment creation. Expect `404` and no mutation.

## Validate boundaries and errors

- Titles of 4 and 151 characters, blank descriptions, blank comment content, and 1001-character comments return `400` with field errors.
- Unknown status values and page sizes outside `1..100` return `400`.
- Missing or malformed request bodies return `400`.
- Every error includes the stable structured fields described in the OpenAPI contract and exposes no stack trace or database detail.

## Automated checks

```bash
cd backend
./mvnw test
```

The suite includes pure JUnit/Mockito service tests, Spring MVC contract tests, H2-backed JPA tests, integration tests, and OpenAPI contract checks. PostgreSQL/Testcontainers execution requires Docker; when Docker is unavailable, the H2 tests still provide deterministic local evidence while PostgreSQL validation remains pending.

The concurrent transition test demonstrates that an optimistic-lock loser cannot overwrite the winning status; the HTTP conflict mapping is covered by the controller error tests.
