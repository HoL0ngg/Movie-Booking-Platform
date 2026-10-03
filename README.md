# Cinema Booking Platform

This repository is in **Phase 2: core domain services**. Phase 1 established buildable Spring Boot technical shells, local infrastructure, health/metrics endpoints, trace propagation, fail-closed security, and machine-readable REST/event contracts. Six service-owned PostgreSQL schemas now have reference SQL, JPA entities, and repositories. Database changes are managed manually on Supabase (ADR-013). Booking, payment, authentication, catalog, scheduling, and notification business logic is not implemented yet.

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

The eight backend deployables are independent Maven modules under `gateway/` and `services/`. Each stateful service owns its database and reference schema at `src/main/resources/db/schema.sql`; Compose retains dedicated PostgreSQL containers for local development. The stateless `ai-service` adds optional Spring AI/Ollama integration (ADR-011), with no business endpoints yet. Kafka, disposable Redis, and an optional Prometheus/Grafana profile are defined in `docker-compose.yml`. Application Prometheus export is currently removed; Actuator health/info and trace logging remain available. See [monitoring status](docs/infrastructure.md#secrets-and-startup) before enabling that profile.

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

Backend test dependencies are temporarily removed to simplify the build. The parent POM sets `maven.test.skip=true`, skipping test compilation and execution; existing `src/test` files are preserved. `mvn test` currently executes no tests. To restore them, add `spring-boot-starter-test` to the modules and, for stateful services, `spring-boot-testcontainers`, `testcontainers-postgresql`, and `testcontainers-junit-jupiter`. Then remove the skip property or use `-Dmaven.test.skip=false` with `mvn test`.

Before starting a stateful service, configure its `*_DB_URL`, `*_DB_USER`, and `*_DB_PASSWORD` in the process environment for its provisioned database. Spring Boot does not load `.env` automatically. For a fresh local database, manually apply only the owning service's `db/schema.sql` once. SQL initialization is disabled and Hibernate only validates mappings; a missing or mismatched schema prevents startup. See [schema management](docs/database-design.md#schema-management) for the reference files. Do not replay these full schemas on deployed databases.

Run a service from the repository root after infrastructure and its database schema are ready:

```powershell
mvn -pl gateway spring-boot:run
mvn -pl services/booking-service spring-boot:run
```

The gateway listens on `8080`; service ports are `8081` through `8087`. Start the AI skeleton with `mvn -pl services/ai-service spring-boot:run`; no model runtime is required by default. See [local infrastructure](docs/infrastructure.md) for configuration. API routes remain denied until authentication and authorization are implemented.

## Development workflow

Before any implementation, read `AGENTS.md`, load the relevant skill under `skills/`, inspect these architecture documents and applicable ADRs, identify the invariant at risk, then make and test the smallest coherent change. Architecture changes require a new or superseding ADR.

Phase 1 established infrastructure and application skeletons. Phase 2 has added the database layer while preserving these boundaries; business APIs and workflows remain future work.
