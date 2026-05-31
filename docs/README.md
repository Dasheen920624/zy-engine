# MedKernel 文档中心

> 版本：10.0 · 2026-05-30（文档体系重构为自包含施工卡）
> 当前执行以 **核心 CONSTITUTION（11 视角不变量）+ 施工卡 cards/ + 共享体验契约 + 任务台账 + 质量基线** 为准。
> 工程节奏：按业务域纵向推进、两波（D0~D6 第一波 B0 真实；第二波 AI 加深）。

AI 开发"读最少、拿最全"：建任一任务只读 **核心 + 该域简报 + 该任务的施工卡**（页面卡另读体验契约），不再通读巨物。

---

## 0. 阅读顺序（按 [AGENTS.md](../AGENTS.md) 权威序）

| 顺序 | 文档 | 一句话 | 受众 |
|---|---|---|---|
| 1 | [CONSTITUTION.md](CONSTITUTION.md) | 核心 — 11 视角全局不变量（恒读）| 所有人 |
| 2 | `cards/<域>/_brief.md` | 域简报 — 本域角色 / 数据 / 依赖 / 域级验收 | 领该域卡的人 |
| 3 | `cards/<域>/<TASK-ID>.md` | 施工卡 — 单个交付物的完整合同（功能/契约/11视角/验收）| 领卡的人 |
| + | [EXPERIENCE_CONTRACT.md](EXPERIENCE_CONTRACT.md) | 共享体验与组件契约（页面卡读）| 前端 / 产品 |

辅助：找卡 [cards/_index.md](cards/_index.md) · 验收方法论 [audit/质量基线.md](audit/质量基线.md) · 名词 [glossary.md](glossary.md) · 任务状态 [backlog.md](backlog.md)。

冲突裁决：核心 > 域简报 > 卡。

---

## 1. 目录结构

```
docs/
├─ CONSTITUTION.md            ← 核心 CORE（11 视角不变量，恒读）
├─ EXPERIENCE_CONTRACT.md     ← 共享体验与组件契约（页面卡读）
├─ AI_DEVELOPMENT_RESTART_PLAN.md ← AI 研发重启执行闸门（开工流程 / 阶段门 / PR 证据）
├─ BUSINESS_IMPLEMENTATION_SCOPE_AUDIT.md ← 业务实现范围核查（S0-S40 / 菜单 / 卡到代码 / B0）
├─ glossary.md                ← 术语表
├─ backlog.md                 ← 任务台账（行→卡链接 / 状态 / 派单）
├─ cards/                     ← 施工卡（一卡 = 一 backlog 可交付物）
│   ├─ _index.md              # 场景 S0–S40 → 卡 + 全卡目录
│   ├─ _template.md           # 施工卡模板（合同）
│   ├─ _brief-template.md     # 域简报模板
│   ├─ _coverage-matrix.md    # 旧锚点 → 新卡 覆盖矩阵（迁移神谕）
│   ├─ D0/ … D6/              # 各域简报 + 该域卡
│   ├─ wave2/                 # 第二波卡（AIK/LLM/KNOWGEN/领域门面）
│   └─ ga/                    # GA 验收卡
├─ audit/质量基线.md           ← 验收方法论 + 门禁 + A1–A9 + 13 角色矩阵
├─ handbook/ adr/ legal/ release/   ← 实施手册 / 决策 / 法务 / 发布证据（不变）
└─ superpowers/               ← 设计/计划证据，不作为并行事实源
```

> **迁移过渡期**：旧巨物 `MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md`、`MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md`、`MEDKERNEL_FOUNDATION_AND_SERVICES.md`、`MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md` 物理保留但**不再权威**，按域搬迁进卡；全部域搬完后（P8）删除。权威以核心 + 卡为准。

---

## 2. 命名与协作

| 维度 | 口径 |
|---|---|
| 卡 ID | = backlog 任务 ID（领一卡 = 领一任务）|
| 分支模型 | trunk-based：远程长期分支只 `main`；日常 `codex/*` 短分支（≤3 天）|
| PR base | 一律 → `main` |
| 文档登记 | 文档与代码改动**同 PR** |
| 细节维护 | 任务级细节进对应施工卡；全局不变量进核心；可复用体验模式进体验契约。不新增并行事实源 |

---

## 3. 关联文档
- [项目根 README](../README.md) · [后端 README](../medkernel-backend/README.md) · [前端 README](../frontend/README.md)

---

**End of docs README v10.0.**
