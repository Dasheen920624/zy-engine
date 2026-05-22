# develop 健康哨兵（AI 接手前必读）

> **这是任何 AI 接手任务前必须看的第一份文件**（早于 02_任务台账.md）。
> 若本文标 🔴 RED，**只允许领"修复编译"类任务，不允许领新功能 / 改造类任务**。

---

## 当前状态

| 字段 | 值 |
|---|---|
| 状态 | 🟢 **GREEN — develop @ `6711e5c` + FIX-DEV-010 主干修复已完成，全量后端/前端门禁通过** |
| 最后更新 | 2026-05-22 |
| 最后验证 commit | `6711e5c` (origin/develop) + FIX-DEV-010 final patch（最终提交后以归档 claim 记录为准） |
| 演示候选 tag | `v0.2-demo`（指向 main `565e8a7`） |
| 验证命令 | `mvn -q compile` + `medkernel-mvp/scripts/run-tests.ps1` + `medkernel-mvp/scripts/build.ps1` + `frontend eslint/typecheck/vitest/vite build` + `check-ai-collaboration.ps1` |
| 结果 | **PASS**：后端 compile/test/build 通过；前端 ESLint 0 error、typecheck/test/build 通过；协作 claim/lock 检查通过 |
| 详细报告 | [docs/engineering/AUDIT-20260520-增量.md](../docs/engineering/AUDIT-20260520-增量.md) |
| 历史派单 | [docs/engineering/2026-05-20-破窗行动.md](../docs/engineering/2026-05-20-破窗行动.md) — FIX-DEV-001..009 已全部修复 |
| 发布 PR | [PR #10](https://github.com/Dasheen920624/medkernel/pull/10) — MERGED via squash 2026-05-21 |

---

## 状态语义

| 标签 | 含义 | 准入规则 |
|---|---|---|
| 🟢 GREEN | `mvn -q compile` + `mvn -q test` 均 PASS | 任何 AI 可按 02_任务台账 正常领任务 |
| 🟡 YELLOW | compile PASS 但 test FAIL，或 lint 失败 | 可领新任务但 review 时要附 test 修复证明 |
| 🔴 RED | compile FAIL，或缺关键 Maven/npm 依赖 | **冻结新任务**；只允许领下面"修复白名单"内的任务 |

---

## 🟢 GREEN 状态下的准入

- ✅ 允许领新功能任务（PR-V2-*、AIK-*、SEC-*、EVAL-* 等），按 02_任务台账 取
- ✅ 允许 push 到 develop（必须本地 `mvn -q compile` PASS + verify-pr.ps1 自检）
- ✅ 演示候选已 tag `v0.2-demo` 指向 main `565e8a7`，可作为客户演示基线

### 演示前 / 部署前还需人工做的事

- [ ] 在 Node 18+ 环境（演示机或 CI 二次环境）跑：
  ```powershell
  cd frontend
  npm install
  npm run lint && npm run typecheck && npm test && npm run build
  ```
- [ ] 浏览器手动验证：dashboard / 配置包中心 / 路径 / 规则 / 质控 / Provider 状态等核心页面均可加载（无 404、无白屏、菜单两段式分组正确）
- [ ] 演示环境 smoke：登录 → 切组织 → 跑一次 rule-engine/evaluate 看 hit；切到 HOSPITAL_ALPHA 看院级质控驾驶舱
- [ ] GitHub Settings → Branches：为 `main` + `develop` 加 Branch Protection（require status checks: backend-build-test + guard-rules + Compile only；require PR review）—— **没这一步还会再坏一次**，强烈建议本次发布同步开启

### 已知缺陷（演示时回避）

详见 [`docs/engineering/2026-05-21-功能矩阵-V3.md`](../docs/engineering/2026-05-21-功能矩阵-V3.md) §5：

| 编号 | 描述 | 演示建议 |
|---|---|---|
| KD-001 | WF-001 待办中心后端全 Mock | 演示只看不点 |
| KD-002 | MpiController 双副本 | 选一份暴露 |
| KD-003 | UserSyncController 双副本 | 同上 |
| KD-004 | HikariCP 未接入 | ~~已修复~~ PR-FINAL-15a/15b 已接入 |
| KD-005~007 | Jackson SNAKE_CASE / OrgContext / Placeholder | 演示功能不受影响或跳过 |

### 当前 RED 责任任务清单

无。FIX-DEV-010 已完成，develop 恢复 GREEN。

### 已解决的历史 RED 阻塞（FIX-DEV-001..010，归档）

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
| FIX-DEV-010 | PR-FINAL-18 × SEC-008 × PR-FINAL-26 集成 | ✅ DONE | 对齐 `SsoIdentityBinding` 拆分仓储引用；修复图谱租户隔离 fallback；收敛前端契约类型与远程布局卫生债；规范化 active claim / lock 元数据 |

2026-05-22 实测：后端 `mvn -q compile` PASS、`run-tests.ps1` PASS、`build.ps1` PASS；前端 ESLint 0 error（保留历史 warning）、TypeScript PASS、Vitest 41 files / 192 tests PASS、Vite build PASS；`verify-pr.ps1 -TaskId FIX-DEV-010` PASS（17 PASS / 0 FAIL / 2 WARN）。

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
| 2026-05-21 上午 | 🟡 YELLOW | mvn 全绿但本机 Node 12 无法验证前端；dashboard/menu 仍为 V1 风格未跟上 30+ 新业务 | Phase 1 设计恢复 + Phase 2 文档梳理 完成；CI 双 check 通过 |
| 2026-05-21 下午 | 🟢 GREEN | develop @ 1387c2f 通过 CI guard-rules + backend-build-test 双 check | PR #10 squash merge 进 main `565e8a7`，tag `v0.2-demo`；等用户开 branch protection 防止再坏 |

---

> 流程依据：[forbidden-patterns §6](../docs/engineering/forbidden-patterns.md#6-多-ai-协作) +
> [AUDIT-20260520-增量 §5](../docs/engineering/AUDIT-20260520-增量.md#5-推荐应对路径)。
> 本文件被 [`docs/engineering/00_总入口与AI接手导航.md`](../docs/engineering/00_总入口与AI接手导航.md) §1 Step 0 强制引用。
