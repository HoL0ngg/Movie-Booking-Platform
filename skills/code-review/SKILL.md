---
name: code-review
description: Review Cinema Booking Platform code, schemas, architecture, tests, and documentation for correctness, contradictions, regressions, and missing evidence.
---

# Code Review

## When to use

Use this skill for an explicit review or before declaring a cross-cutting phase complete. Read `AGENTS.md`, the relevant domain skills, changed files, nearby contracts/docs, and test evidence. Review the diff when Git is available; otherwise inventory and inspect all in-scope files.

## Architectural rules

- Rank concerns by the repository decision priority: correctness, data consistency, security, maintainability, observability, performance, then visual effects.
- Treat the following as release blockers: possible double booking; payment false-positive/double charge; lost critical event; non-idempotent at-least-once consumer; cross-service database access; Redis as booking authority; missing transaction/outbox boundary; trusted frontend payment result.
- Require one clear owner for every datum and transition. Distributed workflows use Saga compensation, not cross-service transactions.
- Documentation, API/event contracts, schemas, state machines, and tests must describe the same behavior.

## Implementation rules

- Trace each critical command from ingress through authorization, validation, transaction/locks, constraints, outbox, consumer idempotency, response, and observability.
- For seat operations, verify normalized deterministic lock order, all-or-nothing validation, explicit legal state transition, short transaction scope, expiry races, and final database assertions.
- For payment, trace idempotency key/request hash, provider identity, signature and amount/currency validation, unique provider IDs, terminal transition rules, late success, and refund compensation.
- For events, verify envelope/version/key, outbox atomicity, broker-ack marking, inbox atomicity, offset-after-commit, replay, schema evolution, and non-sensitive payloads.
- Distinguish facts from assumptions. Cite file and line when reporting findings. Prefer the smallest coherent correction that preserves boundaries.
- If authorized to change files, fix confirmed inconsistencies and re-run relevant validation; otherwise report them with impact and a concrete recommendation.

## Forbidden patterns

- Approving based only on happy-path unit tests or document presence.
- Treating lints, compilation, producer idempotence, or Kafka "exactly once" settings as proof of business correctness.
- Ignoring docs/schema divergence, migration deployment order, failure paths, duplicate delivery, or authorization because code is not yet wired.
- Broad refactors unrelated to a finding.
- Reporting style preferences as defects or hiding unresolved critical risk in a summary.

## Testing requirements

- Confirm required tests exist for changed invariants and use the correct real boundary: Testcontainers PostgreSQL for locking/migrations, Kafka/outbox integration for delivery, fake providers for payment, Playwright for critical UI journeys.
- Inspect assertions for exact outcomes and final durable state, especially the 100-contender one-seat test.
- Run the narrowest relevant checks, then broader checks proportional to risk. Report skipped/unavailable checks explicitly.
- For documentation-only phases, validate links, Mermaid fences, contract names, state transitions, ownership tables, event producer/consumer mappings, and contradictions with `AGENTS.md`.

## Review checklist

- [ ] The change respects `AGENTS.md` and every applicable skill.
- [ ] Service and database ownership are unambiguous; no cross-service SQL appears.
- [ ] Booking and payment state machines have only explicit legal transitions.
- [ ] Locking order, idempotency, Saga compensation, outbox, and at-least-once handling agree across artifacts.
- [ ] API/event names, versions, IDs, errors, and states are consistent.
- [ ] Security, privacy, traceability, accessibility, and operability are addressed.
- [ ] Tests prove critical invariants at realistic boundaries.
- [ ] Findings are fixed or clearly reported with severity and evidence.

