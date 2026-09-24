# Cinema Booking Platform

This repository is at **Phase 1: infrastructure and service skeletons**. It contains the Phase 0 architecture baseline plus buildable Spring Boot technical shells, local infrastructure, health/metrics endpoints, trace propagation, fail-closed security, Flyway baselines, and machine-readable REST/event contracts. It intentionally contains no booking, payment, authentication, catalog, scheduling, or notification business logic yet.

## Repository layout

```text
cinema-booking/
|-- AGENTS.md
|-- README.md
|-- docker-compose.yml
|-- docs/
|   `-- ADR/
|-- frontend/
|   `-- web/
|-- gateway/
|-- services/
|   |-- auth-service/
|   |-- movie-service/
|   |-- cinema-service/
|   |-- booking-service/
|   |-- payment-service/
|   `-- notification-service/
|-- infrastructure/
|   |-- kafka/
|   |-- postgres/
|   |-- redis/
|   |-- monitoring/
|   `-- docker/
`-- skills/
```

The seven deployables are independent Maven modules under `gateway/` and `services/`. Each stateful service has a dedicated PostgreSQL container and migration location. Kafka, disposable Redis, and an optional Prometheus/Grafana profile are defined in `docker-compose.yml`.

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

## Phase 1 quick start

Prerequisites: Java 21+, Maven 3.9+, Docker Engine, and Docker Compose.

```powershell
Copy-Item .env.example .env
# Set every blank password in .env to a local-only value.
docker compose up -d
mvn test
```

Run a service from the repository root after infrastructure is healthy:

```powershell
mvn -pl gateway spring-boot:run
mvn -pl services/booking-service spring-boot:run
```

The gateway listens on `8080`; service ports are `8081` through `8086`. See [local infrastructure](docs/infrastructure.md) for the complete port and ownership map. API routes remain denied until later phases implement authentication and authorization.

## Development workflow

Before any implementation, read `AGENTS.md`, load the relevant skill under `skills/`, inspect these architecture documents and applicable ADRs, identify the invariant at risk, then make and test the smallest coherent change. Architecture changes require a new or superseding ADR.

Phase 1 has established infrastructure and application skeletons while preserving these boundaries. Business logic begins only in a later, explicitly requested phase.
