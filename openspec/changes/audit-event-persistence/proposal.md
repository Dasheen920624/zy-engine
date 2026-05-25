## Why

GA-ENG-BASE-04 requires every write, review, publish, execute, feedback, export and
rollback to leave a tamper-evident trail. The previous PR delivered the in-process
event contract (`com.medkernel.shared.audit.AuditEvent`, `AuditEventPublisher`,
`AuditAction`) and the `audit_event` schema in V2 baseline migrations, but no listener
persists those events and the compliance audit API still returns hard-coded mock rows.
That leaves the engine without a verifiable audit substrate and makes downstream
evidence chain, export and rollback work impossible to ground in real data.

## What Changes

- Add an `AuditPersistenceSink` that listens to `AuditEvent` on the `AFTER_COMMIT`
  phase and writes a row to `audit_event` carrying actor, traceId, OrgScope,
  payload digest and a per-tenant SM3 hash chain.
- Add a V5 migration (5 dialects) that introduces `prev_event_id` / `prev_signature`
  columns on `audit_event` and a small `audit_chain_head` table used to lock and
  advance the latest signature per tenant.
- Add an `AuditEventRepository` and `AuditQueryService` that enforce the
  `RequestContext` org scope and expose cursor-based, server-side paginated reads
  filtered by action, resource, actor and time range.
- Refactor `com.medkernel.compliance.audit.AuditController` so that
  `GET /api/v1/compliance/audit/events` returns real persisted rows and
  `POST /api/v1/compliance/audit/snapshot` publishes a real `EXPORT` audit event
  through `AuditEventPublisher` instead of fabricating data.
- Add metrics and structured error logging when persistence fails so that audit gaps
  surface immediately even though the business transaction is not rolled back.

## Capabilities

### New Capabilities

- `audit-event-persistence`: persistent, hash-chained audit trail backed by
  `audit_event` and served through the compliance audit API.

### Modified Capabilities

None of the existing `medkernel` capabilities change shape; this change adds a
new engine capability that future business modules opt into by calling the
existing `AuditEventPublisher`.

## Impact

- Affected documentation: `docs/MEDKERNEL_FOUNDATION_AND_SERVICES.md` (audit
  evidence section), `docs/backlog.md` (GA-ENG-BASE-04 status), and this OpenSpec
  change folder.
- Affected backend code: new package `com.medkernel.shared.audit.persistence`,
  refactor of `com.medkernel.compliance.audit.AuditController` and its DTO,
  no change to other controllers.
- Affected data: new columns on `audit_event`, new `audit_chain_head` table,
  five Flyway migration scripts (`V5__audit_chain_baseline.sql` per dialect).
- Affected tests: persistence sink chain integrity, signature verification,
  per-tenant query isolation, controller pagination/filter/auth.
- Out of scope: wiring publish calls into existing engine controllers (each
  business package will adopt the publisher as part of its own task), TSA
  timestamping, async export endpoints, and any frontend work.
