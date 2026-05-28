# MedKernel 文档中心

> 版本：9.5 · 2026-05-28（业务细节、第三方接口文档与多维治理口径校准）
> 当前执行以 **基础底座与引擎服务能力总览 + 产品宪法 + 文档语言与 AI 协作规范 + 产品体验固定规范 + 引擎全能力上线计划 + 1 份唯一实现级详细规范 + 单一任务台账** 为准。

当前口径：详细规范继续保留并可细化，但实施先从 **0 业务引擎全能力上线** 开始。引擎验收前，不按业务菜单拆开发泳道，不做单病种硬编码或演示假闭环。

业务接口口径：S0-S40 是业务能力目录，不是接口清单。所有场景统一写 `API 归类`，只分当前引擎 API、后续 E6 业务包装 API、复用已有引擎 API、外部系统集成 API、暂不设独立 API 五类。第三方对接统一通过适配器、标准上下文、临床事件、FHIR/CDS Hooks 风格门面、嵌入、回调、包发布同步和审计证据链管理。

---

## 0. 当前实施主线（必须先读）

| 文档 | 一句话 | 受众 |
|---|---|---|
| [**MEDKERNEL_FOUNDATION_AND_SERVICES.md**](MEDKERNEL_FOUNDATION_AND_SERVICES.md) | **基础底座与引擎服务能力总览** — 先引擎全能力，后业务服务包装 | 所有人 |
| [**MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md**](MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md) | **全系统产品与交互体验固定规范** — 角色、页面、分页、嵌入、可信解释、低打扰和体验门禁 | 产品 / 前端 / AI / QA |
| [**MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md**](MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md) | **集团医疗智能中枢实施落地方案** — 当前执行按 0 业务引擎全能力上线：先打通知识、字典、规则、路径、推荐、评估、随访、包发布、嵌入、模型和证据链 | 所有人 |
| [**MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md**](MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md) | **唯一实现级详细规范** — S0-S40、API 归类、专业领域、系统设计、API/嵌入、AI、评级和验收继续在此细化 | 开发 / AI / QA / 临床评审 |

## 1. 任何人接手前必看治理文档

| 顺序 | 文档 | 一句话 | 受众 |
|---|---|---|---|
| 1 | [**CONSTITUTION.md**](CONSTITUTION.md) | **产品宪法** — 17 条硬约束 / 5 菜单 / 4 状态机 / 7 步流 / 6 阶段租户生命周期 / 术语 / 设计 token | 所有人 |
| 2 | [**DOCUMENTATION_LANGUAGE_POLICY.md**](DOCUMENTATION_LANGUAGE_POLICY.md) | **文档语言与 AI 协作规范** — 当前有效文档必须中文书写，功能完成后 PR 合并到远程 main | 所有人 / AI 团队 |
| 3 | [**backlog.md**](backlog.md) | **单一任务台账** — E1-E5 当前执行，E6 后置业务包装清单只登记不抢跑，字段固定为 id/owner/status | AI 团队 / 实施 |

**阅读顺序**：基础底座与引擎服务能力总览（先看地图）→ 产品宪法（硬约束）→ 文档语言与 AI 协作规范（中文和远程 main 合并门禁）→ 产品体验固定规范（页面和交互门禁）→ 实施落地方案（引擎上线计划）→ 全业务场景详细设计规范（查细节和继续细化）→ 任务台账（E1-E5 执行，E6 只作后置包装清单）

---

## 2. 目录结构

```
docs/
├─ CONSTITUTION.md            ← 产品硬约束和交互边界
├─ MEDKERNEL_FOUNDATION_AND_SERVICES.md ← 基础底座与引擎服务能力总览
├─ MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md ← 全系统产品与交互体验固定规范
├─ MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md ← 0 业务引擎全能力上线计划
├─ MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md ← 全业务场景与电子病历评级实现规范
├─ DOCUMENTATION_LANGUAGE_POLICY.md ← 文档中文书写与 AI 协作规范
├─ backlog.md                 ← 单一任务台账（E1-E5 当前执行；E6 后置包装清单）
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
├─ superpowers/               ← 当前任务的设计/计划证据，不高于权威文档
└─ README.md
```

`openspec/` 位于仓库根目录：`openspec/specs/` 保存当前稳定规格，`openspec/changes/` 只放当前进行中的变更，`openspec/archive/` 仅保存已完成 OpenSpec 变更的审计记录，不作为当前事实源。

**细节维护原则**：实现级细节统一补入 [MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md](MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md)；全局产品和交互体验规则统一补入 [MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md](MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md)。不新增旧版本归档、旧任务锁或并行事实源；保留的 OpenSpec/Superpowers 记录只能用于追溯对应变更。

---

## 3. 命名归一（v1.0 GA 单轨）

| 维度 | v1.0 GA |
|---|---|
| 任务命名 | `GA-ENG-*` 用于当前引擎任务；后续业务服务包装使用 `GA-SVC-*` |
| 代码基线 | 当前生产主链路必须逐步去 mock、去裸 Map、去单病种假闭环 |
| 分支模型 | trunk-based：远程长期分支只保留 `main`，不设 `develop`；日常默认使用 `codex/*` 短分支（≤ 3 天），除非任务明确指定其它前缀 |
| PR base | 一律 → `main` |
| 文档登记 | 文档与代码改动**同 PR** |
| 任务台账 | 单一 [`backlog.md`](backlog.md) |

---

## 4. 关联文档

- [项目根 README](../README.md)
- [后端 README](../medkernel-backend/README.md)
- [前端 README](../frontend/README.md)

---

**End of docs README v9.5.**
