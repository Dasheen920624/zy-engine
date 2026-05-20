# develop 健康哨兵（AI 接手前必读）

> **这是任何 AI 接手任务前必须看的第一份文件**（早于 02_任务台账.md）。
> 若本文标 🔴 RED，**只允许领"修复编译"类任务，不允许领新功能 / 改造类任务**。

---

## 当前状态

| 字段 | 值 |
|---|---|
| 状态 | 🟡 **YELLOW — 后端 GREEN，前端待 CI 验证** |
| 最后更新 | 2026-05-21 |
| 最后验证 commit | `7f2e65b` |
| 验证命令 | `cd medkernel-mvp && mvn -q compile` + `mvn test` |
| 结果 | **261 文件编译 PASS、248 测试 0 失败 0 错误**；前端因本机 Node 12 无法验证（项目要 Node 18+），待 CI / 演示环境验证 |
| 详细报告 | [docs/engineering/AUDIT-20260520-增量.md](../docs/engineering/AUDIT-20260520-增量.md) |
| 历史派单 | [docs/engineering/2026-05-20-破窗行动.md](../docs/engineering/2026-05-20-破窗行动.md) — FIX-DEV-001..009 已由各 owner 自行解决，无需新派单 |

---

## 状态语义

| 标签 | 含义 | 准入规则 |
|---|---|---|
| 🟢 GREEN | `mvn -q compile` + `mvn -q test` 均 PASS | 任何 AI 可按 02_任务台账 正常领任务 |
| 🟡 YELLOW | compile PASS 但 test FAIL，或 lint 失败 | 可领新任务但 review 时要附 test 修复证明 |
| 🔴 RED | compile FAIL，或缺关键 Maven/npm 依赖 | **冻结新任务**；只允许领下面"修复白名单"内的任务 |

---

## 🟡 YELLOW 状态下的准入

- ✅ 允许领新功能任务（PR-V2-*、AIK-*、SEC-*、EVAL-* 等）
- ✅ 允许 push 到 develop（必须本地 `mvn -q compile` PASS）
- ⚠️ **暂不允许把 develop merge 进 main 做发布**——等下面前端验证完成
- ⚠️ 演示前请先在临时环境跑 `npm run lint && npm run typecheck && npm test && npm run build`

### 待跑完的验证清单（转 🟢 之前必须）

- [ ] 前端 `npm install`（需 Node 18+ 环境）
- [ ] `npm run lint` PASS
- [ ] `npm run typecheck` PASS
- [ ] `npm test` PASS
- [ ] `npm run build` PASS（产出 dist/）
- [ ] 用户本机或临时演示环境打开浏览器手动验证：dashboard、配置包中心、路径、规则、质控、Provider 状态等核心页面均可加载
- [ ] 演示环境 smoke：登录 → 切组织 → 跑一次 rule-engine/evaluate 看 hit
- [ ] 验证均通过后由人工把哨兵改 🟢 并启动 develop → main release PR

### 已解决的历史 RED 阻塞（FIX-DEV-001..009，归档）

| Fix Task | 责任任务 | 状态 | 解决方式 |
|---|---|---|---|
| FIX-DEV-001 | DATA-001 | ✅ DONE | data-governance 目录已重命名为 datagovernance（package 与目录一致），5 个 typo 已修 |
| FIX-DEV-002 | PR-V2-09 | ✅ DONE | spring-boot-starter-websocket 已在 pom.xml |
| FIX-DEV-003 | MPI-001 | ✅ DONE | JDK 8 兼容性已修；ON DUPLICATE KEY UPDATE 改写完成 |
| FIX-DEV-004 | SEC-006 | ✅ DONE | UserSyncController 类型已对齐，缺失 method 已补 |
| FIX-DEV-005 | EVAL-002 | ✅ DONE | EvalController import / ErrorCode 已修正 |
| FIX-DEV-006 | CDSS-001 | ✅ DONE | CdssService 符号引用已对齐 |
| FIX-DEV-007 | AIK-001 | ✅ DONE | Knowledge* 符号引用已对齐 |
| FIX-DEV-008 | SEC-011 | ✅ DONE | TenantOnboarding* 类型 + 符号已修 |
| FIX-DEV-009 | 多任务 | ✅ DONE | 散点错全部消解 |

`mvn -q compile` + `mvn test`：**261 文件 0 错、248 测试 0 失败**（2026-05-21 实测）。

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

| 日期 | 状态 | 触发原因 | 解决 |
|---|---|---|---|
| 2026-05-20 | 🔴 RED | 80 个 commit / 8+ 任务在 develop 上交叉冲突，没有任何 owner 在合并后跑过 `mvn compile`；同期 GitHub branch protection 未强制要求 CI 通过 | 2026-05-21：各 owner 在 24h 内自行修绿，FIX-DEV-001..009 已归档 |
| 2026-05-21 | 🟡 YELLOW | mvn 全绿但本机 Node 12 无法验证前端；dashboard/menu 仍为 V1 风格未跟上 30+ 新业务 | 进行中：Phase 1 设计恢复 + Phase 2 文档梳理 + Phase 3 CI/演示环境前端验证 |

---

> 流程依据：[forbidden-patterns §6](../docs/engineering/forbidden-patterns.md#6-多-ai-协作) +
> [AUDIT-20260520-增量 §5](../docs/engineering/AUDIT-20260520-增量.md#5-推荐应对路径)。
> 本文件被 [`docs/engineering/00_总入口与AI接手导航.md`](../docs/engineering/00_总入口与AI接手导航.md) §1 Step 0 强制引用。
