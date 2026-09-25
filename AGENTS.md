# Cinema Booking Platform — Agent Instructions

## 1. Project Mission

Build and maintain a reliable cinema ticket booking platform using a microservices architecture.

The most important system properties, in priority order, are:

1. booking correctness
2. prevention of double booking
3. payment correctness
4. transactional consistency
5. idempotency
6. clear service boundaries
7. maintainability
8. security
9. observability
10. user experience

Correctness must never be sacrificed for performance or implementation convenience.

---

## 2. Technology Stack

**Frontend:** React, TypeScript, Vite, React Router, TanStack Query, Zustand (only for genuine global client state), Framer Motion, Three.js / React Three Fiber.

**Backend:** Java, Spring Boot, Spring Web, Spring Security, Spring Data JPA, Spring Validation, PostgreSQL, Redis, Kafka, Flyway, OpenAPI / Swagger.

**Testing:** JUnit 5, Mockito, Spring Boot Test, Testcontainers, Playwright.

**Infrastructure:** Docker, Docker Compose, PostgreSQL, Redis, Kafka.

---

## 3. Development Commands

> These commands should reflect the actual repository (see current status in Section 4). Verify them against the current Maven wrappers, `pom.xml`, `package.json`, and Docker Compose files before changing them. Do not invent missing commands.

**Environment**
```bash
docker compose up -d
docker compose down
```

**Backend (per service, from the service directory)**
```bash
./mvnw spring-boot:run
./mvnw test
./mvnw test -Dtest=ClassName#methodName
```

**Database migrations**

Use the migration mechanism actually configured by the service. `./mvnw flyway:migrate` may be used only when the Flyway Maven plugin is configured; otherwise use the repository's configured Flyway startup/migration workflow. Check `pom.xml` first.

**Frontend**
```bash
npm install
npm run dev
npm run build
npm run lint
npm run test
npx playwright test
```

Do not invent or guess a command that is not defined here or in the service's own `pom.xml` / `package.json` scripts.

---

## 4. System Architecture

Initial services: `frontend/web`, `api-gateway`, `auth-service`, `movie-service`, `cinema-service`, `booking-service`, `payment-service`, `notification-service`.

Communication:
- REST for synchronous request/response
- Kafka for asynchronous domain events

Distributed workflows use the **Saga** and **Transactional Outbox** patterns.

Do not introduce additional microservices unless there is a clear business capability and bounded-context reason. See Section 16 for relevant implementation guidance..

### Current Status

Infrastructure and initial service skeletons created during Phase 1
already exist.

Treat existing working infrastructure as established repository state.
Inspect it before proposing replacement or regeneration.

Future phases build incrementally on the current repository.

---

## 5. Service Ownership

Each microservice owns its own data. A service **must not**:
- query another service's database directly
- modify another service's tables
- create cross-service SQL joins
- depend on another service's persistence entities

Cross-service communication happens only through APIs or domain events.

---

## 6. Service Responsibilities

**auth-service** — Owns users, authentication, credentials, roles, tokens. Handles registration, login, authentication, authorization, user identity.

**movie-service** — Owns movies, genres, actors, directors, movie metadata. Handles catalog, details, search, filtering, now-showing/upcoming.

**cinema-service** — Owns cinemas, auditoriums, physical seat definitions, showtimes. Handles cinema/auditorium configuration, seat layouts, showtime scheduling. Physical seat definitions do **not** represent booking availability.

**booking-service** — Owns showtime seat inventory, reservations, bookings, booking items, seat holds. Is the authoritative owner of booking state and seat availability. PostgreSQL is the source of truth; Redis must never be the sole authority for seat reservation correctness.

**payment-service** — Owns payment attempts, provider transaction references, payment state, refund state, webhook processing. Payment provider integrations are isolated inside this service; `booking-service` must never integrate directly with an external payment provider.

**notification-service** — Handles booking confirmation, email notifications, payment notifications, optional ticket/QR delivery. Processing is asynchronous; notification failure must never roll back a successful booking.

*(See Section 16 for which skill to read before working on each area.)*

---

## 7. Critical System Invariants

These rules are the **canonical source of truth for correctness**. Other sections may summarize them for context or quick reference, but must not redefine or weaken them.

**Booking**
- For a given `(showtimeId, seatId)`, at most one successful booking may own that seat.
- Booking correctness is enforced on the backend only. Frontend state must never be trusted for seat ownership. PostgreSQL is authoritative.

**Payment**
- A successful frontend redirect is not proof of successful payment. Payment status must be verified server-side.
- Payment operations and provider callbacks must be idempotent.

**Events**
- Kafka consumers must assume at-least-once delivery. Consumers handling business-critical events must be idempotent.
- Critical business state changes and their outgoing events must use the Transactional Outbox pattern.

**Database ownership**
- No service may directly access another service's database (see Section 5).

---

## 8. Backend Architecture

Preferred package structure inside each Spring Boot service:

```text
com.cinema.<service>
├── application/
│   ├── command/
│   ├── query/
│   └── service/
├── domain/
│   ├── model/
│   ├── event/
│   ├── repository/
│   └── exception/
├── infrastructure/
│   ├── persistence/
│   ├── messaging/
│   ├── config/
│   └── client/
└── interfaces/
    └── rest/
```

Use this structure pragmatically — do not create abstractions without a real architectural reason. Controllers stay thin: validate HTTP input, call application services, map responses. No business, transaction, or locking logic in controllers.

---

## 9. Database Rules

- PostgreSQL, with Flyway for schema migrations.
- Persistent environments must never rely on `spring.jpa.hibernate.ddl-auto=update`.
- Use database constraints to protect business invariants.
- Never use floating-point types for currency.

---

## 10. Event-Driven Architecture

Events represent completed business facts. Prefer past-tense, fact-like names:

```text
PaymentSucceeded
BookingConfirmed
SeatHoldExpired
```

Avoid command-like event names such as `UpdateBookingStatus` or `SetSeatAvailable`.

---

## 11. Frontend Architecture

Preferred structure:

```text
frontend/web/src/
├── api/
├── assets/
├── components/
├── features/
│   ├── auth/
│   ├── movies/
│   ├── cinemas/
│   ├── showtimes/
│   ├── booking/
│   ├── payment/
│   └── profile/
├── hooks/
├── layouts/
├── pages/
├── routes/
├── stores/
├── three/
├── types/
└── utils/
```

Use TanStack Query for server state; do not unnecessarily duplicate server data into global client (Zustand) state.

---

## 12. Three.js

Three.js is a progressive enhancement. Core booking functionality must remain usable without WebGL. Three.js must never be required for forms, authentication, seat reservation correctness, checkout, or payment.

---

## 13. Security

Never commit API keys, database passwords, JWT secrets, payment provider secrets, or other private credentials — use environment variables or a secret manager.

Never log passwords, raw JWT tokens, payment secrets, or sensitive provider credentials.

Authorization is enforced on the backend only; never rely on frontend role checks for security.

**Payment data handling:** never store raw card numbers, CVV, or other full cardholder data. `payment-service` stores only provider-issued tokens/transaction references. Any work touching stored payment data should be treated as PCI-DSS-sensitive by default — flag it explicitly rather than assuming it is fine.

---

## 14. Observability

Generate or propagate correlation IDs between services. Prefer structured logging, including identifiers such as `traceId`, `reservationId`, `bookingId`, `paymentId`, `showtimeId` where relevant. Never log sensitive credentials or payment secrets.

The architecture should remain compatible with OpenTelemetry, Prometheus, and Grafana, without requiring unnecessary monitoring complexity during early phases.

---

## 15. Testing

Tests must verify business invariants, not only HTTP status codes.

- Backend: unit tests, integration tests, Testcontainers, concurrency tests.
- Frontend: critical workflows tested with Playwright.
- Concurrency-sensitive booking tests must use real PostgreSQL behavior, not H2.

---

## 16. Skill Routing

Specialized engineering instructions live under `skills/`. Before implementing a task, identify and read **all** relevant skills — a task may need more than one. This is the single skill index; do not duplicate these pointers elsewhere.

| Topic | Read |
|---|---|
| Service boundaries, new services, inter-service APIs, distributed workflows | `skills/microservice-architecture/SKILL.md` |
| Schema, migrations, indexes, constraints, transactions, locking | `skills/database-design/SKILL.md` |
| Seat availability, holds, booking, concurrency, expiration, double-booking prevention | `skills/booking-concurrency/SKILL.md` |
| Payment, provider integration, webhooks, refunds, idempotency | `skills/payment/SKILL.md` |
| Kafka, domain events, Saga, Outbox, consumers/producers | `skills/event-driven/SKILL.md` |
| React, frontend architecture, UI, UX, accessibility | `skills/frontend-ui/SKILL.md` |
| Three.js, React Three Fiber, 3D scenes, WebGL | `skills/threejs/SKILL.md` |
| Unit/integration/E2E tests, Testcontainers, concurrency tests | `skills/testing/SKILL.md` |
| Before marking any implementation task complete | `skills/code-review/SKILL.md` |

If a task sits between two skills' scope, read both. If no skill clearly matches, read the closest one and state that assumption rather than guessing silently.

### Ponytail

Ponytail and similar simplification/review tools are optimization layers, not architecture authorities. They may simplify implementation only when **Section 7 (Critical System Invariants)** and **Section 22 (Forbidden Patterns)** remain fully intact — do not restate those rules here; they apply regardless of tooling.

Run Ponytail, if at all, only after tests pass and `skills/code-review/SKILL.md` has been applied (see Section 20, step 10). Re-run affected tests after any simplification. Never accept a simplification solely because it reduces code volume.

---

## 17. Precedence When Sources Conflict

When repository instructions conflict, use the following precedence:

1. **Critical System Invariants** in Section 7.
2. **Accepted ADRs** under `docs/ADR/` for explicit architectural decisions.
3. **This `AGENTS.md`** for repository-wide rules and constraints.
4. **Relevant `SKILL.md` files** for task-specific implementation guidance.
5. **Other files under `docs/`** for descriptive architecture and design documentation.
6. General engineering conventions.

A lower-precedence source must never silently override a higher-precedence source.

An accepted ADR may refine implementation choices but must never weaken a Critical System Invariant.

If a conflict is found, follow the highest-precedence applicable source, flag the conflict in the task report, and reconcile the documents when appropriate.

---

## 18. Autonomy Boundaries

- Do not run database migrations against anything other than a local/dev environment without explicit confirmation.
- Do not merge pull requests or push directly to protected branches (e.g. `main`) without explicit confirmation.
- Do not delete or rewrite Kafka topics, Postgres data, or Redis state outside a local/dev environment.

When ambiguity materially affects correctness, security, data consistency, payment behavior, or irreversible architecture, follow the inspection order in Section 20 (steps 2–4) before deciding.

If the decision remains materially ambiguous and interactive clarification is available, ask before making an irreversible choice.

If clarification is not practical or the task is intended to run autonomously, choose the safest reversible option, preserve all Critical System Invariants, document the assumption in the task report, and continue with the smallest coherent implementation.

For lower-stakes ambiguity (naming, minor structuring), choose a repository-consistent default, state the assumption when useful, and proceed.

Never silently weaken a Critical System Invariant to resolve ambiguity.

---

## 19. Git & Commit Conventions

- Branch naming: `type/short-description` (e.g. `fix/seat-hold-race-condition`, `feat/refund-webhook`).
- Commit messages: short imperative summary line, optionally followed by a body explaining *why*, not just *what*.
- Keep commits scoped to one logical change; don't mix unrelated services in one commit.
- Do not include secrets, `.env` files, or credentials in any commit.

---

## 20. Task Workflow

Before implementing a task:

1. Read this `AGENTS.md`.
2. Identify affected architectural areas and inspect relevant accepted ADRs.
3. Identify relevant skills (Section 16) and read the required `SKILL.md` files.
4. Inspect other relevant architecture documentation.
5. Inspect the existing implementation and existing tests.
6. Identify which system invariants (Section 7) are affected.
7. Implement the smallest coherent change.
8. Add or update tests.
9. Run the relevant tests.
10. Review using `skills/code-review/SKILL.md`.
11. Optionally apply Ponytail review.

Do not modify architecture implicitly. If a change affects an important architectural decision, update or create an ADR (Section 21).

See Section 4 for current repository status before generating any new scaffolding.

---

## 21. Architecture Documentation

Important design decisions belong in:

```text
docs/
├── architecture.md
├── service-boundaries.md
├── database-design.md
├── booking-flow.md
├── concurrency.md
├── payment-flow.md
├── event-catalog.md
├── api-contracts.md
└── ADR/
```

Division of responsibility:
- `AGENTS.md` defines repository-wide rules and invariants
- accepted ADRs define explicit architectural decisions
- `SKILL.md` defines specialized implementation guidance
- other `docs/` files explain architecture and design

Avoid duplicating large amounts of information between these locations.

---

## 22. Forbidden Patterns — Quick Checklist

Do not:
- share databases between microservices, or perform cross-service SQL joins
- introduce distributed ACID transactions
- use Redis as the only seat-locking mechanism
- use in-memory Java locks for distributed booking correctness
- trust frontend booking or payment state
- remove payment idempotency
- place domain logic inside controllers
- publish critical domain events without the Outbox strategy
- create unnecessary microservices, or synchronous service chains without justification
- expose payment secrets to the frontend
- make core booking depend on Three.js or WebGL
- weaken concurrency guarantees merely to make tests pass
- let Ponytail or another simplification tool remove required correctness mechanisms

---

## 23. Decision Priority

When engineering trade-offs arise, prioritize:

```text
correctness
> data consistency
> security
> maintainability
> observability
> performance
> visual effects
```

An accepted ADR may refine a trade-off only when it does not violate a Critical System Invariant.

---

## 24. Definition of Done

A task is complete only when applicable requirements are satisfied: implementation complete, relevant tests pass, service boundaries remain valid, database migration included when necessary, concurrency invariants protected, idempotency preserved, architecture documentation updated when required, accepted ADRs remain consistent with the implementation, code review skill applied.

**Report scale:** match the report to the size of the change.
- Small, low-risk change (e.g. one validation rule, one bug fix): report `Changed` + `Tests` only.
- Anything touching booking, payment, concurrency, schema, Kafka/event semantics, security, service boundaries, or architecture: report all sections below in full.

### Changed
Files changed.

### Architecture
Architectural impact, if any.

### Database
Schema or migration changes, if any.

### Tests
Tests added or updated, and their results.

### Risks
Known risks or limitations.

### Follow-up
Recommended next step.
