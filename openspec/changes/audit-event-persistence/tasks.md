## 1. Schema And Persistence Contract

- [x] 1.1 Repository test inserts events for two tenants and asserts chain links
      and tenant isolation via `findPage` / `findByEventId`.
- [x] 1.2 V5 Flyway migrations added for h2, postgres, oracle, dm and kingbase;
      `audit_event` gains `prev_event_id` / `prev_signature`; `audit_chain_head`
      created with `(tenant_id PRIMARY KEY, last_event_id, last_signature, updated_at)`.
- [x] 1.3 JDBC-based `AuditEventRepository` implemented with row-level locking
      (`SELECT ... FOR UPDATE`) on `audit_chain_head`.

## 2. Persistence Sink And Signature Chain

- [x] 2.1 `AuditChainWriterTest` asserts first-event-anchors-on-GENESIS,
      subsequent-event-links-to-prior, per-tenant chain isolation, missing-tenant
      fallback to `__SYSTEM__`, and verify() rejects tampering.
- [x] 2.2 `AuditPersistenceSink` listens on `AFTER_COMMIT` and (for no-transaction
      paths) on `@EventListener`; delegates to `AuditChainWriter` whose
      `REQUIRES_NEW` transaction locks the chain head, signs the canonical
      payload with `SmCryptoService.sm3Hex`, and advances the head atomically.
- [x] 2.3 `AuditPersistenceSinkTest` verifies failures are swallowed,
      `medkernel_audit_persistence_failures_total` increments, and the signed
      counter only increments on success.

## 3. Query Service And Compliance API

- [x] 3.1 `AuditQueryServiceTest` asserts tenant context required, filters and
      time range pass through, cursor truncation at size+1, invalid-cursor
      rejected with `BAD_REQUEST`.
- [x] 3.2 `AuditQueryService` implemented; `com.medkernel.compliance.audit.AuditEvent`
      DTO refactored to project from `AuditEventRecord` (now includes signature,
      status, and resource identifiers).
- [x] 3.3 `AuditController` returns `ApiResult<CursorResponse<AuditEvent>>` from
      `/events`, publishes a real `EXPORT` event on `/snapshot`, and looks up the
      persisted view by `eventId`.
- [x] 3.4 `AuditControllerTest` rewritten as a permission + DataScope matrix
      (audit.read / audit.export / no permission / no tenant).

## 4. Verification And Rollout

- [x] 4.1 `mvn -pl medkernel-backend test` exercises 27 audit tests (repository,
      writer, sink, query service, controller, publisher) — all green. Full suite
      passes minus Docker-dependent `FlywayMultiDialectSmokeTest` and
      Testcontainers-profile tests, which run on CI Ubuntu where Docker is
      available.
- [x] 4.2 `docs/backlog.md` marks GA-ENG-BASE-04 as `done`.
- [x] 4.3 Branch `feat/ga-eng-base-04-audit-context` pushed and PR opened
      against `main` referencing this change folder.
