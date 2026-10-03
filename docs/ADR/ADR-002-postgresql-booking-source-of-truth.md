# ADR-002: PostgreSQL as Booking Source of Truth

- Status: Accepted
- Date: 2026-09-24

## Context

Seat availability is highly contended and financially significant. The platform must give one authoritative answer after restarts, cache loss, multiple service instances, delayed events, and network partitions. A cache or frontend view cannot provide the necessary durable transactional invariants.

## Decision

The `booking-service` PostgreSQL database is the sole authority for showtime-seat ownership, reservation state, booking state, and ticket entitlement. The `showtime_seats` composite identity is `(showtime_id, seat_id)`. Transactions, row locks, state validation, unique/check/foreign-key constraints within the booking database, and idempotency records jointly enforce booking invariants.

Kafka events and Redis data are derived from or supportive of this state. They cannot override it. A stale availability response is resolved by revalidation in the reservation transaction.

## Consequences

- Booking correctness survives application scaling, Redis loss, and event redelivery.
- PostgreSQL contention and transaction design become critical capacity concerns and require production-like testing/monitoring.
- Queries from other services cannot inspect seat truth directly; they use booking APIs/events.
- Availability reads may be cached, but every write must revalidate authoritative rows.

## Guardrails

- No booking state is stored only in Redis, memory, Kafka, or the browser.
- Schema changes are managed manually with service-owned reference DDL (amended by [ADR-013](ADR-013-manual-database-schema-management.md)); production configuration validates rather than updates the schema automatically.
- PostgreSQL-specific behavior is tested with Testcontainers, never inferred from H2.
- Performance changes may reduce contention but must preserve the same database-enforced invariant.
