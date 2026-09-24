# ADR-005: Kafka for Asynchronous Domain Events

- Status: Accepted
- Date: 2026-09-24

## Context

Booking, payment, cinema scheduling, and notification need decoupled propagation and recovery without synchronous service chains. The platform also needs replayable operational evidence and per-aggregate ordering where practical. Message delivery can repeat or be delayed.

## Decision

Use Kafka for asynchronous domain events and Saga requests/results. Initial topics are bounded-context topics for showtime, booking, and payment events. Messages carry the standard envelope (`eventId`, `eventType`, `eventVersion`, `aggregateId`, `occurredAt`, `traceId`, `payload`) and use `aggregateId` as the key when aggregate order matters.

The contract is at-least-once. Producers use the Transactional Outbox; state-changing consumers use a durable inbox/processed-event record and acknowledge offsets only after local commit. Event schemas evolve compatibly and explicitly by version.

REST remains appropriate for immediate queries/commands. Kafka does not replace direct responses needed by users.

## Consequences

- Services are decoupled in availability and can recover/replay with explicit contracts.
- Duplicate, late, stale, and poison messages must be handled deliberately.
- Global ordering is unavailable; only partition-local order is meaningful.
- Topic ACLs, lag, retention, schema compatibility, quarantine, and outbox backlog require operations support.

## Guardrails

- Never assume exactly-once business processing, even if broker features reduce duplicates.
- Event payloads contain no secrets or unnecessary personal/provider data.
- Bounded retries and quarantine prevent one permanent failure blocking a partition indefinitely.
- Event names, producers, consumers, keys, and meanings remain cataloged in `docs/event-catalog.md`.

