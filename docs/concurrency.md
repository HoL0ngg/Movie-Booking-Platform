# Booking Concurrency Design

Status: Phase 0 implementation contract

## Correctness target

For every `(showtime_id, seat_id)`, at most one reservation transaction may change the row from `AVAILABLE` to `HELD`, and at most one valid payment result may change it to `SOLD`. This remains true with many application instances, Kafka duplicates, worker overlap, retries, Redis failure, and process crashes.

## PostgreSQL locking strategy

The baseline isolation level is PostgreSQL `READ COMMITTED` plus explicit row locks, expected-state validation, unique/check constraints, and atomic transactions. A higher isolation level does not remove the need for explicit lock order and retry handling.

Before opening the transaction, validate syntax, normalize/deduplicate IDs, and sort seat IDs by one canonical comparator. For UUID seat IDs, the implementation must define and test one PostgreSQL-compatible unsigned-byte ordering; all booking code paths use it.

Inside the transaction, claim/resolve the idempotency key, then acquire locks in that order. The safest baseline executes the following once per sorted seat ID:

```sql
SELECT showtime_id, seat_id, status, reservation_id, hold_expires_at
FROM showtime_seats
WHERE showtime_id = :showtime_id
  AND seat_id = :seat_id
FOR UPDATE;
```

A batched `WHERE seat_id = ANY(...) ORDER BY seat_id FOR UPDATE` implementation is acceptable only if PostgreSQL execution-plan tests demonstrate that locks are acquired in the documented canonical order. Caller order is never used.

After all locks are held, require one row per requested seat and validate all states before changing any row. Updates, reservation/booking state, idempotency outcome, and outbox events commit once. A conflict rolls back the complete seat set.

## Global lock protocol

To minimize deadlocks, transactions use the following order for the rows they need:

1. scoped idempotency or consumer-inbox claim;
2. existing reservation/booking Saga aggregate row, when one exists;
3. `showtime_seats` rows ordered by the canonical seat comparator;
4. dependent records and outbox inserts.

Reservation creation has no existing aggregate, so it locks its idempotency row and then sorted seats before inserting the reservation. No operation may lock a seat and then attempt to lock an existing reservation for that seat.

Transactions are short and contain no REST, provider, Redis, or Kafka call. A numeric version is incremented on updates for diagnostics/stale-write protection but is not the exclusive concurrency mechanism.

## Why this prevents double booking

PostgreSQL grants the first transaction an exclusive row lock. Contenders wait. The winner verifies `AVAILABLE`, writes `HELD`, and commits. Each waiter then sees the committed `HELD` state while holding the same row lock and returns a business conflict without mutation. Database constraints prevent malformed/duplicate inventory, and the transaction prevents partial multi-seat results.

Redis cannot strengthen this invariant and is not consulted for authority. Cache loss, stale cache, or two JVMs do not bypass the PostgreSQL serialization point.

## Idempotency under concurrency

Reservation and checkout keys are scoped to actor plus operation. A unique database constraint atomically admits one request for a key. The stored record includes a canonical request hash and stable result/reference.

- Same key and same hash: wait for or read the original committed outcome; never reserve again.
- Same key and different hash: return `409 IDEMPOTENCY_KEY_REUSED`.
- Original transaction rollback: no successful key/outcome survives; a bounded retry may execute the whole command.

A prior `SELECT` without a unique constraint is insufficient.

## Expiry/payment races

Expiration, cancellation, `PaymentSucceeded`, and `PaymentFailed` first lock the same reservation/Saga aggregate and then the same sorted seat set. The first valid transition wins; the waiter re-reads the new state and performs an idempotent no-op or compensation.

If expiration commits first, a later success cannot move released seats to `SOLD` and produces one `RefundRequested`. If valid success commits first, expiration observes `CONFIRMED`/`SOLD` and does nothing. A duplicate failure cannot cancel confirmed seats.

Expiry workers claim bounded aggregate batches using `FOR UPDATE SKIP LOCKED`. They compare deadlines with database time, verify every seat is still owned by the target reservation, and update reservation, seats, and outbox together.

## Deadlocks and transient database failures

Deterministic lock order prevents the expected seat-set deadlock class but cannot prove no deadlock will ever occur. PostgreSQL deadlock (`40P01`) or serialization (`40001`, if applicable) errors cause a small configurable number of whole-transaction retries with jitter. Locks and transaction-scoped state are reacquired from scratch. Exhaustion becomes a retryable service error with `traceId`, metrics, and structured logs; it is never an infinite loop.

Business conflicts (`SEAT_UNAVAILABLE`, expired state, mismatched idempotency hash) are not transient database retries.

## Required Phase 3 Testcontainers tests

Use the production-compatible PostgreSQL image, independent connections/transactions, and a start barrier. Assert API/application outcomes and final database/outbox state.

| Scenario | Setup | Required result |
|---|---|---|
| 100 users, same seat | 100 synchronized unique users/keys reserve one `(showtimeId, seatId)` | Exactly 1 success, 99 clean `SEAT_UNAVAILABLE` conflicts, one reservation owner, one held seat, one event set |
| Overlapping seat sets | Concurrent requests such as `{A1,A2}` and `{A2,A3}` | At most one set succeeds; loser holds none; no deadlock/hang |
| Reversed caller order | Same seats submitted as `{A1,A2}` and `{A2,A1}` | Canonical lock order; one coherent outcome; no partial state |
| Expired hold | Worker and new reservation race around deadline | Old owner releases transactionally; new owner succeeds only after release; events emitted once |
| Multiple expiry workers | Workers claim the same expired population | Each reservation is released once; duplicate effects absent; backlog drains |
| Duplicate reservation | Same key/hash concurrently and repeatedly | One reservation/event set and the same stable response |
| Key reused with new body | Same key, different showtime/seat set | `IDEMPOTENCY_KEY_REUSED`; original result unchanged |
| Payment after expiration | Expiry wins before success event | No sold seat/confirmed booking; exactly one refund request |
| Duplicate payment result | Same and distinct duplicate event deliveries | One confirmation or one compensation, one derived event set |
| Success versus failure | Conflicting terminal events race | One legal terminal outcome; stale event cannot reverse it; contradiction observable |
| Forced deadlock | Controlled opposing non-production lock path or database fixture | Whole transaction retries within bound, or fails cleanly at exhaustion; no partial writes |
| Redis unavailable/flushed | Disable or clear Redis throughout reservation | Identical booking correctness and winner count |

Tests must have explicit timeouts and deterministic coordination; sleeps alone are not adequate synchronization. H2 is prohibited for this evidence.

