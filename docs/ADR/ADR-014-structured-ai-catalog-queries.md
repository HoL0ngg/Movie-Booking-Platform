# ADR-014: Structured AI Queries with Java Orchestration

- Status: Accepted
- Date: 2026-11-13
- Refines: ADR-011 (replaces fixed prompt grounding, caching and provider timeout)

## Context

Users ask for movies, cinemas and schedules on specific future dates. The previous fixed today/tomorrow prompt snapshot cannot answer these queries reliably. The owner requests structured output followed by Java-controlled API calls.

## Decision

Keep the existing stateless `ai-service` and `POST /api/v1/ai/chat` request/response contract. Make one Spring AI call per turn using `BeanOutputConverter<CatalogQuery>` schema instructions. Parse the response with a strict Jackson mapper into the DTO, rejecting unknown properties, invalid enums/dates and trailing tokens without logging the raw output. Native JSON mode and model tool calling are not required. Provider errors retain `503 AI_PROVIDER_UNAVAILABLE`; invalid structured output returns a safe clarification with no owner calls and no model retries.

Java validates intent, filter length and control characters, date ordering and time ranges. Supported intents are MOVIES, CINEMAS, SHOWTIMES, GREETING, CLARIFY and UNSUPPORTED. Wrong-intent filters are rejected rather than silently discarded. History remains bounded client input, used for reference resolution, never as authoritative catalog data. Model extraction can still misunderstand a question; enum/schema validation does not prove semantic understanding.

Java alone calls these fixed public GET routes, using encoded query values:

- `movie-service`: `/api/v1/movies?status=&query=&genre=` for published movies and real IDs.
- `cinema-service`: `/api/v1/cinemas` for city/name matching against real cinemas, including normalized Vietnamese city aliases.
- `cinema-service`: `/api/v1/showtimes?date=&movieId=` once per requested day. `movieId` is supplied only when exactly one real movie matches; otherwise Java filters the returned schedule against real movie/cinema IDs.

MOVIES loads only movies; CINEMAS loads only cinemas. SHOWTIMES loads movies, cinemas and at most eight daily schedules, stopping early on no matches. Dates default to today in `ai.catalog.timezone` (Asia/Ho_Chi_Minh); dates must be today or later, within 90 days, and span at most eight inclusive days. Time filters are inclusive and evaluated in each cinema's timezone. Past-start, unpublished and unknown-reference showtimes are excluded. Cross-midnight ranges require clarification. No URL, SQL, remote ID or operation supplied by a model is executed.

Java renders the factual answer directly, preserving the existing `{message}` response and avoiding a second inference call. Movie suggestions match requested title/genre/status; they are not a personalized or semantic ranking system. Results are bounded (40 movies, 20 cinemas, 80 showtimes by default), also fit the 4000-character history-message limit, and disclose truncation. Catalog reads are fresh per query, with no shared prompt cache. Owner failures return an explicit unavailable response, discarding partial schedule results. Empty results mean only no matching published data.

Use a 20-second provider timeout without retries, and 1-second connect / 2-second read timeouts per owner request. At most ten owner requests occur per turn. The gateway retains its 60-second AI timeout. These individual timeouts bound normal network waiting, not an absolute wall-clock SLA; DNS/runtime delays can exceed it.

## Consequences

- No new service, database, schema, Kafka role, vector store or persistent conversation memory.
- No booking/payment/seat-availability calls or mutations. Java cannot execute such an operation even if extraction is wrong.
- Model output controls bounded public search filters only; factual answer content comes from owning APIs.
- Chat remains temporarily unauthenticated; production authentication and request limits are still outstanding.
- AI module restores focused JUnit/Spring Boot tests only. Run `mvn -pl services/ai-service test -Dmaven.test.skip=false`; no live provider, model download, Docker or database is needed.

## Verification scope

Tests cover structured parsing, malformed/unexpected output, history, bounded query dispatch, date/time/city filtering, unsupported intents, owner failure versus empty results, public adapter contracts, and disabled/fake-key startup without inference. They do not measure live-model Vietnamese extraction quality. A separately authorized live evaluation is needed before production use.
