# Project Status

Current phase: Phase 2

Completed:
- Phase 0 — Architecture
- Phase 1 — Infrastructure

In progress:
- Phase 2 — Core Domain Services

Implemented in Phase 2:
- Six service-owned PostgreSQL V2 schemas: 28 domain and infrastructure tables, with matching JPA entities and repositories.
- Flyway migration and Hibernate mapping validation against PostgreSQL Testcontainers.
- Database-level tests for seat row locking and unique sale, published showtime overlap, and payment webhook deduplication.

Not yet implemented:
- Business APIs and application services for authentication, catalog, scheduling, booking, payment, and notification.
- Kafka relays/consumers, reservation expiry processing, and end-to-end Saga workflows.
- Frontend booking flows and the full concurrency/idempotency test suite for those workflows.

Next:
- Phase 3 — Booking Engine
