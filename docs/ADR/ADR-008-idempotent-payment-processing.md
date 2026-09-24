# ADR-008: Idempotent Payment Processing

- Status: Accepted
- Date: 2026-09-24

## Context

Users retry requests, networks time out, Kafka redelivers, and providers repeat/reorder webhooks. Retrying an ambiguous charge incorrectly can double-charge a customer; processing a callback twice can double-confirm or double-refund. Browser redirects are forgeable and not authoritative.

## Decision

Payment API/Saga requests, provider create operations, webhooks, and refunds use durable scoped idempotency. Persist request keys and canonical hashes, payment attempts, provider transaction IDs, provider event IDs, merchant references, state, amount/currency, and timestamps with appropriate unique constraints.

Only verified server-to-server provider results or an authoritative provider query establish success. The service verifies signature, provider/account, merchant reference, exact amount/currency, and allowed state transition. The provider event record, payment/refund transition, and result outbox event commit together.

Ambiguous provider calls are queried or retried only with a provider-supported stable idempotency/reference. Frontend redirects trigger status lookup only.

## Consequences

- Duplicate commands/callbacks converge on one durable result.
- Provider-specific idempotency and reconciliation capabilities must be understood before integration.
- More data and state transitions are retained for audit and support.
- Contradictory terminal callbacks require alerting/reconciliation rather than blind overwrite.

## Guardrails

- A key reused with a different request hash is rejected.
- New idempotency identities are not generated for transport/internal retries of the same intent.
- Money uses integer minor units and ISO currency; success requires an exact match.
- Late success follows refund compensation and cannot sell released seats.
- Real provider calls are excluded from automated tests; deterministic adapters model retries and ambiguity.

