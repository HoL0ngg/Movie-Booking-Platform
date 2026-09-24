# Service Boundaries and Data Ownership

Status: Phase 0 baseline

## Ownership matrix

| Deployable | Responsibilities | Exclusively owned persistent data | Synchronous surface | Event role |
|---|---|---|---|---|
| `api-gateway` | Routing, edge authentication enforcement, rate limits, correlation IDs, request limits | No domain data | Public route facade | None initially |
| `auth-service` | Registration, authentication, token/session lifecycle, roles, account security | Users, credentials, roles, refresh tokens/sessions, audit/security records | Auth/account commands and identity lookup | May later publish account lifecycle events |
| `movie-service` | Movie catalog, genres, media metadata, content lifecycle | Movies, genres, cast/content metadata | Movie catalog queries/admin commands | Publishes catalog changes when needed |
| `cinema-service` | Cinemas, auditoriums, physical seats, showtime scheduling and cancellation | Cinemas, auditoriums, seat definitions, showtimes, schedule rules | Cinema/showtime queries/admin commands | Publishes `ShowtimePublished` and `ShowtimeCancelled` |
| `booking-service` | Bookable seat inventory, holds, reservations, checkout state, bookings, ticket entitlement, hold expiry, booking Saga coordination | `showtime_seats`, local showtime/price snapshots, reservations, reservation seats, bookings/tickets, Saga state, request idempotency, outbox/inbox | Seat availability, reservation, checkout, booking/ticket status | Publishes reservation/seat/booking/refund-request events; consumes showtime and payment results |
| `payment-service` | Payment attempts, provider sessions/adapters, verified webhooks, reconciliation, refunds | Payments, attempts, provider transactions/events, refunds, idempotency, outbox/inbox | Payment status/checkout instruction, provider webhook ingress | Consumes payment/refund requests; publishes payment/refund results |
| `notification-service` | Notification preferences, templates, rendering, send attempts and delivery results | Preferences, templates, notification jobs, provider message IDs/delivery status, inbox | Internal/admin status only initially | Consumes confirmed/cancelled/payment events; delivery is non-transactional to booking |

## Boundary rules

1. The owning service is the only writer and the only SQL reader of its database.
2. A service identifier carried elsewhere is an opaque reference, not a cross-service foreign key.
3. Shared business information is obtained through a versioned API or copied as an event-driven local projection with provenance and freshness semantics.
4. A shared PostgreSQL server is permitted for local development only with separate databases, users, and grants. It is not a shared data model.
5. Libraries may share technical primitives such as event-envelope serialization, but must not share JPA entities, repositories, mutable domain models, or migrations.
6. The gateway does not aggregate domain transactions. A client may call multiple routed APIs, but correctness resides in the owning services.

## Important collaborations

| Collaboration | Contract | Consistency and failure behavior |
|---|---|---|
| Cinema -> Booking | `ShowtimePublished`/`ShowtimeCancelled` events | Booking creates or closes its local bookable-seat projection idempotently. Existing booking policy for cancellation must be explicit before implementation. |
| Booking -> Payment | `PaymentRequested` event | Booking persists `PAYMENT_PENDING` plus outbox; payment creates one attempt per idempotent request. No shared transaction. |
| Payment -> Booking | `PaymentSucceeded`/`PaymentFailed` events | Booking applies result once under local locks. Late success triggers compensation rather than seat resurrection. |
| Booking -> Payment | `RefundRequested` event | Payment performs/refers to an idempotent refund and publishes the result. |
| Booking/Payment -> Notification | Confirmed/cancelled/payment domain events | Notification deduplicates and retries delivery. Delivery failure cannot alter authoritative booking/payment state. |
| Web -> services | REST through gateway | Backend response is authoritative; cached UI state and payment redirects are not. |

## Avoiding accidental coupling

- `cinema-service` owns the definition that seat `A5` exists; `booking-service` owns whether `A5` for a specific showtime is available, held, payment-pending, or sold.
- `booking-service` owns the expected amount and immutable price snapshot for a checkout; `payment-service` owns whether funds were successfully captured/refunded by a provider.
- `auth-service` owns identity records. Other databases store the stable user ID plus any strictly necessary historical snapshot, not credential/profile tables.
- `notification-service` may render copied display data carried in an event, but it does not query booking/payment databases.

## Why there are no more services

Ticket entitlement belongs with the booking lifecycle; seat inventory belongs with booking correctness; refund processing belongs with payments; hold-expiration workers operate within `booking-service`; outbox relays are infrastructure components embedded per owner. None currently provides a strong independent bounded-context reason for another microservice.

