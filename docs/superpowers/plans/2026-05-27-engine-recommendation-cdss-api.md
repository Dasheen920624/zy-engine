# 推荐/CDSS API Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 `GA-ENG-API-07` 推荐/CDSS API 后端合同，提供触发、推荐卡、来源解释、医师反馈和疲劳治理输入的可审计最小闭环。

**Architecture:** 新增 `com.medkernel.engine.recommendation` 边界包，采用 Spring Data JDBC record 聚合、服务层校验、REST 控制器和五方言 Flyway 迁移。首版不生成医学推荐，只接收并保存上游候选卡和来源解释，为后续综合推荐引擎提供事实合同。

**Tech Stack:** Java 21、Spring Boot 3.3、Spring Data JDBC、Flyway、JUnit 5、AssertJ、MockMvc、H2。

---

## Chunk 1: 设计与迁移基线

### Task 1: 设计文档与实施计划

**Files:**
- Create: `docs/superpowers/specs/2026-05-27-engine-recommendation-cdss-api-design.md`
- Create: `docs/superpowers/plans/2026-05-27-engine-recommendation-cdss-api.md`

- [ ] **Step 1: 写入设计与计划**
  - 记录 API 边界、非目标、表结构、接口、错误码、医疗安全和验收标准。

- [ ] **Step 2: 检查文档语言**
  Run: `rg -n "[A-Za-z]{20,}" docs/superpowers/specs/2026-05-27-engine-recommendation-cdss-api-design.md docs/superpowers/plans/2026-05-27-engine-recommendation-cdss-api.md`
  Expected: 仅出现必要英文标识符或标题。

- [ ] **Step 3: 提交文档**
  Run: `git add docs/superpowers/specs/2026-05-27-engine-recommendation-cdss-api-design.md docs/superpowers/plans/2026-05-27-engine-recommendation-cdss-api.md && git commit -m "docs: 规划推荐 CDSS API"`

### Task 2: V13 五方言迁移

**Files:**
- Create: `medkernel-backend/src/main/resources/db/migration/h2/V13__recommendation_cdss_api.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/postgres/V13__recommendation_cdss_api.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/oracle/V13__recommendation_cdss_api.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/dm/V13__recommendation_cdss_api.sql`
- Create: `medkernel-backend/src/main/resources/db/migration/kingbase/V13__recommendation_cdss_api.sql`
- Modify: `medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java`
- Modify: `medkernel-backend/src/test/java/com/medkernel/migration/H2BaselineMigrationTest.java`
- Modify: `medkernel-backend/src/test/java/com/medkernel/migration/FlywayMultiDialectSmokeTest.java`

- [ ] **Step 1: 写失败迁移合同测试**
  - 期望迁移序列包含 V13。
  - 期望五张推荐表、索引、约束、租户字段、审计字段和状态字段存在。
  Run: `mvn -f medkernel-backend/pom.xml -Dtest=MigrationBaselineContractTest,H2BaselineMigrationTest test`
  Expected: FAIL，提示 V13 缺失。

- [ ] **Step 2: 新增五方言 V13 迁移**
  - 表：`recommendation_trigger`、`recommendation_card`、`recommendation_source`、`recommendation_feedback`、`recommendation_fatigue_signal`。
  - 约束：状态、风险、打扰级别、反馈类型、来源类型、疲劳信号、唯一业务键。
  - 索引：按租户时间、患者、场景、状态、风险、疲劳键和卡片来源查询。

- [ ] **Step 3: 跑迁移测试**
  Run: `mvn -f medkernel-backend/pom.xml -Dtest=MigrationBaselineContractTest,H2BaselineMigrationTest test`
  Expected: PASS。

- [ ] **Step 4: 提交迁移**
  Run: `git add medkernel-backend/src/main/resources/db/migration medkernel-backend/src/test/java/com/medkernel/migration && git commit -m "feat: 添加推荐 CDSS 迁移基线"`

## Chunk 2: 领域、服务与 API

### Task 3: 领域模型与仓储

**Files:**
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/recommendation/*.java`
- Modify: `medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java`
- Modify: `medkernel-backend/src/test/java/com/medkernel/shared/api/error/ErrorCodeTest.java`
- Test: `medkernel-backend/src/test/java/com/medkernel/engine/recommendation/RecommendationRepositoryTest.java`

- [ ] **Step 1: 写失败仓储测试**
  - 保存触发、推荐卡、来源、反馈、疲劳信号。
  - 按卡片和触发查询关联事实。
  Run: `mvn -f medkernel-backend/pom.xml -Dtest=RecommendationRepositoryTest,ErrorCodeTest test`
  Expected: FAIL，类或表不存在。

- [ ] **Step 2: 新增 record 聚合、枚举和 Repository**
  - 枚举覆盖触发状态、卡状态、风险、打扰级别、卡类型、来源类型、反馈类型、疲劳信号。
  - record 字段与 V13 表保持一致。
  - Repository 使用 Spring Data JDBC，复杂查询用 `@Query`。

- [ ] **Step 3: 新增错误码**
  - 添加 `ENG_REC_001` 至 `ENG_REC_006`。
  - 更新错误码唯一性测试。

- [ ] **Step 4: 跑领域测试**
  Run: `mvn -f medkernel-backend/pom.xml -Dtest=RecommendationRepositoryTest,ErrorCodeTest test`
  Expected: PASS。

### Task 4: 服务层校验与反馈流转

**Files:**
- Create/Modify: `medkernel-backend/src/main/java/com/medkernel/engine/recommendation/*.java`
- Test: `medkernel-backend/src/test/java/com/medkernel/engine/recommendation/RecommendationEngineServiceTest.java`

- [ ] **Step 1: 写失败服务测试**
  - 有卡触发落库并生成来源和疲劳信号。
  - 无卡触发状态为 `NO_CARD`。
  - 无来源卡被拒绝。
  - 高风险卡缺少医师确认被拒绝。
  - 医师反馈更新卡状态并写入疲劳信号。
  - 已关闭卡重复终态反馈被拒绝。
  - 诊断返回卡片、反馈、疲劳信号数量和安全提示。
  Run: `mvn -f medkernel-backend/pom.xml -Dtest=RecommendationEngineServiceTest test`
  Expected: FAIL，服务不存在。

- [ ] **Step 2: 实现 DTO 和服务**
  - Request/Response 全部使用 record 和 Bean Validation。
  - 服务使用 `RequestContext` 获取租户、操作者和 traceId。
  - 校验失败抛 `ApiException`，不吞掉来源或高风险门禁错误。

- [ ] **Step 3: 跑服务测试**
  Run: `mvn -f medkernel-backend/pom.xml -Dtest=RecommendationEngineServiceTest test`
  Expected: PASS。

### Task 5: REST API 与权限

**Files:**
- Create: `medkernel-backend/src/main/java/com/medkernel/engine/recommendation/RecommendationEngineController.java`
- Modify: `medkernel-backend/src/main/java/com/medkernel/engine/security/PermissionCode.java`
- Modify: `medkernel-backend/src/main/java/com/medkernel/engine/security/DefaultPermissionPolicy.java`
- Test: `medkernel-backend/src/test/java/com/medkernel/engine/recommendation/RecommendationEngineControllerSecurityTest.java`

- [ ] **Step 1: 写失败安全测试**
  - 医生可读卡和提交反馈，但缺租户被数据范围拒绝。
  - 医生不能创建触发。
  - 信息科或医务处可创建触发，但缺租户被数据范围拒绝。
  - 访客不能读取推荐卡。
  Run: `mvn -f medkernel-backend/pom.xml -Dtest=RecommendationEngineControllerSecurityTest test`
  Expected: FAIL，接口或权限不存在。

- [ ] **Step 2: 实现控制器和权限**
  - `POST /api/v1/engine/recommendations/triggers`
  - `GET /api/v1/engine/recommendations/cards`
  - `GET /api/v1/engine/recommendations/cards/{cardId}`
  - `GET /api/v1/engine/recommendations/cards/{cardId}/sources`
  - `POST /api/v1/engine/recommendations/cards/{cardId}/feedback`
  - `GET /api/v1/engine/recommendations/fatigue-signals`
  - `GET /api/v1/engine/recommendations/triggers/{triggerId}/diagnose`

- [ ] **Step 3: 跑 API 安全测试**
  Run: `mvn -f medkernel-backend/pom.xml -Dtest=RecommendationEngineControllerSecurityTest test`
  Expected: PASS。

## Chunk 3: 台账、验证与合并

### Task 6: 更新台账

**Files:**
- Modify: `docs/backlog.md`

- [ ] **Step 1: 将 `GA-ENG-API-07` 标记为 `codex done`**
  - 修订记录追加 4.18，说明 V13 迁移、推荐 API 和验证结果。

- [ ] **Step 2: 提交台账**
  Run: `git add docs/backlog.md && git commit -m "docs: 更新推荐 CDSS API 台账"`

### Task 7: 完整验证与 PR

- [ ] **Step 1: 跑推荐相关测试**
  Run: `mvn -f medkernel-backend/pom.xml -Dtest=RecommendationRepositoryTest,RecommendationEngineServiceTest,RecommendationEngineControllerSecurityTest,MigrationBaselineContractTest,H2BaselineMigrationTest,ErrorCodeTest test`
  Expected: PASS。

- [ ] **Step 2: 跑完整后端测试**
  Run: `mvn -f medkernel-backend/pom.xml test`
  Expected: PASS；无 Docker 时 Testcontainers 多方言烟测按既有机制跳过。

- [ ] **Step 3: 检查空白错误**
  Run: `git diff --check`
  Expected: 无输出。

- [ ] **Step 4: 推送、创建中文 PR、等待检查并合并到远端 `main`**
  - PR 必须说明变更范围、验证结果、未完成事项、医疗安全影响、部署和数据迁移影响。
