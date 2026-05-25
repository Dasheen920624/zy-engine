## ADDED Requirements

### Requirement: Persisted Audit Event Trail
The system SHALL persist every `AuditEvent` published through `AuditEventPublisher`
to the `audit_event` table once the originating business transaction commits, and
SHALL NOT block or fail the business transaction when persistence fails.

#### Scenario: Publishing inside a committed transaction
- **WHEN** a transactional service publishes an `AuditEvent` and its transaction commits
- **THEN** a row is written to `audit_event` carrying the published `action`,
  `resourceType`, `resourceId`, `actorUserId`, `traceId`, `payloadDigest`, and the
  `tenantId`, `hospitalId` and `departmentId` from the request's `OrgScope`
- **AND** the row's `status` is `SIGNED`.

#### Scenario: Publishing inside a rolled-back transaction
- **WHEN** a transactional service publishes an `AuditEvent` and its transaction rolls back
- **THEN** no row is written to `audit_event` for that event.

#### Scenario: Persistence fails during sink processing
- **WHEN** the audit persistence sink fails to write a row to the database
- **THEN** the originating business call is unaffected
- **AND** the `audit_persistence_failures_total` metric is incremented
- **AND** an ERROR log entry is written containing the failed event's id, action,
  resourceType and resourceId.

#### Scenario: Event published without a resolved tenant
- **WHEN** an `AuditEvent` is published while `RequestContext.currentOrgScope()`
  has no `tenantId`
- **THEN** the row is persisted under the synthetic tenant identifier `__SYSTEM__`
- **AND** participates in the `__SYSTEM__` hash chain.

### Requirement: Per-Tenant SM3 Hash Chain
The system SHALL maintain a tamper-evident SM3 hash chain over `audit_event` rows
partitioned by `tenant_id`, anchored at the literal string `GENESIS`.

#### Scenario: First event for a tenant
- **WHEN** the first `AuditEvent` for a tenant is persisted
- **THEN** its `prev_event_id` is null
- **AND** its `prev_signature` is the literal string `GENESIS`
- **AND** its `signature` equals `SM3Hex("GENESIS\n" + canonicalPayload)`.

#### Scenario: Subsequent events for a tenant
- **WHEN** another `AuditEvent` for the same tenant is persisted
- **THEN** its `prev_event_id` and `prev_signature` reference the prior row's
  `event_id` and `signature`
- **AND** its `signature` equals
  `SM3Hex(prev_signature + "\n" + canonicalPayload)`.

#### Scenario: Concurrent persistence within one tenant
- **WHEN** two `AuditEvent`s for the same tenant are persisted concurrently
- **THEN** the two rows are linked in a single linear chain with distinct
  `prev_event_id` values and consistent `prev_signature` references.

#### Scenario: Tampered row breaks verification
- **WHEN** a stored row's `action`, `resourceType`, `resourceId`, `actorUserId`,
  `tenantId`, `payloadDigest` or `occurredAt` is altered after the fact
- **THEN** recomputing `SM3Hex(prev_signature + "\n" + canonicalPayload)` for that
  row no longer matches the stored `signature`.

### Requirement: Tenant-Scoped Audit Read API
The system SHALL serve `GET /api/v1/compliance/audit/events` with cursor-based
server-side pagination and filtering that always restricts results to the caller's
tenant from `RequestContext`.

#### Scenario: Default page request
- **WHEN** an authenticated user requests `/events` without filters
- **THEN** the response contains at most 50 rows for the caller's tenant ordered by
  `id` descending
- **AND** the response carries a `nextCursor` when more rows remain.

#### Scenario: Filtered request
- **WHEN** an authenticated user supplies any of `action`, `resourceType`, `actorUserId`,
  `from` or `to`
- **THEN** the rows returned satisfy every supplied filter
- **AND** still belong to the caller's tenant.

#### Scenario: Cross-tenant request
- **WHEN** a caller from tenant `t-2` requests `/events`
- **THEN** no rows belonging to any other tenant are returned, regardless of
  filter values supplied.

#### Scenario: Page size override
- **WHEN** the caller supplies a `size` greater than 500
- **THEN** the response includes at most 500 rows.

### Requirement: Real Snapshot Publishing
The `POST /api/v1/compliance/audit/snapshot` endpoint SHALL publish a real
`EXPORT` audit event through `AuditEventPublisher` and return the persisted view.

#### Scenario: Snapshot publishes and reads back
- **WHEN** an authenticated user posts to `/snapshot` with a reason
- **THEN** an `AuditEvent` with `action = EXPORT`, `resourceType = "audit"` and a
  `payloadDigest` derived from the reason is published
- **AND** the response body matches the persisted row for that event including its
  `signature`.
