## 1. Deployment Contract And Runtime Configuration

- [x] 1.1 Add a deployment-asset contract test and verify that it fails before Docker assets exist.
- [x] 1.2 Add the Spring `container` profile and validate PostgreSQL/Flyway configuration through the contract test.
- [x] 1.3 Add committed environment templates with an external runtime-root default and no committed secrets.

## 2. MedKernel Container Stack

- [x] 2.1 Add pinned PostgreSQL, Neo4j, backend, and frontend gateway services with persistent external mounts and health checks.
- [x] 2.2 Add multi-stage backend/frontend images and Nginx routing for `/medkernel/`.
- [x] 2.3 Add Prometheus/Grafana Compose attachment that reuses the current monitoring configuration.
- [x] 2.4 Validate Compose rendering and run backend/frontend build verification.

## 3. Dify And Operations

- [x] 3.1 Add runtime bootstrap logic that prepares local secrets, clones the official Dify `v1.14.0` release using Git tag `1.14.0`, and locks validated default images by digest under the external runtime root.
- [x] 3.2 Add `core` and `full` start/stop/health scripts, keeping the official Dify Compose project separate.
- [x] 3.3 Add PostgreSQL backup/restore operations and operator documentation for local and later-server deployment.
- [x] 3.4 Remove the previous offline/systemd/nginx deployment files so Docker is the only active deployment entrypoint.

## 4. Runtime Installation And Acceptance

- [x] 4.1 Install and start Docker Desktop for Apple Silicon and verify Docker Compose compatibility.
- [x] 4.2 Bootstrap the runtime at `/Users/zhikunzheng/work/medkernel/runtime/` and start core mode.
- [x] 4.3 Verify Flyway migration state, backend ping/health, frontend gateway, Neo4j reachability, and monitoring.
- [x] 4.4 Start full mode, verify Dify availability, prove optional-service degradation, and create a PostgreSQL backup artifact.
