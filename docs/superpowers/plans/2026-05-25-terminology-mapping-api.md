# Terminology Mapping API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build GA-ENG-API-04 terminology mapping backend APIs with publish and rollback lifecycle.

**Architecture:** Add a focused `engine.terminology` package that mirrors the knowledge API baseline: tenant-scoped repositories, a service layer with mapping and package state transitions, and one controller returning `ApiResult` envelopes. Add V4 migrations for all five database dialects.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Data JDBC, Spring Security method authorization, Flyway SQL migrations, JUnit 5, Mockito, MockMvc.

---

### Task 1: Service Contract And Tests

**Files:**
- Create: `medkernel-backend/src/test/java/com/medkernel/engine/terminology/TerminologyServiceTest.java`
- Create later: `medkernel-backend/src/main/java/com/medkernel/engine/terminology/*.java`

- [x] Write tests for paged local term lookup, tenant-missing rejection, candidate confirmation, conflict resolution, package full publish, and package rollback.
- [x] Run `mvn -Dtest=TerminologyServiceTest test` and verify the tests fail because the package does not exist.
- [x] Implement the minimal terminology records, repositories, and service methods needed to pass those tests, including package status transitions and release events.
- [x] Re-run `mvn -Dtest=TerminologyServiceTest test` and verify green.

### Task 2: Controller Security

**Files:**
- Create: `medkernel-backend/src/test/java/com/medkernel/engine/terminology/TerminologyControllerSecurityTest.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/terminology/TerminologyController.java`

- [x] Write MockMvc tests proving `ROLE_DOCTOR` can reach read endpoints but fails data scope without tenant, `ROLE_GUEST` is forbidden, `ROLE_SPECIALIST` can reach confirmation but still needs tenant, and `ROLE_IT_OPS` can reach publish/rollback but still needs tenant.
- [x] Run `mvn -Dtest=TerminologyControllerSecurityTest test` and verify failure.
- [x] Implement the controller using `ApiResult`, `PageRequest`, `@DataScope`, and `@PreAuthorize`.
- [x] Re-run the controller test and verify green.

### Task 3: Migrations

**Files:**
- Create: `medkernel-backend/src/main/resources/db/migration/h2/V4__terminology_mapping_baseline.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/postgres/V4__terminology_mapping_baseline.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/oracle/V4__terminology_mapping_baseline.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/dm/V4__terminology_mapping_baseline.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/kingbase/V4__terminology_mapping_baseline.sql`
- Modify: `medkernel-backend/src/test/java/com/medkernel/platform/migration/H2BaselineMigrationTest.java`

- [x] Add five-dialect V4 migrations for `standard_term`, `local_term`, `term_mapping`, `mapping_candidate`, `mapping_conflict`, `term_mapping_package`, `term_mapping_package_item`, and `term_mapping_package_release`.
- [x] Update the H2 migration smoke assertion to include version `4`.
- [x] Run `mvn -Dtest=H2BaselineMigrationTest test` and verify green.

### Task 4: Verification

**Files:**
- No new files unless tests expose a focused defect.

- [x] Run `mvn -Dtest=TerminologyServiceTest,TerminologyControllerSecurityTest,H2BaselineMigrationTest,DefaultPermissionPolicyTest test`.
- [x] Run `mvn test` if targeted tests pass within available time. Result: blocked by missing Docker/Testcontainers environment before completing the full suite.
- [x] Run `git diff --check`.
