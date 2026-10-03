---
name: database-design
description: Design or review PostgreSQL schemas, manual schema changes, constraints, indexes, transactions, outbox/inbox tables, and service data ownership for the Cinema Booking Platform.
---

# Database Design

## When to use

Use this skill for schemas, entities, repositories, SQL, indexes, constraints, manual schema changes, transaction boundaries, or persistence reviews. Also use it when an event projection or idempotency record changes database state. Read `AGENTS.md` and `docs/database-design.md` first.

## Architectural rules

- Each microservice exclusively owns its database and credentials. Cross-service SQL, foreign keys, views, and joins are prohibited.
- PostgreSQL in `booking-service` is the authoritative source for seat ownership. Redis is disposable support infrastructure, never the source of booking truth.
- Enforce invariants with PostgreSQL primary keys, unique constraints, foreign keys inside one service, check constraints, and transactions in addition to application validation.
- Critical business mutations and their outbox rows share one local transaction.
- Persist durable idempotency/inbox records whenever retries or at-least-once delivery can repeat an operation.

## Implementation rules

- Apply schema changes manually to the owning managed database and update its `src/main/resources/db/schema.sql` snapshot (ADR-013). Application SQL initialization is disabled. Persistent configurations use Hibernate schema validation, never automatic update. Do not replay full snapshots on an existing database.
- Make identifiers, timestamps, money, and enums explicit: prefer UUID/ULID-compatible IDs, `timestamptz` in UTC, integer minor currency units plus ISO currency, and constrained state values.
- Define nullability, defaults, checks, unique keys, foreign keys, and query-driven indexes deliberately. Index foreign keys and hot predicates after validating query shape.
- Keep transactions short; perform no remote HTTP or Kafka calls while holding database locks.
- For `showtime_seats`, use `(showtime_id, seat_id)` as the primary identity and lock rows pessimistically in a deterministic seat order for reservation transitions.
- Treat a numeric `version` as audit/optimistic-conflict support, not as a substitute for required row locking.
- Use `INSERT ... ON CONFLICT`, unique constraints, or locked state transitions to make idempotent behavior atomic; a prior read alone is not sufficient.
- Migration plans must address rollback/forward-fix, existing data, online compatibility, and deployment order.

## Forbidden patterns

- `spring.jpa.hibernate.ddl-auto=update` in persistent environments.
- H2 as proof of PostgreSQL locking, isolation, constraint, or SQL behavior.
- Check-then-write concurrency logic without a transaction and appropriate lock/constraint.
- Database-enforced relationships to another service's tables.
- Floating-point storage for money, local-time timestamps for domain events, or secrets in migrations.
- Destructive migrations without an explicit safe rollout/data-retention plan.
- Publishing a critical Kafka event after commit without an outbox row written in that commit.

## Testing requirements

- When backend testing is restored, initialize disposable PostgreSQL Testcontainers with the owning service's `db/schema.sql` and run repository integration tests.
- Verify all important constraints with both accepted and rejected rows.
- Test migrations from the previously released schema, not only clean database creation.
- For booking tables, run contention, expiration, idempotency, rollback, and deadlock/retry tests against real PostgreSQL.
- Verify outbox/inbox uniqueness and crash-window behavior.

## Review checklist

- [ ] The owning service and database are explicit.
- [ ] Primary, unique, foreign-key, check, and nullability constraints protect stated invariants.
- [ ] Queries used for locking and hot paths have appropriate indexes.
- [ ] Transaction boundaries contain all required state and outbox writes.
- [ ] No remote call occurs while locks are held.
- [ ] Migration ordering and backward compatibility are safe.
- [ ] Idempotency is enforced atomically in PostgreSQL.
- [ ] Real PostgreSQL tests cover database-specific behavior.
