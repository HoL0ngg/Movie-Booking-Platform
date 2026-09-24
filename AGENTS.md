# Cinema Booking Platform — Agent Instructions

## 1. Project Mission

Build and maintain a reliable cinema ticket booking platform using a microservices architecture.

The most important system properties are:

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

# 2. Technology Stack

## Frontend

* React
* TypeScript
* Vite
* React Router
* TanStack Query
* Zustand when global client state is appropriate
* Framer Motion
* Three.js / React Three Fiber

## Backend

* Java
* Spring Boot
* Spring Web
* Spring Security
* Spring Data JPA
* Spring Validation
* PostgreSQL
* Redis
* Kafka
* Flyway
* OpenAPI / Swagger

## Testing

* JUnit 5
* Mockito
* Spring Boot Test
* Testcontainers
* Playwright

## Infrastructure

* Docker
* Docker Compose
* PostgreSQL
* Redis
* Kafka

---

# 3. System Architecture

The initial system consists of:

* frontend/web
* api-gateway
* auth-service
* movie-service
* cinema-service
* booking-service
* payment-service
* notification-service

Communication:

* REST for synchronous request/response operations
* Kafka for asynchronous domain events

Distributed workflows should use:

* Saga
* Transactional Outbox

Do not introduce additional microservices unless there is a clear business capability and bounded-context reason.

---

# 4. Service Ownership

Each microservice owns its own data.

A service MUST NOT:

* query another service's database directly
* modify another service's tables
* create cross-service SQL joins
* depend on another service's persistence entities

Cross-service communication must happen through:

* APIs
* domain events

---

# 5. Service Responsibilities

## auth-service

Owns:

* users
* authentication
* credentials
* roles
* tokens

Responsibilities:

* registration
* login
* authentication
* authorization
* user identity

---

## movie-service

Owns:

* movies
* genres
* actors
* directors
* movie metadata

Responsibilities:

* movie catalog
* movie details
* search
* filtering
* now-showing and upcoming movies

---

## cinema-service

Owns:

* cinemas
* auditoriums
* physical seat definitions
* showtimes

Responsibilities:

* cinema configuration
* auditorium configuration
* seat layouts
* showtime scheduling

Physical seat definitions do not represent booking availability.

---

## booking-service

Owns:

* showtime seat inventory
* reservations
* bookings
* booking items
* seat holds

booking-service is the authoritative owner of booking state and seat availability.

PostgreSQL is the authoritative source of truth for seat ownership.

Redis must never be the only authority for seat reservation correctness.

For any booking-related work, read:

`skills/booking-concurrency/SKILL.md`

---

## payment-service

Owns:

* payment attempts
* provider transaction references
* payment state
* refund state
* webhook processing

Payment provider integrations must be isolated inside payment-service.

booking-service must never integrate directly with an external payment provider.

For payment-related work, read:

`skills/payment/SKILL.md`

---

## notification-service

Responsibilities:

* booking confirmation
* email notifications
* payment notifications
* optional ticket or QR delivery

Notification processing should be asynchronous.

Notification failure must not roll back a successful booking.

---

# 6. Critical System Invariants

The following rules must always remain true.

## Booking

For a given:

`(showtimeId, seatId)`

at most one successful booking may own that seat.

Booking correctness must be enforced on the backend.

Frontend state must never be trusted for seat ownership.

PostgreSQL remains authoritative.

---

## Payment

A successful frontend redirect is not proof of successful payment.

Payment status must be verified server-side.

Payment operations and provider callbacks must support idempotency.

---

## Events

Kafka consumers must assume at-least-once delivery.

Consumers handling business-critical events must be idempotent.

Critical business state changes and corresponding outgoing events must use the Transactional Outbox pattern.

---

## Database Ownership

No service may directly access another service's database.

---

# 7. Backend Architecture

Prefer the following package structure inside Spring Boot services:

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

Use this structure pragmatically.

Do not create abstractions without a real architectural reason.

Controllers should be thin.

Controllers should primarily:

* validate HTTP input
* call application services
* map responses

Do not place complex business, transaction, or locking logic inside controllers.

---

# 8. Database Rules

Use PostgreSQL.

Use Flyway for schema migrations.

Persistent environments must not rely on:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Use database constraints where appropriate to protect business invariants.

Never use floating-point types for currency.

For any work involving:

* schema design
* indexes
* migrations
* transaction boundaries
* locking
* database constraints

read:

`skills/database-design/SKILL.md`

---

# 9. Event-Driven Architecture

Use Kafka for asynchronous domain communication.

Events represent completed business facts.

Prefer:

```text
PaymentSucceeded
BookingConfirmed
SeatHoldExpired
```

instead of command-like event names such as:

```text
UpdateBookingStatus
SetSeatAvailable
```

For work involving:

* Kafka
* events
* Saga
* Transactional Outbox
* event consumers
* event producers

read:

`skills/event-driven/SKILL.md`

---

# 10. Frontend Architecture

Prefer feature-based organization:

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

Use TanStack Query primarily for server state.

Do not unnecessarily duplicate server data into global client state.

For React UI/UX work, read:

`skills/frontend-ui/SKILL.md`

---

# 11. Three.js

Three.js is a progressive enhancement.

Core booking functionality must remain usable without WebGL.

Three.js must not be required for:

* forms
* authentication
* seat reservation correctness
* checkout
* payment

For Three.js or React Three Fiber work, read:

`skills/threejs/SKILL.md`

---

# 12. Security

Never commit:

* API keys
* database passwords
* JWT secrets
* payment provider secrets
* private credentials

Use environment variables or secret management.

Never log:

* passwords
* raw JWT tokens
* payment secrets
* sensitive provider credentials

Authorization must be enforced on the backend.

Do not rely on frontend role checks for security.

---

# 13. Observability

Generate or propagate correlation IDs between services.

Prefer structured logging.

Where relevant, include identifiers such as:

* traceId
* reservationId
* bookingId
* paymentId
* showtimeId

Do not log sensitive credentials or payment secrets.

The architecture should remain compatible with:

* OpenTelemetry
* Prometheus
* Grafana

without requiring unnecessary monitoring complexity during early phases.

---

# 14. Testing

Tests must verify business invariants, not only HTTP status codes.

Backend testing may include:

* unit tests
* integration tests
* Testcontainers
* concurrency tests

Frontend critical workflows should be tested with Playwright.

For implementation or testing work, read:

`skills/testing/SKILL.md`

Concurrency-sensitive booking tests must use real PostgreSQL behavior rather than H2.

---

# 15. Skill Routing

Specialized engineering instructions are stored under:

`skills/`

Before implementing a task, identify and read all relevant skills.

A task may require multiple skills.

## Microservices

For:

* service boundaries
* new services
* inter-service APIs
* distributed workflows

read:

`skills/microservice-architecture/SKILL.md`

---

## Database

For:

* PostgreSQL
* schema
* migration
* indexes
* constraints
* transactions
* locking

read:

`skills/database-design/SKILL.md`

---

## Booking & Concurrency

For:

* seat availability
* seat reservation
* holds
* booking
* concurrency
* expiration
* double-booking prevention

read:

`skills/booking-concurrency/SKILL.md`

---

## Payment

For:

* payment
* payment provider
* webhook
* refund
* idempotency

read:

`skills/payment/SKILL.md`

---

## Event-Driven Architecture

For:

* Kafka
* domain events
* Saga
* Outbox
* event consumers
* event producers

read:

`skills/event-driven/SKILL.md`

---

## Frontend

For:

* React
* frontend architecture
* UI
* UX
* accessibility

read:

`skills/frontend-ui/SKILL.md`

---

## Three.js

For:

* Three.js
* React Three Fiber
* 3D scenes
* WebGL effects

read:

`skills/threejs/SKILL.md`

---

## Testing

For:

* unit tests
* integration tests
* Testcontainers
* concurrency tests
* E2E tests

read:

`skills/testing/SKILL.md`

---

## Code Review

Before considering an implementation task complete, read:

`skills/code-review/SKILL.md`

---

# 16. Task Workflow

Before implementing a task:

1. Read this `AGENTS.md`.
2. Identify relevant skills.
3. Read the required `SKILL.md` files.
4. Inspect relevant architecture documentation.
5. Inspect the existing implementation.
6. Inspect existing tests.
7. Identify affected system invariants.
8. Implement the smallest coherent change.
9. Add or update tests.
10. Run relevant tests.
11. Review the implementation using `skills/code-review/SKILL.md`.

Do not modify architecture implicitly.

If a change affects an important architectural decision, update or create an ADR.

---

# 17. Architecture Documentation

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

`AGENTS.md` defines repository-wide rules.

`SKILL.md` defines specialized implementation guidance.

`docs/` explains architecture and design decisions.

Avoid duplicating large amounts of information between these locations.

---

# 18. Forbidden Patterns

Do not:

* share databases between microservices
* perform cross-service SQL joins
* introduce distributed ACID transactions
* use Redis as the only seat-locking mechanism
* use in-memory Java locks for distributed booking correctness
* trust frontend booking state
* trust frontend payment success
* remove payment idempotency
* place domain logic inside controllers
* publish critical domain events without the Outbox strategy
* create unnecessary microservices
* introduce synchronous service chains without justification
* expose payment secrets to the frontend
* make core booking depend on Three.js or WebGL
* weaken concurrency guarantees merely to make tests pass

---

# 19. Decision Priority

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

unless an accepted ADR explicitly establishes a different trade-off.

---

# 20. Definition of Done

A task is complete only when applicable requirements are satisfied:

* implementation is complete
* relevant tests pass
* service boundaries remain valid
* database migration is included when necessary
* concurrency invariants remain protected
* idempotency is preserved
* architecture documentation is updated when required
* code review skill has been applied

At completion, report:

## Changed

Files changed.

## Architecture

Architectural impact, if any.

## Database

Schema or migration changes, if any.

## Tests

Tests added or updated and their results.

## Risks

Known risks or limitations.

## Follow-up

Recommended next step.
