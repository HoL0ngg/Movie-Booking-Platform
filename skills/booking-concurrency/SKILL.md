---
name: booking-concurrency
description: Implement or review seat holds, reservation state transitions, expiration, booking confirmation, and concurrent access in booking-service.
---

# Booking Concurrency

## When to use

Use this skill for any change affecting `showtime_seats`, reservations, bookings, hold expiration, checkout transitions, payment-result handling, or code that can race on the same showtime/seat. Read `AGENTS.md`, `docs/booking-flow.md`, and `docs/concurrency.md` before implementation.

## Architectural rules

- PostgreSQL owned by `booking-service` is authoritative for seat state. Correctness must survive Redis loss and multiple application instances.
- For each `(showtime_id, seat_id)`, at most one successful booking may exist.
- The only normal seat states are `AVAILABLE`, `HELD`, `PAYMENT_PENDING`, and `SOLD`; every transition is an explicit domain operation.
- Reservation is all-or-nothing for the requested seat set.
- Creation, seat updates, reservation/booking updates, idempotency outcome, and outbox events that belong to one command commit atomically.
- Late payment success cannot revive an expired/released hold. It triggers idempotent compensation/refund handling.

## Implementation rules

For a reservation command:

1. Validate and deduplicate seat IDs, then sort them by a canonical stable representation.
2. Begin a PostgreSQL transaction and atomically claim or resolve the request idempotency key.
3. Lock every requested `showtime_seats` row with `SELECT ... FOR UPDATE`, acquiring locks in that sorted order. A single ordered query is acceptable only when its plan preserves the documented lock order; locking one sorted row at a time is the safest baseline.
4. Verify the exact requested row count and that every row is `AVAILABLE`; otherwise roll back the whole command and return a conflict.
5. Write all rows as `HELD`, create the reservation, persist the repeatable response/idempotency outcome, and insert `ReservationCreated`/`SeatsHeld` outbox records.
6. Commit before publishing or making remote calls.

- Use the same aggregate-first and sorted-seat lock order in expiration, cancellation, payment success, and payment failure paths.
- Expiration workers claim bounded batches with row locks and `SKIP LOCKED`, re-check state and deadline under lock, release seats atomically, and emit an outbox event. Multiple workers must be safe.
- Use database time consistently for expiry comparisons. Keep hold duration configurable.
- Treat a duplicate idempotency key with the same request hash as the same command/result; reject a key reused with a different payload.
- Classify PostgreSQL deadlock/serialization failures and retry the whole transaction a small bounded number of times with jitter; never retry business conflicts.
- A `version` column may detect stale writes, but does not replace row locks for multi-seat reservation.

## Forbidden patterns

- Redis locks, JVM `synchronized`, in-memory maps, or frontend state as the seat-ownership authority.
- Reading availability and updating it in separate transactions.
- Locking caller-provided seat order or partially committing a requested seat set.
- Calling payment providers, REST services, or Kafka while holding seat locks.
- Blind state updates without expected-current-state predicates or locked validation.
- Confirming seats from a frontend redirect or accepting a payment result after release without compensation.
- Infinite deadlock retries or turning a database/business conflict into HTTP 500.

## Testing requirements

- With Testcontainers PostgreSQL, launch 100 synchronized concurrent attempts for the same `(showtimeId, seatId)` and assert exactly one success, 99 clean conflicts, and one durable owner.
- Test overlapping multi-seat sets for atomicity and absence of partial holds.
- Test expired holds, concurrent expiration workers, duplicate reservation keys, mismatched idempotency payloads, payment after expiration, duplicate payment events/callback effects, and deadlock retry exhaustion.
- Assert persisted seat, reservation, booking, idempotency, and outbox state after each race; HTTP counts alone are insufficient.
- Do not use H2 to validate locking behavior.

## Review checklist

- [ ] Seat IDs are normalized, deduplicated, and locked in deterministic order.
- [ ] All requested rows are locked and validated before any seat mutation.
- [ ] The transaction is all-or-nothing and includes outbox/idempotency state.
- [ ] Expiration is transactional, idempotent, and multi-worker safe.
- [ ] Late/duplicate payment outcomes cannot double-confirm or resurrect seats.
- [ ] Database failures have bounded retry rules; business conflicts do not retry.
- [ ] No remote call occurs inside the lock-holding transaction.
- [ ] Real PostgreSQL concurrency tests prove the one-winner invariant.

