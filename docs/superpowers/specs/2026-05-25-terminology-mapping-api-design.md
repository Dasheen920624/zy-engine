# Terminology Mapping API Design

## Goal

Deliver the GA-ENG-API-04 backend capability for terminology mapping so standard terms, local terms, mapping candidates, mapping conflicts, manual confirmation, mapping package publishing, and package rollback can run through tenant-scoped APIs.

## Scope

This slice implements the terminology mapping API plus a complete terminology package lifecycle: build a draft package from confirmed mappings, publish it to gray or full scope, preserve package item snapshots, supersede older full packages on full publish, and roll back to a chosen previous package. It does not implement generic cross-asset package export/sync targets; those remain GA-ENG-PKG-01.

## Architecture

The backend adds `com.medkernel.engine.terminology` beside the existing `engine.knowledge` package. Records model mapping tables (`standard_term`, `local_term`, `term_mapping`, `mapping_candidate`, `mapping_conflict`) and package tables (`term_mapping_package`, `term_mapping_package_item`, `term_mapping_package_release`). Services read the tenant from `RequestContext`, repositories require tenant filters, and controllers use `ApiResult`, `PageResponse`, `@DataScope(requireTenant = true)`, and `@PreAuthorize("@perm.has('term.*')")`.

## API

- `GET /api/v1/engine/terminology/standard-terms`
- `GET /api/v1/engine/terminology/local-terms`
- `GET /api/v1/engine/terminology/mappings`
- `GET /api/v1/engine/terminology/candidates`
- `GET /api/v1/engine/terminology/conflicts`
- `POST /api/v1/engine/terminology/candidates/{id}/confirm`
- `POST /api/v1/engine/terminology/conflicts/{id}/resolve`
- `GET /api/v1/engine/terminology/packages`
- `POST /api/v1/engine/terminology/packages`
- `POST /api/v1/engine/terminology/packages/{id}/publish`
- `POST /api/v1/engine/terminology/packages/{id}/rollback`

## Data Rules

All tables include `tenant_id`, status fields, source/evidence fields where applicable, and audit timestamps/users. High-risk or conflict-prone mappings remain explicit candidates/conflicts until a user confirms or resolves them. Confirming a candidate creates or updates a `term_mapping` row and marks the candidate `CONFIRMED`.

Package versions are immutable after publish. Full publish supersedes previous full packages for the same code and organization scope. Rollback marks the current package `ROLLED_BACK`, reactivates the requested target package as `PUBLISHED`, and records a `ROLLBACK` release event with reason and actor.

## Testing

Add service tests first for tenant scoping, keyword normalization, confirmation, conflict resolution, package publish, and rollback. Add controller security tests to prove read/write/publish permissions and tenant data scope behavior. Add a migration smoke assertion that H2 applies through V4.
