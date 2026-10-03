# Phase 1 Local Infrastructure

## Scope

Phase 1 supplies a reproducible local platform and buildable application shells. It deliberately does not implement authentication, domain tables, seat reservation, payment providers, Saga handlers, outbox relays, notifications, or frontend behavior.

## Pinned baseline

| Component | Version/image |
|---|---|
| Java target | 21 |
| Spring Boot | 4.1.1 |
| Spring AI (ai-service only) | 2.0.1 |
| Spring Cloud | 2025.1.3 |
| springdoc-openapi | 3.1.1 |
| PostgreSQL | `postgres:18.6-alpine3.24` |
| Kafka | `apache/kafka:4.3.1` |
| Redis | `redis:8.10.2-alpine3.23` |
| Prometheus | `prom/prometheus:v3.14.0` |
| Grafana | `grafana/grafana:13.2.2` |

Versions are pinned for reproducible local development. Upgrades require compatibility verification, tests, and an explicit pull request; architecture-changing upgrades require an ADR.

## Local ports and ownership

| Process | HTTP/host port | Owned database |
|---|---:|---|
| api-gateway | `8080` | none |
| auth-service | `8081` | `cinema_auth` on `54321` |
| movie-service | `8082` | `cinema_movie` on `54322` |
| cinema-service | `8083` | `cinema_cinema` on `54323` |
| booking-service | `8084` | `cinema_booking` on `54324` |
| payment-service | `8085` | `cinema_payment` on `54325` |
| notification-service | `8086` | `cinema_notification` on `54326` |
| ai-service | `8087` | none (stateless, optional) |
| Kafka | `29092` | n/a |
| Redis | `6379` | disposable cache only |
| Prometheus (optional) | `9090` | local volume |
| Grafana (optional) | `3000` | local volume |

Each local database has a distinct database, application role, credential, container, volume, and JDBC configuration. Managed databases retain service ownership and isolated credentials (ADR-009); no service queries or changes another service's data.

Redis persistence is disabled intentionally. Loss or flushing of Redis must never affect seat ownership or other authoritative state.

## Secrets and startup

Copy `.env.example` to the git-ignored `.env`, then set all blank password values. The Compose file refuses to start a database or Grafana when its required password is missing. Local Kafka uses plaintext listeners and the health/info endpoints are unauthenticated; bind these components only to loopback as configured and do not use this setup as production security configuration.

```powershell
Copy-Item .env.example .env
docker compose config
docker compose up -d
docker compose ps
```

Start optional monitoring with:

```powershell
docker compose --profile observability up -d
```

Application Prometheus export is currently removed: backend modules have no `micrometer-registry-prometheus` and do not expose `/actuator/prometheus`. The optional Compose profile is retained for future use; its monitoring provisioning files are currently absent from this checkout. Restore provisioning and the registry/exposure before using it to collect application metrics. Actuator health/info and trace logging remain available.

Stop containers without deleting state using `docker compose down`. Deleting named volumes is destructive and requires an explicit decision.

## Kafka topics

`kafka-init` idempotently creates:

- `cinema.showtime-events.v1`
- `cinema.booking-events.v1`
- `cinema.payment-events.v1`
- one corresponding `.quarantine.v1` topic for each event topic

The local broker has three partitions and replication factor one. Application skeletons configure manual offset acknowledgement, disabled consumer auto-commit, producer `acks=all`, and idempotent producer retries as a foundation. Those settings do not replace inbox idempotency, Transactional Outbox, or state-machine validation when consumers and producers are implemented.

## Application configuration

Every stateful service uses Hibernate schema validation (`ddl-auto=validate`) with SQL initialization disabled (`spring.sql.init.mode=never`). Flyway has been removed ([ADR-013](ADR/ADR-013-manual-database-schema-management.md)). Schema updates are managed manually on Supabase and mirrored in each owning service's `src/main/resources/db/schema.sql`. These files describe empty databases; do not replay them on existing data. Provision a fresh local database manually before starting its service. `ddl-auto=update` remains prohibited.

For a managed database, set the service-specific `*_DB_URL`, `*_DB_USER`, and `*_DB_PASSWORD` in the process environment; Spring Boot does not load `.env` automatically. Use the JDBC connection details for that service's isolated database/role. Supabase documents direct and session-pooler connections for persistent backends in its [connection guide](https://supabase.com/docs/guides/database/connecting-to-postgres). No deployment credentials or connection targets are changed by this cleanup.

Health and diagnostics:

- `/actuator/health` exposes readiness/liveness without sensitive details.
- `/actuator/info` identifies the application and phase.
- `/openapi/openapi.yaml` serves the static contract.
- `/swagger-ui.html` renders it during local development.

All business routes are fail-closed in Phase 1. A rejecting authentication provider prevents Spring Security from generating a development password that might be mistaken for an intentional auth design.

The gateway accepts a safe `X-Trace-Id` or creates one, sends it downstream, and returns it to the caller. MVC services perform the same validation and place the value in structured log context. This is correlation infrastructure, not a replacement for future OpenTelemetry propagation.

## Build and test

`ai-service` runs on the host like existing application modules; Docker Compose infrastructure is unchanged. Start it with `mvn -pl services/ai-service spring-boot:run`. Default configuration needs no AI runtime. Export `AI_CHAT_PROVIDER=ollama`, `AI_OLLAMA_BASE_URL`, and `AI_OLLAMA_MODEL` in the Maven process environment after provisioning the model separately. Spring Boot does not load `.env` automatically. This enables model beans only; no inference endpoint exists yet.

Docker-independent packaging checks: `mvn -pl gateway,services/ai-service -am package`. Frontend uses Playwright only: `npm test` from `frontend/`; if Chromium is missing, run `npx playwright install chromium`.

From the repository root:

```powershell
mvn package
```

Backend test dependencies are temporarily removed and the parent sets `maven.test.skip=true`; `mvn test` currently runs no tests. Existing test sources remain in place. Restore the dependencies as described in [README](../README.md#local-quick-start) and remove the skip property or pass `-Dmaven.test.skip=false` before running tests. Restored stateful service tests require Docker and real PostgreSQL Testcontainers, initialized with `db/schema.sql`, to verify mappings and constraints. Booking concurrency tests must also use PostgreSQL rather than H2.

## Production gaps

Before any production deployment, define secret management, TLS, authenticated Kafka listeners and ACLs, multi-broker replication, database backup/restore, resource limits, network policy, image scanning, immutable application images, alert rules, dashboards, and deployment-specific availability targets.
