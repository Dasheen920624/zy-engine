## ADDED Requirements

### Requirement: Persistent MedKernel Core Deployment
The project SHALL provide a Docker Compose development deployment that runs MedKernel with a
persistent PostgreSQL 16 business database, backend application, frontend gateway, and optional
Neo4j 5.23 projection service.

#### Scenario: Start core development deployment
- **WHEN** a developer starts the core Docker deployment with a configured runtime root
- **THEN** PostgreSQL, Neo4j, the backend, and the frontend gateway become healthy or reachable
- **AND** PostgreSQL data is retained under the external runtime root across container recreation

### Requirement: Authoritative Data Boundary
The deployment SHALL keep MedKernel's PostgreSQL database authoritative and SHALL NOT use Neo4j
or Dify internal storage as the authoritative store for MedKernel business records.

#### Scenario: Optional projection service is unavailable
- **WHEN** Neo4j is stopped while PostgreSQL and the MedKernel application remain running
- **THEN** the MedKernel health and ping paths remain reachable through the PostgreSQL-backed core deployment

#### Scenario: Optional workflow service is unavailable
- **WHEN** Dify is not started or is stopped
- **THEN** the MedKernel core deployment remains available without moving business data into Dify storage

### Requirement: Pinned Official Dify Integration
The project SHALL provide a managed full-development startup path that installs and runs the
official Dify self-hosted Docker Compose distribution at pinned version `v1.14.0` in a separate
runtime directory.

#### Scenario: Start full development deployment
- **WHEN** a developer starts the full mode on a Docker Compose compatible host
- **THEN** MedKernel services and the pinned official Dify service set start from separate Compose projects
- **AND** the Dify web entry point becomes reachable for workflow development

### Requirement: Portable Runtime State And Secrets
The deployment SHALL keep persistent data and secret-bearing environment files outside Git using
`MEDKERNEL_RUNTIME_ROOT`, defaulting on the development Mac to
`/Users/zhikunzheng/work/medkernel/runtime`.

#### Scenario: Deploy to another server
- **WHEN** an operator copies the committed deployment assets to another Docker-enabled server
- **AND** sets `MEDKERNEL_RUNTIME_ROOT` and local secrets for that host
- **THEN** the deployment starts without editing repository-owned Compose definitions for host paths

### Requirement: Backup And Health Verification
The deployment SHALL provide operational checks and PostgreSQL backup/restore commands that
validate the running core platform and preserve authoritative business data.

#### Scenario: Verify healthy deployment
- **WHEN** an operator runs the supplied health verification after startup
- **THEN** it checks container state, PostgreSQL readiness, Flyway-applied backend availability, and browser-facing endpoints

#### Scenario: Back up authoritative database
- **WHEN** an operator runs the supplied PostgreSQL backup operation
- **THEN** a timestamped database backup is written under the external runtime root
- **AND** the documented restore operation can target that backup artifact
