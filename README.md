# Cinema Booking Platform

This repository is in **Phase 2: core domain services**. Phase 1 established buildable Spring Boot technical shells, local infrastructure, health/metrics endpoints, trace propagation, fail-closed security, and machine-readable REST/event contracts. Six service-owned PostgreSQL schemas now have reference SQL, JPA entities, and repositories. Database changes are managed manually on Supabase (ADR-013). Public movie catalog reads are implemented; booking, payment, authentication, scheduling, catalog administration and notification business logic are not implemented yet.

## Repository layout

```text
cinema-booking/
|-- AGENTS.md
|-- README.md
|-- docker-compose.yml
|-- docs/
|   `-- ADR/
|-- frontend/
|   |-- src/
|   `-- e2e/
|-- gateway/
|-- services/
|   |-- auth-service/
|   |-- movie-service/
|   |-- cinema-service/
|   |-- booking-service/
|   |-- payment-service/
|   |-- notification-service/
|   `-- ai-service/
|-- infrastructure/
|   |-- kafka/
|   |-- postgres/
|   |-- redis/
|   |-- monitoring/
|   `-- docker/
`-- skills/
```

The eight backend deployables are independent Maven modules under `gateway/` and `services/`. Each stateful service owns its database and reference schema at `src/main/resources/db/schema.sql`; Compose retains dedicated PostgreSQL containers for local development. The stateless `ai-service` adds optional Spring AI/Google GenAI integration with `gemini-3.8-flash` (ADR-011) and a temporary stateless chat endpoint. Kafka, disposable Redis, and an optional Prometheus profile are defined in `docker-compose.yml`. Application Prometheus export is currently removed; Actuator health/info and trace logging remain available. See [monitoring status](docs/infrastructure.md#secrets-and-startup) before enabling that profile.

## Non-negotiable foundations

- `booking-service` and its PostgreSQL database are authoritative for seat ownership.
- Seat reservations use short PostgreSQL transactions, row locks, deterministic seat ordering, constraints, and all-or-nothing updates.
- Redis is optional acceleration only; flushing it cannot violate booking correctness.
- Each microservice owns an isolated database. No cross-service SQL or shared mutable schema is allowed.
- Distributed booking/payment work uses a Saga. Critical state and event publication use a Transactional Outbox.
- Kafka is at-least-once; consumers persist idempotency/inbox state and tolerate duplicates.
- `payment-service` alone integrates payment providers. Browser redirects never confirm payment.

## Documentation map

- [Architecture](docs/architecture.md)
- [Service boundaries](docs/service-boundaries.md)
- [Database design](docs/database-design.md)
- [Booking flow](docs/booking-flow.md)
- [Concurrency](docs/concurrency.md)
- [Payment flow](docs/payment-flow.md)
- [Event catalog](docs/event-catalog.md)
- [API contracts](docs/api-contracts.md)
- [Local infrastructure](docs/infrastructure.md)
- [Architecture decisions](docs/ADR/)

Machine-readable contracts live with each service at `src/main/resources/static/openapi/openapi.yaml`. Kafka contracts live in `infrastructure/kafka/contracts/`.

## Local quick start

Prerequisites: Java 21+, Maven 3.9+, Docker Engine, and Docker Compose.

```powershell
Copy-Item .env.example .env
# Set every blank password in .env to a local-only value.
docker compose up -d
mvn package
```

Backend test dependencies are temporarily removed to simplify the build. The parent POM sets `maven.test.skip=true`, skipping test compilation and execution; existing `src/test` files are preserved except in `ai-service`, where tests with missing dependencies were removed at the owner's request. `mvn test` currently executes no tests. To restore them, add `spring-boot-starter-test` to the modules and, for stateful services, `spring-boot-testcontainers`, `testcontainers-postgresql`, and `testcontainers-junit-jupiter`. Then remove the skip property or use `-Dmaven.test.skip=false` with `mvn test`.

Before starting a stateful service, configure its `*_DB_URL`, `*_DB_USER`, and `*_DB_PASSWORD` in the process environment for its provisioned database. Spring Boot does not load `.env` automatically. For a fresh local database, manually apply only the owning service's `db/schema.sql` once. SQL initialization is disabled and Hibernate only validates mappings; a missing or mismatched schema prevents startup. See [schema management](docs/database-design.md#schema-management) for the reference files. Do not replay these full schemas on deployed databases.

### Movie catalog configuration and frontend

`movie-service` now explicitly imports `.env` using Spring Boot Config Data, with no dotenv dependency. It looks at `../../.env` (when started from `services/movie-service`) and then `./.env` (repository-root launch or a module-local override). Set the IDE working directory to the repository root or the movie-service directory. AI service uses the same Config Data import pattern. Other services still require process environment variables.

Set `MOVIE_DB_URL`, `MOVIE_DB_USER`, and `MOVIE_DB_PASSWORD` for the movie-owned database. Existing `DB_URL`, `DB_USER`, and `DB_PASSWORD` are supported as movie-only fallback aliases. Do not define both sets; a defined but empty `MOVIE_DB_*` value also takes precedence. Process environment variables override imported file values for the same key. Use plain `KEY=value` Java-properties syntax: no `export`, shell expansion, or surrounding quotes; escape literal backslashes as `\\`. Keep `.env` ignored and never put database credentials in frontend `VITE_*` values.

```powershell
mvn -pl services/movie-service spring-boot:run
# In a second terminal:
mvn -pl gateway spring-boot:run
# In a third terminal, from frontend/:
npm run dev
```

The frontend defaults to `/api/v1`; Vite proxies `/api` to `http://localhost:8080`. Optional frontend settings are in `frontend/.env.example`; `API_PROXY_TARGET` changes the development gateway address. Production hosting must route `/api` to the gateway, or provide a public `VITE_API_BASE_URL` and configure CORS at the gateway for that deployment.

Only catalog GET requests are public at gateway and movie-service. List and detail hooks use HTTP without mock fallback. The current schema exposes title, synopsis, duration, release date and derived display status; only `PUBLISHED` records are public. A future release date (UTC) produces `COMING_SOON`; a past/current or missing date produces `NOW_SHOWING`. Posters use the nullable `poster_url` column exposed as `posterUrl`; missing artwork uses a local placeholder. Apply `services/movie-service/src/main/resources/db/add-poster-url-2026-10-04.sql` to an existing movie schema before deploying the updated entity. Unavailable credits and ratings are not fabricated. Cinema/showtime, booking/payment, authentication and admin data remain prototype mocks, so database movie UUIDs do not automatically have mock showtimes. This change does not implement a real booking flow.

Run a service from the repository root after infrastructure and its database schema are ready:

```powershell
mvn -pl gateway spring-boot:run
mvn -pl services/booking-service spring-boot:run
```

The gateway listens on `8080`; service ports are `8081` through `8087`. Start AI with `mvn -pl services/ai-service spring-boot:run`; default provider `google-genai` requires `GEMINI_API_KEY`. Set `AI_CHAT_PROVIDER=none` to start without credentials. See the Gemini setup below. See [local infrastructure](docs/infrastructure.md) for configuration. AI chat is temporarily public; see each service contract for its current routes.

### Gemini AI service

The only supported AI provider is the Gemini Developer API via Google GenAI. The model is `gemini-3.8-flash` and provider defaults to `google-genai`; no local model runtime is needed. Set `GEMINI_API_KEY` in the git-ignored repository-root `.env` (or module-local `.env`), or export it in the shell running Maven:

```powershell
$env:GEMINI_API_KEY = "<your Gemini API key>"
mvn -pl services/ai-service spring-boot:run
```

AI service imports `../../.env` followed by `./.env`, matching repository-root and service-directory launches. Use plain `KEY=value` properties syntax without surrounding quotes; process environment variables take precedence.

Obtain the key from [Google AI Studio](https://aistudio.google.com/apikey) and keep it server-side, outside Git and frontend `VITE_*` variables. Set `AI_CHAT_PROVIDER=none` to disable inference. `POST /api/v1/ai/chat` accepts `message` and optional `history`, and returns `message`. It provides general cinema conversation without live catalog/showtime data or booking/payment actions. The prototype endpoint is public at both gateway and service; add authorization and request limits before production.

## Development workflow

Before any implementation, read `AGENTS.md`, load the relevant skill under `skills/`, inspect these architecture documents and applicable ADRs, identify the invariant at risk, then make and test the smallest coherent change. Architecture changes require a new or superseding ADR.

Phase 1 established infrastructure and application skeletons. Phase 2 has added the database layer and public movie catalog reads while preserving these boundaries; remaining business APIs and workflows are future work.

AI chat uses a 45-second Google SDK call timeout with one attempt, and a 60-second gateway route response timeout. Other gateway routes retain 5 seconds. Google overload/network errors return `503 AI_PROVIDER_UNAVAILABLE`; restart both AI service and gateway after these configuration changes.

For AI chat `503 AI_PROVIDER_UNAVAILABLE`, inspect the AI service warning `AI provider unavailable` and its `traceId`, `exceptionType`, and `providerStatus`. The public 503 can represent an upstream API error or a network failure; it does not identify the Google error by itself. Provider error messages, credentials, and chat content are omitted from this diagnostic. Restart AI service to apply the logging change.

`exceptionType=UnknownHostException` means DNS resolution failed before an HTTP response was received; it does not establish an API-key or model error. Check `Resolve-DnsName generativelanguage.googleapis.com` on Windows, then restart the IntelliJ run process after restoring connectivity. A separate failure is possible when DNS succeeds but only IPv6 reaches Google: Java normally tries IPv4 first, and the SDK can exhaust its deadline on those unreachable addresses. The AI module's `spring-boot:run` now uses `-Djava.net.preferIPv6Addresses=system` to preserve the OS resolver order. For IntelliJ's direct `AiServiceApplication` run configuration, add the same flag under **Run → Edit Configurations → Modify options → Add VM options**, then restart. For a packaged JAR, put the flag before `-jar`. This is a JVM startup option; putting it in `.env` or `application.yml` does not configure Java networking. It preserves IPv4 support and does not fix a DNS outage. See [Java networking properties](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/net/doc-files/net-properties.html).

For chat `403` responses through the Vite proxy or gateway, compare with a direct request to AI service. Keep `spring.ai.model.chat: ${AI_CHAT_PROVIDER:google-genai}` in AI service configuration: its chat controller, service, and bounded Gemini client are conditional on this property. Removing it disables those beans, and the missing endpoint's error dispatch can return 403 under the service's deny-all policy. Restart AI service after restoring the property. The frontend's default `/api/v1` prefix correctly uses Vite's `/api` proxy; the gateway already permits `POST /api/v1/ai/chat`.

Do not configure custom temperature/top-p/top-k for Gemini 3.8 Flash; Google recommends omitting these sampling parameters for Gemini 3.6+. Spring AI 2.0.1 still supplies sampling defaults internally; removing YAML properties does not remove those SDK request defaults. An upstream `providerStatus=400` indicates a request/client error, even though the public AI endpoint maps it to 503. See [Gemini troubleshooting](https://ai.google.dev/gemini-api/docs/troubleshooting).
