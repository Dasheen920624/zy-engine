# Containerized Development Platform Design

## Goal

Provide a persistent, reproducible development deployment of MedKernel on this Mac and a
portable deployment baseline for later Linux servers. The environment must run the current
application against PostgreSQL, make Neo4j available as an optional projection target, and run
an official pinned Dify self-hosted stack without making either Neo4j or Dify authoritative for
MedKernel business data.

## Context

MedKernel currently has release and offline deployment assets in `deploy/`, but it has no Docker
Compose development stack. The backend defaults to an H2 in-memory `dev` profile with Flyway
disabled. The product documents establish these boundaries:

- PostgreSQL or another supported relational database holds authoritative business state.
- Neo4j is a disposable, rebuildable graph projection.
- Dify is an optional workflow runner or synchronization target; it is not a required database
  or a source of clinical truth.
- Core application behavior must still operate when Dify and Neo4j are stopped.

The target Mac is Apple Silicon with 16 GiB RAM and sufficient disk capacity, but Docker is not
currently installed. Docker Desktop is approved as the runtime because Dify's official macOS
self-host guidance requires Docker Desktop and Docker Compose 2.24.0 or later.

## Selected Approach

Use two adjacent Compose projects with one managed entry point:

1. The repository owns the **MedKernel stack** under `deploy/docker/`.
2. A setup script installs a pinned checkout of the **official Dify stack** under the external
   runtime root at `/Users/zhikunzheng/work/medkernel/runtime/dify/v1.14.0/`.
3. Wrapper scripts bring up either MedKernel core services or the complete platform, while
   preserving separate volumes, credentials, upgrade boundaries, and backup procedures.

This design deliberately does not merge MedKernel's PostgreSQL with Dify's internal PostgreSQL.
Dify owns its application database, Redis, vector store, plugin daemon, sandbox, and proxy
dependencies. MedKernel owns its database, application lifecycle, Neo4j projection, and
observability data.

## Alternatives Considered

### One Combined Compose File

Copying Dify's services into one MedKernel Compose file would create a single startup command,
but it would fork upstream Dify operations and make upgrades difficult to audit. It also invites
confusion between MedKernel's authoritative database and Dify's internal application state.

### MedKernel Core Only

Starting PostgreSQL, Neo4j, backend, frontend, and monitoring without Dify would require fewer
resources. It remains useful as a day-to-day mode, but it does not satisfy the requested full
development environment, so it is retained only as the `core` startup profile.

## Runtime Layout

Committed portable assets:

```text
deploy/docker/
|-- README.md
|-- compose.yml
|-- compose.monitoring.yml
|-- .env.example
|-- backend/Dockerfile
|-- frontend/Dockerfile
|-- frontend/nginx.conf
|-- scripts/
|   |-- bootstrap-runtime.sh
|   |-- up.sh
|   |-- down.sh
|   |-- healthcheck.sh
|   |-- backup.sh
|   `-- restore.sh
`-- dify/
    |-- .env.override.example
    `-- README.md
```

Machine-local runtime state outside Git:

```text
/Users/zhikunzheng/work/medkernel/runtime/
|-- env/                         # local secret-bearing .env files
|-- data/
|   |-- medkernel-postgres/
|   |-- neo4j/
|   `-- monitoring/
|-- backups/
`-- dify/
    `-- v1.14.0/                 # pinned official Dify checkout and Dify data
```

On another server, `MEDKERNEL_RUNTIME_ROOT` replaces the local absolute runtime path, so the
same repository deployment assets can be used without editing Compose files.

## Components

| Component | Ownership | Role | Persistence |
| --- | --- | --- | --- |
| PostgreSQL 16 | MedKernel | Sole authoritative development business database | Runtime bind mount and database backups |
| Backend | MedKernel | Spring Boot application using PostgreSQL and Flyway | Stateless image |
| Frontend gateway | MedKernel | Static React build plus reverse proxy to backend | Stateless image |
| Neo4j 5.23 Community | MedKernel | Optional rebuildable graph projection endpoint | Runtime bind mount; not authoritative |
| Prometheus and Grafana | MedKernel | Existing monitoring dashboards and metrics collection | Runtime bind mounts |
| Dify v1.14.0 official Compose | Dify upstream | Optional workflow execution environment | Dify-owned volumes under runtime root |

The Dify stack includes its official internal dependencies such as its PostgreSQL database,
Redis, Weaviate, Nginx, sandbox, and plugin daemon. These are not reused by MedKernel.

## Startup Modes

### Core Mode

Starts MedKernel PostgreSQL, Neo4j, backend, and frontend gateway. Monitoring can be enabled as
an attached Compose file. Core mode is the default working environment because it remains within
the required DB-first, no-Dify execution boundary.

### Full Mode

Starts core mode, monitoring, and the pinned official Dify stack. This is the requested full
environment and is the acceptance mode for initial setup. Because Dify is resource-heavy, full
mode is intended for integration and workflow development rather than always-on lightweight work.

## Application Configuration

Add a dedicated backend Docker profile rather than mutating the current H2 `dev` profile:

- PostgreSQL JDBC connection is sourced from container environment variables.
- Flyway is enabled and points to `classpath:db/migration/postgres`.
- Development-only security bypass remains explicit and must never be used for production.
- Neo4j and Dify endpoint variables are present but optional; until application providers are
  implemented, they document and health-check external endpoints without creating false runtime
  coupling.

The frontend is built as static assets and served through Nginx. Nginx proxies `/medkernel/` to
the backend container, providing one stable browser entry point and a deployment shape similar to
server installation.

## Data, Secrets, And Backups

- Committed `.env.example` files contain only safe placeholders and pinned versions.
- Runtime secrets are generated or edited under `runtime/env/` and remain outside Git.
- PostgreSQL backup uses `pg_dump` and restore uses `pg_restore` against a named backup artifact.
- Neo4j and monitoring mounts can be copied for convenience, but losing them must not prevent
  restoring MedKernel business state from PostgreSQL.
- Dify data backup is treated separately using the official Dify deployment directory and its
  own database/volume backup procedure.

## Failure Handling

- Compose health checks gate backend startup on PostgreSQL readiness and expose unhealthy
  containers clearly.
- If Neo4j fails, MedKernel remains reachable in core DB-only mode; projection features are
  reported unavailable.
- If Dify fails or is stopped, MedKernel remains reachable and the integration endpoint is marked
  unavailable; workflows that require Dify cannot run, but no authoritative data is lost.
- If Docker Desktop is not running, wrapper scripts exit early with installation/startup
  instructions rather than partially mutating runtime state.

## Verification

The implementation is accepted when all of the following are demonstrated:

1. Docker Desktop and Compose meet Dify's documented prerequisites.
2. `core` mode starts healthy PostgreSQL, Neo4j, backend, and frontend gateway containers.
3. Flyway applies the existing PostgreSQL migrations to the persistent MedKernel database.
4. Backend ping/health endpoints and frontend gateway respond through container networking.
5. `full` mode brings up the pinned official Dify stack and its web entry point responds.
6. Stopping Dify and Neo4j does not prevent the DB-backed MedKernel health/ping path from
   responding.
7. A PostgreSQL backup file is created in the external runtime root and its restore command is
   documented.
8. Deployment files require only environment value changes to run on a later Docker-enabled
   Linux server.

## Version Baseline

This design pins the initial development deployment to:

- Docker Desktop for Mac ARM `4.74.0`, as listed in Docker's ARM release feed on 2026-05-25.
- PostgreSQL `16`, matching the repository's PostgreSQL deployment profile.
- Neo4j Community `5.23`, matching the backend driver and product documentation.
- Dify `v1.14.0`, shown as the current official latest release on 2026-05-25. Its official
  checkout tag is `1.14.0`; the prefixed label remains the readable runtime directory name.

The official Dify Compose topology is kept upstream, while a committed override locks the
validated default service images by digest, including upstream helper services that otherwise use
mutable tags. Version or topology upgrades must be separate reviewed changes with refreshed
digests and backup/restore checks before data volumes are reused.

## Official References

- Dify Docker Compose deployment: <https://docs.dify.ai/en/self-host/quick-start/docker-compose>
- Dify releases: <https://github.com/langgenius/dify/releases>
- PostgreSQL official container image: <https://hub.docker.com/_/postgres/>
- Neo4j Docker operations documentation: <https://neo4j.com/docs/operations-manual/current/docker/introduction/>
