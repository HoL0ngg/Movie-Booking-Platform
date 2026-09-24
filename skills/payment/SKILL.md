---
name: payment
description: Design, implement, or review payment attempts, provider adapters, webhooks, idempotency, refunds, and booking-payment Saga behavior.
---

# Payment

## When to use

Use this skill for `payment-service`, checkout/payment APIs, provider adapters, callbacks/webhooks, refunds, reconciliation, or payment events consumed by `booking-service`. Read `AGENTS.md`, `docs/payment-flow.md`, and `docs/event-catalog.md` first.

## Architectural rules

- `payment-service` is the sole owner of payment-provider integration, payment attempts, provider transaction/event IDs, and refunds.
- Providers sit behind an internal provider interface; VNPay, MoMo, ZaloPay, and Stripe are future adapters, not domain dependencies.
- The booking/payment workflow is a Saga. No distributed transaction spans payment and booking databases.
- Critical payment mutations and emitted events share one local transaction through the Transactional Outbox.
- Provider callbacks and Kafka messages are at-least-once inputs and must be durably idempotent.
- Only a verified provider server-to-server result or authoritative provider query can establish payment success. Frontend redirects are informational.

## Implementation rules

- Persist payment ID, reservation/booking reference, request idempotency key and request hash, amount in minor units, ISO currency, provider, provider transaction ID, provider event ID, state, failure code, and timestamps.
- Enforce uniqueness for scoped idempotency keys and non-null provider transaction/event identifiers where applicable.
- Validate webhook authenticity, provider/account, amount, currency, merchant reference, and allowed transition before acknowledging success.
- Record the provider event and transition/payment outbox atomically. A duplicate event returns a successful acknowledgement without repeating effects.
- Model transitions explicitly, including requested/pending, succeeded, failed, refund pending, and refunded. Terminal results cannot be overwritten by contradictory duplicates; suspicious contradictions require reconciliation.
- Use bounded provider timeouts and retry only operations proven safe by a provider idempotency key or status query.
- A late success for an expired/cancelled booking remains a successful payment record but enters the idempotent refund compensation path; it never sells released seats.
- Do not log credentials, signatures, tokens, full sensitive payloads, or payment secrets. Retain only fields needed for audit and reconciliation.

## Forbidden patterns

- Calling a provider directly from `booking-service`, the gateway, or the frontend.
- Treating a browser redirect/query parameter as payment confirmation.
- Generating a new idempotency key on every internal retry.
- Check-then-insert duplicate handling without a unique constraint/transaction.
- Emitting `PaymentSucceeded`, `PaymentFailed`, or refund events outside an outbox transaction.
- Storing money as floating point or comparing unverified provider amounts loosely.
- Automatically retrying an ambiguous charge request without a provider-safe idempotency/reconciliation mechanism.

## Testing requirements

- Use a deterministic fake provider adapter; do not contact real providers in automated tests.
- Test repeated API requests, concurrent same-key requests, key/payload mismatch, duplicate and reordered webhooks, bad signatures, wrong amounts/currencies, contradictory terminal callbacks, and provider timeouts.
- Test crash windows around local commit, outbox publication, Kafka redelivery, and webhook acknowledgement.
- Test successful refund, duplicate refund request, refund failure/retry, and late payment after hold expiry.
- Verify secrets and sensitive payloads are absent from logs.

## Review checklist

- [ ] Provider code is isolated behind the provider abstraction.
- [ ] Every externally repeatable command has durable idempotency.
- [ ] Webhook signature and business fields are verified before transition.
- [ ] Provider transaction/event IDs have correct uniqueness constraints.
- [ ] State transitions reject duplicate, stale, or contradictory updates safely.
- [ ] Payment/outbox or refund/outbox changes commit atomically.
- [ ] Late success follows compensation and cannot claim released seats.
- [ ] No frontend response is treated as authoritative confirmation.

