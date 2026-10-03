# Project Status

Current phase: Phase 2 — Core Domain Services and frontend prototype

Completed:
- Phase 0 — Architecture
- Phase 1 — Infrastructure

In progress:
- Phase 2 — Core Domain Services

Implemented in Phase 2:
- Admin UI prototype under `/admin`: dashboard, movie/cinema/showtime demo editors, and read-only booking/payment views. See `docs/admin-ui.md`; backend admin APIs and role enforcement are not implemented.
- Optional stateless `ai-service` skeleton using Spring AI 2.0.1/Ollama, fail-closed security, diagnostics, trace IDs, and reserved gateway route (ADR-011).
- Six service-owned PostgreSQL V2 schemas: 28 domain and infrastructure tables, with matching JPA entities and repositories.
- Flyway migration and Hibernate mapping validation against PostgreSQL Testcontainers.
- Database-level tests for seat row locking and unique sale, published showtime overlap, and payment webhook deduplication.
- A responsive React/TypeScript frontend prototype covering discovery, movie details, showtime and cinema selection, accessible seat selection, mock checkout/payment, confirmation, authentication screens, and booking history.
- Typed frontend contracts and a replaceable `UI -> TanStack Query hook -> service -> mock API` boundary. Mock state is persisted locally for demo purposes only.

Not yet implemented:
- AI chat/recommendation endpoints and catalog grounding. The optional Spring AI skeleton (port 8087) and gateway route exist; no persistence or live inference is implemented.
- Business APIs and application services for authentication, catalog, scheduling, booking, payment, and notification.
- Kafka relays/consumers, reservation expiry processing, and end-to-end Saga workflows.
- Real frontend-to-backend integration and the full concurrency/idempotency test suite for those workflows.

Next:
- Phase 3 — Implement auth and read-only movie/cinema APIs, then replace the corresponding frontend mock services.
- Phase 4 — Booking Engine and real seat availability/reservation integration.
