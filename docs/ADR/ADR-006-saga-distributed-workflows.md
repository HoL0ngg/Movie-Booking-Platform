# ADR-006: Saga for Distributed Workflows

- Status: Accepted
- Date: 2026-09-24

## Context

A booking spans seat ownership in `booking-service` and provider payment state in `payment-service`. These databases and external providers cannot participate safely in one atomic transaction. Failures can occur after either local commit, and payment confirmation can arrive after a hold expires.

## Decision

Use an event-driven Saga for booking/payment workflows. `booking-service` coordinates the Saga with durable local reservation/booking state: it requests payment, consumes verified results, confirms seats when still valid, releases them on definitive failure, and requests refund compensation when successful payment cannot be fulfilled.

`payment-service` independently owns payment/refund state and provider operations. It consumes stable requests and publishes authoritative results. Communication uses Kafka events backed by each service's Transactional Outbox. No new central Saga service is introduced.

## Consequences

- Each local transition is atomic, but cross-service state is eventually consistent.
- Compensation is a business action, not a rollback; refunds can fail and require reconciliation.
- Users need explicit pending states and status polling/recovery.
- Durable Saga state, timeout policy, observability, and idempotency are mandatory.

## Guardrails

- A late `PaymentSucceeded` never resurrects expired/released seats; it creates one `RefundRequested` compensation.
- Duplicate or stale results cannot reverse a legal terminal state.
- No XA/two-phase commit or synchronous "both commits succeed" protocol is allowed.
- Each success, failure, timeout, and compensation path is documented and tested, including service restarts and duplicate events.

