# Cinema Booking Platform — Codex Instructions

## Mission

Build and maintain a reliable cinema ticket booking platform using a microservices architecture.

The highest-priority system properties are:

1. booking correctness
2. prevention of double booking
3. payment correctness
4. transactional consistency
5. idempotency
6. service ownership boundaries
7. maintainability
8. security
9. observability
10. user experience

Performance optimizations must never weaken booking or payment correctness.

---

# Architecture

The system contains:

* frontend/web
* API Gateway
* auth-service
* movie-service
* cinema-service
* booking-service
* payment-service
* notification-service

Infrastructure:

* PostgreSQL
* Redis
* Kafka
* Docker Compose

Communication:

* REST for synchronous requests
* Kafka for domain events

Distributed workflow:

* Saga
* Transactional Outbox

---

# Core Architecture Rule

Each microservice owns its data.

A service MUST NOT:

* query another service's database
* write another service's tables
* create cross-service SQL joins

Inter-service data must be accessed through:

* API calls
* events

---

# Booking Correctness Rule

booking-service is the authoritative owner of showtime seat availability.

PostgreSQL is the source of truth.

Redis is NOT the source of truth for seat ownership.

Redis may only support:

* caching
* TTL helpers
* rate limiting
* transient notifications

Never implement booking correctness exclusively with:

* Redis lock
* frontend state
* in-memory lock
* synchronized Java blocks

because the application may run multiple instances.

---

# Seat State Machine

Allowed states:

AVAILABLE
HELD
PAYMENT_PENDING
SOLD

Common transitions:

AVAILABLE -> HELD

HELD -> PAYMENT_PENDING

PAYMENT_PENDING -> SOLD

HELD -> AVAILABLE

PAYMENT_PENDING -> AVAILABLE

Transitions must be explicit domain operations.

Do not update seat states directly from controllers.

---

# Reservation Transaction

When reserving seats:

1. validate request
2. sort seat IDs
3. begin transaction
4. lock requested showtime seat rows
5. check availability
6. reject entire reservation if any required seat is unavailable
7. update all requested seats atomically
8. create reservation
9. create outbox event
10. commit

Use PostgreSQL row-level locking where required.

Acquire locks in deterministic order.

Prefer correctness over lock-free complexity.

---

# Double Booking Invariant

For every:

(showtimeId, seatId)

at most one successful booking may exist.

This invariant must be protected by:

* database transaction
* locking
* constraints
* state validation

not by UI behavior.

---

# Reservation Expiration

Seat holds must expire.

Expiration duration must be configurable.

Expiration processing must be:

* transactional
* idempotent
* safe when multiple workers run concurrently

Expired holds must eventually return seats to AVAILABLE.

---

# Payment

payment-service owns provider integrations.

booking-service must never call a payment gateway directly.

Use a payment provider interface.

Example implementations may later include:

* Stripe
* VNPay
* MoMo
* ZaloPay

Payment API requests must be idempotent.

Payment webhooks must be idempotent.

Persist:

* payment attempt
* provider transaction ID
* provider event ID
* idempotency key
* state
* amount
* timestamps

Never trust frontend redirects as proof of successful payment.

---

# Distributed Transactions

Never attempt cross-service database transactions.

Use:

Saga
+
Transactional Outbox

Important workflow:

ReservationCreated
→ PaymentRequested
→ PaymentSucceeded
→ BookingConfirmed

Failure:

PaymentFailed
→ ReservationReleased

---

# Outbox Rule

For any critical domain event:

business mutation
+
outbox insert

must happen in the same local database transaction.

Never implement:

save database
commit
publish Kafka event

as independent critical steps.

---

# Event Consumers

Kafka consumers must assume:

at-least-once delivery.

Therefore all event handlers must be idempotent.

Store processed event IDs where necessary.

Never assume an event is delivered exactly once.

---

# API Design

Controllers should:

* parse HTTP requests
* perform basic validation
* call application services
* map responses

Controllers must not contain:

* transaction logic
* locking logic
* payment logic
* complex domain logic

Use standard error responses.

Example:

{
"code": "SEAT_ALREADY_RESERVED",
"message": "One or more requested seats are unavailable.",
"traceId": "...",
"timestamp": "..."
}

---

# Backend Package Structure

Prefer:

com.cinema.<service>
├── application
│   ├── command
│   ├── query
│   └── service
├── domain
│   ├── model
│   ├── event
│   ├── repository
│   └── exception
├── infrastructure
│   ├── persistence
│   ├── messaging
│   ├── config
│   └── client
└── interfaces
└── rest

Avoid unnecessary abstraction.

Do not introduce interfaces without a real boundary.

---

# Database

Use Flyway.

Every schema change requires a migration.

Never use Hibernate automatic schema updates in production-oriented configuration.

Do not use:

spring.jpa.hibernate.ddl-auto=update

for persistent environments.

Use:

validate

when appropriate.

---

# PostgreSQL Constraints

Use database constraints whenever an invariant can be enforced by the database.

Examples:

* foreign keys inside service boundaries
* unique constraints
* check constraints
* indexes

Do not rely only on Java validation.

---

# Concurrency Testing

booking-service changes affecting reservation state MUST include real PostgreSQL integration tests.

Use Testcontainers.

Required scenario:

100 concurrent attempts to reserve the same seat.

Expected:

success count = 1

Do not use H2 to validate locking behavior.

---

# Redis

Redis is optional for correctness.

The application should remain logically correct even if Redis cache is flushed.

Never keep irreplaceable booking state only in Redis.

---

# Frontend

Frontend stack:

* React
* TypeScript
* Vite
* TanStack Query
* React Router
* React Three Fiber
* Framer Motion

Prefer feature organization.

frontend/web/src/features/

* auth
* movies
* cinemas
* showtimes
* booking
* payment
* profile

Server state belongs primarily in TanStack Query.

Do not duplicate server data unnecessarily in global state.

---

# Three.js

Three.js is progressive enhancement.

Never make core booking dependent on WebGL.

Support:

prefers-reduced-motion.

Lazy load heavy visual components.

Avoid unnecessary GPU-intensive animation.

---

# Accessibility

Interactive components must support:

* keyboard navigation
* visible focus state
* semantic HTML
* appropriate aria labels
* sufficient contrast

Seat maps must not rely exclusively on color.

Every seat needs an accessible label.

Example:

"A5, VIP seat, available"

---

# Testing

Backend:

* unit tests
* integration tests
* Testcontainers
* concurrency tests

Frontend:

* component tests where useful
* Playwright critical journeys

Critical E2E flow:

movie
→ cinema
→ showtime
→ seats
→ reservation
→ checkout
→ payment
→ ticket

---

# Security

Never commit:

* API keys
* database passwords
* JWT secrets
* payment secrets

Use environment variables.

Never log:

* passwords
* payment secrets
* JWTs
* sensitive provider payloads

Validate authorization at backend boundaries.

---

# Observability

Generate or propagate correlation IDs.

Prefer structured logging.

Important operations should log:

* traceId
* userId when safe
* reservationId
* bookingId
* paymentId
* showtimeId

Never log sensitive payment credentials.

---

# Code Change Workflow

Before implementing a task:

1. read this AGENTS.md
2. locate relevant skill
3. inspect architecture docs
4. inspect current implementation
5. inspect tests
6. identify affected invariant
7. implement smallest coherent change
8. add/update tests
9. run tests
10. summarize changes

---

# Skills

Relevant skills are stored under:

skills/

Use the corresponding skill before working on that area.

Examples:

booking change:
skills/booking-concurrency/SKILL.md

database change:
skills/database-design/SKILL.md

payment change:
skills/payment/SKILL.md

microservice boundary change:
skills/microservice-architecture/SKILL.md

frontend UI change:
skills/frontend-ui/SKILL.md

Three.js:
skills/threejs/SKILL.md

testing:
skills/testing/SKILL.md

code review:
skills/code-review/SKILL.md

---

# Forbidden Changes

Do not:

* share databases between services
* use distributed transactions
* depend solely on Redis locks
* mutate booking state from frontend confirmation
* trust frontend payment success
* remove idempotency
* remove concurrency tests
* put domain logic inside controllers
* publish critical events outside the Outbox pattern
* introduce synchronous service chains without justification
* create a new microservice for trivial functionality
* add Three.js to simple UI where CSS is sufficient

---

# Decision Priority

When trade-offs arise, prioritize:

correctness

> data consistency
> security
> maintainability
> observability
> performance
> visual effects

unless an ADR explicitly states otherwise.

---

# Completion Requirement

For every completed task report:

## Changed

Files changed.

## Architecture

Any architectural implications.

## Database

Migration changes.

## Tests

Tests added and results.

## Risks

Known risks.

## Follow-up

Recommended next step.
