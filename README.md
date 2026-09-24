# Cinema Booking Platform

This repository is at **Phase 0: architecture and engineering foundations**. It contains service boundaries, database and concurrency rules, API/event contracts, ADRs, and reusable Codex skills. It intentionally contains no Spring Boot or React application implementation yet.

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

The service and infrastructure directories are Phase 0 placeholders. `docker-compose.yml` is intentionally empty until Phase 1 defines pinned infrastructure images, health checks, networks, volumes, and local credentials.

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
- [Architecture decisions](docs/ADR/)

## Development workflow

Before any implementation, read `AGENTS.md`, load the relevant skill under `skills/`, inspect these architecture documents and applicable ADRs, identify the invariant at risk, then make and test the smallest coherent change. Architecture changes require a new or superseding ADR.

Phase 1 may create infrastructure and application skeletons only after preserving these boundaries. Business logic begins in a later phase.

