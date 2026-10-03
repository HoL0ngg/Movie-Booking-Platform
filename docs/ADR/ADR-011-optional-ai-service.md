# ADR-011: Optional AI Assistance Service

- Status: Accepted
- Date: 2026-10-03
- Extends: ADR-001 deployable set; preserves ADR-009 ownership

## Context

The project owner requests an independent AI Service using Spring AI. Cinema assistance and recommendations have separate prompt/model evolution, provider resource requirements, privacy concerns, and failure behavior from catalog, booking, and payment. This capability remains optional and non-authoritative.

## Decision

Add `services/ai-service` as an independently runnable Maven module on port 8087. Use Spring AI 2.0.1, compatible with Spring Boot 4.1.1, and its Ollama model starter. Default `AI_CHAT_PROVIDER=none` permits startup without a provider. `ollama` configures a local pre-provisioned model. Startup never downloads models or performs inference.

The initial deliverable is a technical skeleton matching existing services: health/metrics, static OpenAPI, trace IDs, and fail-closed security. Gateway reserves `/api/v1/ai/**`; no business endpoint exists or is authorized yet. Define the assistance contract, service authorization, request limits, inference timeouts, and errors before exposing inference. Provider integrations remain inside this service.

Initially the service owns no persistent data and uses no PostgreSQL, Flyway, Kafka, Redis, vector store, or chat memory. Future persistence requires a service-owned schema and migration decision. Catalog context comes from owner APIs or deliberate event projections, never cross-service SQL.

## Consequences and guardrails

- Prompts/provider lifecycle evolve independently, at the cost of another deployable.
- AI downtime cannot prevent discovery, booking, checkout, payment, or ticket delivery.
- Model output is a suggestion, never proof of availability, ownership, price, payment, or authorization.
- No model tools mutate reservations, bookings, payments, or refunds in this skeleton.
- Credentials remain server-side. Never send secrets, payment data, or unnecessary personal data to a model; do not log prompts/responses by default.
- Configuration/security tests run without live inference, credentials, or model downloads.

## References

- [Spring AI compatibility](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [Ollama configuration](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
