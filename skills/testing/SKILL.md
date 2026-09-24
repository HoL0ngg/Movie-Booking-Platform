---
name: testing
description: Plan, implement, or review backend, frontend, contract, integration, concurrency, and end-to-end tests for the Cinema Booking Platform.
---

# Testing

## When to use

Use this skill when adding tests, choosing test boundaries, changing critical behavior, investigating race/failure cases, or reviewing evidence for completion. Read `AGENTS.md` and the domain-specific skill for the affected invariant.

## Architectural rules

- Tests must prove externally meaningful behavior and protected invariants, not implementation details alone.
- PostgreSQL-specific transactions, locks, migrations, indexes, and constraints require the supported real PostgreSQL version through Testcontainers. H2 is not evidence for them.
- Kafka/event tests assume at-least-once delivery and verify durable idempotency, outbox/inbox behavior, replay, and offset-after-commit.
- The critical E2E journey is movie -> cinema -> showtime -> seats -> reservation -> checkout -> payment -> ticket.
- Security, observability, and accessibility are test concerns, not deferred manual checks.

## Implementation rules

- Use focused unit tests for pure domain transitions and validation; integration tests for persistence, messaging, security boundaries, and adapters; Playwright for critical browser journeys.
- Make concurrency starts deliberate with a barrier/latch, use independent transactions/connections, collect every result, and assert final database state.
- The same-seat test launches 100 attempts for one `(showtimeId, seatId)` and requires exactly one success and 99 clean conflicts.
- Make time controllable for expiration/payment timeout tests without replacing PostgreSQL locking semantics.
- Use deterministic fake payment providers and controllable message publication; never call live providers in automated tests.
- Test both the response/event and committed state. Include correlation/idempotency identifiers in assertions where relevant.
- Keep retries in the system under test bounded; tests must fail rather than hang. Preserve useful diagnostics while excluding secrets.

## Forbidden patterns

- H2 or mocks as the only evidence for concurrency, transaction, migration, constraint, or PostgreSQL SQL behavior.
- Sleep-based race tests when a barrier, clock, poll-with-timeout, or observable condition is available.
- Tests that accept "at least one" success for exclusive seat ownership.
- Mocking away the exact boundary the change is meant to validate.
- Live payment-provider calls, shared mutable test data, order-dependent tests, or assertions only on HTTP status.
- Disabling flaky critical tests instead of finding the race or making synchronization deterministic.

## Testing requirements

- Booking state changes: unit transition tests plus Testcontainers integration and required concurrency cases.
- Payment changes: idempotency, webhook verification/duplication, amount validation, ambiguous outcome, late success, and refund compensation.
- Event changes: schema contracts, outbox replay, duplicate consumer delivery, poison handling, and restart recovery.
- Database changes: clean migration, upgrade migration, constraint behavior, and representative query/locking behavior.
- Frontend changes: useful component tests, accessibility checks, and Playwright coverage for changed critical journeys.
- Authorization changes: accepted and rejected identities at the service boundary.

## Review checklist

- [ ] Each changed invariant has a test at the lowest meaningful level and at real boundaries where needed.
- [ ] PostgreSQL behavior is tested in Testcontainers, not H2.
- [ ] Concurrency tests coordinate starts and assert final durable state.
- [ ] Duplicate/retry/crash windows and compensation paths are covered.
- [ ] Tests are deterministic, isolated, time-bounded, and diagnostic.
- [ ] Security, sensitive logging, trace IDs, and accessibility are covered where relevant.
- [ ] The reported commands/results match what actually ran.

