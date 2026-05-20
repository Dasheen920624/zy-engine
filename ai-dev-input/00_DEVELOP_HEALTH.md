# develop 健康哨兵（AI 接手前必读）

> **这是任何 AI 接手任务前必须看的第一份文件**（早于 02_任务台账.md）。
> 若本文标 🔴 RED，**只允许领"修复编译"类任务，不允许领新功能 / 改造类任务**。

---

## 当前状态

| 字段 | 值 |
|---|---|
| 状态 | 🔴 **RED — DEVELOP 编译失败** |
| 最后更新 | 2026-05-20 |
| 最后验证 commit | `b37ffd9`（AUDIT-20260520-增量 报告 `669517c` 同步检查） |
| 验证命令 | `cd medkernel-mvp && mvn -q compile` |
| 结果 | **23 文件 ~210 ERROR**（编译失败） |
| 详细报告 | [docs/engineering/AUDIT-20260520-增量.md](../docs/engineering/AUDIT-20260520-增量.md) |
| 派单 | [docs/engineering/2026-05-20-破窗行动.md](../docs/engineering/2026-05-20-破窗行动.md) |

---

## 状态语义

| 标签 | 含义 | 准入规则 |
|---|---|---|
| 🟢 GREEN | `mvn -q compile` + `mvn -q test` 均 PASS | 任何 AI 可按 02_任务台账 正常领任务 |
| 🟡 YELLOW | compile PASS 但 test FAIL，或 lint 失败 | 可领新任务但 review 时要附 test 修复证明 |
| 🔴 RED | compile FAIL，或缺关键 Maven/npm 依赖 | **冻结新任务**；只允许领下面"修复白名单"内的任务 |

---

## 🔴 RED 状态下的修复白名单（仅当当前是 RED）

### 当前 RED 的责任任务清单

按 [AUDIT-20260520-增量 §1](../docs/engineering/AUDIT-20260520-增量.md#1-编译错误清单按文件--责任任务) 派给原 owner。**在 develop 转 GREEN 之前，AI 只允许领以下 9 个修复任务**：

| Fix Task | 责任任务 | 范围 | 估时 |
|---|---|---|---|
| FIX-DEV-001 | DATA-001 | 5×repo `getJdbcUrl()` → `getUrl()` + 重命名 `data-governance/` → `datagovernance/` | 15 min |
| FIX-DEV-002 | PR-V2-09 | `pom.xml` 加 `spring-boot-starter-websocket` 依赖 | 5 min |
| FIX-DEV-003 | MPI-001 | `MpiHashUtil.repeat()` 改 JDK 1.8 兼容；3 处 `ON DUPLICATE KEY UPDATE` 改 SELECT→UPDATE/INSERT | 1 h |
| FIX-DEV-004 | SEC-006 | `UserSyncController` 类型 + 补 `ApiResult.notFound()` 或换为 ConfigNotFoundException | 30 min |
| FIX-DEV-005 | EVAL-002 | `EvalController` import OrganizationContext + `ErrorCode.NOT_FOUND` → `CONFIG_NOT_FOUND` | 15 min |
| FIX-DEV-006 | CDSS-001 | `CdssService` 4 处找不到符号 | 30 min |
| FIX-DEV-007 | AIK-001 | `KnowledgeController/Service` 多处找不到符号 | 1 h |
| FIX-DEV-008 | SEC-011 | `TenantOnboardingController/Service` 类型 + 找不到符号 | 1 h |
| FIX-DEV-009 | 多任务 | 其它 `EnginePersistenceService/NotificationService/RuleActionLog*/SecurityBaseline` 散点错 | 2 h |

> 每个 FIX-DEV-* 必须以 `claim` + `lock` 形式领走，commit 前本地必须跑通 `mvn -q compile`。
> 修完一个，把对应行从本文移除并把状态部分更新；全部修完后状态改 🟢。

---

## 🔴 RED 状态下的禁止事项

- ❌ 不允许领新功能任务（PR-V2-*、AIK-*、SEC-*、EVAL-* 等业务任务）
- ❌ 不允许 push 到 develop 任何会引入新代码的 commit（修文档 / 修审计报告 / 修本文件除外）
- ❌ 不允许把 develop merge 进 main
- ❌ 不允许把 develop 作为发布候选 tag
- ❌ 不允许把 develop 拉去做 demo / smoke test（结果不可信，编译都过不去）

---

## 状态转换协议

### RED → YELLOW

满足全部：
1. `mvn -q compile` PASS（exit 0）
2. 上面"责任任务清单"全部清空
3. 本文件状态字段更新为 🟡 + 新的最后验证 commit

### YELLOW → GREEN

满足全部：
1. `mvn -q test` PASS
2. `cd frontend && npm run lint && npm run typecheck && npm test && npm run build` PASS
3. 本文件状态字段更新为 🟢

### GREEN → YELLOW / RED

任何 AI 在 push 后发现编译/测试失败：

1. **立即**编辑本文件，把状态降级为 🟡 / 🔴
2. 补全"责任任务清单"
3. 同时 commit 本文件 + 修复任务 claim，并 push 通知

---

## 自检命令

AI 接手任何任务前先跑：

```powershell
.\scripts\check-develop-health.ps1
```

脚本会自动跑 `mvn -q compile`，并把结果与本文件状态对比。如发现本文件标 🟢 但实际编译失败，会强制要求 AI 先更新本文件再走流程。

---

## 历史事故复盘

| 日期 | 状态 | 触发原因 | 修复 commit |
|---|---|---|---|
| 2026-05-20 | 🔴 → 🟡 → 🟢 待 | 80 个 commit / 8+ 任务在 develop 上交叉冲突，没有任何 owner 在合并后跑过 `mvn compile`；同期 GitHub branch protection 未强制要求 CI 通过 | 待 FIX-DEV-001..009 全部清零 |

---

> 流程依据：[forbidden-patterns §6](../docs/engineering/forbidden-patterns.md#6-多-ai-协作) +
> [AUDIT-20260520-增量 §5](../docs/engineering/AUDIT-20260520-增量.md#5-推荐应对路径)。
> 本文件被 [`docs/engineering/00_总入口与AI接手导航.md`](../docs/engineering/00_总入口与AI接手导航.md) §1 Step 0 强制引用。
