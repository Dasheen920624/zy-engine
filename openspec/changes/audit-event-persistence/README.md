# audit-event-persistence

Persist `AuditEvent` to the `audit_event` table with a per-tenant SM3 hash chain, and
serve real audit data through the compliance audit API (replacing the current mock).
