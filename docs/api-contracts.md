# REST API Contract Baseline

Status: Contract baseline with implemented public movie reads and admin movie CRUD; implementation status for other operations is service-specific.

Static OpenAPI 3.1 contracts are published from each Spring Boot module at `/openapi/openapi.yaml` and listed by the gateway contract. They are contract artifacts, not evidence that an operation is implemented. This document remains the semantic authority until implementation-time DTO schemas and contract tests are added.

## Conventions

`ai-service` exposes the temporary `POST /api/v1/ai/chat` conversation operation when `AI_CHAT_PROVIDER=openai`. It uses the OpenRouter OpenAI-compatible API with server-side `OPENROUTER_API_KEY` and model `openrouter/free`. It accepts the current message and a bounded history from the client; it does not persist conversation state or query another service's database. The current prototype is unauthenticated and must not be treated as production authorization. Provider calls are never part of booking/payment transactions or Sagas.

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
| `GET /api/v1/admin/movies` | ADMIN: all movies including draft/archived |
| `GET /api/v1/admin/movies/{movieId}` | ADMIN: movie metadata and genre IDs |
| `POST /api/v1/admin/movies` | ADMIN: create, `201` with Location |
| `PUT /api/v1/admin/movies/{movieId}` | ADMIN: replace metadata and genre membership |
| `DELETE /api/v1/admin/movies/{movieId}` | ADMIN: archive, `204`; repeat archive also `204` |
| `GET /api/v1/admin/genres` | ADMIN: existing `{id, name}` genre choices |

Admin routes accept the auth-service HS256 bearer token, verify its signature, issuer `cinema-auth-service` and expiry, and require `ADMIN` in the `roles` claim inside movie-service. Gateway forwards these requests without granting domain permission. Set the same `AUTH_JWT_SECRET` (at least 32 UTF-8 bytes) as auth-service; movie-service now requires this at startup, including for public reads.

Admin movie input contains `title` (nonblank, max 160, trimmed), nullable `synopsis` (max 3000), nullable HTTP(S) `posterUrl` (max 2048), `durationMinutes` (1–600), optional ISO `releaseDate` (omitted/null defaults to the current UTC date on create/update), lifecycle `status` (`DRAFT/PUBLISHED/ARCHIVED`), and required `genreIds` (1–50 distinct existing UUIDs). The response adds the server-generated `id`. Public display status remains derived from release date, separate from admin lifecycle. Genre membership and movie metadata commit in one local transaction; updates/archive serialize on the owning movie row. Empty genre lists, unknown or duplicate genre IDs, malformed bodies/UUIDs and invalid fields return `400 VALIDATION_ERROR`. Unauthenticated/non-admin requests return `401`/`403`; unknown movie IDs return `404 MOVIE_NOT_FOUND`.

Delete archives instead of removing the ID or historical metadata. Archiving/unpublishing removes the movie from public catalog reads; it does not cancel existing cinema-owned showtimes, release seats, refund payments or change booking snapshots. Editing catalog duration does not reschedule existing showtimes. An admin may restore a movie by changing its lifecycle back to DRAFT/PUBLISHED. These metadata operations do not publish business-critical events or participate in booking/payment workflows. POST create is not idempotent: the UI disables duplicate submissions and does not retry mutations automatically; after an ambiguous network failure, reload the list before submitting again. Concurrent full updates use last committed input; edit-conflict/version UI is not implemented.

The admin movie page reads/writes only these APIs and invalidates admin/public movie queries after success. Cinema, room, physical-seat and schedule administration also use owner APIs (see below). Overview, booking and payment administration remain isolated read-only demo fixtures; their totals and movie names are not synchronized with the real catalog. The form exposes the fields the backend persists; original title, age rating, director, cast and ratings have no persistence contract yet. Genres are selected from existing records; genre CRUD is outside movie CRUD.

Implemented public movie reads return an array for list and one object for detail. Fields: `id` (UUID), `title`, nullable `synopsis`, nullable `posterUrl`, `durationMinutes`, nullable `releaseDate`, derived `status` (`NOW_SHOWING/COMING_SOON`) and `genres` (names). Only lifecycle `PUBLISHED` records are exposed; draft/archived details return `404 MOVIE_NOT_FOUND`. Future release dates in UTC are coming soon; current, past or absent dates are now showing. Optional `query` matches title substrings and `genre` matches a genre name, both case-insensitive. Invalid status or malformed UUID returns `400 VALIDATION_ERROR`. Cursor pagination is not implemented. The frontend falls back to a local poster placeholder. Credits and ratings are not part of this response.

## Cinema service

| Method/path | Purpose |
|---|---|
| `GET /api/v1/cinemas` | Browse cinemas by supported filters/location |
| `GET /api/v1/cinemas/{cinemaId}` | Cinema/auditorium summary |
| `GET /api/v1/cinemas/{cinemaId}/showtimes?date=YYYY-MM-DD&movieId=...` | Scheduled showtimes |
| `GET /api/v1/showtimes/{showtimeId}` | Static showtime details |

Static auditorium/seat labels may be returned by cinema APIs, but live availability comes only from booking service.

### Implemented cinema administration

All `/api/v1/admin/cinemas/**`, `/admin/auditoriums/**` and `/admin/showtimes/**` operations require a verified auth-service JWT with `ADMIN` in cinema-service. Gateway routing grants no permission. Existing `/showtimes` create/publish/cancel commands continue to support ADMIN or assigned CINEMA_MANAGER authorization.

| Method/path (under `/api/v1`) | Request | Response |
|---|---|---|
| `GET /admin/cinemas` | — | All cinemas, ordered by name |
| `POST /admin/cinemas` | `{name, address, city, timezone?}` | `201` cinema with server-generated UUID |
| `PUT /admin/cinemas/{id}` | Same fields | Updated cinema |
| `GET /admin/cinemas/{id}/auditoriums` | — | `{id, cinemaId, name}[]` |
| `POST /admin/cinemas/{id}/auditoriums` | `{name}` | `201` auditorium with server-generated UUID |
| `PUT /admin/auditoriums/{id}` | `{name}` | Renamed auditorium; cinema cannot change |
| `GET /admin/auditoriums/{id}/seats` | — | Physical seats, ordered by row and number |
| `PUT /admin/auditoriums/{id}/seats` | `{seats: [...]}` | Atomically replaced physical layout |
| `GET /admin/showtimes?cinemaId=...` | Optional cinema UUID | All statuses, ordered by start |
| `PUT /admin/showtimes/{id}` | Same fields as create below | Updated DRAFT; auditorium cannot change |
| `POST /showtimes` | `{auditoriumId, movieId, startsAt, endsAt, salesCloseAt, priceMinor, currency}` | `201` DRAFT |
| `POST /showtimes/{id}/publish` | — | PUBLISHED showtime; repeat returns existing result |
| `POST /showtimes/{id}/cancel` | `{reasonCode}` | CANCELLED showtime; repeat returns existing result |

Cinema objects contain `{id, name, address, city, timezone}`. Names/addresses/cities are nonblank and trimmed (maximum 200/500/100 characters). Timezone is an IANA identifier, default `Asia/Ho_Chi_Minh` when omitted. Auditorium names are nonblank, at most 100 characters and unique within their cinema.

A physical seat contains `{id, auditoriumId, rowLabel, seatNumber, seatType, isActive, pairedSeatId?}`. A replacement contains 1–780 seats with unique UUIDs and positions, rows `A`–`Z`, numbers 1–30, and types `STANDARD`, `VIP` or `COUPLE`. COUPLE pairs must be reciprocal, adjacent in the same row and room, with matching active flags. Other types have no partner. IDs belonging to another auditorium cannot be reassigned. This configures physical definitions, never booking availability.

Layout replacement locks the auditorium and rejects with `409 LAYOUT_LOCKED` once **any** showtime exists, including DRAFT or CANCELLED. Showtime creation/edit/publication/cancellation also lock the room before the showtime, preventing races with layout replacement. Only DRAFT is editable. Published snapshots cannot change in place: cancel and create another showtime. Publication validates active seats, preserves the existing transactional outbox and PostgreSQL exclusion constraint against overlapping published sessions. Cancellation preserves existing outbox/idempotency behavior. Physical layout and draft changes do not emit inventory events because they have not established bookable inventory.

Showtime responses contain `{id, movieId, cinemaId, auditoriumId, startsAt, endsAt, salesCloseAt, price:{amountMinor,currency}, status}`. Status is `DRAFT`, `PUBLISHED` or `CANCELLED`. Validation requires `startsAt < endsAt`, `salesCloseAt <= startsAt`, nonnegative integer price and uppercase three-letter currency. `movieId` remains a logical reference without cross-service SQL. The form selects actual catalog records; format/language are omitted because the service does not persist them. Date filtering uses the cinema timezone; the form labels device-local input and sends UTC instants.

Errors use the standard shape: `400 VALIDATION_ERROR` (or `INVALID_TIMEZONE`, `INVALID_SEAT`, `INVALID_COUPLE`), `401`, `403`, `404`, `409 DATA_CONFLICT` for database conflicts, and `409 INVALID_STATE` for forbidden transitions. Failed reads show retry controls without demo substitution. Mutations do not retry automatically; after an ambiguous network failure, reload before creating again. No delete APIs are introduced. Real admin pages do not depend on demo localStorage. `/admin/publishing` and `/admin/showtimes` share the real schedule screen.

Before deploying, apply the additive [seat-layout upgrade](../services/cinema-service/src/main/resources/db/admin-seat-layout-upgrade.sql) to the owning database after review. Do not replay `schema.sql` on existing data. Legacy COUPLE rows need an explicit partner audit before publication; existing published showtimes are not rewritten.

## Booking service

| Method/path | Purpose | Key behavior |
|---|---|---|
| `GET /api/v1/showtimes/{showtimeId}/seats` | Authoritative booking availability snapshot | May become stale immediately; reservation command still locks/revalidates |
| `POST /api/v1/reservations` | Atomically hold a seat set | Requires auth and `Idempotency-Key`; `201`, repeatable response, or clean `409` |
| `GET /api/v1/reservations/{reservationId}` | Read owned reservation/deadline | Owner/support authorization |
| `POST /api/v1/reservations/{reservationId}/checkout` | Create a pending booking and move the valid hold to payment pending | Requires `Idempotency-Key`; atomically returns `202` with `bookingId`, `paymentId`, and `status=PAYMENT_PENDING`. `PaymentRequested` is written to the booking outbox; payment creation is asynchronous |
| `POST /api/v1/reservations/{reservationId}/release` | Explicitly release an eligible owned hold | Idempotent command; ambiguous payment state may reject/defer |
| `GET /api/v1/bookings/{bookingId}` | Read booking/Saga status | Does not infer success from frontend redirect |
| `GET /api/v1/bookings/by-payment/{paymentId}` | Find the current user's booking by payment ID | Returns `404` until booking-service processes the verified payment result; never exposes another user's booking |
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

AI chat provider failures return HTTP `503` with `code=AI_PROVIDER_UNAVAILABLE`, a safe message, canonical `traceId`, and timestamp. OpenRouter calls have a 45-second deadline and no SDK retries; the gateway AI route waits up to 60 seconds. Other routes retain the global 5-second response timeout.
