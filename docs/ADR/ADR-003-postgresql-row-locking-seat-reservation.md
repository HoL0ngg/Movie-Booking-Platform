# ADR-003: PostgreSQL Row-Level Locking for Seat Reservation

- Status: Accepted
- Date: 2026-09-24

## Context

Concurrent customers can read the same seat as available and attempt overlapping multi-seat reservations. An optimistic read followed by updates can double-allocate or partially allocate seats. Multi-row locking in different orders can deadlock.

## Decision

Reservation transitions use short local PostgreSQL transactions and `SELECT ... FOR UPDATE` on every requested `showtime_seats` row. Seat IDs are normalized, deduplicated, and sorted by one tested canonical comparator before locking. All code paths acquire seat locks in that order.

After locking the complete set, the transaction verifies exact row count and expected state. If any seat is missing/unavailable, the entire command rolls back. Otherwise it updates every seat, creates/updates the aggregate and idempotency outcome, and inserts outbox records before one commit.

The conservative baseline locks individual rows in sorted order. A batched ordered query is permitted only with PostgreSQL execution-plan tests proving the required lock order. Operations on existing reservations lock the aggregate before its sorted seats.

## Consequences

- Contenders serialize on authoritative rows; one wins and waiters observe the new state.
- Transactions may wait and deadlocks remain theoretically possible outside the expected pattern.
- Remote calls are excluded from transactions to keep lock duration short.
- Multi-seat requests are atomic rather than partially successful.

## Guardrails

- PostgreSQL deadlock/serialization failures retry the entire transaction only a small bounded number of times with jitter.
- Business conflicts do not retry as infrastructure failures.
- A version column complements but never replaces required row locks.
- A Testcontainers test with 100 synchronized contenders must prove exactly one success and final database ownership.

