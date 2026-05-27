# 评估质控 API 实施计划

> **供代理执行者使用：** 实施本计划时必须采用测试驱动开发；如获得并行代理授权，可使用 `superpowers:subagent-driven-development`，否则使用 `superpowers:executing-plans`。每个步骤使用复选框跟踪。

**目标：** 完成 `GA-ENG-API-08` 评估质控 API 后端合同，提供指标版本、运行事实、结果、质控问题、整改和复核的可审计最小闭环。

**架构：** 新增 `com.medkernel.engine.evaluation` 边界包，采用 Spring Data JDBC 记录模型、服务层状态门禁、REST 控制器和五方言 Flyway `V14` 迁移。首版只接收授权上游或人工质控形成的事实，不实现指标计算、病例扫描或模型增强；`GA-ENG-EVAL-01` 继续承接执行引擎。

**技术栈：** Java 21、Spring Boot 3.3、Spring Data JDBC、Flyway、JUnit 5、AssertJ、MockMvc、H2。

---

## Chunk 1：迁移与领域合同

### Task 1：V14 五方言迁移合同

**文件：**
- 新建：`medkernel-backend/src/main/resources/db/migration/h2/V14__evaluation_quality_api.sql`
- 新建：`medkernel-backend/src/main/resources/db/migration/postgres/V14__evaluation_quality_api.sql`
- 新建：`medkernel-backend/src/main/resources/db/migration/oracle/V14__evaluation_quality_api.sql`
- 新建：`medkernel-backend/src/main/resources/db/migration/dm/V14__evaluation_quality_api.sql`
- 新建：`medkernel-backend/src/main/resources/db/migration/kingbase/V14__evaluation_quality_api.sql`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/H2BaselineMigrationTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/FlywayMultiDialectSmokeTest.java`

- [ ] **Step 1：写入失败迁移合同测试**
  - 将权威迁移序列扩展到 `V14__evaluation_quality_api.sql`。
  - 断言六张表：`evaluation_indicator`、`evaluation_run`、`evaluation_result`、`quality_finding`、`rectification_task`、`rectification_review`。
  - 断言租户字段、审计字段、状态字段、业务唯一约束和常用查询索引。
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=MigrationBaselineContractTest,H2BaselineMigrationTest test`
  - 预期：失败，指出 `V14` 或新表缺失。

- [ ] **Step 2：新增五方言 V14 迁移**
  - `evaluation_indicator`：版本口径、来源、适用范围、发布/激活状态；约束状态为 `DRAFT/PENDING_REVIEW/PUBLISHED/ACTIVE/OFFLINE/ARCHIVED`。
  - `evaluation_run`：事实入口与诊断状态；约束运行类型为 `MANUAL_SAMPLE/UPSTREAM_RESULT/BATCH_IMPORT`、状态为 `RECEIVED/RECORDED/FAILED`。
  - `evaluation_result`：指标版本快照、对象类型、判定级别和证据摘要；约束级别为 `PASS/ATTENTION/NON_COMPLIANT/CRITICAL`。
  - `quality_finding`：问题级别和闭环状态；约束级别为 `P0/P1/P2/P3`、状态为 `NEW/ASSIGNED/REMEDIATING/CLOSED/WAIVED`。
  - `rectification_task`：整改状态、责任、时限和提交证据；约束状态为 `ASSIGNED/SUBMITTED/RETURNED/CLOSED/WAIVED`。
  - `rectification_review`：追加式结论；约束结论为 `APPROVED/RETURNED/WAIVED`。

- [ ] **Step 3：验证迁移合同转绿**
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=MigrationBaselineContractTest,H2BaselineMigrationTest test`
  - 预期：通过。

- [ ] **Step 4：提交迁移切片**
  - 提交信息：`feat: 添加评估质控迁移基线`

### Task 2：领域记录、仓储、错误码和权限编码

**文件：**
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationIndicator.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationRun.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationResult.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/QualityFinding.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/RectificationTask.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/RectificationReview.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/*Status.java` 与业务枚举文件
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/*Repository.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/engine/security/PermissionCode.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/engine/security/DefaultPermissionPolicy.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/shared/api/error/ErrorCodeTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/engine/security/DefaultPermissionPolicyTest.java`
- 新建：`medkernel-backend/src/test/java/com/medkernel/engine/evaluation/EvaluationRepositoryTest.java`

- [ ] **Step 1：写失败仓储与编码测试**
  - 保存并按租户读取六类记录。
  - 校验新增 `ENG_EVAL_001` 至 `ENG_EVAL_007` 可 roundtrip。
  - 校验 `evaluation.execute/remediate/review` 默认授权：质控办全闭环、信息科仅运行写入、科主任仅整改、医生不获整改/复核。
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=EvaluationRepositoryTest,ErrorCodeTest,DefaultPermissionPolicyTest test`
  - 预期：失败，提示新记录、表或枚举缺失。

- [ ] **Step 2：实现记录模型、枚举和 Repository**
  - 记录字段与 `V14` 精确一致；使用 `@Table`、`@Id` 与 `@Column` 延续现有 Spring Data JDBC 风格。
  - Repository 提供租户隔离查询、状态筛选、分页查询、同编码激活版本查询及问题关联明细查询。

- [ ] **Step 3：实现错误码与权限编码**
  - 追加错误码 `ENG-EVAL-001..007`。
  - 追加权限 `evaluation.execute`、`evaluation.remediate`、`evaluation.review`，不改动已发布权限顺序含义。
  - 更新默认权限策略的职责分离映射。

- [ ] **Step 4：验证领域合同转绿**
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=EvaluationRepositoryTest,ErrorCodeTest,DefaultPermissionPolicyTest test`
  - 预期：通过。

- [ ] **Step 5：提交领域切片**
  - 提交信息：`feat: 建立评估质控领域合同`

## Chunk 2：指标和运行事实

### Task 3：指标版本状态机与 API

**文件：**
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationIndicatorCreateRequest.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationIndicatorFilter.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationIndicatorResponse.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationEngineService.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationEngineController.java`
- 新建：`medkernel-backend/src/test/java/com/medkernel/engine/evaluation/EvaluationEngineServiceTest.java`
- 新建：`medkernel-backend/src/test/java/com/medkernel/engine/evaluation/EvaluationEngineControllerSecurityTest.java`

- [ ] **Step 1：写失败指标服务测试**
  - 创建指标产生 `DRAFT` 版本并写审计/状态历史。
  - `DRAFT -> PENDING_REVIEW -> PUBLISHED -> ACTIVE` 顺序流转。
  - 激活新版本时同编码旧 `ACTIVE` 版本变为 `OFFLINE`。
  - 非法状态转换返回 `ENG-EVAL-003`。
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=EvaluationEngineServiceTest test`
  - 预期：失败，服务或状态流转不存在。

- [ ] **Step 2：实现指标 DTO 与状态机**
  - 创建请求必须包含编码、版本、名称、对象类型、分母、分子、时间窗、组织范围、责任科室与来源。
  - 服务从 `RequestContext` 获取租户、操作者和 `traceId`。
  - 激活以同事务保存新旧状态，审计动作复用 `CREATE/REVIEW/PUBLISH/UPDATE`。

- [ ] **Step 3：写失败指标权限测试**
  - 质控办可创建、提交、发布与激活，但无租户时被拒绝。
  - 医务处可读但不可发布评估指标。
  - 科主任可读但不可创建指标。
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=EvaluationEngineControllerSecurityTest test`
  - 预期：失败，控制器尚不存在。

- [ ] **Step 4：实现指标接口并验证转绿**
  - 实现 `POST/GET /api/v1/engine/evaluations/indicators`、详情、`submit`、`publish`、`activate`。
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=EvaluationEngineServiceTest,EvaluationEngineControllerSecurityTest test`
  - 预期：通过。

### Task 4：运行、结果与问题入库门禁

**文件：**
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationRunRequest.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationResultRequest.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/QualityFindingRequest.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationRunResponse.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/QualityFindingDetailResponse.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationEngineService.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationEngineController.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/engine/evaluation/EvaluationEngineServiceTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/engine/evaluation/EvaluationEngineControllerSecurityTest.java`

- [ ] **Step 1：写失败运行事实测试**
  - 使用 `ACTIVE` 指标创建运行，保存运行和结果，状态为 `RECORDED`。
  - 使用 `DRAFT/OFFLINE` 指标运行返回 `ENG-EVAL-004`。
  - 结果缺少证据摘要返回 `ENG-EVAL-001`。
  - `P0/P1` 问题缺责任科室、期限或证据时返回 `ENG-EVAL-006`。
  - 合法 `P0/P1` 问题同步创建 `ASSIGNED` 整改任务。
  - `P2/P3` 不携带派单字段时只保存 `NEW` 问题。

- [ ] **Step 2：运行红灯测试**
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=EvaluationEngineServiceTest test`
  - 预期：失败，运行服务方法不存在。

- [ ] **Step 3：实现运行入库和查询**
  - 实现 `POST /api/v1/engine/evaluations/run`。
  - 实现 `GET /api/v1/engine/evaluations/results`、`GET /api/v1/engine/evaluations/findings` 与问题详情。
  - 每次运行、问题派单记录状态历史与审计事件。

- [ ] **Step 4：补权限测试并验证转绿**
  - 质控办与信息科可写运行事实；科主任与医生不可写运行事实。
  - 所有查询无租户上下文时返回现有租户错误。
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=EvaluationEngineServiceTest,EvaluationEngineControllerSecurityTest test`
  - 预期：通过。

- [ ] **Step 5：提交运行事实切片**
  - 提交信息：`feat: 实现评估质控运行事实接口`

## Chunk 3：整改、复核、诊断与交付

### Task 5：整改复核闭环与运行诊断

**文件：**
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/RectificationSubmitRequest.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/RectificationReviewRequest.java`
- 新建：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/RectificationResponse.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationEngineService.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/EvaluationEngineController.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/engine/evaluation/EvaluationEngineServiceTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/engine/evaluation/EvaluationEngineControllerSecurityTest.java`

- [ ] **Step 1：写失败闭环与诊断测试**
  - `ASSIGNED/RETURNED` 任务可提交整改，问题进入 `REMEDIATING`。
  - 复核 `APPROVED` 关闭任务和问题并写不可覆写记录。
  - 复核 `RETURNED` 将任务退回并保持问题待整改。
  - `P0` 问题请求 `WAIVED` 返回 `ENG-EVAL-007`，其他豁免必须填写理由。
  - 运行诊断返回结果、问题、整改关联与运行 `traceId`。

- [ ] **Step 2：运行红灯测试**
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=EvaluationEngineServiceTest test`
  - 预期：失败，整改/复核/诊断方法不存在。

- [ ] **Step 3：实现整改、复核和诊断**
  - 实现 `POST /findings/{findingId}/rectification` 与 `POST /findings/{findingId}/review`。
  - 实现 `GET /runs/{runId}/diagnose`，复用 `DiagnoseResponseAssembler`。
  - 写入 `AuditAction.UPDATE/REVIEW` 和 `StateTransitionRecorder`。

- [ ] **Step 4：补安全测试并验证转绿**
  - 科主任可提交整改但不能复核；质控办可复核；信息科不能关闭问题；医生不具备闭环操作权限。
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=EvaluationEngineServiceTest,EvaluationEngineControllerSecurityTest test`
  - 预期：通过。

- [ ] **Step 5：提交闭环切片**
  - 提交信息：`feat: 完成评估质控整改复核闭环`

### Task 6：台账、完整验证和 PR

**文件：**
- 修改：`docs/backlog.md`

- [ ] **Step 1：执行专项验证**
  - 运行：`mvn -f medkernel-backend/pom.xml -Dtest=MigrationBaselineContractTest,H2BaselineMigrationTest,EvaluationRepositoryTest,EvaluationEngineServiceTest,EvaluationEngineControllerSecurityTest,ErrorCodeTest,DefaultPermissionPolicyTest test`
  - 预期：通过。

- [ ] **Step 2：执行完整后端验证**
  - 运行：`mvn -f medkernel-backend/pom.xml test`
  - 预期：通过；本机 Docker 不可用时，容器依赖的三项多方言烟测按既有机制跳过。

- [ ] **Step 3：更新单一任务台账**
  - 将 `GA-ENG-API-08` 标记为 `codex / done`。
  - 追加中文修订记录，写明 `V14` 表族、接口范围、权限、医疗安全边界与测试数量。

- [ ] **Step 4：检查提交内容并提交文档**
  - 运行：`git diff --check`
  - 预期：无输出。
  - 提交信息：`docs: 更新评估质控 API 台账`

- [ ] **Step 5：推送、创建中文 PR 并按门禁合并**
  - PR 描述写清变更范围、验证结果、未完成事项、医疗安全影响、部署与数据迁移影响。
  - 等待远端检查通过后合并至 `main`，确认 `origin/main` 包含合并提交。
