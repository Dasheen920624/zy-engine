## Context

The `audit_event` table is already part of the V2 baseline migration across all five
dialects and the in-process event contract is in place, but no listener moves events
to persistent storage and the compliance API responds with hard-coded sample rows.
That gap blocks every downstream evidence requirement: rule publish trail, knowledge
asset audit, CDSS feedback, export snapshots, rollback proofs. The hash chain is
required so that any single tampered row breaks verification of every subsequent
row, even when storage is shared with operational systems.

## Goals / Non-Goals

**Goals:**

- Persist every published `AuditEvent` to `audit_event` with full request context.
- Make the chain tamper-evident per tenant using SM3, reusing `SmCryptoService`.
- Serve real, tenant-scoped, paginated audit data through the existing compliance
  audit endpoints.
- Provide deterministic verification of any stored event by re-computing its
  signature from its own canonical payload plus its predecessor.

**Non-Goals:**

- Adopting the publisher inside every existing engine controller; each package
  takes that on as part of its own ticket.
- Implementing TSA (trusted timestamping) signing or external evidence packaging.
- Building an async export/CSV pipeline (covered by GA-ENG-API-13).
- Changing the frontend audit view, which still lives behind GA-ENG-BASE-06.

## Decisions

### Per-Tenant Hash Chain Anchored In `audit_chain_head`

A new lightweight table holds the latest `event_id` and `signature` for each tenant
and is updated inside the same transaction as the new `audit_event` insert. The sink
performs `SELECT ... FOR UPDATE` on the tenant's head row (and inserts one with
signature `GENESIS` the first time the tenant emits an event). This serialises chain
advancement per tenant rather than globally, allowing concurrent writes across
tenants without sacrificing tamper detection. A global single-chain alternative was
rejected because it would serialise every audit write across the platform and create
an obvious hot row.

Events published without a resolved tenant (for example LOGIN failures arriving
before the JWT is parsed, or scheduled jobs running outside any request) are
written under the synthetic tenant identifier `__SYSTEM__`. The synthetic tenant
owns its own chain, which means platform-level events form their own verifiable
trail without being silently dropped and without colliding with real tenant rows.

### Canonical Payload Format

Each row's signature is computed as
`SM3Hex(prev_signature + "\n" + canonical)` where `canonical` joins the
fields most likely to be tampered with using `|` and excludes mutable storage
metadata such as the auto-generated primary key:

```
action|resourceType|resourceId|actorUserId|tenantId|hospitalId|departmentId|occurredAtIso|payloadDigest
```

`payloadDigest` itself is supplied by callers via `AuditEvent.withPayloadDigest`
and is treated as opaque. `prev_signature` for the first event in a tenant is the
literal string `GENESIS`, which makes the chain self-anchoring without requiring a
seed row.

### After-Commit Listener With Loud Failure

The sink uses `@TransactionalEventListener(phase = AFTER_COMMIT)`. If the business
transaction rolls back, no audit row is written, which prevents audit trails that
do not match what actually happened. Persistence failures increment a
`audit_persistence_failures_total` counter, emit an ERROR log with the failed
event's identifiers, and never bubble up to the business call site. This trades a
narrow window of audit-loss-on-database-failure for never blocking a successful
business operation behind an audit storage hiccup. Operators have the metric to
catch and reconstruct gaps.

### Service-Layer Org Scope Enforcement

Query endpoints obtain the tenant from `RequestContext.currentOrgScope().tenantId()`
and refuse to serve cross-tenant rows. The hospital and department fields are used
only as optional filters. There is intentionally no admin "see everything" path in
this change; it can be introduced when the role and permission model has an
explicit `AUDIT_GLOBAL_READ` permission.

### Cursor Pagination With Capped Page Size

Reads use `id`-descending cursor pagination with default size 50 and a hard cap of
500, returning the next cursor in the payload. This avoids deep `OFFSET` scans
on what is expected to become a large append-only table, and matches the contract
that GA-ENG-API-13 will eventually formalise.

### Self-Demonstration Through The Snapshot Endpoint

`POST /api/v1/compliance/audit/snapshot` is changed from a fake row generator to
the first real publisher inside this change: it calls
`AuditEventPublisher.publish(AuditAction.EXPORT, ...)`, attaches an SM3 digest of
the export reason, and returns the persisted row. This validates the chain end to
end in the change itself without coupling the change to other engine controllers.

## Risks / Trade-offs

- [Risk] Audit-loss on database failure is invisible to the caller.
  -> [Mitigation] Counter plus ERROR log plus structured fields make gaps queryable.
- [Risk] Per-tenant `SELECT ... FOR UPDATE` could become a hot row for a very
  active tenant.
  -> [Mitigation] Single-row update per audit insert is cheap and dwarfed by the
     business write itself; we accept this in exchange for chain integrity.
- [Risk] Switching the snapshot endpoint to real persistence changes its observable
  payload shape.
  -> [Mitigation] DTO is already loosely consumed by an internal mock UI; the new
     payload is a strict superset (id, occurredAt, actor, summary, traceId,
     signature) and the change is called out in the PR description.

## Migration Plan

1. Write the failing tests for sink chain, repository, query service, and
   controller behaviour.
2. Add the V5 migrations for each dialect; verify they apply on an existing
   database without dropping the V2 table.
3. Implement the repository, sink, and query service; refactor the controller
   and its DTO.
4. Run `mvn -pl medkernel-backend test` and confirm green.
5. Open the PR. No data migration is required because the chain is anchored on
   `GENESIS` for any tenant that has never emitted an event.

Rollback is non-destructive: the new columns and `audit_chain_head` table can
remain in place if the application code is reverted, because the existing
`audit_event` columns are untouched. The previous behaviour (mock rows from the
controller) can be restored by reverting the controller alone.

## Open Questions

None blocking. The eventual `AUDIT_GLOBAL_READ` admin permission and TSA chain
extension will be proposed separately when their stakeholders surface.
