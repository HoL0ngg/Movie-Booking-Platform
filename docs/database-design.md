# Database Design Strategy

Status: Phase 0 logical design; not an executable migration

## Ownership and deployment

Each stateful service receives a separate PostgreSQL database and restricted database role. A common PostgreSQL instance may host these databases in local development, but grants must prevent cross-service access.

| Service | Logical database | Principal owned data |
|---|---|---|
| `auth-service` | `cinema_auth` | users, credentials, roles, sessions/tokens, security audit |
| `movie-service` | `cinema_movie` | movies and catalog metadata |
| `cinema-service` | `cinema_cinema` | cinemas, auditoriums, seats, showtimes |
| `booking-service` | `cinema_booking` | showtime seats, reservations, bookings/tickets, price snapshots, Saga/idempotency/outbox/inbox |
| `payment-service` | `cinema_payment` | payments, attempts, provider events/transactions, refunds, idempotency/outbox/inbox |
| `notification-service` | `cinema_notification` | preferences, notification jobs, attempts, delivery state, inbox |

The gateway has no domain database. Foreign keys exist only within one database. References such as `user_id`, `movie_id`, and cinema-owned `showtime_id` are opaque external IDs unless represented by an intentionally maintained local projection.

## Migration rules

- Flyway owns every schema change. Persistent environments use Hibernate `ddl-auto=validate` (or no schema generation), never `update`.
- Migrations define all primary keys, unique constraints, foreign keys, checks, defaults, and indexes.
- Changes must be compatible with rolling deployment: expand, backfill/observe, switch readers/writers, then contract in a later release.
- Destructive changes require a retention/export decision and a forward-fix or restore plan.
- Store instants as UTC `timestamptz`; store money as integer minor units plus ISO-4217 currency; never use floating point.

## Booking database core

`showtime_seats` is the authoritative seat-state table. The conceptual PostgreSQL shape is:

```sql
CREATE TABLE showtime_seats (
    showtime_id     uuid        NOT NULL,
    seat_id         uuid        NOT NULL,
    status          text        NOT NULL,
    held_by         uuid,
    reservation_id  uuid,
    booking_id      uuid,
    hold_expires_at timestamptz,
    version         bigint      NOT NULL DEFAULT 0,
    updated_at      timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (showtime_id, seat_id),
    CHECK (status IN ('AVAILABLE', 'HELD', 'PAYMENT_PENDING', 'SOLD')),
    CHECK (version >= 0),
    CHECK (
        (status = 'AVAILABLE'
            AND held_by IS NULL AND reservation_id IS NULL
            AND booking_id IS NULL AND hold_expires_at IS NULL)
        OR
        (status = 'HELD'
            AND held_by IS NOT NULL AND reservation_id IS NOT NULL
            AND booking_id IS NULL AND hold_expires_at IS NOT NULL)
        OR
        (status = 'PAYMENT_PENDING'
            AND held_by IS NOT NULL AND reservation_id IS NOT NULL
            AND booking_id IS NOT NULL AND hold_expires_at IS NOT NULL)
        OR
        (status = 'SOLD'
            AND held_by IS NULL AND reservation_id IS NOT NULL
            AND booking_id IS NOT NULL AND hold_expires_at IS NULL)
    )
);

CREATE INDEX ix_showtime_seats_expiring
    ON showtime_seats (hold_expires_at, showtime_id, seat_id)
    WHERE status IN ('HELD', 'PAYMENT_PENDING');
```

The final migration may use PostgreSQL enum types or constrained text after implementation review. `reservation_id` and `booking_id` become local foreign keys once table creation order is defined. The composite primary key prevents duplicate inventory rows, while locked state validation prevents two owners from progressing the same row. `version` supports auditing/stale-write detection but does not replace pessimistic locks.

Related booking tables are expected to include:

- `reservations`: user/showtime, state, hold deadline, request identity, created/updated times.
- `reservation_seats`: reservation and the composite showtime/seat IDs; uniqueness prevents duplicate membership.
- `bookings`: unique reservation, immutable amount/currency/price snapshot, state, confirmation time.
- `tickets` or ticket entitlement: unique booking/seat identity; exact representation remains open.
- `idempotency_keys`: operation scope, actor, key, request hash, status/result reference/serialized safe response, expiry.
- `booking_sagas`: durable workflow state and applied payment/refund references if not fully represented by booking state.
- `outbox_events` and `processed_events` as described below.

No booking table has a foreign key into cinema or auth databases.

## Reservation transaction and lock order

After validation, seat IDs are normalized, deduplicated, and sorted by a canonical representation. The application begins one transaction, resolves the idempotency key, and acquires every row lock in that order:

```sql
SELECT showtime_id, seat_id, status, hold_expires_at, reservation_id
FROM showtime_seats
WHERE showtime_id = :showtime_id
  AND seat_id = ANY(:seat_ids)
ORDER BY seat_id
FOR UPDATE;
```

The implementation must verify that the query plan preserves the intended lock order; locking individual rows in sorted order is the conservative fallback. It rejects the whole command if the row count differs or any row is not `AVAILABLE`. All seat updates, the reservation, idempotent result, and outbox records commit together. No HTTP/provider/Kafka call occurs while locks are held.

Operations on an existing reservation first lock that reservation/Saga row and then its seat rows in the same canonical seat order. Deadlock or serialization failures may retry the entire transaction a small bounded number of times with jitter; conflicts and invalid states are not retried.

## Expiration

One or more workers claim bounded expired-reservation batches with `FOR UPDATE SKIP LOCKED`. For each claimed reservation they lock associated seats in canonical order, re-check the state and database-time deadline, move matching `HELD` or timed-out `PAYMENT_PENDING` rows to `AVAILABLE`, update the aggregate, and insert the expiration/release outbox records in one transaction. Repeated execution is a no-op after the first valid transition.

## Payment database core

Payment persistence must represent:

- `payments`: stable payment ID, reservation/booking references, amount minor/currency, provider, state, idempotency key/request hash, timestamps.
- `payment_attempts`: provider request identity, provider transaction ID, attempt state, safe diagnostic codes, timestamps.
- `provider_events`: unique `(provider, provider_event_id)`, signature-verification result metadata, received/processed timestamps, safe payload digest.
- `refunds`: payment, requested amount, provider refund ID, idempotency key, state, failure/reconciliation fields.
- `outbox_events` and `processed_events`.

Uniqueness must prevent the same provider event or transaction from creating multiple successful local effects. A provider transaction may be absent before provider creation, so uniqueness is partial/non-null as appropriate. Raw secrets and sensitive provider payloads are not stored by default.

## Outbox and inbox shape

Every event-producing database uses an `outbox_events` table with an immutable event ID, type/version, aggregate ID, occurred time, trace ID, serialized payload, publication state/attempt metadata, and created time. Dispatchers claim bounded rows safely and mark published only after Kafka acknowledgement; a crash can cause a duplicate publish.

Every state-changing consumer uses a `processed_events`/inbox table with a unique `(consumer_name, event_id)`. The inbox insert, local business effect, and any derived outbox rows commit together. Kafka offsets are acknowledged afterward.

## Redis

Redis may hold caches, rate-limit counters, ephemeral TTL hints, or transient notifications. It stores no irreplaceable reservation, booking, payment, or idempotency truth. Deleting all Redis data must not allow a double booking, lose a confirmed payment, or prevent PostgreSQL-based expiry recovery.

## Required database tests in later phases

- Flyway clean-start and previous-version upgrade tests
- Constraint and repository tests against the supported PostgreSQL image
- 100 concurrent transactions contending for one showtime seat: exactly one winner
- Overlapping multi-seat sets, rollback, duplicate idempotency keys, expiration workers, late payment, duplicate events, and bounded deadlock retry
- Outbox/inbox crash windows and duplicate publication/delivery

