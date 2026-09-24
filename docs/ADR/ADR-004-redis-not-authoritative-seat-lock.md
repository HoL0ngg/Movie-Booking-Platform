# ADR-004: Redis Is Not an Authoritative Distributed Seat Lock

- Status: Accepted
- Date: 2026-09-24

## Context

Redis can provide low-latency coordination and TTLs, but lease expiry, failover, clock/timing issues, network partitions, cache eviction, and split ownership between Redis and PostgreSQL make it unsafe as the exclusive seat-booking authority. The durable seat state already requires a PostgreSQL commit.

## Decision

Do not use Redis locks as the correctness mechanism for seat reservation, confirmation, or release. PostgreSQL transactions and row locks are authoritative.

Redis may support disposable caching, rate limiting, TTL hints, and transient notifications. Every Redis-assisted path must remain logically correct when Redis is empty, stale, or unavailable. A Redis key never grants ownership and never authorizes a state transition without PostgreSQL validation.

## Consequences

- There is one durable serialization point and no lock/state split-brain to reconcile.
- Some operations may have higher database contention than a cache-first design.
- Redis can be tuned or removed without migrating booking ownership.
- Availability caches are explicitly advisory and may be stale.

## Guardrails

- No irreplaceable reservation, booking, payment, idempotency, or expiry state exists only in Redis.
- Tests clear/disable Redis and still prove the one-winner booking invariant.
- JVM locks, frontend state, and in-memory maps are equally non-authoritative.
- Any future Redis locking proposal must supersede this ADR and prove that it does not weaken PostgreSQL authority; performance alone is insufficient.

