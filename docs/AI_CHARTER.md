# MedKernel · AI 实施宪法（1 页纸）

> 版本：1.0 · 2026-05-21  
> 任何 AI 接手任何任务前**必读 5 分钟**。本文是「**最少必要约束**」，违反任何一条 = 任务自动拒绝。详细规范见末尾「权威文档链」。

---

## 0. 上手前 60 秒自检（5 个问题答不上来就别动手）

1. **我在哪个分支？** → AI 必须在 `ai/<TASK-ID>/<slug>` 或 `develop`。**不许直接动 main**。
2. **任务编号是什么？** → 必须在 [`docs/engineering/02_任务台账.md`](engineering/02_任务台账.md) 里能找到。
3. **我的能力级别？** → 看 [`AI能力分级匹配清单.md`](engineering/AI能力分级匹配清单.md) — 初级不能领架构/跨模块任务。
4. **我要改哪些文件？** → 在 [`ai-dev-input/10_task_claims/active/`](../ai-dev-input/10_task_claims/active/) 显式声明 `write_scope`，**别人锁的文件不许碰**。
5. **DEVELOP_HEALTH 是什么颜色？** → 看 [`ai-dev-input/00_DEVELOP_HEALTH.md`](../ai-dev-input/00_DEVELOP_HEALTH.md) — 🔴 就**只能修编译错误**，不准领新任务。

---

## 1. 产品红线（违反即 STOP，无例外）

来自 [`docs/01_产品事实源.md §7 22 不变量`](01_产品事实源.md)，下面 7 条是 AI 最容易踩的：

| # | 红线 | 怎么落地 |
|---|---|---|
| R1 | **不许硬编码单医院 / 单院区 / 单系统厂商逻辑** | 所有业务表必须有 `tenant_id`；组织差异通过 `OrganizationContext` 或配置包解决 |
| R2 | **DB-only 模式必须可独立验收** | 不许把 Neo4j / Dify / 大模型设为强依赖。所有外部 Provider 必须可降级 |
| R3 | **同 code+version 配置内容不同 = ERROR** | 发布前比对 `content_hash`，不许静默覆盖已发布版本 |
| R4 | **关键医疗建议不许绕过医生确认** | `action_mode=BLOCK` 必须有 `doctor_decision` 字段，医生填理由才能继续 |
| R5 | **医学/医保/质控配置缺来源不许发布**（ADR-0004） | 调用 `SourceReviewChecker`；缺则返回 `MISSING_SOURCE` 阻断 |
| R6 | **发布 / 回滚 / 审核必须写审计** | 走 `ENGINE_AUDIT_LOG`；审计表不许 DELETE / UPDATE |
| R7 | **日志不许打密码 / API Key / 患者完整身份** | 用户名可打，身份证 / 手机号必须脱敏（前 3 后 4） |

---

## 2. 代码硬约束（CI 会自动卡）

### 后端（Spring Boot 2.7 / JDK 1.8）

- ✅ **Controller 必须返回 `ApiResult<T>`**，禁止裸返 `List<X>` / `Map`
- ✅ **三层强制**：Controller → Service → Repository（Controller **禁止 @Autowired Repository**）
- ✅ **Controller 入参必须 DTO + `@Valid`**，**禁止 `@RequestBody Map<String, Object>`**（新增即 FAIL）
- ✅ **JDK 1.8 only**：禁止 `String.repeat()` `List.of()` `Map.of()` `Optional#or` `Stream#toList()` 等 9+ API
- ✅ **SQL 跨 5 方言**：禁止 MySQL 专有 `ON DUPLICATE KEY UPDATE` / `LIMIT N` / 反引号 `；用 SELECT→UPDATE/INSERT 二选一
- ✅ **包名小写无连字符**：`com.medkernel.datagovernance`（不是 `data-governance`）
- ✅ **业务表必带 `tenant_id NOT NULL`**；写操作 `created_by` 从 `SecurityContextHolder` 取
- ✅ **catch 不许吞**：`catch (Exception e) { return null; }` = FAIL；必须 `throw new ApiException(...)` 或显式记 log + rethrow
- ⚠️ **`DriverManager.getConnection` 净增 = WARN**（KD-004 历史债 29 处，HikariCP 接入前不许新增）
- ⚠️ **单文件 > 500 行 = WARN**，> 800 行 = FAIL（除非已在豁免名单）

### 前端（React 18 + TS strict + Vite 5 + AntD 5）

- ✅ **颜色 / 字号 / 间距必须用 `var(--mk-*)` token**（ADR-0003），ESLint `no-hardcoded-color` 自动拦
- ✅ **禁止 `any`** — `tsconfig.strict` 已开；`@ts-ignore` 必须改成 `@ts-expect-error <理由>`
- ✅ **禁止 `console.log` / `alert` / `confirm` / `prompt`** — 进 production 即 FAIL
- ✅ **状态语义用 `<StatusBadge>` 不用 `<Tag>`**；含医学语义的组件必须挂 `<SourceInfo>`
- ✅ **AntD 默认色禁用** — 必须用 `ConfigProvider theme.token.colorPrimary = var(--mk-primary)`
- ✅ **单函数 > 80 行 / 单文件 > 500 行 = WARN**

### 文档

- ✅ **禁止引用 `docs/_archive/` / `frontend-prototype/` / `zy-engine-mvp/`**（已物理删除，只能 git checkout 历史 tag 溯源）
- ✅ **新增功能 = 先更 `01_产品事实源.md §5` + `04_页面规格书.md`**，禁止「先实现后补设计」
- ✅ **架构变更 = 先开 ADR**（用 [`adr/template.md`](engineering/adr/template.md)），禁止 ADR 静默违反
- ✅ **新增治理域 = 先在 [`docs/engineering/2026-05-21-功能矩阵-V3.md`](engineering/2026-05-21-功能矩阵-V3.md) §1 加小节**，否则不动手

---

## 3. 必读 5 份文档（按需展开）

| 任务类型 | 必读 |
|---|---|
| **任何任务** | 本文 + [`02_任务台账.md`](engineering/02_任务台账.md) + [`forbidden-patterns.md`](engineering/forbidden-patterns.md) |
| 后端新 API | [`06_后端开发规范.md`](engineering/06_后端开发规范.md) + [`数据库Provider与离线AI开发约定.md`](engineering/数据库Provider与离线AI开发约定.md) |
| 前端新页面 | [`03_设计系统.md`](03_设计系统.md) + [`04_页面规格书.md`](04_页面规格书.md)（只读你那一节）+ [`07_前端开发规范.md`](engineering/07_前端开发规范.md) |
| 架构变更 | 4 个 ADR（[`adr/`](engineering/adr/)）+ [`05_架构总图与服务边界.md`](engineering/05_架构总图与服务边界.md) |
| 涉及医学/医保/质控 | [`产品功能业务核查与开工清单.md`](engineering/产品功能业务核查与开工清单.md) |
| 涉及部署/国产化 | [`08_国产化兼容性规约.md`](engineering/08_国产化兼容性规约.md) + [`09_内网部署与版本管理.md`](engineering/09_内网部署与版本管理.md) |

---

## 4. 提交流程（每步缺一步都不许 push）

```
1. scripts/verify-task-prereq.ps1 -TaskId PR-V2-XX -Level senior   ← 接手前自检
2. 在 ai-dev-input/10_task_claims/active/ 创建 claim（声明 write_scope）
3. 读 docs/engineering/reference-implementations/<对应样板>
4. 编码（ESLint / TypeScript strict 实时拦）
5. scripts/verify-pr.ps1 -TaskId PR-V2-XX                          ← 提交前自检（10+ 项 grep + mvn compile + npm lint/test/build）
6. 全 PASS（FAIL=0）才能 commit + push HEAD:develop
7. 在 ai-dev-input/11_ai_reviews/pending/ 创建 review
8. APPROVED 后归档 claim / review
```

> **commit 信息约定**：`<TASK-ID>: <动词> <对象>`，正文用中文短句。
> **PR 目标分支永远是 `develop`**，`develop → main` 的发布 PR 只能由人发起。

---

## 5. 当前已知缺陷（KD-001~007，AI 接手相关任务前先看这里）

来自 [`docs/engineering/2026-05-21-功能矩阵-V3.md §5`](engineering/2026-05-21-功能矩阵-V3.md) + [`AUDIT-20260521-V3-基线核查.md`](engineering/AUDIT-20260521-V3-基线核查.md)：

| 编号 | 描述 | 触碰这块前怎么办 |
|---|---|---|
| KD-001 | `WorkflowTodoService` 全 Mock，审批/驳回/转办不落库 | 触碰 `workflow/` → 先看 WF-001 是否还在 active，没接通持久化别演示审批流 |
| KD-002 | `MpiController` 双副本（patient/ + patientindex/） | 触碰 MPI 前必须查 ADR-0005（如未起草，先起草并跟产品对齐保留哪份） |
| KD-003 | `UserSyncController` 双副本（security/ + security/usersync/） | 同上，需 ADR 决定 |
| KD-004 | HikariCP 未接入，**29 个文件**直接 `DriverManager.getConnection`（pom 缺 spring-boot-starter-jdbc） | **禁止新增 DriverManager 调用**；接 HikariCP 是单独的 FIX-DEV-* 任务，需架构级 AI 接 |
| KD-005 | Jackson 全局未启用 `SNAKE_CASE` | application.yml 已显式注释为技术债（会破 20+ 测试），新增 DTO 用 `@JsonProperty("snake_case")` 显式声明 |
| KD-006 | `OrgContext` 类型在前后端仍未完全统一 | 改 `frontend/src/api/types.ts` 必须先看 PR-V2-06 / PR-V2-11 是否在锁 |
| KD-007 | 6 个前端入口仍为占位（`/adapter/hub` `/dify/workflows` `/mpi/patients` `/tenant/onboarding` `/admin/users` `/admin/audit`） | 后端已就绪，前端接通是后续 PR；演示时说"规划中"或不走这些菜单 |

---

## 6. 权威文档链（深入时再看）

- **产品事实源**：[`docs/01_产品事实源.md`](01_产品事实源.md)
- **任务状态唯一权威**：[`docs/engineering/02_任务台账.md`](engineering/02_任务台账.md)
- **AI 一致性 7 套机制**：[`docs/engineering/AI一致性保证.md`](engineering/AI一致性保证.md)
- **禁用模式 30+ 条**：[`docs/engineering/forbidden-patterns.md`](engineering/forbidden-patterns.md)
- **架构决策（ADR）**：[`docs/engineering/adr/`](engineering/adr/)
- **参考实现样板**：[`docs/engineering/reference-implementations/`](engineering/reference-implementations/)
- **能力分级**：[`docs/engineering/AI能力分级匹配清单.md`](engineering/AI能力分级匹配清单.md)
- **分支策略**：[`docs/engineering/分支策略与发布管理.md`](engineering/分支策略与发布管理.md)
- **V3 功能矩阵**（写新 endpoint 前必看）：[`docs/engineering/2026-05-21-功能矩阵-V3.md`](engineering/2026-05-21-功能矩阵-V3.md)

---

**记住一件事：本文里没说"可以做"的事情，默认就是「不许做」。** 有疑问 → 先开 ADR，不要自由发挥。
