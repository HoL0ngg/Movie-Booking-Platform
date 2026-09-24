# ADR-007: Transactional Outbox for Reliable Event Publishing

- Status: Accepted
- Date: 2026-09-24

## Context

Committing domain state and then publishing Kafka creates a crash window where state changes but its event is lost. Publishing first creates the inverse inconsistency if the database transaction fails. PostgreSQL and Kafka do not share a platform transaction.

## Decision

Every critical producer writes the business mutation and immutable outbox event in the same local PostgreSQL transaction. A separate relay claims unpublished rows, publishes the stored event, waits for broker acknowledgement, and records publication status.

The relay is at-least-once: if it crashes after Kafka acknowledgement and before its local mark, it republishes the same `eventId`. State-changing consumers therefore write a unique `(consumer_name, event_id)` inbox record in the same transaction as local effects and derived outbox records, then acknowledge the Kafka offset after commit.

## Consequences

- A committed critical state change has a durable publication intent.
- Duplicates are expected and harmless only when consumers and downstream operations are idempotent.
- Outbox/inbox storage, cleanup, dispatch latency, retries, and monitoring add operational work.
- Event publication is eventually consistent with local commit rather than immediate.

## Guardrails

- Direct best-effort publish after commit is prohibited for critical domain events.
- Retries preserve the event ID, semantic payload, aggregate key, and original occurrence time.
- Relays use bounded safe multi-worker claiming and expose backlog age/failure metrics.
- Inbox insertion before or outside the local effect transaction is insufficient.
- Offset acknowledgement occurs only after a successful local commit.

