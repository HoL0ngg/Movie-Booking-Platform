# ADR-011: Optional AI Assistance Service

- Status: Accepted
- Date: 2026-10-03
- Extends: ADR-001 deployable set; preserves ADR-009 ownership
- Updated: 2026-10-15 — OpenRouter OpenAI-compatible API only, model `openrouter/free`

## Context

The project owner requests an independent AI Service using Spring AI. Cinema assistance and recommendations have separate prompt/model evolution, provider resource requirements, privacy concerns, and failure behavior from catalog, booking, and payment. This capability remains optional and non-authoritative.

## Decision

Add `services/ai-service` as an independently runnable Maven module on port 8087. Use Spring AI 2.0.1, compatible with Spring Boot 4.1.1, with only the OpenAI model starter. Default `AI_CHAT_PROVIDER=openai` enables OpenRouter and requires `OPENROUTER_API_KEY`. Explicit `AI_CHAT_PROVIDER=none` permits startup without a provider or credentials. `AI_CHAT_PROVIDER=openai` configures model `openrouter/free` through the OpenRouter OpenAI-compatible API using server-side `OPENROUTER_API_KEY`. AI service imports the root or module-local git-ignored `.env` through Spring Boot Config Data, with environment variables taking precedence. No other provider is configured; the free router selects the underlying model. Startup never performs inference. OpenRouter uses base URL `https://openrouter.ai/api/v1`, a 45-second SDK timeout and no SDK retries. `OPENROUTER_MODEL` defaults to `openrouter/free`; `API_KEY` remains a compatibility fallback for existing local configuration. Non-chat OpenAI models are disabled. The gateway grants only the AI route a 60-second response timeout; other routes retain 5 seconds. Provider/network failures return a safe `503 AI_PROVIDER_UNAVAILABLE` response instead of leaking provider errors.

The initial deliverable is a technical skeleton matching existing services: health/info, static OpenAPI, trace IDs, and fail-closed security. A temporary stateless `POST /api/v1/ai/chat` conversation endpoint is now available only when `AI_CHAT_PROVIDER=openai`, using the OpenRouter OpenAI-compatible API through Spring AI. The prototype accepts a bounded client-supplied history and does not persist data, query service databases, or invoke booking/payment operations. The endpoint is temporarily permitted without authentication for frontend integration; production authorization and request limits remain required before launch. Provider integrations remain inside this service.

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
- [Spring AI OpenAI configuration](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)
- [OpenRouter quickstart](https://openrouter.ai/docs/quickstart)
