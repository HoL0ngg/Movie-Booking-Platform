# ADR-009: Database Ownership per Microservice

- Status: Accepted
- Date: 2026-09-24

## Context

Shared schemas allow convenient joins but couple deployments, bypass service authorization/business rules, and permit one service to invalidate another service's invariants. Clear bounded contexts require enforceable data ownership.

## Decision

Every stateful microservice owns an isolated PostgreSQL database and role. Only that service's runtime and migrations receive access. Other services use versioned REST APIs or Kafka events and maintain deliberate local projections where necessary.

There are no cross-service SQL queries, writes, joins, views, triggers, foreign keys, shared JPA entities, repositories, or migration directories. A local development PostgreSQL instance may host several databases, but their credentials and grants remain isolated.

The API Gateway has no domain database. Technical libraries may share non-domain primitives, such as envelope serialization, without sharing mutable domain models.

## Consequences

- Ownership, authorization, and schema evolution are explicit.
- Cross-service queries require APIs/projections and accept documented consistency trade-offs.
- Referential integrity across services is handled through stable IDs, validation, events, and reconciliation rather than database foreign keys.
- Local setup needs multiple databases/roles and migration executions.

## Guardrails

- Every new table has one owning service and migration location.
- Database credentials grant no access to another service's database.
- Reporting/analytics needs do not justify production cross-service joins; they require a separate read/export design and decision.
- Contract/projection staleness and deletion/cancellation behavior are documented and tested.

