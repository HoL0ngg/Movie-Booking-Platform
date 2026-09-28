# Physical database design

Status: **implemented schema, application workflows pending**. Phase 2 is active. Each service has a V2 Flyway migration, JPA entities, and repositories for the tables in [schema.dbml](database/schema.dbml); [ER diagrams](database/database-er-diagram.md) show the same schema. The V1 migrations remain comment-only baselines. This document's reservation, Saga, and provider workflows describe requirements for later service logic.

## Ownership and ID strategy

| Database | Owning service | Tables |
|---|---|---|
| `cinema_auth` | auth-service | `users`, `roles`, `user_roles`, `refresh_sessions` |
| `cinema_movie` | movie-service | `movies`, `genres`, `movie_genres` |
| `cinema_cinema` | cinema-service | `cinemas`, `auditoriums`, `seats`, `showtimes`, `outbox_events` |
| `cinema_booking` | booking-service | `showtime_snapshots`, `showtime_seats`, `reservations`, `reservation_seats`, `bookings`, `booking_items`, `idempotency_keys`, `outbox_events`, `processed_events` |
| `cinema_payment` | payment-service | `payments`, `payment_attempts`, `provider_events`, `refunds`, `outbox_events`, `processed_events` |
| `cinema_notification` | notification-service | `notifications` |

The gateway owns no database. Each database has its own connection, role, migrations, and transactions. DBML namespaces are a visual grouping of **separate physical databases**, not a proposal to put these tables into one shared PostgreSQL database. All DBML `Ref` declarations stay within one database.

Use UUID primary identifiers for domain rows and events. A service generates an ID before publishing it, so other services can carry the same opaque ID through retries without relying on a central sequence; UUIDv7 is a suitable generation choice if every producer uses a compatible implementation, but the column type remains PostgreSQL `uuid`. Composite identities are used where the invariant requires them: `(showtime_id, seat_id)` for booking inventory, `(consumer_name, event_id)` for inboxes, and composite keys for junction/deduplication records. Time values are `timestamptz` UTC instants. Money is nonnegative integer minor units plus uppercase ISO-4217 currency, never floating point.

## Physical and logical relationships

Within a database, the DBML `Ref` lines represent implemented physical FKs. The important local chains are `users → refresh_sessions/user_roles ← roles`, `movies → movie_genres ← genres`, `cinemas → auditoriums → seats/showtimes`, `showtime_snapshots → showtime_seats/reservations → bookings → booking_items`, and `payments → payment_attempts/provider_events/refunds`. `reservation_seats` records the immutable selected set and locked-in unit prices; `booking_items` records confirmed ticket entitlements. Neither requires another service's database.

The following are **logical IDs only**, with no physical FK or cross-database join:

| Referencing database/column | Owning database/identity | Propagation |
|---|---|---|
| cinema `showtimes.movie_id` | movie `movies.id` | Versioned API/validated schedule command |
| booking `showtime_snapshots.showtime_id/cinema_id/auditorium_id/movie_id` and `showtime_seats.seat_id` | cinema and movie identities | `ShowtimePublished` snapshot |
| booking `reservations.user_id`, `bookings.user_id`, `idempotency_keys.actor_id` | auth `users.id` | Authenticated request identity |
| booking `bookings.payment_id` | payment `payments.id` | Stable ID reserved by booking and carried in `PaymentRequested` |
| payment `payments.booking_id/reservation_id/user_id` and `refunds.refund_request_id` | booking/auth identities | Saga events |
| notification `notifications.source_event_id/user_id` | Kafka event/auth identity | Consumed event and authorized recipient lookup |

The references do not grant read/write access to remote tables. Local composite FKs pair reservation, booking, and seat IDs with their `showtime_id` so records cannot silently attach to another showtime; the booking-to-reservation FK also enforces the same `user_id`. Ordinary FKs use `ON DELETE RESTRICT/NO ACTION`; historical booking/payment rows are not cascaded away. Role assignment and catalog genre membership are explicit junction tables. There is no blanket soft-delete scheme.

## Cinema schedule and inventory ownership

`cinema.seats` is the physical auditorium layout. It has no live availability field. `cinema.showtimes` owns the schedule and starts with one uniform `price_minor` per seat; `ShowtimePublished` copies each active seat's ID, label, type, and that price into an immutable bookable snapshot. If seat-specific pricing becomes a requirement, add an explicit cinema-owned pricing model before changing this contract.

`booking.showtime_snapshots` is a local, versioned projection of published showtimes. `booking.showtime_seats` is the authoritative live inventory. Booking consumes `ShowtimePublished` idempotently, creates one row per seat, and never overwrites held/sold ownership with a replay or later schedule message. Showtime cancellation closes new sales; handling already paid tickets requires a separate explicit policy. There is **no `showtime_seats` table in the cinema database**, because booking could not atomically lock it without crossing a service boundary.

The cinema publication/cancellation transaction writes `showtimes` and `outbox_events` together. Booking's event transaction writes `processed_events`, its local snapshot/inventory effect, and any derived outbox row together. No remote database participates in either transaction.

## Constraints and important indexes

The DBML lists PKs, local FKs, unique keys, ordinary indexes, types, and nullable columns. The V2 Flyway migrations implement these PostgreSQL-specific rules; DBML is documentation, not executable DDL:

| Table(s) | Required checks / specialized indexes |
|---|---|
| auth `users`, `refresh_sessions` | `email_normalized = lower(btrim(email))`; unique normalized email and token hash. Store only a hash of the refresh token; `expires_at > created_at`. |
| movie `movies`; cinema `seats`/`showtimes` | Positive duration/seat number; `starts_at < ends_at`, `sales_close_at <= starts_at`, `price_minor >= 0`, `snapshot_version > 0`. Published showtimes in one auditorium cannot overlap: a PostgreSQL GiST exclusion constraint on `(auditorium_id, tstzrange(starts_at, ends_at, '[)'))` where status is `PUBLISHED` uses `btree_gist`. |
| booking `showtime_seats` | PK `(showtime_id, seat_id)`; `version >= 0`, `price_minor >= 0`. A status-shape CHECK requires: AVAILABLE has no owner/deadline; HELD has reservation/deadline but no booking; PAYMENT_PENDING has reservation, booking, deadline; SOLD has reservation/booking but no deadline. The expiry index is partial on `(hold_expires_at, showtime_id, seat_id)` for HELD/PAYMENT_PENDING. |
| booking `reservation_seats`, `bookings`, `booking_items` | Positive/nonnegative unit prices and amount; one booking per reservation; one payment ID per booking. Insert `booking_items` only in the confirming transaction. Unique `(showtime_id, seat_id)` on booking items is the durable second guard against selling the same seat twice. This initial design never resells a confirmed seat after refund. |
| booking `idempotency_keys`; booking/payment `processed_events` | Atomic PK claims, canonical request hashes, and stable stored outcomes for committed commands. The CHECK pairs response status/body nullability; application transactions must not commit an unfinished claim. Inbox PK `(consumer_name, event_id)` commits with local effects. |
| payment `payments`, `payment_attempts`, `provider_events`, `refunds` | Nonnegative amounts and currency format checks; unique scoped payment request key, `booking_id`, provider merchant reference, non-null provider transaction ID, `(provider, event_identity)`, refund request ID, scoped refund request key, and provider refund ID. The initial design allows one full refund per payment. PostgreSQL unique indexes permit multiple NULL provider IDs while preventing duplicate non-NULL IDs; exact cross-service amount/currency matching remains application work. |
| cinema/booking/payment `outbox_events` | `event_id` PK, positive event version, nonnegative publish attempts; partial relay index on `(next_attempt_at, created_at)` where `published_at IS NULL`. Publication marks only after broker acknowledgment. |
| notification `notifications` | Unique `(source_event_id, channel)` deduplicates event delivery creation; nonnegative attempt count and due-work index on pending/failed notifications. Use a stable notification ID as the provider idempotency reference when supported. |

All status columns have PostgreSQL CHECK constraints rather than unconstrained free text. Allowed values: user ACTIVE/DISABLED; movie DRAFT/PUBLISHED/ARCHIVED; cinema showtime DRAFT/PUBLISHED/CANCELLED; booking snapshot PUBLISHED/CANCELLED; seat AVAILABLE/HELD/PAYMENT_PENDING/SOLD; reservation HELD/PAYMENT_PENDING/CONFIRMED/RELEASED/EXPIRED; booking PAYMENT_PENDING/CONFIRMED/CANCELLED/REFUND_PENDING/REFUNDED; payment REQUESTED/PROVIDER_PENDING/SUCCEEDED/FAILED/REFUND_PENDING/REFUNDED/REFUND_FAILED; attempt REQUESTED/PROVIDER_PENDING/SUCCEEDED/FAILED/UNKNOWN; refund REQUESTED/PROVIDER_PENDING/COMPLETED/FAILED; notification PENDING/SENT/FAILED. Currency columns have a three-uppercase-letter check and still need ISO-4217 validation at the service boundary.

## Seat reservation concurrency

PostgreSQL in booking-service is the sole serialization point. The seat lifecycle uses the existing platform names `AVAILABLE → HELD → PAYMENT_PENDING → SOLD`; `SOLD` is the requested “BOOKED” outcome. Valid release/expiry/failure transitions return a still-owned HELD or PAYMENT_PENDING seat to AVAILABLE. The original hold deadline continues through PAYMENT_PENDING unless a separately approved bounded extension policy is designed.

For a reservation command, validate the authenticated actor, seat IDs, and idempotency key; deduplicate and sort IDs using one PostgreSQL-compatible UUID byte order. In a short `READ COMMITTED` transaction, atomically claim the scoped idempotency key, then lock each requested `showtime_seats` row in canonical order with `SELECT ... FOR UPDATE`. After all locks are acquired, require exactly the requested rows, a published/onsale showtime, and AVAILABLE status for every seat. Create the reservation and immutable `reservation_seats`, set all seats to HELD with one database-time deadline, store the repeatable outcome, and insert `ReservationCreated`/`SeatsHeld` outbox records before commit. A business conflict rolls back the command, including its key claim, without changing any seat. No provider, REST, Kafka, or Redis call occurs while locks are held.

```sql
-- Execute once per seat ID, in the canonical sorted order.
SELECT status, reservation_id, booking_id, hold_expires_at
FROM showtime_seats
WHERE showtime_id = :showtime_id AND seat_id = :seat_id
FOR UPDATE;
```

If two users request the same seat, the second waits on its row lock. After the first commits HELD, the waiter re-reads that row under lock, sees it is no longer AVAILABLE, and gets `SEAT_UNAVAILABLE`; it cannot hold another part of its requested set. The composite PK protects one inventory row per showtime seat, while unique `booking_items(showtime_id, seat_id)` protects one lifetime successful sale. A same-key retry of a committed command returns its prior outcome; a committed key reused with a different request hash conflicts. A rolled-back business conflict may be retried and re-evaluated.

Checkout, user release, expiry workers, and payment-result consumers lock the existing reservation/booking aggregate **before** the same sorted seat rows. Checkout verifies the deadline with database time, freezes amount/currency, creates one pending booking and stable payment ID, moves seats to PAYMENT_PENDING, and writes `PaymentRequested` to the outbox. Expiry workers claim bounded expired aggregates with `FOR UPDATE SKIP LOCKED`, recheck ownership/deadline, release once, and emit events atomically. A late `PaymentSucceeded` cannot reclaim released seats; it creates one `RefundRequested`. Deadlock/serialization failures retry the whole local transaction a bounded number of times; business conflicts do not retry.

## Booking and payment lifecycle

`reservations` begin HELD and end CONFIRMED, RELEASED, or EXPIRED; checkout is PAYMENT_PENDING. `bookings` are created at checkout as PAYMENT_PENDING. A matching verified success while the hold is valid confirms seats, creates booking items, and changes the booking to CONFIRMED in one transaction. Definitive failure or expiry cancels the pending booking. A verified late success for a cancelled/expired booking moves its Saga view to REFUND_PENDING, then REFUNDED only after a verified refund result. No browser redirect can confirm either state. A future post-confirmation cancellation/refund policy is not assumed here.

`payment-service` owns provider calls and local payment/refund state. `PaymentRequested` carries a stable booking-reserved `payment_id`, amount/currency, and request key. A unique payment ID, unique `booking_id`, unique `(user_id, request_key)`, request hash, and `processed_events` key prevent duplicate requests from creating a second payment. `payment_attempts` retains a stable provider/merchant reference across safe retries; ambiguous provider timeouts enter UNKNOWN/reconciliation rather than a new charge identity or false FAILED result.

After raw-byte signature and merchant/amount/currency checks, a webhook transaction inserts unique `(provider, event_identity)`, locks the payment, validates the legal transition, and writes the payment state plus outbox event before acknowledging the provider. `event_identity` is the provider event ID or a provider-documented stable digest when no ID exists. Duplicate or contradictory callbacks cannot emit another success; contradictions remain visible for reconciliation. Refund requests are similarly deduplicated by `refund_request_id`, one-refund-per-payment uniqueness, and stable provider reference. Only provider-issued references are retained; raw card numbers, CVV, signing secrets, and raw sensitive webhook payloads are excluded. Stored payment references and recipient data require PCI/privacy review before implementation.

## Rollout and unresolved choices

The V2 schemas use forward-only Flyway migrations; no cross-service DDL transaction exists. PostgreSQL/Testcontainers validated migration application, JPA mappings, seat locks, the unique sale guard, schedule overlap, and provider-event uniqueness. The 100-contender booking race, overlapping seat sets, expiry/payment races, outbox replay, and duplicate webhook processing require application services and remain future gates before activating booking or payment flows.

1. Specify the auth token format and refresh rotation/replay policy before using `refresh_sessions`.
2. Confirm the cinema pricing contract. This design uses one showtime price for every seat; differentiated seat pricing requires cinema-owned pricing data and a versioned publication contract.
3. Define compensation for cancellation of a published showtime with confirmed bookings, and whether any sold seat may ever be resold. The implemented unique booking-item constraint deliberately forbids resale.
4. Choose a real provider before finalizing provider event identity, safe retries, refund capabilities, and webhook response policy.
5. Fix the hold duration and payment cutoff policy, including how to handle an in-flight provider payment when a hold expires.
6. Reconcile idempotency semantics for a rolled-back seat conflict: this design follows ADR-003's full rollback, so repeating that key may re-evaluate availability. If the API must replay an identical `409`, decide how to persist that outcome without weakening the all-or-nothing seat rule.
