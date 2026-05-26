# MedKernel · 架构决策记录（ADR）

> 状态：骨架占位 · 重大架构决策才写
> 适用：v1.0 GA · 全周期

---

## 1. 文档定位

ADR（Architecture Decision Record）只在以下情况产出：

- 引擎能力的核心接口/数据模型发生**非兼容变更**
- 选型决策具有长期影响（例如：模型网关协议、五方言迁移策略、审计链算法、嵌入模式）
- 与 [产品宪法](../CONSTITUTION.md) 中的硬约束相关的让步或例外

**不写**：

- 单次代码重构
- 单个 PR 的实现细节
- 已有文档涵盖的常规决策（这些进 [详细规范](../MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md)）

---

## 2. 文件命名

格式：`NNNN-short-title.md`（4 位顺序号，从 0001 开始）

例：`0001-jwt-claim-baseline.md`、`0002-audit-chain-algorithm.md`、`0003-five-dialect-migration-strategy.md`

---

## 3. ADR 模板

每个 ADR 必须包含以下小节：

```markdown
# ADR-NNNN: <短标题>

> 日期：YYYY-MM-DD
> 状态：proposed / accepted / superseded
> 决策人：

## 背景
（约束、问题、为什么不能用现状继续）

## 决策
（选择了什么；如果替换了之前的 ADR，注明 supersedes）

## 影响
（对代码、文档、迁移、SLA、回滚的影响）

## 备选方案
（列出考虑过但未采纳的方案与拒绝理由）
```

---

## 4. 启用阶段

当前 v1.0 GA E0/E1 期间无新增 ADR。后续进入 E2..E5，预计在以下时点产生 ADR：

- E4 模型能力网关协议落地
- E4 嵌入模式（iframe / SDK / 纯 API）的同步/异步契约
- E5 审计链与证据导出格式
