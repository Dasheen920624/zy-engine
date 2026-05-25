## Context

The existing repository has offline deployment scripts and monitoring assets but no Docker
Compose platform for day-to-day development. The backend's local profile currently uses
in-memory H2 with Flyway disabled. The product architecture requires a relational database as
the business authority and treats Neo4j and Dify as optional projection or workflow services.
The selected local persistent state root is `/Users/zhikunzheng/work/medkernel/runtime/`.

## Goals / Non-Goals

**Goals:**

- Run MedKernel against persistent PostgreSQL 16 with its existing PostgreSQL Flyway migrations.
- Expose a browser-accessible frontend/backend deployment and optional Neo4j 5.23 projection.
- Preserve Dify's official `v1.14.0` Compose ownership while providing a single managed full-mode entry point.
- Retain deployment assets, health checks, and PostgreSQL backup/restore operations for use on later servers.

**Non-Goals:**

- Making Neo4j or Dify an authoritative MedKernel store.
- Implementing application-level graph synchronization or Dify workflow bindings that are not yet in the backend.
- Building a production HA or hospital air-gapped distribution in this development change.

## Decisions

### Separate MedKernel And Dify Compose Projects

`deploy/docker/` owns MedKernel's services and wrapper scripts; a bootstrap script clones the
official Dify release under `${MEDKERNEL_RUNTIME_ROOT}/dify/v1.14.0`. This preserves upstream
Dify upgrade mechanics and prevents its internal PostgreSQL, Redis, and vector storage from
being confused with MedKernel's authoritative database. A combined copied Compose file was
rejected because it would fork a rapidly changing external platform.

### Persistent External Runtime Root

Compose binds data, local environment files, Dify checkout, and backups below
`${MEDKERNEL_RUNTIME_ROOT}`. The provided local default is
`/Users/zhikunzheng/work/medkernel/runtime`, but scripts accept another value for Linux hosts.
This keeps secrets and database state out of Git while leaving repository assets portable.

### Container Spring Profile And Static Frontend Gateway

The backend receives a dedicated `container` profile that connects to PostgreSQL and enables
Flyway migrations from `db/migration/postgres`. A multi-stage backend image builds the Spring
Boot JAR, while a multi-stage frontend image builds Vite assets and serves them through Nginx,
proxying `/medkernel/` to the backend container.

### Core And Full Modes

Core mode starts PostgreSQL, Neo4j, backend, and frontend, with optional monitoring Compose
assets. Full mode also starts the pinned official Dify stack. Core mode explicitly demonstrates
that Dify is not required; full mode satisfies workflow development and initial acceptance.

### Verification And Recovery

Deployment validation begins with a repository script test that checks required files,
configuration contracts, and safe secret handling. Runtime verification checks Compose health,
backend endpoints, frontend routing, Dify availability in full mode, and DB-only degradation.
PostgreSQL `pg_dump`/`pg_restore` operations preserve the authoritative data state separately
from reconstructible Neo4j data and Dify-owned data.

## Risks / Trade-offs

- [Risk] Full Dify plus application and monitoring services may strain a 16 GiB Mac.
  -> [Mitigation] Provide core mode for regular work and full mode for integration checks.
- [Risk] Dify's Compose layout changes in later releases.
  -> [Mitigation] Pin `v1.14.0` and handle upgrades through a separately reviewed change.
- [Risk] Optional services could appear to be active before backend integrations exist.
  -> [Mitigation] Document endpoints as available infrastructure only and verify DB-backed core behavior separately.
- [Risk] Local secret-bearing runtime files can be lost or copied insecurely.
  -> [Mitigation] Keep templates in Git, generate local values under the runtime root, and document backup boundaries.

## Migration Plan

1. Install Docker Desktop for Apple Silicon and verify Docker Compose meets the Dify minimum.
2. Add deployment assets, profile configuration, validation checks, and documentation in the repository.
3. Bootstrap `${MEDKERNEL_RUNTIME_ROOT}` and the pinned Dify checkout without committing secrets or data.
4. Start core mode and verify PostgreSQL migrations plus application endpoints.
5. Start full mode and verify official Dify availability and monitoring services.
6. Create and locate a PostgreSQL backup artifact.

Rollback consists of stopping both Compose projects and retaining `${MEDKERNEL_RUNTIME_ROOT}`
until the PostgreSQL backup is verified. Removing optional Neo4j or Dify state never substitutes
for restoring the authoritative PostgreSQL data.

## Open Questions

No blocking questions remain for the development deployment. Application-level graph projection
and Dify workflow integration will be proposed separately when their backend contracts exist.
