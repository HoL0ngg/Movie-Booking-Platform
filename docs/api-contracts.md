# REST API Contract Baseline

Status: Phase 1 contract baseline with implemented public movie catalog reads; other business operations remain unimplemented

Static OpenAPI 3.1 contracts are published from each Spring Boot module at `/openapi/openapi.yaml` and listed by the gateway contract. They are contract artifacts, not evidence that an operation is implemented. This document remains the semantic authority until implementation-time DTO schemas and contract tests are added.

## Conventions

`ai-service` publishes an empty static contract at `http://localhost:8087/openapi/openapi.yaml`. Gateway reserves `/api/v1/ai/**`. No chat/recommendation operation is implemented; business requests are denied at service and gateway. ADR-011 requires service authorization, request limits, bounded inference timeouts, and errors before adding operations. Provider calls are never part of booking/payment transactions or Sagas.

- Public APIs are rooted at `/api/v1` and routed by the API Gateway to the owning service.
- JSON uses UTC ISO-8601 timestamps and opaque string IDs. Money is `{ "amountMinor": 125000, "currency": "VND" }`.
- Mutating reservation, checkout, and payment/refund commands require `Idempotency-Key`. The same key must be reused for transport retries of one user intent.
- The gateway accepts/creates `X-Trace-Id`; services validate and propagate it. The canonical trace ID is returned in responses.
- Authentication is normally a bearer token. Each owning service performs authorization; gateway checks do not replace service checks.
- Pagination uses opaque cursors where needed. Contracts are additive within v1; breaking changes require a new API version.

## Standard errors

```json
{
  "code": "SEAT_UNAVAILABLE",
  "message": "One or more requested seats are unavailable.",
  "traceId": "01J...",
  "timestamp": "2026-09-24T12:34:56.789Z",
  "details": {}
}
```

`message` is safe for users; `code` is stable for clients. `details` must not expose secrets or other users' data.

| HTTP | Representative codes | Meaning |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Structurally/semantically invalid request |
| `401` | `AUTHENTICATION_REQUIRED`, `TOKEN_INVALID` | No valid identity |
| `403` | `FORBIDDEN` | Identity cannot perform this action |
| `404` | `MOVIE_NOT_FOUND`, `SHOWTIME_NOT_FOUND`, `RESERVATION_NOT_FOUND`, `PAYMENT_NOT_READY` | Resource absent, intentionally concealed, or asynchronously not created yet |
| `409` | `SEAT_UNAVAILABLE`, `INVALID_STATE`, `IDEMPOTENCY_KEY_REUSED` | Current state conflicts with command |
| `410` | `RESERVATION_EXPIRED` | The known hold can no longer be used |
| `422` | `PAYMENT_MISMATCH` | Valid syntax but unacceptable business data |
| `429` | `RATE_LIMITED` | Retry according to policy |
| `503` | `DEPENDENCY_UNAVAILABLE`, `TRANSACTION_RETRY_EXHAUSTED` | Bounded transient failure; retry only with same key |

## Auth service

| Method/path | Purpose | Notes |
|---|---|---|
| `POST /api/v1/auth/register` | Create an account | Rate-limited; password policy and safe duplicate behavior required |
| `POST /api/v1/auth/login` | Authenticate | Never logs credentials/tokens |
| `POST /api/v1/auth/refresh` | Rotate/refresh session | Replay/rotation policy is auth-owned |
| `POST /api/v1/auth/logout` | Revoke current session | Idempotent |
| `GET /api/v1/me` | Current profile/roles | Only safe profile fields |

Token format and refresh storage are open security decisions for Phase 1.

## Movie service

| Method/path | Purpose |
|---|---|
| `GET /api/v1/movies` | Public published catalog; optional `status=NOW_SHOWING` or `COMING_SOON` |
| `GET /api/v1/movies/{movieId}` | Movie detail |

Administrative catalog commands will be specified with role checks when requested; they are not needed for initial customer skeletons.

Implemented movie reads return an array for list and one object for detail. Fields: `id` (UUID), `title`, nullable `synopsis`, `durationMinutes`, nullable `releaseDate` (ISO date), and `status` (`NOW_SHOWING`/`COMING_SOON`). Only database records with lifecycle status `PUBLISHED` are exposed; draft/archived details also return `404 MOVIE_NOT_FOUND`. Display status is derived from release date against the current UTC date: future is coming soon; current, past or absent is now showing. Invalid status or malformed UUID returns `400 VALIDATION_ERROR`. Search and cursor pagination are reserved future capabilities, not currently supported. Poster, credits, genre names and ratings are not yet part of this response.

## Cinema service

| Method/path | Purpose |
|---|---|
| `GET /api/v1/cinemas` | Browse cinemas by supported filters/location |
| `GET /api/v1/cinemas/{cinemaId}` | Cinema/auditorium summary |
| `GET /api/v1/cinemas/{cinemaId}/showtimes?date=YYYY-MM-DD&movieId=...` | Scheduled showtimes |
| `GET /api/v1/showtimes/{showtimeId}` | Static showtime details |

Static auditorium/seat labels may be returned by cinema APIs, but live availability comes only from booking service.

## Booking service

| Method/path | Purpose | Key behavior |
|---|---|---|
| `GET /api/v1/showtimes/{showtimeId}/seats` | Authoritative booking availability snapshot | May become stale immediately; reservation command still locks/revalidates |
| `POST /api/v1/reservations` | Atomically hold a seat set | Requires auth and `Idempotency-Key`; `201`, repeatable response, or clean `409` |
| `GET /api/v1/reservations/{reservationId}` | Read owned reservation/deadline | Owner/support authorization |
| `POST /api/v1/reservations/{reservationId}/checkout` | Move valid hold to payment pending | Requires new command key; returns `202` with stable booking/payment IDs/status links |
| `POST /api/v1/reservations/{reservationId}/release` | Explicitly release an eligible owned hold | Idempotent command; ambiguous payment state may reject/defer |
| `GET /api/v1/bookings/{bookingId}` | Read booking/Saga status | Does not infer success from frontend redirect |
| `GET /api/v1/bookings/{bookingId}/ticket` | Read confirmed ticket entitlement | `409 BOOKING_NOT_CONFIRMED` while pending; only confirmed owner/support |

Reservation request:

```json
{
  "showtimeId": "uuid",
  "seatIds": ["uuid-a1", "uuid-a2"]
}
```

Successful hold response:

```json
{
  "reservationId": "uuid",
  "showtimeId": "uuid",
  "seatIds": ["uuid-a1", "uuid-a2"],
  "status": "HELD",
  "holdExpiresAt": "2026-09-24T12:44:56.789Z",
  "traceId": "01J..."
}
```

The service normalizes, rejects duplicates/invalid seats, locks all rows in canonical order, and commits all-or-none. `GET` availability never promises that a later `POST` will win.

Seat availability returns the explicit states `AVAILABLE`, `HELD`, `PAYMENT_PENDING`, or `SOLD`. For privacy it does not expose another customer's `heldBy`, reservation ID, or booking ID.

## Payment service

| Method/path | Purpose | Key behavior |
|---|---|---|
| `GET /api/v1/payments/{paymentId}` | Read authorized payment/checkout state | May return provider checkout instruction while pending; never exposes secrets |
| `GET /api/v1/payments/{paymentId}/refunds` | Read authorized refund state | Customer/support view, filtered fields |
| `POST /provider-webhooks/{provider}` | Provider server-to-server ingress | Dedicated route, raw-byte signature support, provider-specific authentication; not a user API |

Payment creation is triggered by `PaymentRequested`, not an unaudited frontend call. `paymentId` is a stable command identity reserved by booking and becomes the primary payment identity owned/persisted by payment service. Before the asynchronous request has been consumed, status lookup may return retryable `404 PAYMENT_NOT_READY`; it never starts another payment. Once the payment row exists, its requested/provider-pending state is returned normally.

Webhook success responds according to the provider protocol only after signature/business validation and durable idempotent processing. Browser redirect URLs may lead to a frontend status page, but their parameters do not update payment or booking state.

## Notification service

There is no public customer command API in the initial boundary. Notification service consumes events and manages local delivery state. Future preference APIs remain notification-owned and require their own authorization contract.

## Consistency visible to clients

- Reservation responses are strongly consistent with the committed booking database transaction.
- Payment and Saga status are eventually consistent across services. Clients poll with backoff or use a future non-authoritative update channel, then verify via the owning status API.
- `202 Accepted` means workflow accepted/pending, never payment or booking success.
- A timeout does not mean failure. The client repeats the same mutation with the same idempotency key or queries the returned stable resource.

## Contract testing requirements

Phase 1 skeletons must publish machine-readable OpenAPI/event schemas. Later implementations add provider/consumer contract tests, standard error-shape tests, authorization tests, idempotency tests, and compatibility checks for rolling versions.
