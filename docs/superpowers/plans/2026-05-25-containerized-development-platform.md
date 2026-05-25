# Containerized Development Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy a persistent Docker-based MedKernel development platform with PostgreSQL, Neo4j, observability, and a separately managed pinned official Dify environment.

**Architecture:** Repository-owned assets in `deploy/docker/` build and operate the MedKernel stack; persistent data, local secrets, backups, and the pinned upstream Dify checkout live under `${MEDKERNEL_RUNTIME_ROOT}`. MedKernel PostgreSQL is authoritative, while Neo4j and Dify are optional services that can be stopped without removing DB-backed application availability.

**Tech Stack:** Docker Desktop for Mac ARM, Docker Compose v2, PostgreSQL 16.14, Neo4j Community 5.23.0, Dify v1.14.0, Spring Boot 3.3/Flyway, React/Vite/Nginx, Prometheus/Grafana, POSIX shell.

---

### Task 1: Deployment Asset Contract Test

**Files:**
- Create: `deploy/docker/tests/validate-deployment-assets.sh`

- [ ] **Step 1: Write the deployment contract test before deployment assets**

Create an executable shell test that fails unless the final deployment file set exists, the
container Spring profile enables PostgreSQL migrations, the Compose file keeps PostgreSQL and
Neo4j separate, and scripts reference `MEDKERNEL_RUNTIME_ROOT`:

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
required=(
  "deploy/docker/.env.example"
  "deploy/docker/compose.yml"
  "deploy/docker/compose.monitoring.yml"
  "deploy/docker/backend/Dockerfile"
  "deploy/docker/frontend/Dockerfile"
  "deploy/docker/frontend/nginx.conf"
  "deploy/docker/scripts/bootstrap-runtime.sh"
  "deploy/docker/scripts/up.sh"
  "deploy/docker/scripts/down.sh"
  "deploy/docker/scripts/healthcheck.sh"
  "deploy/docker/scripts/backup.sh"
  "deploy/docker/scripts/restore.sh"
  "deploy/docker/dify/README.md"
  "medkernel-backend/src/main/resources/application-container.yml"
)

for path in "${required[@]}"; do
  test -f "$ROOT/$path" || { printf 'missing required deployment file: %s\n' "$path" >&2; exit 1; }
done

grep -q 'classpath:db/migration/postgres' "$ROOT/medkernel-backend/src/main/resources/application-container.yml"
grep -q 'postgres:16.14-bookworm' "$ROOT/deploy/docker/compose.yml"
grep -q 'neo4j:5.23.0-community' "$ROOT/deploy/docker/compose.yml"
grep -q 'MEDKERNEL_RUNTIME_ROOT' "$ROOT/deploy/docker/compose.yml"
grep -q 'v1.14.0' "$ROOT/deploy/docker/scripts/bootstrap-runtime.sh"
grep -q '^DIFY_GIT_REF=1.14.0$' "$ROOT/deploy/docker/.env.example"
grep -q 'DIFY_GIT_REF' "$ROOT/deploy/docker/scripts/bootstrap-runtime.sh"
grep -q 'pg_dump' "$ROOT/deploy/docker/scripts/backup.sh"
grep -q 'pg_restore' "$ROOT/deploy/docker/scripts/restore.sh"
printf 'deployment asset contract passed\n'
```

- [ ] **Step 2: Run it to confirm the missing assets fail**

Run:

```bash
chmod +x deploy/docker/tests/validate-deployment-assets.sh
./deploy/docker/tests/validate-deployment-assets.sh
```

Expected: FAIL with `missing required deployment file: deploy/docker/.env.example`.

### Task 2: Container Runtime Profile And Core Compose

**Files:**
- Create: `medkernel-backend/src/main/resources/application-container.yml`
- Create: `deploy/docker/.env.example`
- Create: `deploy/docker/compose.yml`
- Create: `deploy/docker/backend/Dockerfile`
- Create: `deploy/docker/frontend/Dockerfile`
- Create: `deploy/docker/frontend/nginx.conf`

- [ ] **Step 1: Add the container Spring profile**

Create `application-container.yml` with PostgreSQL and Flyway enabled; Compose activates
`dev,container` so the current development JWT decoder exists while this profile overrides H2:

```yaml
spring:
  datasource:
    url: ${MEDKERNEL_DB_URL:jdbc:postgresql://postgres:5432/medkernel}
    driver-class-name: org.postgresql.Driver
    username: ${MEDKERNEL_DB_USERNAME:medkernel}
    password: ${MEDKERNEL_DB_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration/postgres
  h2:
    console:
      enabled: false
```

- [ ] **Step 2: Add pinned environment and Compose definitions**

Create `.env.example` with `MEDKERNEL_RUNTIME_ROOT=/Users/zhikunzheng/work/medkernel/runtime`,
pinned image values, public ports, and placeholder passwords. Create `compose.yml` containing:

```yaml
name: medkernel-dev
services:
  postgres:
    image: ${POSTGRES_IMAGE:-postgres:16.14-bookworm}
    environment:
      POSTGRES_DB: ${MEDKERNEL_DB_NAME:-medkernel}
      POSTGRES_USER: ${MEDKERNEL_DB_USERNAME:-medkernel}
      POSTGRES_PASSWORD: ${MEDKERNEL_DB_PASSWORD}
    volumes:
      - ${MEDKERNEL_RUNTIME_ROOT:?set MEDKERNEL_RUNTIME_ROOT}/data/postgres:/var/lib/postgresql/data
  neo4j:
    image: ${NEO4J_IMAGE:-neo4j:5.23.0-community}
    environment:
      NEO4J_AUTH: neo4j/${MEDKERNEL_NEO4J_PASSWORD}
    volumes:
      - ${MEDKERNEL_RUNTIME_ROOT:?set MEDKERNEL_RUNTIME_ROOT}/data/neo4j/data:/data
      - ${MEDKERNEL_RUNTIME_ROOT:?set MEDKERNEL_RUNTIME_ROOT}/data/neo4j/logs:/logs
  backend:
    build:
      context: ../..
      dockerfile: deploy/docker/backend/Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: dev,container
      MEDKERNEL_DB_URL: jdbc:postgresql://postgres:5432/${MEDKERNEL_DB_NAME:-medkernel}
      MEDKERNEL_DB_USERNAME: ${MEDKERNEL_DB_USERNAME:-medkernel}
      MEDKERNEL_DB_PASSWORD: ${MEDKERNEL_DB_PASSWORD}
  frontend:
    build:
      context: ../..
      dockerfile: deploy/docker/frontend/Dockerfile
```

Fill in health checks, ports, restart policy, dependencies, and the shared network explicitly in
the final file.

- [ ] **Step 3: Add container images and reverse proxy**

Create a Maven/Temurin multi-stage backend image producing `app.jar`, and a Node/Nginx
multi-stage frontend image. Configure Nginx so static SPA routing uses `/index.html` and:

```nginx
location /medkernel/ {
    proxy_pass http://backend:18080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Trace-Id $http_x_trace_id;
}
```

- [ ] **Step 4: Re-run the contract check**

Run:

```bash
./deploy/docker/tests/validate-deployment-assets.sh
```

Expected: still FAIL because monitoring and operational scripts have not yet been added.

### Task 3: Monitoring, Dify Bootstrap, And Operations

**Files:**
- Create: `deploy/docker/compose.monitoring.yml`
- Create: `deploy/docker/scripts/bootstrap-runtime.sh`
- Create: `deploy/docker/scripts/up.sh`
- Create: `deploy/docker/scripts/down.sh`
- Create: `deploy/docker/scripts/healthcheck.sh`
- Create: `deploy/docker/scripts/backup.sh`
- Create: `deploy/docker/scripts/restore.sh`
- Create: `deploy/docker/dify/README.md`
- Create: `deploy/docker/dify/.env.override.example`
- Create: `deploy/docker/README.md`
- Modify: `deploy/README.md`
- Modify: `README.md`

- [ ] **Step 1: Add optional observability Compose services**

Create `compose.monitoring.yml` using `prom/prometheus:v2.55.1` and
`grafana/grafana:11.2.2`, mounting existing files from `deploy/monitoring/` and persistent
Grafana/Prometheus directories below `${MEDKERNEL_RUNTIME_ROOT}`. Override Prometheus's backend
target with a Docker-network configuration that scrapes `backend:18080`.

- [ ] **Step 2: Add runtime bootstrap and start/stop scripts**

Implement `bootstrap-runtime.sh` to:

```bash
RUNTIME_ROOT="${MEDKERNEL_RUNTIME_ROOT:-/Users/zhikunzheng/work/medkernel/runtime}"
DIFY_VERSION="${DIFY_VERSION:-v1.14.0}"
DIFY_GIT_REF="${DIFY_GIT_REF:-1.14.0}"
mkdir -p "$RUNTIME_ROOT"/{env,data/postgres,data/neo4j/data,data/neo4j/logs,data/prometheus,data/grafana,backups,dify}
```

It copies `.env.example` into `$RUNTIME_ROOT/env/medkernel.env` only when absent, generates
development passwords when placeholders remain, and clones the displayed release `v1.14.0`
using its actual upstream tag
`https://github.com/langgenius/dify.git --branch "$DIFY_GIT_REF"` into
`$RUNTIME_ROOT/dify/$DIFY_VERSION`.

Implement `up.sh core|full` and `down.sh core|full` so `core` controls the MedKernel Compose
project and `full` additionally invokes official Dify Compose from its pinned checkout.

- [ ] **Step 3: Add health and recovery scripts**

Implement `healthcheck.sh` to call `docker compose ps`, PostgreSQL `pg_isready`, backend
`/medkernel/api/v1/system/ping`, frontend `/`, Neo4j HTTP availability, Prometheus/Grafana in
full monitoring mode, and Dify's web URL in `full` mode. Implement `backup.sh` using:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres \
  pg_dump -U "$MEDKERNEL_DB_USERNAME" -d "$MEDKERNEL_DB_NAME" -Fc > "$BACKUP_FILE"
```

Implement `restore.sh <backup-file>` using `pg_restore --clean --if-exists --no-owner`.

- [ ] **Step 4: Document operations and make scripts executable**

Document local Docker Desktop prerequisites, `core`/`full` commands, ports, secrets, backups,
degradation testing, and later-server migration. Then run:

```bash
chmod +x deploy/docker/scripts/*.sh deploy/docker/tests/*.sh
./deploy/docker/tests/validate-deployment-assets.sh
```

Expected: PASS with `deployment asset contract passed`.

### Task 4: Docker Installation And Static Validation

**Files:**
- Machine install: `/Applications/Docker.app`

- [ ] **Step 1: Install Docker Desktop ARM release**

Download Docker Desktop ARM `4.74.0`, mount it, copy `Docker.app` to `/Applications`, start the
application, and wait for the daemon:

```bash
curl -fL -o /tmp/Docker.dmg https://desktop.docker.com/mac/main/arm64/227015/Docker.dmg
hdiutil attach /tmp/Docker.dmg -nobrowse
cp -R /Volumes/Docker/Docker.app /Applications/
hdiutil detach /Volumes/Docker
open -a Docker
until docker info >/dev/null 2>&1; do sleep 5; done
docker compose version
```

Expected: Docker Compose version is `2.24.0` or newer.

- [ ] **Step 2: Validate Compose and source builds**

Run:

```bash
export MEDKERNEL_RUNTIME_ROOT=/Users/zhikunzheng/work/medkernel/runtime
deploy/docker/scripts/bootstrap-runtime.sh --skip-dify
docker compose --env-file "$MEDKERNEL_RUNTIME_ROOT/env/medkernel.env" -f deploy/docker/compose.yml config -q
docker compose --env-file "$MEDKERNEL_RUNTIME_ROOT/env/medkernel.env" -f deploy/docker/compose.yml -f deploy/docker/compose.monitoring.yml config -q
(cd medkernel-backend && mvn -B -q test)
(cd frontend && npm ci && npm test -- --run && npm run build)
```

Expected: both Compose configurations parse, backend tests pass, and frontend tests/build pass.

### Task 5: Core And Full Runtime Acceptance

**Files:**
- Runtime only: `/Users/zhikunzheng/work/medkernel/runtime/`

- [ ] **Step 1: Bootstrap and start core mode**

Run:

```bash
export MEDKERNEL_RUNTIME_ROOT=/Users/zhikunzheng/work/medkernel/runtime
deploy/docker/scripts/bootstrap-runtime.sh
deploy/docker/scripts/up.sh core
deploy/docker/scripts/healthcheck.sh core
```

Expected: PostgreSQL, Neo4j, backend, and frontend are available; backend startup logs show
Flyway has applied PostgreSQL migrations through version `4`.

- [ ] **Step 2: Start and validate full mode**

Run:

```bash
deploy/docker/scripts/up.sh full
deploy/docker/scripts/healthcheck.sh full
```

Expected: Prometheus/Grafana and the pinned official Dify web/API service respond in addition to
MedKernel core.

- [ ] **Step 3: Verify optional-service degradation**

Run:

```bash
deploy/docker/scripts/down.sh optional
curl -fsS http://localhost:18080/medkernel/api/v1/system/ping
curl -fsS http://localhost:8088/medkernel/api/v1/system/ping
```

Expected: both MedKernel ping paths respond while Dify and Neo4j are stopped; restarting `full`
restores optional services.

- [ ] **Step 4: Back up authoritative PostgreSQL state**

Run:

```bash
deploy/docker/scripts/backup.sh
find "$MEDKERNEL_RUNTIME_ROOT/backups" -name 'medkernel-postgres-*.dump' -type f -size +0c
```

Expected: at least one non-empty PostgreSQL custom-format backup exists below the runtime root.
