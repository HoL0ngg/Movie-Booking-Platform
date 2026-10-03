# ADR-013: Manual Database Schema Management

- Status: Accepted
- Date: 2026-10-03
- Amends: ADR-002 schema tooling; preserves ADR-009 database ownership

## Context

The project owner has provisioned databases on Supabase and requests removing Flyway and managing changes there manually. Developers still need readable, service-owned SQL describing the original schema.

## Decision

Remove Flyway dependencies, startup configuration, and migration-specific test code from the six stateful services. Consolidate their existing SQL into `src/main/resources/db/schema.sql`; cinema includes the manager assignment table. Preserve all existing DDL, constraints, indexes, and extensions. Gateway and AI remain stateless.

Keep `spring.jpa.hibernate.ddl-auto=validate` and set `spring.sql.init.mode=never`. Application startup checks entity mappings but never creates or changes tables. The snapshots describe empty databases; they are references and disposable-test initialization scripts, not upgrade scripts for deployed databases.

Operators apply reviewed changes manually to each owning database and update the corresponding snapshot and entity mappings together. Existing data needs an explicit incremental change plan. No remote database changes are part of this repository cleanup. Existing database history tables are left untouched.

Database and credential isolation required by ADR-009 remains in force. This decision does not approve a shared service database or cross-service SQL. Runtime JDBC connections continue to use each service's existing environment variables; no project URLs or credentials are committed.

## Consequences

- Builds no longer resolve Flyway and startup no longer performs migrations or checks migration history.
- There is no automatic migration ordering, checksum verification, or upgrade tracking; operators must maintain deployment order and keep reference SQL synchronized.
- Hibernate validation detects mapping mismatches, but does not prove that every unique/check/exclusion constraint or index exists remotely. Those must be verified separately before enabling critical business workflows.
- A fresh local database must be provisioned manually before starting a stateful service.
- Backend tests remain temporarily paused. Their retained PostgreSQL containers initialize from `db/schema.sql` when testing is restored; critical locking, uniqueness, and idempotency assertions remain. The obsolete Flyway version-upgrade test is removed.
