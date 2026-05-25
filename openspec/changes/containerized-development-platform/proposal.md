## Why

MedKernel needs a stable, persistent development deployment that exercises its PostgreSQL-first
runtime and can be reproduced on later servers. The repository currently provides offline
installation assets but no Docker-based development platform, while local development still uses
an in-memory H2 profile and cannot exercise Neo4j or optional Dify integration.

## What Changes

- Add repository-owned Docker deployment assets for MedKernel PostgreSQL, backend, frontend
  gateway, Neo4j projection, Prometheus, and Grafana.
- Add a container runtime profile that connects the backend to PostgreSQL and applies the
  existing PostgreSQL Flyway migrations on startup.
- Add scripts and documentation for `core` and `full` startup modes, health verification,
  PostgreSQL backup/restore, and deployment on another Docker-enabled server.
- Pin and bootstrap the official Dify `v1.14.0` self-hosted Compose distribution under the
  external runtime root without merging its internal database with MedKernel's database.
- Establish `/Users/zhikunzheng/work/medkernel/runtime/` as the local persistent state root,
  while keeping the deployment portable via `MEDKERNEL_RUNTIME_ROOT`.

## Capabilities

### New Capabilities

- `containerized-development-platform`: Persistent Docker development deployment, service
  boundaries, operational scripts, and migration path for later servers.

### Modified Capabilities

None. This change adds an operational capability without altering the existing product identity
or change-planning requirements.

## Impact

- Affected documentation: `README.md`, `deploy/README.md`, Docker deployment documentation,
  Superpowers design/plan files, and OpenSpec specifications.
- Affected backend configuration: a new container-oriented Spring profile; existing API behavior
  is unchanged.
- Affected infrastructure: Docker Desktop on this Mac, PostgreSQL 16 persistent development
  storage, Neo4j 5.23 optional projection, Prometheus/Grafana observability, and official Dify
  `v1.14.0` self-hosted services.
- Affected migrations: existing PostgreSQL Flyway migrations are executed in the new runtime;
  no new application tables are introduced by this change.
- Verification: deployment-file validation, backend/frontend builds, PostgreSQL migration
  startup, endpoint health checks, Dify availability, degradation checks, and database backup.
