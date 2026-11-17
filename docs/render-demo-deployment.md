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
If Kafka credentials are unavailable, keep Kafka-dependent workflows unavailable
until the connection is restored. Durable outbox rows must be preserved; do not
delete events or fake successful payments. The current demo has working Kafka
TLS/authentication and publishing/consumers enabled.

Aiven offers a genuine Kafka Free plan (no card/time-limited credit trial).
It allows five topics with two partitions each. Create three domain topics plus
one shared quarantine topic, `cinema.events.quarantine.v1`, and point the
showtime/booking/payment quarantine properties there. Source/error headers
must remain available for diagnosis; verify quarantine handling after connection.
Use SASL/SCRAM over TLS with the supplied Aiven CA in the runtime
`SPRING_APPLICATION_JSON` PEM truststore, keeping certificate validation enabled. Never disable
TLS verification to simplify setup. The owner creates the Aiven account.

Render Free blocks SMTP ports. Auth uses existing OTP logging mode for the demo;
read OTP only from private server logs. Existing-account login works normally.
Notification uses log mode; `MANAGEMENT_HEALTH_MAIL_ENABLED=false` excludes the
unused SMTP connection from auth/notification health. Do not expose OTP in a public API. HTTPS email delivery
requires a separate provider/configuration if actual delivery is needed.

Hosted payment uses VNPay Sandbox and explicitly disables
`PAYMENT_VNPAY_DEV_SETTLE_FROM_RETURN`. Provider return redirects use the public
payment service; frontend results use the Vercel `/payment/result` route.
Only verified server-side results can confirm payments. No real-money test runs.
AI uses `openrouter/free` only when an existing server key is available; otherwise
it starts disabled and does not block booking.

The original seeded schedules were in the past. Six fresh showtimes on November
17/18 were created through cinema's API after Kafka connected. Verify their
booking inventory projection; create future schedules for a later class date.
Never shift historical sold sessions or seed another service's database directly.

Free services sleep and share Render's monthly instance hours. Wake the services
before class with `powershell -File infrastructure/scripts/warm-render-demo.ps1`
from the repository root; this checks readiness in parallel for up to four minutes,
then stops instead of generating continuous keep-alive traffic. Outbound bandwidth/build
allowances are finite and overages may be billed if a payment method is attached.
No upgrade or payment method is authorized by this deployment.

## Checks

Package all modules; AI's 21 focused tests use fake providers and pass. Frontend
build/rewrite assertions pass. Existing frontend lint errors and lack of a local
Docker Engine are documented in [movie deployment](render-movie-deployment.md).
After each cloud deploy, check terminal `live` state, health, API responses,
authorization rejection and end-to-end Vercel routing. Health alone does not
prove Kafka delivery or the booking/payment Saga.

## Verified deployment on 2026-11-17

All eight Render web services are `live` on Free instances. Redis is Free; the
existing Supabase organization is Free. The readiness script reports all eight
`UP` after excluding unused SMTP health from the two log-mail services.

- Vercel movie API returns the 40 published Supabase movies; authenticated profile
  lookup works, while unauthenticated profile access returns 401.
- Six published sessions on November 17/18 at Cinemate Ben Thanh each projected
  100 seats through Kafka into booking's inventory (600 total).
- Live API checks passed: seat hold, same-key replay returning the same reservation,
  conflicting hold rejection, idempotent cancellation and seat release.
- Checkout emitted through Kafka and payment generated the signed VNPay Sandbox
  URL with the public Render return URL. No real or sandbox charge was executed;
  successful provider IPN/query verification still requires a manual sandbox test.
- A transient 502 during gateway redeployment had an ambiguous create outcome.
  The persisted draft was inspected before retrying; an accidental duplicate draft
  from timestamp formatting in the temporary smoke script was cancelled through
  the owning API. No historical sold sessions were changed.
- One pre-existing refund older than its 24-hour reconciliation window was marked
  unresolved/failed by the normal job and needs manual business follow-up. It was
  not deleted or made successful for the demo.

AI is deployed with inference disabled because no OpenRouter key was present in
the deployment environment. To enable it later, set the server-side existing key
and `AI_CHAT_PROVIDER=openai`, retaining `OPENROUTER_MODEL=openrouter/free`.
Auth OTP is visible only in private server logs; notification email is logged.
For actual VNPay callbacks, configure the merchant Sandbox IPN URL as
`https://movie-booking-payment-service.onrender.com/provider-webhooks/vnpay`.

References: [Render Free](https://render.com/docs/free),
[Aiven Kafka Free](https://aiven.io/docs/products/kafka/free-tier/kafka-free-tier),
[Supabase connections](https://supabase.com/docs/guides/database/connecting-to-postgres).
