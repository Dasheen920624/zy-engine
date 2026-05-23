# AI 一致性保证 — 7 套机制

> 版本：V2.0 · 2026-05-18  
> 目的：**让不同能力的 AI 执行同一任务时，输出 80% 以上一致**。  
> 核心理念：**让"对"的事情容易做，让"错"的事情做不出来。**

---

## 1. 为什么需要这个

V2 体系把任务做到了 PR 级别精度，但仍有以下风险：

| 风险 | 后果 |
|---|---|
| 不同 AI 对"实施步骤"理解不同 | 同一 PR 出 5 个版本的实现 |
| 初级 AI 拿到高级任务 | 写出能跑但架构错误的代码 |
| AI 没读文档就动手 | 重复造轮子、违反不变量 |
| AI 完成自评 "看起来 OK" | 实际不满足 DoD |
| 多 AI 并行改共享文件 | 后到的覆盖先到的工作 |
| AI 跳过测试 / lint | 提交后 CI 红 |

---

## 2. 7 套机制总览

| # | 机制 | 类型 | 作用阶段 | 强制度 |
|---|---|---|---|:---:|
| 1 | [ADR 架构决策记录](adr/) | 输入 | 接手前阅读 | 强制 |
| 2 | [参考实现样板](reference-implementations/) | 输入 | 实现时复制 | 强制 |
| 3 | [禁用模式清单](forbidden-patterns.md) | 输入 + 过程 | 实现时避免 | 强制 |
| 4 | [ESLint 自定义规则](../../frontend/eslint-rules/) | 过程 | 编码时自动 | 强制 |
| 5 | [verify-task-prereq.ps1](../../scripts/verify-task-prereq.ps1) | 流程 | 接手前自检 | 强制 |
| 6 | [verify-pr.ps1](../../scripts/verify-pr.ps1) | 输出 | 提交前自检 | 强制 |
| 7 | [AI 能力分级匹配清单](AI能力分级匹配清单.md) | 流程 | 领任务时匹配 | 强制 |

---

## 3. 机制详解

### 3.1 ADR 架构决策记录（输入端）

**位置：** [`adr/`](adr/)

**用途：** 记录"为什么这么设计"的不可违反决策。

**当前 ADR：**
| 编号 | 标题 | 状态 |
|---|---|---|
| 0001 | 三产品分层架构（A/B/C） | Accepted |
| 0002 | V2 PR 命名空间隔离（PR-V2-XX 不复用历史 FE-XXX） | Accepted |
| 0003 | 禁止硬编码颜色 / 字号 / 间距（必须用 Design Token） | Accepted |
| 0004 | 医学内容必须有来源（MISSING_SOURCE 阻断发布） | Accepted |

**AI 接手必做：** 实现前必读对应任务相关的 ADR。修改架构需新增 ADR，不能私自违反。

### 3.2 参考实现样板（输入端）

**位置：** [`reference-implementations/`](reference-implementations/)

**用途：** 提供可直接复制的样板代码，避免 AI 自由发挥。

**当前样板：**
- `status-badge-component.md` — C01 StatusBadge 完整实现（Storybook + 单元测试 + 类型）
- `rest-controller-pattern.md` — 后端 Controller 标准模式
- `page-with-six-states.md` — 6 状态前端页面模式
- `api-call-pattern.md` — API 调用标准模式

**AI 接手必做：** 实现 X 类组件/页面前，先读 X 类的参考样板。

### 3.3 禁用模式清单（输入 + 过程端）

**位置：** [`forbidden-patterns.md`](forbidden-patterns.md)

**用途：** 列出"绝对不能这么写"的模式。

**核心条款：**
- 禁止硬编码颜色（必须用 `var(--mk-*)` token）
- 禁止 `any` 类型（TypeScript strict）
- 禁止跳过 `ApiResult` 包装
- 禁止前端直接调 Repository（必须经 Service）
- 禁止规则发布缺来源
- 禁止 `console.log` 进入 production
- 禁止 ZyEngine* 类名（重命名后已禁用）

**强制方式：** ESLint 规则 + verify-pr.ps1 自动检测。

### 3.4 ESLint 自定义规则（过程端）

**位置：** [`frontend/eslint-rules/`](../../frontend/eslint-rules/)

**当前规则（3 条最关键）：**
- `no-hardcoded-color.js` — 禁止 `#xxx` `rgb()` `hsl()` 出现在非 token 文件
- `require-status-badge.js` — `<Tag>` 含状态语义必须用 `<StatusBadge>` 替代
- `require-source-info-for-medical.js` — 含"医保""规则""来源"语义的组件必须有 `<SourceInfo>`

**集成：** `frontend/eslint.config.js` 已加载。`npm run lint` 自动跑。

### 3.5 verify-task-prereq.ps1（流程端 - 接手前）

**位置：** [`scripts/verify-task-prereq.ps1`](../../scripts/verify-task-prereq.ps1)

**用法：**
```powershell
.\scripts\verify-task-prereq.ps1 -TaskId PR-V2-01
```

**检查：**
1. 任务编号在 `docs/engineering/02_任务台账.md` 存在
2. 依赖任务状态为 DONE
3. 工作树是否干净（`git status` 无未提交改动）
4. 远端 main 同步状态
5. 你的能力等级是否匹配（输入 -Level junior/middle/senior）
6. active claim 是否已创建

**强制方式：** 任何 active claim 创建前必须跑过此脚本。

### 3.6 verify-pr.ps1（输出端 - 提交前）

**位置：** [`scripts/verify-pr.ps1`](../../scripts/verify-pr.ps1)

**用法：**
```powershell
.\scripts\verify-pr.ps1 -TaskId PR-V2-01
```

**自动执行 9 项检查：**
1. 跑 lint（前端 + 后端）
2. 跑 test（前端 + 后端）
3. 跑 build（前端 + 后端）
4. 跑 `verify-encoding.cmd`（UTF-8 无 BOM）
5. 跑 `check-ai-collaboration.ps1`（独占文件冲突）
6. 检查 git diff 范围与独占文件清单一致
7. 检查 ADR 不变量未被违反（grep 禁用模式）
8. 检查 DoD 清单（自动从 V2 实施手册抽取）
9. 检查 feature acceptance 记录（高风险 PR 必须）

**全 PASS 才能 commit + push。**

### 3.7 AI 能力分级匹配清单（流程端）

**位置：** [`AI能力分级匹配清单.md`](AI能力分级匹配清单.md)

**用途：** AI 自评能力等级，匹配可领任务范围。

**分级与可领任务：**
- **初级 AI（如 GPT-3.5 / Claude Haiku）**：只能领样式调整、文案修改、单测补齐
- **中级 AI（如 GPT-4 / Claude Sonnet）**：可领前端页面实现、组件开发、单 Controller
- **高级 AI（如 GPT-4-turbo / Claude Opus / Sonnet 4.x）**：可领架构变更、跨模块、性能优化

**配套：** 接手前在 `verify-task-prereq.ps1 -Level` 自评。

---

## 4. AI 接手新任务的完整流程

```
1. 读 docs/README.md（项目入口）
2. 读 docs/05_AI实施手册.md 找到 PR-V2-XX 卡片
3. 读 docs/engineering/AI能力分级匹配清单.md，确认你的等级能领
4. 读对应的 ADR（在 PR 卡片"对应文档"中标注）
5. 跑 scripts/verify-task-prereq.ps1 -TaskId PR-V2-XX -Level senior
6. 通过后创建 ai-dev-input/10_task_claims/active/<claim_id>.md
7. 读 docs/engineering/reference-implementations/<对应样板>
8. 编码（ESLint 实时检查 + 禁用模式提示）
9. 跑 scripts/verify-pr.ps1 -TaskId PR-V2-XX
10. 全 PASS 后才能 commit + push
11. 创建 ai-dev-input/11_ai_reviews/pending/<review_id>.md
12. APPROVED 后归档 claim/review
```

---

## 5. 一致性度量

每月统计：

- 不同 AI 完成同类 PR 的代码风格差异（diff 行数）
- DoD 自检通过率（首次通过 vs 反复修改）
- review 一次通过率
- 跨 AI 共享文件冲突次数

目标：

- DoD 首次通过率 ≥ 80%
- review 一次通过率 ≥ 70%
- 同类 PR 代码风格差异 ≤ 30%
- 跨 AI 共享文件冲突 = 0
