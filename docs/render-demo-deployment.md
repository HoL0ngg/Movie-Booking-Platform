# Classroom demo on Free plans

The Vercel frontend forwards `/api/v1/**` to the existing API gateway, which
routes public HTTPS requests to the owning Render services. Each service is a
separate Free web service in Singapore with its own `Dockerfile.render-*`.
No disks, paid compute, new database projects or payment methods are required.

## Configuration

All containers bind Render's `PORT`. PostgreSQL uses the existing Supabase
session pooler on 5432 with `sslmode=require` and each owner's `currentSchema`.
Credentials remain in Render environment variables, never browser variables.

| Service | Database schema | Application role | URL / credential variables |
|---|---|---|---|
| movie | movie | movie_service | DB_URL, DB_USER, DB_PASSWORD |
| auth | cinema_auth | auth_app | AUTH_DB_URL, AUTH_DB_USER, AUTH_DB_PASSWORD |
| cinema | cinema_cinema | cinema_app | CINEMA_DB_URL, DB_USER, DB_PASSWORD |
| booking | booking | booking_service | BOOKING_DB_URL, BOOKING_DB_USER, BOOKING_DB_PASSWORD |
| payment | payment | payment_service | PAYMENT_DB_URL, PAYMENT_DB_USER, PAYMENT_DB_PASSWORD |
| notification | cinema_notification | notification_app | NOTIFICATION_DB_URL, NOTIFICATION_DB_USER, NOTIFICATION_DB_PASSWORD |

Pooled usernames append `.<project-ref>`. Gateway and AI have no database.
Gateway requires the six `*_SERVICE_URL` variables. All JWT-using services share
the existing `AUTH_JWT_SECRET`; auth/notification share `INTERNAL_API_TOKEN`.
Redis is the Free disposable `movie-booking-demo-redis` in Singapore. Auth and
booking use `REDIS_HOST`/`REDIS_PORT`; seat ownership remains in PostgreSQL.

The owner approved merging PR #7, hosted role provisioning and deployment on
2026-11-17. New movie/booking/payment roles have only their owner-schema access.
The incremental [CHECK patch](database/render-demo-schema-patch.sql) restores 24
constraints already defined by the reference DDL; existing rows were valid and
no data was deleted. Cinema's paired seat field is currently transient, so its
missing reference column is not a Hibernate startup requirement.

## Kafka and other Free limits

Kafka delivery must work before claiming the booking/payment demo is complete.
Until credentials are supplied, Kafka listeners/outbox publishing and payment
reconciliation/refund dispatch stay paused by runtime configuration. Durable
outbox rows are preserved; do not delete events or fake successful payments.

Aiven offers a genuine Kafka Free plan (no card/time-limited credit trial).
It allows five topics with two partitions each. Create three domain topics plus
one shared quarantine topic, `cinema.events.quarantine.v1`, and point the
showtime/booking/payment quarantine properties there. Source/error headers
must remain available for diagnosis; verify quarantine handling after connection.
Use SASL/SCRAM over TLS, keeping certificate validation enabled. Never disable
TLS verification to simplify setup. The owner creates the Aiven account.

Render Free blocks SMTP ports. Auth uses existing OTP logging mode for the demo;
read OTP only from private server logs. Existing-account login works normally.
Notification uses log mode. Do not expose OTP in a public API. HTTPS email delivery
requires a separate provider/configuration if actual delivery is needed.

Hosted payment uses VNPay Sandbox and explicitly disables
`PAYMENT_VNPAY_DEV_SETTLE_FROM_RETURN`. Provider return redirects use the public
payment service; frontend results use the Vercel `/payment/result` route.
Only verified server-side results can confirm payments. No real-money test runs.
AI uses `openrouter/free` only when an existing server key is available; otherwise
it starts disabled and does not block booking.

All current seeded schedules are in the past. Create fresh showtimes through
cinema's API after Kafka works, then verify their booking inventory projection.
Never shift historical sold sessions or seed another service's database directly.

Free services sleep and share Render's monthly instance hours. Wake the services
before class; do not use continuous keep-alive traffic. Outbound bandwidth/build
allowances are finite and overages may be billed if a payment method is attached.
No upgrade or payment method is authorized by this deployment.

## Checks

Package all modules; AI's 21 focused tests use fake providers and pass. Frontend
build/rewrite assertions pass. Existing frontend lint errors and lack of a local
Docker Engine are documented in [movie deployment](render-movie-deployment.md).
After each cloud deploy, check terminal `live` state, health, API responses,
authorization rejection and end-to-end Vercel routing. Health alone does not
prove Kafka delivery or the booking/payment Saga.

References: [Render Free](https://render.com/docs/free),
[Aiven Kafka Free](https://aiven.io/docs/products/kafka/free-tier/kafka-free-tier),
[Supabase connections](https://supabase.com/docs/guides/database/connecting-to-postgres).
