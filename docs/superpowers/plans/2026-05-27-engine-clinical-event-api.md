# GA-ENG-API-02 临床事件 API Implementation Plan（实施计划）

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付 DB-Only 临床事件 API，覆盖同步、异步、批量、查询、payload、诊断、重放、重试和死信。

**Architecture:** 以 Spring Boot + Spring Data JDBC + Flyway 五方言迁移为基础，新增 payload 旁路表和 outbox 表，`ClinicalEventService` 负责接收与查询，`ClinicalEventOutboxWorker` 负责可靠处理。所有状态、错误、traceId 和审计都落到现有可观测性与审计底座。

**Tech Stack:** Java 21、Spring Boot 3.3、Spring Data JDBC、Flyway、JUnit 5、Mockito、React/Vite 既有前端门禁。

---

## Chunk 1: 迁移、模型和契约

### Task 1: V10 五方言迁移

**Files:**
- Create: `medkernel-backend/src/main/resources/db/migration/h2/V10__clinical_event_api.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/postgres/V10__clinical_event_api.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/oracle/V10__clinical_event_api.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/dm/V10__clinical_event_api.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/kingbase/V10__clinical_event_api.sql`
- Modify: `medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java`

- [x] **Step 1: 写失败测试**
  - 在迁移合同测试中加入 `clinical_event_payload`、`clinical_event_outbox`、新增索引和 `clinical_event` 新列断言。

- [x] **Step 2: 运行迁移合同测试确认失败**
  - Run: `./mvnw -pl medkernel-backend -Dtest=MigrationBaselineContractTest test`
  - Expected: FAIL，提示 V10 表或列不存在。

- [x] **Step 3: 写最小迁移**
  - 新建 payload 旁路表、outbox 表。
  - 给 `clinical_event` 增加 `patient_id`、`encounter_id`、`package_version`、`error_code`、`error_class`、`retry_count`、`root_event_id`。
  - 给 payload/outbox 增加唯一约束和查询索引。

- [x] **Step 4: 运行迁移合同测试确认通过**
  - Run: `./mvnw -pl medkernel-backend -Dtest=MigrationBaselineContractTest test`
  - Expected: PASS。

### Task 2: Java 模型、Repository、权限和错误码

**Files:**
- Modify: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEvent.java`
- Modify: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventStatus.java`
- Modify: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventRepository.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventPayload.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventPayloadRepository.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventOutbox.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventOutboxRepository.java`
- Modify: `medkernel-backend/src/main/java/com/medkernel/engine/security/PermissionCode.java`
- Modify: `medkernel-backend/src/main/java/com/medkernel/engine/security/DefaultPermissionPolicy.java`
- Modify: `medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java`
- Test: `medkernel-backend/src/test/java/com/medkernel/engine/security/DefaultPermissionPolicyTest.java`
- Test: `medkernel-backend/src/test/java/com/medkernel/shared/api/error/ErrorCodeTest.java`

- [x] **Step 1: 写失败测试**
  - 断言 `event.read` 和 `event.write` 默认角色生效。
  - 断言 `ENG-EVENT-001..006` 的 HTTP 状态、分类和 retryable 正确。

- [x] **Step 2: 运行定向测试确认失败**
  - Run: `./mvnw -pl medkernel-backend -Dtest=DefaultPermissionPolicyTest,ErrorCodeTest test`
  - Expected: FAIL，提示权限码或错误码不存在。

- [x] **Step 3: 写最小实现**
  - 扩展实体和 repository。
  - 加入 `SUPERSEDED` 状态。
  - 加入权限码、默认角色和错误码。

- [x] **Step 4: 运行定向测试确认通过**
  - Run: `./mvnw -pl medkernel-backend -Dtest=DefaultPermissionPolicyTest,ErrorCodeTest test`
  - Expected: PASS。

## Chunk 2: 接收、查询与诊断

### Task 3: DTO 与 ClinicalEventService

**Files:**
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventRequest.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventBatchRequest.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventAcceptedResponse.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventBatchResponse.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventDetailResponse.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventPayloadResponse.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventReplayResponse.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventFilter.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventProperties.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventService.java`
- Test: `medkernel-backend/src/test/java/com/medkernel/engine/context/ClinicalEventServiceTest.java`

- [x] **Step 1: 写失败测试**
  - 覆盖异步接收落三张表、同 eventId 同 digest 幂等返回、同 eventId 不同 digest 报 409、payload 超限报 400、诊断返回 payload 摘要、重放新建 root_event_id。

- [x] **Step 2: 运行服务测试确认失败**
  - Run: `./mvnw -pl medkernel-backend -Dtest=ClinicalEventServiceTest test`
  - Expected: FAIL，提示服务或 DTO 不存在。

- [x] **Step 3: 写最小实现**
  - 从 `RequestContext` 读取 tenant/user/traceId。
  - payload 用 `ObjectMapper` 规范化后计算 SHA-256 digest。
  - 写入 `clinical_event`、`clinical_event_payload`、`clinical_event_outbox`。
  - 复用 `DiagnoseResponseAssembler` 组装诊断。

- [x] **Step 4: 运行服务测试确认通过**
  - Run: `./mvnw -pl medkernel-backend -Dtest=ClinicalEventServiceTest test`
  - Expected: PASS。

### Task 4: Controller 与安全测试

**Files:**
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventController.java`
- Test: `medkernel-backend/src/test/java/com/medkernel/engine/context/ClinicalEventControllerSecurityTest.java`

- [x] **Step 1: 写失败测试**
  - 覆盖未认证 401/403、缺租户 400、`event.write` 可创建、`event.read` 可查询、无写权限不能 replay。

- [x] **Step 2: 运行 Controller 测试确认失败**
  - Run: `./mvnw -pl medkernel-backend -Dtest=ClinicalEventControllerSecurityTest test`
  - Expected: FAIL。

- [x] **Step 3: 写最小实现**
  - 添加 8 个 endpoint。
  - 同步接收等待 worker 处理结果，超时返回受理状态。
  - 所有响应通过现有 API 包装方式返回。

- [x] **Step 4: 运行 Controller 测试确认通过**
  - Run: `./mvnw -pl medkernel-backend -Dtest=ClinicalEventControllerSecurityTest test`
  - Expected: PASS。

## Chunk 3: Outbox 处理、重试和死信

### Task 5: Processor 与 Worker

**Files:**
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventProcessor.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventProcessedEvent.java`
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventOutboxWorker.java`
- Test: `medkernel-backend/src/test/java/com/medkernel/engine/context/ClinicalEventOutboxWorkerTest.java`

- [x] **Step 1: 写失败测试**
  - 覆盖 PENDING → CLAIMED → PROCESSED、处理失败后退避、超过最大重试 DEAD、成功时发 Spring 事件。

- [x] **Step 2: 运行 worker 测试确认失败**
  - Run: `./mvnw -pl medkernel-backend -Dtest=ClinicalEventOutboxWorkerTest test`
  - Expected: FAIL。

- [x] **Step 3: 写最小实现**
  - 用 repository 查询待处理任务并标记 CLAIMED。
  - 每条事件单事务处理，失败隔离。
  - 写状态历史、审计和 outbox 状态。

- [x] **Step 4: 运行 worker 测试确认通过**
  - Run: `./mvnw -pl medkernel-backend -Dtest=ClinicalEventOutboxWorkerTest test`
  - Expected: PASS。

## Chunk 4: 文档、台账和全量验证

### Task 6: 更新任务台账与证据

**Files:**
- Modify: `docs/backlog.md`
- Modify: `docs/release/v1.0.0-ga-evidence.md`

- [x] **Step 1: 更新中文文档**
  - 将 `GA-ENG-API-02` 标为 done。
  - 修订记录写清 V10 迁移、事件 API、outbox、payload、诊断和验证结果。

- [x] **Step 2: 后端全量验证**
  - Run: `./mvnw -pl medkernel-backend test`
  - Expected: PASS。

- [x] **Step 3: 前端门禁验证**
  - Run: `cd frontend && npm run lint && npm run typecheck && npm test -- --run && npm run build`
  - Expected: PASS。

- [x] **Step 4: 最终状态检查**
  - Run: `git status --short`
  - Expected: 只包含本任务修改。

- [x] **Step 5: 提交**
  - Run: `git add docs medkernel-backend && git commit -m "feat: 完成临床事件 API"`
  - Expected: commit 成功。
