# ADR-001: Microservices Architecture

- Status: Accepted
- Date: 2026-09-24

## Context

The platform separates identity, catalog, cinema scheduling, booking correctness, payments, and notifications. These areas have different security, consistency, integration, and scaling concerns. Booking and payment changes in particular must remain isolated and auditable. At the same time, unnecessary services would add network failure modes and operational overhead before bounded contexts are proven.

## Decision

Use the following initial deployables: `api-gateway`, `auth-service`, `movie-service`, `cinema-service`, `booking-service`, `payment-service`, and `notification-service`, plus `frontend/web`.

Services communicate synchronously through versioned REST contracts when an immediate response is required and asynchronously through Kafka domain events. Each service owns its data and local transactions. Distributed booking/payment work uses a Saga and Transactional Outbox, never a distributed database transaction.

Do not add another microservice unless a durable bounded context has independent ownership, lifecycle, security/scale needs, and enough behavior to justify its operational cost. Such a change requires an ADR.

## Consequences

- Teams can evolve bounded contexts independently and apply least-privilege data access.
- Remote calls, eventual consistency, contract evolution, observability, and failure recovery become first-class design concerns.
- Local development and deployment are more complex than a monolith.
- Ticket entitlement remains with booking, refunds with payment, and expiration workers with booking; none warrants another service today.

## Guardrails

- The API Gateway contains edge concerns, not domain workflow logic.
- No synchronous chain is introduced without a documented immediate-response need, timeouts, and failure behavior.
- Critical local changes publish through an outbox; consumers are idempotent under at-least-once delivery.
- Service and database ownership follow `docs/service-boundaries.md`.

