# GA-ENG-BASE-07 运行底座实施计划

> **给 AI 执行者：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐项实施。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 上线可测试、可展示、可交付的运行底座合同，覆盖 Feature Flag、运行配置、监控、健康、备份恢复和国产化 profile。

**架构：** 后端新增 `shared.runtime` 聚合运行快照；配置以 `medkernel.runtime.*` 为唯一入口；前端 `Provider 状态` 页面改读真实运行快照；部署脚本补备份摘要校验。所有变更先写失败测试，再实现。

**技术栈：** Spring Boot 3.3、Actuator、Micrometer、React 18、React Query、Ant Design、Docker Compose Bash 脚本。

---

### 任务 1：后端运行合同接口

**文件：**
- 新增：`medkernel-backend/src/test/java/com/medkernel/shared/runtime/RuntimeOperationsControllerTest.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/shared/runtime/RuntimeProperties.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/shared/runtime/RuntimeOperationsService.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/shared/runtime/RuntimeOperationsController.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/shared/runtime/RuntimeOperationsSnapshot.java`
- 修改：`medkernel-backend/src/main/resources/application.yml`
- 修改：`medkernel-backend/src/main/resources/application-dev.yml`
- 修改：`medkernel-backend/src/main/resources/application-test.yml`
- 修改：`medkernel-backend/src/main/resources/application-container.yml`

- [x] **步骤 1：编写失败的 API 测试**

创建 Spring Boot MockMvc 测试，访问 `/api/v1/system/operations`，断言返回标准 `ApiResult` 包络，并包含 `data.featureFlags`、`data.dependencies`、`data.backup`、`data.domesticProfile`、激活 profile 和运行元数据。

- [x] **步骤 2：运行红灯测试**

运行：`mvn -B -Dtest=RuntimeOperationsControllerTest test`

预期：失败，原因是接口返回 404 或控制器不存在。

- [x] **步骤 3：实现最小运行合同**

增加配置绑定、record DTO、服务和控制器。服务读取 `Environment`、`HealthEndpoint`、`MeterRegistry` 和 `RuntimeProperties`，且永不返回密钥。

- [x] **步骤 4：运行绿灯测试**

运行：`mvn -B -Dtest=RuntimeOperationsControllerTest test`

预期：通过。

### 任务 2：国产化 profile 与部署合同

**文件：**
- 新增：`medkernel-backend/src/main/resources/application-govcloud.yml`
- 新增：`medkernel-backend/src/test/java/com/medkernel/shared/runtime/RuntimeConfigurationContractTest.java`
- 修改：`deploy/docker/scripts/backup.sh`
- 修改：`deploy/docker/scripts/restore.sh`
- 修改：`deploy/docker/tests/validate-deployment-assets.sh`
- 修改：`docs/handbook/runbooks/backup-restore.md`
- 修改：`docs/handbook/runbooks/upgrade-rollback.md`

- [x] **步骤 1：编写失败的配置合同测试**

创建文件级合同测试，断言 `application-govcloud.yml` 存在，现有 profile 文件含 `medkernel.runtime` 键，备份脚本写入 `.sha256`，恢复脚本校验 `.sha256`，部署资产验证脚本也检查这些规则。

- [x] **步骤 2：运行红灯测试**

运行：`mvn -B -Dtest=RuntimeConfigurationContractTest test`

预期：失败，原因是 `application-govcloud.yml` 和摘要校验规则尚不存在。

- [x] **步骤 3：增加 profile 与摘要校验**

增加 `application-govcloud.yml`，声明达梦/金仓可配置数据库方言和 `medkernel.runtime.domestic-profile` 目标。更新备份/恢复脚本，加入可移植 SHA-256 辅助函数，并更新中文运行手册。

- [x] **步骤 4：运行绿灯测试和部署资产合同**

运行：

```bash
mvn -B -Dtest=RuntimeConfigurationContractTest test
./deploy/docker/tests/validate-deployment-assets.sh
```

预期：全部通过。

### 任务 3：前端真实运行页

**文件：**
- 新增：`frontend/src/pages/compliance/SystemProviders.test.tsx`
- 修改：`frontend/src/shared/api/hooks.ts`
- 修改：`frontend/src/pages/compliance/SystemProviders.tsx`

- [x] **步骤 1：编写失败的页面测试**

模拟 `useRuntimeOperations`，断言页面渲染运行快照、Feature Flag、依赖状态和备份摘要策略；同时断言旧静态 Provider 名称不再出现。

- [x] **步骤 2：运行红灯测试**

运行：`npm test -- SystemProviders.test.tsx`

预期：失败，原因是 hook 和真实渲染尚不存在。

- [x] **步骤 3：实现 hook 与页面**

增加类型化 `RuntimeOperationsSnapshot` 接口和 `useRuntimeOperations`；用紧凑指标、依赖表、Feature Flag 表和备份就绪面板替换静态 Provider 卡片。

- [x] **步骤 4：运行前端绿灯测试**

运行：

```bash
npm test -- SystemProviders.test.tsx
npm test -- pages.smoke.test.tsx
```

预期：全部通过。

### 任务 4：台账与完成验证

**文件：**
- 修改：`docs/backlog.md`
- 修改：`docs/superpowers/plans/2026-05-26-runtime-foundation.md`

- [x] **步骤 1：更新中文台账**

将 `GA-ENG-BASE-07` 标记为 `done`，负责人改为 `codex`，并增加 `4.9` 修订记录。

- [x] **步骤 2：完整验证**

运行：

```bash
mvn -B clean test
npm run verify && npm run build
./deploy/docker/tests/validate-deployment-assets.sh
git diff --check
```

预期：无失败；既有前端 warning 如实记录，但不作为本任务新增错误。

- [ ] **步骤 3：远程主干交付**

提交中文 commit，推送 `codex/ga-eng-base-07-runtime-foundation`，创建中文 PR，等待所有远端检查通过，squash 合并到远程 `main`，再确认 `origin/main` 包含合并提交并移除临时工作区。
