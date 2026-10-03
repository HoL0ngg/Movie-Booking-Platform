# ADR-012: Simple Service Package Structure

- Status: Accepted
- Date: 2026-10-03

## Context

The course project focuses on Spring Boot, Kafka, and booking concurrency. The owner requests concrete package names such as entity, repository, and DTO so responsibilities are easier to locate. Existing persistence entities and repositories share one infrastructure package; REST error responses are nested inside handlers.

## Decision

Within each service's existing `com.cinema.<service>` root, organize classes by responsibility: `entity`, `repository`, `dto`, `exception`, `config`, and `filter`. Add `controller`, `service`, `messaging`, and `client` only when corresponding behavior is implemented. Extract existing API error records into each service's own `dto` package, preserving their JSON fields.

Use the same `config` and `filter` names for the gateway. The application entry points and test roots stay in their original root packages so component, entity, and repository scanning still covers all service-local packages. Do not create empty packages or placeholder business classes.

## Consequences and guardrails

- Package names become familiar to Spring Boot learners; there is no separate application/domain/infrastructure directory layer for the current skeleton.
- This changes Java locations and imports, not service boundaries, database tables, API contracts, locking queries, security policies, or Kafka semantics.
- Business logic and transaction boundaries belong in service classes; controllers, filters, and message adapters remain thin.
- Entities/repositories remain owned by one service and must not be shared or imported across services.
- Booking correctness, idempotency, Saga, and Transactional Outbox requirements remain unchanged.
- Clean builds are required after relocation to avoid stale compiled classes in the previous packages.

## References

- [Repository package rules](../../AGENTS.md)
- [Service boundaries](../service-boundaries.md)
