# MedKernel

## Purpose

Define the stable product identity, repository boundaries, documentation authority, and
change-planning expectations for the MedKernel group medical intelligence hub.

## Requirements

### Requirement: Product Identity
The system SHALL present MedKernel as the group medical intelligence hub for healthcare networks.

#### Scenario: New contributor orientation
- **GIVEN** a contributor opens the repository
- **WHEN** they read the root README or this spec
- **THEN** they can identify the product name, the broad mission, and the main documentation sources.

### Requirement: Two-Layer Model
The system SHALL be understood as a two-layer platform consisting of a foundation layer and an engine-services layer.

#### Scenario: Architecture review
- **GIVEN** a reviewer is trying to understand the platform
- **WHEN** they inspect the project documentation
- **THEN** they can distinguish shared platform capabilities from engine capabilities such as knowledge, dictionaries, rules, paths, recommendations, assessments, follow-up, publishing, embedding, and model gateway support.

### Requirement: Repository Boundaries
The system SHALL keep implementation separated into backend, frontend, docs, and deployment areas.

#### Scenario: Code navigation
- **GIVEN** a developer is looking for the implementation of a feature
- **WHEN** they inspect the repository structure
- **THEN** they can find backend services under `medkernel-backend/`, the UI under `frontend/`, operational assets under `deploy/`, and canonical written guidance under `docs/`.

### Requirement: Documentation Authority
The system SHALL treat the existing product and implementation documents as the authoritative source for behavior, constraints, and rollout order.

#### Scenario: Change planning
- **GIVEN** a new change is being planned
- **WHEN** the team checks the project context
- **THEN** they can use the linked documents to confirm scope, constraints, and current execution order before editing code.

### Requirement: Auditable Change Flow
The system SHALL record proposed work as change folders under `openspec/changes/` and keep current behavior in `openspec/specs/`.

#### Scenario: Preparing a feature
- **GIVEN** a new change request
- **WHEN** the team starts OpenSpec planning
- **THEN** they create a change folder containing proposal, design, tasks, and delta specs before implementation starts.
