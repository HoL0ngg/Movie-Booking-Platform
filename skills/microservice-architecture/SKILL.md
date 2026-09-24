---
name: microservice-architecture
description: Design or change Cinema Booking Platform service boundaries, synchronous APIs, distributed workflows, ownership, or deployment topology.
---

# Microservice Architecture

## When to use

Use this skill for service-boundary changes, new cross-service workflows, API Gateway routing, service-to-service communication, data ownership, or proposals for another deployable service. Read `AGENTS.md`, `docs/architecture.md`, and `docs/service-boundaries.md` before changing architecture.

## Architectural rules

- Keep the initial deployables: `api-gateway`, `auth-service`, `movie-service`, `cinema-service`, `booking-service`, `payment-service`, and `notification-service`. Add a service only for a durable bounded context with independent data, lifecycle, and operational needs; record the choice in an ADR.
- Every service owns an isolated PostgreSQL database or equivalently isolated database/role. Other services have no SQL access, foreign keys, views, or joins into it.
- Use REST for bounded synchronous queries/commands and Kafka domain events for asynchronous propagation. Avoid latency-amplifying synchronous call chains.
- Keep business transactions local. Coordinate distributed booking/payment work with a Saga; never use XA or cross-service database transactions.
- Write critical business state and its Transactional Outbox record in the same local transaction.
- Treat Kafka as at-least-once. Consumers must be idempotent and durable; ordering is only assumed for messages sharing an aggregate partition key.
- `booking-service` owns reservations, bookings, ticket entitlement, and the authoritative `showtime_seats` availability state. `payment-service` alone owns provider integrations and payment/refund records.
- Keep the gateway focused on routing, authentication enforcement, rate limiting, correlation IDs, and edge concerns. It does not own domain workflows or data.

## Implementation rules

- Put domain transitions in application/domain services, not controllers, Kafka listeners, or gateway filters.
- Define explicit API/event contracts, timeouts, retry limits, error semantics, ownership, and compatibility rules before adding a dependency.
- Propagate `traceId`; use stable aggregate IDs and idempotency keys across retries.
- Store remote identifiers or deliberate local projections, not remote database entities. No cross-service foreign key is enforceable.
- Prefer asynchronous events when the caller does not require an immediate authoritative result. If synchronous REST is necessary, fail within a bounded timeout and do not hide partial distributed state.
- Record decisions that alter boundaries, consistency, or topology in `docs/ADR/`.

## Forbidden patterns

- Shared service databases, cross-database SQL, cross-service ORM relationships, or shared mutable domain tables.
- Distributed ACID/XA transactions or "commit both services" assumptions.
- Critical `commit` then direct Kafka publish without an outbox.
- Exactly-once delivery assumptions, non-idempotent consumers, or unbounded retries.
- Booking or payment business logic in the gateway, frontend, controllers, or message adapters.
- A new microservice for a helper, table, scheduled job, or trivial feature.
- A synchronous service chain whose correctness depends on every downstream service being available.

## Testing requirements

- Add contract tests for changed REST/event contracts and compatibility tests for event-version changes.
- Add integration tests at every changed persistence or messaging boundary; use real PostgreSQL through Testcontainers for database behavior.
- Test duplicate events, retry after partial failure, timeout behavior, out-of-order events where possible, and recovery after process restart.
- Test authorization at the receiving service; gateway authentication alone is insufficient.

## Review checklist

- [ ] One service clearly owns every mutated datum and invariant.
- [ ] No service reads or writes another service's database.
- [ ] All distributed state changes have explicit Saga success and compensation paths.
- [ ] Every critical event is recorded through a local Transactional Outbox.
- [ ] Consumers are idempotent under duplicate delivery.
- [ ] REST dependencies are bounded and do not create unjustified call chains.
- [ ] The gateway contains no domain logic.
- [ ] Tests cover failure, retry, duplicate, and authorization behavior.

