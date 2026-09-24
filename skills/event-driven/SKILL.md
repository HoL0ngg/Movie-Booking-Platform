---
name: event-driven
description: Design or change Kafka event contracts, producers, consumers, Saga messaging, Transactional Outbox, inbox deduplication, and delivery semantics.
---

# Event-Driven Architecture

## When to use

Use this skill when adding or changing Kafka topics, event schemas, outbox publishers, consumers, retries, dead-letter behavior, projections, or Saga messages. Read `AGENTS.md`, `docs/event-catalog.md`, and the relevant workflow document first.

## Architectural rules

- Kafka delivery is at-least-once. Duplicates are normal; exactly-once business processing is never assumed.
- Each event envelope contains `eventId`, `eventType`, `eventVersion`, `aggregateId`, `occurredAt`, `traceId`, and `payload`.
- A service emits only facts/requests it owns. Consumers do not infer permission to mutate another service's data.
- Critical local state and its outbox event are written in one database transaction.
- A Saga coordinates booking/payment success and compensation without cross-service transactions. `booking-service` owns the durable booking Saga state; it does not own payment-provider state.
- Partition by a stable aggregate key when per-aggregate order matters. Do not assume global order or cross-topic order.

## Implementation rules

- Outbox dispatchers claim bounded rows safely, publish with `aggregateId` as the key, mark published only after broker acknowledgement, and tolerate republishing after crashes.
- Consumers insert a unique `(consumer_name, event_id)` inbox/processed record in the same local transaction as side effects and derived outbox writes. Commit before acknowledging the Kafka offset.
- Make retries bounded with backoff. Separate transient infrastructure errors from permanent schema/business failures; quarantine poison messages with enough non-sensitive diagnostic context.
- Version schemas explicitly. Prefer backward-compatible additive changes; keep old consumers valid during rolling deployment. Breaking meaning requires a new version and migration plan.
- Validate required envelope/payload fields and authorization/trust at ingress. Propagate the original `traceId`; create one only when absent at a trusted boundary.
- Keep handlers thin: deserialize/validate, call an application command, commit, then acknowledge.
- Document producer, consumers, triggering transaction, key, payload semantics, idempotency behavior, and privacy classification in the event catalog.

## Forbidden patterns

- Database commit followed by best-effort direct publish for a critical event.
- Non-idempotent side effects, memory-only deduplication, or marking processed before business effects commit.
- Acknowledging Kafka before the local transaction commits.
- Reusing an `eventId` for a different fact or generating a new event ID when replaying the same outbox row.
- Assuming duplicates cannot occur because producer idempotence or Kafka transactions are enabled.
- Silently changing an existing event's meaning or placing secrets/sensitive provider payloads in events.
- Infinite retries that block a partition without operational visibility.

## Testing requirements

- Contract-test serialization and every supported event version.
- Integration-test outbox creation, publication acknowledgement, replay after crash, inbox uniqueness, duplicate delivery, and offset-after-commit behavior.
- Test reordered events and stale transitions where the workflow permits them.
- Test Saga success, each compensation path, duplicate success/failure results, and restart recovery.
- Verify partition keys preserve required per-aggregate order and logs/telemetry contain `eventId` and `traceId` without secrets.

## Review checklist

- [ ] The producer owns the event and its triggering state change.
- [ ] The envelope, version, partition key, and payload semantics are documented.
- [ ] Business change and outbox insert are one transaction.
- [ ] Consumer effects and inbox insert are one transaction.
- [ ] Offset acknowledgement happens only after commit.
- [ ] Duplicate, stale, reordered, poison, and replay behavior is defined.
- [ ] Saga compensation is explicit and idempotent.
- [ ] Schema evolution remains compatible during rolling deployment.

