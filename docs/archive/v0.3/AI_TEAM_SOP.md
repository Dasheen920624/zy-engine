# MedKernel · AI 团队标准化执行 SOP

> 版本：2.0 · 2026-05-23（v1.0 GA 并行开发）
> 适用：所有为 MedKernel 工作的 AI（Claude / Codex / GPT / CodeBuddy / 其它）
> 目的：**让 5+ 个不同模型在多 worktree / 并发开发场景下，输出能像同一个团队一样高质量交付**。
> 上游依赖：[`AI_CHARTER.md`](AI_CHARTER.md)（What 必读规则）+ [`PRODUCT_SIMPLIFICATION_V1_GA.md`](PRODUCT_SIMPLIFICATION_V1_GA.md)（客户主线）+ [`AI_TEAM_PR_BACKLOG_V1.0_GA.md`](AI_TEAM_PR_BACKLOG_V1.0_GA.md)（任务入口）

---

## 0. GA 阶段执行覆盖说明

- 下一版本唯一目标是 **v1.0 GA / tag `v1.0.0`**；`v0.3-pilot`、`PR-V3-*`、`PR-FINAL-*` 均为历史证据，不再作为新任务入口。
- 新任务编号统一使用 `GA-<泳道>-<序号>`，例如 `GA-UX-01`、`GA-SEC-02`、`GA-DTO-01`。
- 所有任务先通过产品收口闸门：是否服务 `试点准备 → 临床运行 → 质控改进 → 合规运维`，是否能让客户在 30 分钟内看懂闭环。
- 并发开发先完成 Batch 0（治理/并发/发布闸门），再最大并行 Batch 1/2；共享文件只能由架构师 AI 集中改。
- 任何 claim / lock / review 残留、重复 task、`write_scope` 父子目录重叠，均由 `check-ai-collaboration.ps1 -Strict` 阻断。

## 1. 角色分工（按能力级别分）

| 角色 | 对应 AI 模型 | 可领任务范围 | 不允许做 |
|---|---|---|---|
| **架构师 AI**（架构级） | Opus 4.7+ / GPT-5 Pro / 同级 | 起 ADR / 跨模块重构 / 性能优化 / 安全设计 / 拆分超长文件 / Controller 归并 | 单 Controller 实现（杀鸡用牛刀）|
| **高级 AI**（GA 主体任务） | Sonnet 4.7 / GPT-4.1 / Codex 同级 | 整页面实现 / Controller + Service + Repository 三层 / 跨前后端 PR | 起 ADR（必须找架构师 AI） |
| **中级 AI**（GA 子任务） | Sonnet 3.7 / GPT-4o / CodeBuddy | 单组件 / 单 API 接口 / 单元测试补齐 / 文档同步 | 改架构 / 改菜单结构 / 改 tokens.css |
| **初级 AI**（杂活）| Haiku / GPT-4o-mini | 文案 / 单测 / lint 修复 / 单文件改名 | 任何涉及医学/安全/合规的改动 |

**自评原则**：AI 接手前必须**自评能力级别 + 跑 `scripts/verify-task-prereq.ps1 -Level <senior/middle/junior>`**。若 verify-pr 检测到任务超过能力范围（如初级 AI 改了 tokens.css）→ 自动 FAIL + 标 `OVER_SCOPE` 标签由人复议。

---

## 2. 任务领取流程（10 步标准 SOP）

```
[1. 自评] 我能领什么级别？→ 看 §1
[2. 看板] 打开 docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md → 从 Batch 0 开始找可领取任务
[3. 登记] 确认 docs/engineering/02_任务台账.md 已有同名 GA 任务；没有则先登记
[4. 预检] 跑 scripts/verify-task-prereq.ps1 -TaskId GA-XXX-00 -Level senior
       ├─ 通过 → 进 [5]
       └─ 失败 → 看输出原因（依赖未 DONE / develop 不健康 / 能力不匹配）→ 换任务或修依赖
[5. 锁文件] 创建 ai-dev-input/10_task_claims/active/GA-XXX-00_<myId>_<timestamp>.md
       内含：write_scope（独占文件清单）/ read_scope / 预计 commit 数 / 预计完成时间 / 自评级别
[6. 必读] 严格按以下顺序读 5 份文档（20 分钟）：
       a) docs/AI_CHARTER.md（5 分钟）
       b) docs/PRODUCT_SIMPLIFICATION_V1_GA.md（8 分钟）
       c) docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md 对应 GA 卡片
       d) docs/PRODUCT_ARCHITECTURE_FINAL.md 对应架构边界
       e) docs/engineering/reference-implementations/<对应样板>
[7. 编码] 在独占文件清单内改代码；任何对清单外文件的需求 → STOP 重新拆任务
[8. 自测] 后端：cd medkernel-mvp && mvn -q compile && mvn -q test
       前端：cd frontend && npm run typecheck && npm run lint && npm test -- --run && npm run build
       任何一项 FAIL → 不许进 [9]
[9. 提交] 跑 scripts/verify-pr.ps1 -TaskId GA-XXX-00
       FAIL=0 → 进 [10]
       FAIL>0 → 修到 0
[10. PR] git commit + git push origin HEAD:develop
       commit message 格式：「GA-XXX-00: <动词> <对象>」（中文，≤ 70 字）
[11. Review] 创建 ai-dev-input/11_ai_reviews/pending/GA-XXX-00_<myId>.md
       内含：DoD 自查表 / 测试结果 / 性能数据 / 已知限制 / 截图（前端 PR）
       等待 APPROVED → 归档 claim + review 到 archive/
```

---

## 3. 多 AI 并发冲突仲裁

### 3.1 冲突判定

两个 AI **不允许同时锁同一文件**。判定方法：

```bash
# AI-A 启动前必跑：
.\medkernel-mvp\scripts\check-ai-collaboration.ps1 -Strict
# 如果出现 write_scope_overlap / orphan_lock / duplicate_task → 冲突，不许启动
```

### 3.2 冲突场景与裁决

| 场景 | 裁决规则 |
|---|---|
| **同一文件**：A 已锁，B 想锁 | B 必须等 A 释放（claim 归档）或者找 A 拆任务 |
| **同一文件**：A 锁了但 > 48h 无 commit | 视为 **stale claim**，B 可在 claim 上加 `<!-- STALE_TAKEOVER by <B-id> at <timestamp> -->`，征求 A 同意或人工仲裁 24h 后接管 |
| **共享文件**（如 `api/types.ts` `menuConfig.tsx` `tokens.css` `routes.tsx`）：多 AI 都想改 | 这类文件**只能由架构师 AI 改**，其它 AI 提 issue 给架构师 AI 排期 |
| **跨 PR 依赖冲突**：A 改了 B 依赖的 API 形状 | A 必须先在 [`docs/engineering/02_任务台账.md`](engineering/02_任务台账.md) 该任务行加 `breaks: [GA-XXX-00]`；B 收到通知后 rebase + 适配 |
| **同时 commit 到 develop**：先 push 的赢 | 后 push 的必须 `git fetch + git rebase origin/develop`，解冲突，再 push |

### 3.3 共享文件清单（必须由架构师 AI 改，名单）

| 文件 | 影响 |
|---|---|
| `frontend/src/api/types.ts` | 全前端 API 契约 |
| `frontend/src/router/menuConfig.tsx` | 全局菜单 |
| `frontend/src/router/routes.tsx` | 全局路由 |
| `frontend/src/styles/tokens.css` | 全局设计 token |
| `frontend/src/App.tsx` | ConfigProvider 根 |
| `medkernel-mvp/pom.xml` | 后端依赖 |
| `medkernel-mvp/src/main/resources/application.yml` | 后端全局配置 |
| `medkernel-mvp/src/main/java/com/medkernel/common/**` | 通用层（ApiResult / ErrorCode / Exception） |
| `medkernel-mvp/src/main/java/com/medkernel/persistence/**` | 持久化基础设施 |
| `scripts/verify-pr.ps1` `scripts/verify-task-prereq.ps1` | CI 门禁 |
| `docs/AI_CHARTER.md` `docs/PRODUCT_ARCHITECTURE_FINAL.md` | 宪法和白皮书 |

---

## 4. 验收标准模板（DoD）

每个 `GA-*` 必须满足以下 8 项 DoD（Definition of Done）：

```markdown
# GA-XXX-00 DoD 自查表

## 功能
- [ ] PR 卡片中列出的所有 endpoint / 页面 100% 实现（非占位）
- [ ] 与 PRODUCT_ARCHITECTURE_FINAL.md §1.2 表对照，菜单 + 路由 + Controller 命名 100% 一致
- [ ] 6 大演示剧本中关联的剧本能 fixture 一键加载后跑通

## 测试
- [ ] 后端：每个新 endpoint ≥ 1 个 success case + 1 个 fail case 契约测试
- [ ] 前端：每个新页面 ≥ 1 个 render 测试 + 1 个交互测试（vitest）
- [ ] mvn test / npm test 全 PASS
- [ ] 覆盖率：本 PR 改动文件后端 ≥ 70% / 前端 ≥ 60%

## 代码质量
- [ ] 不违反 AI_CHARTER §2 任何一条（verify-pr.ps1 全 PASS）
- [ ] 新增文件 ≤ 500 行；新增函数 ≤ 80 行
- [ ] 任何 Controller 不用 raw Map<String, Object>；用 DTO + @Valid
- [ ] 任何颜色 / 字号 / 间距用 var(--mk-*) token

## 国情合规（如涉及登录 / 患者数据 / 医疗决策）
- [ ] 涉及登录 → 满足 PRODUCT_SIMPLIFICATION_V1_GA §4：默认账号登录，统一身份认证配置化展示，合规信息完整但不干扰登录
- [ ] 涉及患者数据 → 字段最小化 + 脱敏
- [ ] 涉及医疗决策 → 显示来源 + 显式医生确认 + traceId 可追溯

## 文档
- [ ] 更新 docs/engineering/02_任务台账.md（status TODO → DONE）
- [ ] 更新 docs/engineering/2026-05-21-功能矩阵-V3.md（新 endpoint 加行）
- [ ] 若涉及 API 变更 → 更新 docs/engineering/api-examples.http
- [ ] 若涉及架构变更 → 必须先有 ADR

## 演示
- [ ] 在 ai-dev-input/13_feature_acceptance/ 创建 FA-GA-XXX-00-*.md，含 3+ 张截图（前端 PR）
- [ ] 实施工程师能跟着 demo 脚本独立跑通

## 安全
- [ ] 不打印 password / API Key / 完整身份证号到日志
- [ ] 所有写操作有 created_by（从 SecurityContextHolder 取）
- [ ] 所有业务表有 tenant_id NOT NULL

## 回滚
- [ ] 描述 rollback 方案（DB migration 必须有 .down 脚本）
```

---

## 5. 测试覆盖率与质量门槛

### 5.1 后端

| 指标 | 当前基线 | v1.0 GA 准入 |
|---|---|---|
| `mvn test` 通过率 | 100% | 100% |
| 行覆盖率（jacoco） | ≥ 60% | ≥ 70% |
| 分支覆盖率 | ≥ 50% | ≥ 60% |
| 契约测试覆盖（每个 Controller） | ≥ 1 success + 1 fail | ≥ 3 success + 3 fail |
| 性能 P95（核心 API） | < 500ms | < 300ms |

### 5.2 前端

| 指标 | 当前基线 | v1.0 GA 准入 |
|---|---|---|
| `npm run typecheck` | exit 0 | exit 0 |
| `npm run lint` | 0 error 0 warn | 0 error 0 warn |
| `npm test` 通过率 | 100% | 100% |
| 行覆盖率 | ≥ 40% | ≥ 60% |
| Lighthouse 性能分（A 知识工厂主页）| ≥ 70 | ≥ 90 |
| Lighthouse 无障碍分 | ≥ 80 | ≥ 95 |
| 主流浏览器兼容 | Chrome 100+ / Edge 100+ | + Firefox 100+ / Safari 16+ |

---

## 6. 文档同步要求

任何改动必须同步以下文档（缺一即 PR FAIL）：

| 改动类型 | 必须同步 |
|---|---|
| 新增 Controller / endpoint | `02_任务台账.md` + `2026-05-21-功能矩阵-V3.md` + `api-examples.http` + `PRODUCT_ARCHITECTURE_FINAL.md §1.2` |
| 新增前端页面 / 路由 | `04_页面规格书.md` + `menuConfig.tsx`（如有菜单入口）+ `PRODUCT_ARCHITECTURE_FINAL.md §1.2` |
| 改动 tokens / 设计系统 | `03_设计系统.md` + ADR-0003 不允许违反 |
| 改动核心架构 | 新增 ADR + 更新 `05_架构总图.md` + `PRODUCT_ARCHITECTURE_FINAL.md` |
| 新增国情合规能力 | `PRODUCT_SIMPLIFICATION_V1_GA.md §4` + `COMP-001_合规基线与证据包.md` 补证据 |
| Breaking change（API / DB schema） | `CHANGELOG.md` Breaking 段 + `02_任务台账.md` 在依赖任务行加 `breaks: []` |

---

## 7. 性能与压测红线

### 7.1 后端

| 红线 | 阈值 | 检测 |
|---|---|---|
| 单次 API P95 | < 300ms（v1.0 GA） | Jenkins / GitHub Actions 跑 wrk 30s 持续 |
| 单文件函数复杂度 | 圈复杂度 ≤ 10 | jacoco / SonarQube |
| DB 连接 leak | 0 | Hikari `leak-detection-threshold=2000` |
| 慢 SQL | 0 个 > 500ms | application.yml `spring.jpa.properties.hibernate.generate_statistics=true` |
| 内存 leak | OOM 0 次 / 24h | JVM `-XX:+HeapDumpOnOutOfMemoryError` |

### 7.2 前端

| 红线 | 阈值 |
|---|---|
| Bundle 大小（gzip） | < 800KB（首屏） |
| 首屏 LCP | < 2.5s |
| TTI | < 3.5s |
| FID | < 100ms |
| CLS | < 0.1 |

---

## 8. Git Flow（v1.0 GA 起强制）

### 8.1 分支模型

```
main ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ← 永远稳定，每个 commit 是 release tag
        ↑↑                ↑↑                ↑↑
  发布 PR (release manager) 发布 PR (release manager)
        │                 │                 │
develop ━━┻━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━ ← AI 都推这里
          ↑↑↑↑↑           ↑↑↑↑↑
   ai/GA-UX-01 PR    ai/GA-SEC-01 PR    ai/GA-QA-01 PR  ...
       │                 │                 │
   feature branch    feature branch    feature branch
```

### 8.2 强制规则

| 规则 | 强制方式 |
|---|---|
| AI 任何 PR 必须 → `develop`，不许直接 → `main` | `verify-pr.ps1 §0` `Test-BranchPolicy` 拦 |
| `main` 分支保护：禁直接 push、强制 PR、强制 2 个 CI check 通过 | GitHub Branch Protection Rule |
| `develop` → `main` 只能由 release manager 在证据齐备后执行 | 项目规约 |
| AI 任务分支命名 `ai/GA-XXX-00/<slug>` | `verify-pr.ps1` 加分支名 grep |
| 每次 `develop → main` merge 后立即打 tag `stable-YYYY-MM-DD-<short-hash>` | release manager 责任 |
| 每个 commit message 含任务编号 | `verify-pr.ps1` 检查 |

### 8.3 worktree 管理（多 AI 物理隔离）

每个 AI 在独立 worktree 工作，避免 git 状态相互污染：

```bash
# 架构师 AI 给中级 AI 创建 worktree
git worktree add -b ai/GA-UX-01/customer-route ../worktrees/ga-ux-01-customer-route develop

# 中级 AI 进入工作
cd ../worktrees/pr-v3-01-adapter-hub
# 改代码 / commit / push origin HEAD:develop

# 任务结束后架构师清理
git worktree remove ../worktrees/pr-v3-01-adapter-hub
git branch -D ai/GA-UX-01/customer-route
git worktree prune
```

---

## 9. 一致性度量（月度）

每月由架构师 AI 输出一份「一致性度量月报」：

| 指标 | 计算方式 | 目标 |
|---|---|---|
| **DoD 首次通过率** | `count(verify-pr 首次 PASS) / count(总 PR)` | ≥ 80% |
| **review 一次通过率** | `count(review 直接 APPROVED) / count(总 review)` | ≥ 70% |
| **跨 AI 共享文件冲突次数** | git log 中 conflict marker 的 commit 数 | = 0 |
| **stale claim 比例** | `count(claim > 48h 无 commit) / count(总 claim)` | < 10% |
| **平均 PR 周期** | `claim → APPROVED` 时间 | < 3 工作日 |
| **AI 自评准确率** | `count(自评 senior 任务真的需要 senior) / count(自评 senior)` | ≥ 90% |

---

## 10. 紧急处置预案

### 10.1 develop 编译/测试 RED

> 触发：CI 显示 develop 上 mvn compile 或 mvn test FAIL

1. release manager 立即把 `ai-dev-input/00_DEVELOP_HEALTH.md` 状态改 🔴 RED
2. 通知所有 active claim AI **STOP 当前任务，回头修自己的 commit**
3. 找出导致 RED 的 commit（`git log --since=1d`），找原 AI 修
4. RED 期间禁止新 PR 合入 develop
5. 修完 → 状态改 🟢 GREEN → 解禁

### 10.2 main 受污染（不该合的合进来了）

> 触发：main 上发现违反不变量的 commit

1. 立即 `git revert <bad-commit>` 在 main 上，发紧急 PR 走 main 保护规则（hotfix）
2. 复盘流程：为什么这个 commit 通过了 2 个 CI check？补门禁
3. 发 ADR 记录此次事故 + 改进措施

### 10.3 安全事件（密钥泄露 / SQL 注入 / XSS）

1. 立即在 `ai-dev-input/00_DEVELOP_HEALTH.md` 标 🔴 RED + 写明事件性质
2. 通知所有 AI STOP
3. 安全 AI（架构师级）专责修复 + 写 ADR + 加 verify-pr 拦截规则
4. 修复完 → 跑全套等保 2.0 自测脚本（`scripts/security-baseline-check.ps1`）→ 解禁

---

## 11. 常见错误模式与纠正

| 错误模式 | 后果 | 纠正方法 |
|---|---|---|
| AI 拿到任务直接开始改代码，不读文档 | 违反不变量 / 重复造轮子 | 必须按 §2 [5] 读 4 份文档，verify-task-prereq 加文档时间戳检查 |
| 改非独占文件 | 别人未提交工作被覆盖 | claim 加 write_scope 强制；check-ai-collaboration.ps1 已检测 |
| 测试 skip / mock 过头 | 「测试 PASS 但生产 broken」 | review 时检查测试质量，不只看通过率 |
| commit message 写「fix bug」 | 后续无法定位是哪个任务 | verify-pr 检查 commit message 必须含 `GA-XXX-00` |
| 文档不同步 | 「文档说 X 已 DONE，代码里还是 TODO」 | DoD §6 文档同步强制 |
| 自评 senior 实际写出 junior 代码 | 高级任务交付质量差 | 月度一致性度量统计「自评准确率」 |
| 跨 AI 互相依赖锁死 | 项目停滞 | 每周架构师 AI 跑 dependency graph，检测循环依赖 |

---

## 12. AI 接手 GA-* 任务的 5 分钟启动清单

```
□ [1 分钟] 跑 git pull origin develop && git status（确认 worktree 干净）
□ [1 分钟] 读 ai-dev-input/00_DEVELOP_HEALTH.md（确认 🟢 GREEN）
□ [1 分钟] 读 docs/AI_CHARTER.md（确认红线）
□ [1 分钟] 读 docs/PRODUCT_SIMPLIFICATION_V1_GA.md（确认任务服务客户四段主线）
□ [1 分钟] 读 docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md 找到 GA-XXX-00 行（确认批次/依赖/独占范围）

5 分钟后：
□ 跑 scripts/verify-task-prereq.ps1 -TaskId GA-XXX-00 -Level <senior/middle/junior>
□ 创建 active claim
□ 开始编码
```

---

**End of AI team SOP.**
**核心原则**：本 SOP 不是「建议」，是「合约」。AI 违反任何一条 → PR 自动 FAIL，违反 3 条以上 → 标 `BAD_AI_BEHAVIOR` 在 review 中由架构师 AI 决定是否禁用该模型。**让 AI 团队像专业团队一样工作。**
