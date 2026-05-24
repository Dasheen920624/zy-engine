# MedKernel 文档中心

> 版本：8.0 · 2026-05-24（0 业务引擎全能力上线基线）
> v1.0 GA 阶段：旧 5 套命名 + 金本位 5 份已归档到 `docs/archive/v0.3/`，当前执行以 **基础底座与引擎服务能力总览 + 产品宪法 + 产品体验固定规范 + 引擎全能力上线计划 + 1 份唯一实现级详细规范 + 单一任务台账** 为准。

当前口径：详细规范继续保留并可细化，但实施先从 **0 业务引擎全能力上线** 开始。引擎验收前，不按业务菜单拆开发泳道，不做单病种硬编码或演示假闭环。

---

## 0. 当前实施主线（必须先读）

| 文档 | 一句话 | 受众 |
|---|---|---|
| [**MEDKERNEL_FOUNDATION_AND_SERVICES.md**](MEDKERNEL_FOUNDATION_AND_SERVICES.md) | **基础底座与引擎服务能力总览** — 先引擎全能力，后业务服务包装 | 所有人 |
| [**MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md**](MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md) | **全系统产品与交互体验固定规范** — 角色、页面、分页、嵌入、可信解释、低打扰和体验门禁 | 产品 / 前端 / AI / QA |
| [**MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md**](MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md) | **集团医疗智能中枢实施落地方案** — 当前执行按 0 业务引擎全能力上线：先打通知识、字典、规则、路径、推荐、评估、随访、包发布、嵌入、模型和证据链 | 所有人 |
| [**MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md**](MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md) | **唯一实现级详细规范** — S0-S40、专业领域、系统设计、API/嵌入、AI、评级和验收继续在此细化 | 开发 / AI / QA / 临床评审 |

## 1. 任何人接手前必看治理文档

| 顺序 | 文档 | 一句话 | 受众 |
|---|---|---|---|
| 1 | [**CONSTITUTION.md**](CONSTITUTION.md) | **产品宪法** — 16 条硬约束 / 5 菜单 / 4 状态机 / 7 步流 / 6 阶段租户生命周期 / 术语 / 设计 token | 所有人 |
| 2 | [**backlog.md**](backlog.md) | **单一任务台账** — 当前只登记基础底座与引擎全能力上线任务 + 3 字段（id/owner/status） | AI 团队 / 实施 |

**阅读顺序**：基础底座与引擎服务能力总览（先看地图）→ 产品宪法（硬约束）→ 产品体验固定规范（页面和交互门禁）→ 实施落地方案（引擎上线计划）→ 全业务场景详细设计规范（查细节和继续细化）→ 任务台账（只领当前引擎任务）

---

## 2. 目录结构

```
docs/
├─ CONSTITUTION.md            ← 产品宪法（最高优先级）
├─ MEDKERNEL_FOUNDATION_AND_SERVICES.md ← 基础底座与引擎服务能力总览
├─ MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md ← 全系统产品与交互体验固定规范
├─ MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md ← 0 业务引擎全能力上线计划
├─ MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md ← 全业务场景与电子病历评级实现规范
├─ backlog.md                 ← 单一任务台账（当前只登记引擎全能力上线任务）
├─ README.md                  ← 你在这里
├─ handbook/                  ← 实施手册（按需写，≤ 10 份）
│   ├─ implementation.md      # 实施工程师手册
│   ├─ operations.md          # 运维手册
│   ├─ user-guides/           # 4 治理模块用户手册
│   └─ training/              # 3 角色培训材料
├─ adr/                       ← 架构决策记录（重大架构决策才写）
├─ legal/                     ← 合同 / SLA / 隐私 / DPA / License
├─ release/                   ← 发布证据
│   └─ v1.0.0-ga-evidence.md  # 框架和业务验收后填齐
└─ archive/v0.3/              ← v0.3 历史归档（仅供查阅）
    ├─ 01-05_*.md             # 旧金本位 5 份
    ├─ engineering/           # 旧 35+ ADR 与历史文档
    ├─ ai-dev-input/          # 旧 claim/lock 协作体系
    ├─ legacy-embed/          # 旧临床嵌入器构建（embed.html / embed-build.config.ts）
    └─ ai-branches-snapshot-20260524.md  # 25 个旧 ai/GA-* 分支 SHA 索引
```

**细节维护原则**：实现级细节统一补入 [MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md](MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md)；全局产品和交互体验规则统一补入 [MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md](MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md)。旧计划和无关参考入口已清除，避免多事实源。

---

## 3. 命名归一（v1.0 GA 单轨）

| 维度 | v1.0 GA |
|---|---|
| 任务命名 | `GA-ENG-*` 用于当前引擎任务；后续业务服务包装使用 `GA-SVC-*`，旧任务命名全部废弃 |
| 代码基线 | 当前生产主链路必须逐步去 mock、去裸 Map、去旧命名、去单病种假闭环 |
| 分支模型 | trunk-based：`main` 单线 + `feat/*` 短分支（≤ 3 天），废除 `develop` |
| PR base | 一律 → `main` |
| 文档登记 | 文档与代码改动**同 PR**，废除独立文档登记流程 |
| 任务台账 | 单一 [`backlog.md`](backlog.md)，废除 `02_任务台账.md` 与 `ai-dev-input/10_task_claims/` 体系 |

---

## 4. 历史 v0.3 查询

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

## 5. 关联文档

- [项目根 README](../README.md)
- [后端 README](../medkernel-backend/README.md)
- [前端 README](../frontend/README.md)

---

**End of docs README v8.0.**
