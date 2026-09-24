# ADR-010: Monorepo for Initial Development

- Status: Accepted
- Date: 2026-09-24

## Context

The initial team must establish consistent service skeletons, infrastructure, contracts, documentation, and engineering rules across several deployables. Separate repositories would add coordination and tooling overhead before release cadence and team ownership demand it.

## Decision

Keep frontend, gateway, services, infrastructure definitions, documentation, and project-local Codex skills in one repository for initial development. Each deployable remains independently buildable, testable, configurable, migratable, and releasable within its directory.

The monorepo does not imply shared runtime databases, shared domain models, lockstep deployment, or permission to reach across service internals. Contract artifacts may be shared deliberately; generated clients or compatibility tests are preferred over importing another service's implementation.

## Consequences

- Cross-cutting architectural changes and local environment setup are easier to review atomically.
- CI can validate contracts and affected services together.
- Build/test scope and ownership rules will be needed as the repository grows.
- Accidental code/data coupling is easier unless directory and dependency boundaries are enforced.

## Guardrails

- Each service owns its build, migrations, tests, configuration, and runtime artifact.
- CI eventually detects forbidden dependencies and runs affected plus contract/integration checks.
- Secrets and local credentials are never committed.
- A future repository split requires an ADR and must preserve contracts, history, ownership, and independent deployment.

