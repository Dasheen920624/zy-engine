# MedKernel 文档中心

> 版本：5.0 · 2026-05-23（v1.0 GA 全量重启基线）
> v1.0 GA 阶段：旧 5 套命名 + 金本位 5 份已归档到 `docs/archive/v0.3/`，文档体系简化为 **3 张权威 + 按需手册**。

---

## 0. 任何人接手前必看 3 张

| 顺序 | 文档 | 一句话 | 受众 |
|---|---|---|---|
| 1 | [**CONSTITUTION.md**](CONSTITUTION.md) | **产品宪法** — 12 条硬约束 / 5 菜单 / 4 状态机 / 7 步流 / 6 阶段租户生命周期 / 术语 / 设计 token | 所有人 |
| 2 | [**V1_GA_REWRITE_PLAN.md**](V1_GA_REWRITE_PLAN.md) | **12 周方案** — Phase-0~6 / W1 Day-by-Day / 技术栈 / 风险 | 产品 / 架构 / 项目 |
| 3 | [**backlog.md**](backlog.md) | **单一任务台账** — 74 项 GA-* + 3 字段（id/owner/status） | AI 团队 / 实施 |

**阅读顺序**：1（必读 5 分钟）→ 2（按需 10 分钟）→ 3（领任务时查）

---

## 1. 目录结构

```
docs/
├─ CONSTITUTION.md            ← 产品宪法（最高优先级）
├─ V1_GA_REWRITE_PLAN.md      ← 12 周方案
├─ backlog.md                 ← 单一任务台账
├─ README.md                  ← 你在这里
├─ handbook/                  ← 实施手册（按需写，≤ 10 份）
│   ├─ implementation.md      # 实施工程师手册
│   ├─ operations.md          # 运维手册
│   ├─ user-guides/           # 4 治理模块用户手册
│   └─ training/              # 3 角色培训材料
├─ adr/                       ← 架构决策记录（重大架构决策才写）
├─ legal/                     ← 合同 / SLA / 隐私 / DPA / License
├─ release/                   ← 发布证据
│   └─ v1.0.0-ga-evidence.md  # W12 出版前填齐
└─ archive/v0.3/              ← v0.3 历史归档（仅供查阅）
    ├─ 01-05_*.md             # 旧金本位 5 份
    ├─ engineering/           # 旧 35+ ADR 与历史文档
    ├─ ai-dev-input/          # 旧 claim/lock 协作体系
    ├─ legacy-embed/          # 旧临床嵌入器构建（embed.html / embed-build.config.ts）
    └─ ai-branches-snapshot-20260524.md  # 25 个旧 ai/GA-* 分支 SHA 索引
```

---

## 2. 命名归一（v1.0 GA 单轨）

| 维度 | v1.0 GA |
|---|---|
| 任务命名 | `GA-<DOMAIN>-<NN>` 单一（旧 PR-V2-* / PR-V3-* / PR-FINAL-* / DOC-V2-* 已废） |
| 分支模型 | trunk-based：`main` 单线 + `feat/*` 短分支（≤ 3 天），废除 `develop` |
| PR base | 一律 → `main` |
| 文档登记 | 文档与代码改动**同 PR**，废除 DOC-V2-* 独立登记流程 |
| 任务台账 | 单一 [`backlog.md`](backlog.md)，废除 `02_任务台账.md` 与 `ai-dev-input/10_task_claims/` 体系 |

---

## 3. 历史 v0.3 查询

需要查 v0.3 时代的产品方案 / 任务历史 / 旧代码：

```bash
# 旧 main 状态
git checkout legacy/v0.3-main-20260524

# 旧 develop 状态（含 567 个领先 commit）
git checkout legacy/v0.3-develop-20260524

# 25 个 ai/GA-* 分支 SHA 索引
cat docs/archive/v0.3/ai-branches-snapshot-20260524.md
```

→ 旧文档原文全部保留在 [`docs/archive/v0.3/`](archive/v0.3/)。

---

## 4. 关联文档

- [项目根 README](../README.md)
- [后端 README](../medkernel-backend/README.md)
- [前端 README](../frontend/README.md)

---

**End of docs README v5.0.**
