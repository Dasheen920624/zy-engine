# AI 团队 v1.0 GA 并行领单卡

> 版本：1.0 · 2026-05-23  
> 当前基线：`v0.3-final` 已发布，tag 指向 main `339616f`。  
> 下一版本：`v1.0 GA`，目标 tag 为 `v1.0.0`。不再新增 `v0.3-pilot` 作为独立开发版本；真实医院试点验证并入 v1.0 GA 准入证据。

本文是 v1.0 GA 阶段 AI 团队的唯一新任务入口。旧的 `PR-V2-*`、`PR-V3-*`、`PR-FINAL-*` 文档只作历史追溯和实现证据，不再作为可领取任务来源。

---

## 0. GA 完成定义

v1.0 GA 不是“能演示”，而是“可以签正式合同、交付客户、承担 SLA”。所有任务围绕 7 个硬指标收口：

| 维度 | GA 硬指标 |
|---|---|
| D1 编译与测试 | 后端 compile/test、前端 lint/typecheck/test/build、E2E 6 剧本、覆盖率后端 70% / 前端 60% |
| D2 功能完整 | 27 个菜单 100% 可达真实页面，无客户可见 PlaceholderPage；6 大剧本一键 fixture 可跑通 |
| D3 性能并发 | 100 并发 30 min 无连接泄漏，核心 API P95 < 300ms，有基线报告 |
| D4 合规安全 | 等保 2.0 三级、国密密评、个保法、数据出境、审计链、MFA/RBAC/脱敏闭环 |
| D5 跨数据库 | Oracle、达梦、PostgreSQL、KingbaseES smoke 全过，Flyway migration 可升级可回滚 |
| D6 文档培训 | 用户手册、运维手册、API OpenAPI、培训材料、合同/SLA/隐私政策齐备 |
| D7 运维就绪 | Prometheus/Grafana/告警/APM、备份恢复演练、升级回滚演练、SLA 证据齐备 |

---

## 1. 并发开发铁律

所有 GA 任务默认采用“任务分支 + claim + lock + review”的模式，避免多个 AI 同时改同一处。

| 规则 | 要求 |
|---|---|
| 分支 | 每个任务使用 `ai/<TASK-ID>/<slug>` 分支；AI 不直接在 `main` 开发，批量并发时也不直接推 `develop` |
| 认领 | 先跑 `scripts/verify-task-prereq.ps1 -TaskId <TASK-ID> -Level <level>`，再创建 active claim 和 active_locks |
| 独占范围 | `write_scope` 必须写具体文件或目录；共享文件只允许架构师 AI 任务修改 |
| 冲突检查 | 开工前、提交前都必须跑 `medkernel-mvp/scripts/check-ai-collaboration.ps1 -Strict` |
| Review | 任务完成后创建 pending review；`open_findings=0` 后才允许合入 develop |
| 归档 | claim/review 归档时必须同提交删除对应 lock；残留 lock 视为阻断项 |

共享文件清单：`frontend/src/api/types.ts`、`frontend/src/router/menuConfig.tsx`、`frontend/src/router/routes.tsx`、`frontend/src/styles/tokens.css`、`frontend/src/App.tsx`、`medkernel-mvp/pom.xml`、`medkernel-mvp/src/main/resources/application.yml`、`scripts/verify-pr.ps1`、`scripts/verify-task-prereq.ps1`、`medkernel-mvp/scripts/check-ai-collaboration.ps1`、`docs/AI_CHARTER.md`、`docs/PRODUCT_ARCHITECTURE_FINAL.md`、`docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md`。

---

## 2. 最大并行批次

### Batch 0 · 开工闸门

| 任务 | 优先级 | 等级 | 依赖 | 独占范围 | DoD |
|---|---|---|---|---|---|
| GA-GOV-01 | P0 | 架构师 | 无 | `scripts/**`、`medkernel-mvp/scripts/check-ai-collaboration.ps1`、`ai-dev-input/10_task_claims/**` | orphan lock、重复 task、write_scope 重叠均自动阻断 |
| GA-GOV-02 | P0 | 架构师 | GA-GOV-01 | `docs/**`、`ai-dev-input/README.md` | 所有入口明确“下一版本 = v1.0 GA”，旧 v0.3 文档标历史 |
| GA-PROD-01 | P0 | 产品架构师 | GA-GOV-02 | `docs/PRODUCT_SIMPLIFICATION_V1_GA.md`、`docs/04_页面规格书.md`、`frontend/src/pages/{ConfigPackages,Pathway,Rule,Graph}/**` | 配置包、路径、规则、图谱等全部客户可见功能符合“1 个主目标、1 个主按钮、最多 3 个默认筛选，高级能力折叠”的极简/完整/易用原则；主菜单保持左侧 SideMenu，不改顶部导航 |
| GA-REL-01 | P0 | 架构师 | GA-GOV-01 | `.github/workflows/**`、`scripts/**`、`VERSIONING.md` | main/develop 保护、release evidence、tag 流程可自动校验 |

Batch 0 完成前，不允许大规模派发业务任务。

### Batch 1 · 质量门禁与客户可见完整度

| 任务 | 优先级 | 等级 | 可并行 | 独占范围 | DoD |
|---|---|---|---|---|---|
| GA-QA-01 | P0 | 高级 | 是 | `medkernel-mvp/pom.xml`、`medkernel-mvp/src/test/**`、`.github/workflows/**` | Jacoco 覆盖率报告接入，后端目标 70% |
| GA-QA-02 | P0 | 高级 | 是 | `frontend/**`、`.github/workflows/**` | Vitest coverage + 前端 CI，目标 60% |
| GA-QA-03 | P0 | 高级 | 是 | `frontend/e2e/**`、`ai-dev-input/06_samples/scenarios/**` | 6 大客户剧本 E2E 稳定 PASS |
| GA-UX-01 | P0 | 高级 | 是 | `frontend/src/router/**`、`frontend/src/pages/**` | 客户可见路由无 PlaceholderPage、无 404、无空白页 |
| GA-UX-02 | P0 | 高级 | 是 | `frontend/src/pages/**`、`docs/04_页面规格书.md` | 所有页面按 PRODUCT_SIMPLIFICATION §6 复核：默认路径清晰，高级参数折叠，客户不需要理解技术名词即可完成主任务 |
| GA-API-01 | P0 | 架构师 | 是 | `medkernel-mvp/src/main/java/**`、`docs/engineering/api-examples.http` | OpenAPI 与 Controller 一致，前端类型生成可复现 |

### Batch 2 · 安全合规与生产运维

| 任务 | 优先级 | 等级 | 可并行 | 独占范围 | DoD |
|---|---|---|---|---|---|
| GA-SEC-01 | P0 | 架构师 | 是 | `medkernel-mvp/src/main/java/com/medkernel/security/**`、`docs/engineering/COMP-001_合规基线与证据包.md` | 等保 2.0 三级控制点自查闭环 |
| GA-SEC-02 | P0 | 架构师 | 是 | `medkernel-mvp/src/main/java/com/medkernel/common/crypto/**`、`application.yml` | 国密套件配置、密钥轮换、兼容模式文档齐备 |
| GA-DATA-01 | P0 | 架构师 | 是 | `medkernel-mvp/src/main/java/com/medkernel/**`、`ai-dev-input/04_database/**` | HEALTH_DATA 加密、脱敏、最小化展示策略落地 |
| GA-OPS-01 | P0 | 高级 | 是 | `monitoring/**`、`deploy/**` | Grafana 看板、告警规则、healthcheck、SLO 证据齐备 |
| GA-DB-01 | P0 | 高级 | 是 | `ai-dev-input/04_database/**`、`medkernel-mvp/src/main/resources/db/**` | 4 方言 smoke 矩阵和 Flyway rollback 证据齐备 |

### Batch 3 · 商业化交付材料

| 任务 | 优先级 | 等级 | 可并行 | 独占范围 | DoD |
|---|---|---|---|---|---|
| GA-DOC-01 | P1 | 中级 | 是 | `docs/manuals/**` | 4 治理模块用户手册可直接给医院信息科 |
| GA-DOC-02 | P1 | 高级 | 是 | `docs/ops/**`、`deploy/**` | 运维手册、备份恢复、升级回滚、故障应急手册齐备 |
| GA-DOC-03 | P1 | 中级 | 是 | `docs/training/**` | 医院 IT、医生、实施工程师培训材料齐备 |
| GA-LEGAL-01 | P1 | 高级 | 是 | `docs/legal/**` | 合同、SLA、隐私政策、DPA 模板完成法务初稿 |
| GA-COMM-01 | P1 | 高级 | 是 | `medkernel-mvp/src/main/java/com/medkernel/license/**`、`frontend/src/pages/**` | License、用量报告、授权到期提醒闭环 |

### Batch 4 · 技术债收口

| 任务 | 优先级 | 等级 | 可并行 | 独占范围 | DoD |
|---|---|---|---|---|---|
| GA-DTO-01 | P0 | 高级 | 是 | `medkernel-mvp/src/main/java/com/medkernel/adapter/**` | Adapter Controller 入参 DTO + @Valid，无新增 raw Map |
| GA-DTO-02 | P0 | 高级 | 是 | `medkernel-mvp/src/main/java/com/medkernel/knowledge/**` | Knowledge / AI review Controller DTO 化 |
| GA-DTO-03 | P1 | 高级 | 是 | `medkernel-mvp/src/main/java/com/medkernel/quality/**`、`cdss/**` | 质控 / CDSS Controller DTO 化 |
| GA-REFIT-01 | P1 | 架构师 | 是 | `medkernel-mvp/src/main/java/com/medkernel/**` | 存量 >800 行文件拆分到可维护边界 |
| GA-PERF-01 | P1 | 高级 | 是 | `scripts/perf/**`、`docs/performance/**` | 100 并发 30 min 压测报告，P95/P99/错误率可追溯 |

---

## 3. 领取顺序

1. 先完成 Batch 0，确认并发机制、文档入口、release 闸门都通过。
2. Batch 1 和 Batch 2 可以最大并行，但共享文件由架构师集中提交。
3. Batch 3 可与 Batch 2 后半段并行，由文档/实施/法务 AI 分流。
4. Batch 4 穿插执行，但不能阻塞 P0 客户验收路径。

---

## 4. 出厂评审清单

v1.0 GA 发布前必须由 release manager 出具一份 `docs/release/v1.0.0-ga-evidence.md`，至少包含：

| 证据 | 必须内容 |
|---|---|
| 代码质量 | commit、CI 链接、覆盖率报告、E2E 报告 |
| 安全合规 | 等保/密评/渗透测试状态，未完成项风险接受记录 |
| 客户体验 | 6 大剧本演示录像或截图，客户可见页面清单 |
| 数据库 | Oracle/DM/PostgreSQL/KingbaseES smoke 结果 |
| 运维 | 监控、告警、备份恢复、升级回滚演练结果 |
| 文档 | 用户手册、运维手册、培训材料、合同/SLA 模板 |
| 发布 | tag、artifact checksum/signature、rollback 指南 |

---

**End of v1.0 GA backlog.**
