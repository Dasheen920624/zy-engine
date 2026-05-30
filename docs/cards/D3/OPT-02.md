# OPT-02 · CDS Hooks 风格事件契约

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D3 域简报](_brief.md)。
> 迁移来源（覆盖矩阵锚点）：详规 S8 临床嵌入运行 · 核心 §10 集成边界 · 详规 §1.4 触发点归类。

## 身份
- 卡 ID：OPT-02（引擎/契约卡；6 触发点事件契约单一归属）
- 域：D3 临床运行
- 关联场景：S8 临床嵌入运行
- 依赖卡：[API-02](API-02.md) 事件 · [API-07](API-07.md) 推荐 · [EMBED-01](EMBED-01.md) 嵌入 · [API-01](../D2/API-01.md) 上下文
- 工作量：5d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
定义并落地 **CDS Hooks 风格 6 触发点事件契约**，作为临床事件/推荐/嵌入三方共用的统一触发语义：`patient-view` / `order-sign` / `medication-prescribe` / `result-review` / `discharge-sign` / `followup-alert`。

## 现状（搬迁时核查 2026-05-30，以 `medkernel-backend` 为准）
基本待建/框架化：现有 `engine/context` 临床事件与 `engine/recommendation` 触发为项目自有形态，**无统一 CDS Hooks 风格触发点契约**。本卡＝抽出 6 触发点的统一事件契约（请求/上下文/响应卡片格式），供 [API-02](API-02.md)/[API-07](API-07.md)/[EMBED-01](EMBED-01.md) 引用，非各自造一套。

## 功能要求（原子可测条目）
- [ ] FR-1 触发点枚举：6 类触发点为受控枚举，每类定义必备上下文字段。
- [ ] FR-2 请求契约：触发请求统一 `hook` + `context` + 患者/就诊/包版本（§1.4 12 字段）。
- [ ] FR-3 响应契约：返回 cards（推荐卡）+ 可选 system-actions，结构统一。
- [ ] FR-4 单一引用：[API-02](API-02.md)/[API-07](API-07.md)/[EMBED-01](EMBED-01.md) 均引用本契约，不重定义触发语义。
- [ ] FR-5 版本化：契约自身版本化，向后兼容策略明确。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
- 契约对象：`CdsHookRequest{hook, hookInstance, context, prefetch}` → `CdsHookResponse{cards[]}`（Record DTO + Bean Validation）
- 触发点：6 枚举常量，作为 [API-02](API-02.md) 事件与 [API-07](API-07.md) 推荐的触发分类键
- 响应信封：`ApiResult` / `ProblemDetail`；trace（[OBS-01](../D0/OBS-01.md)）
- 幂等 / 错误码：`hookInstance` 幂等键

## 数据与迁移
- N·A 或轻量：触发点为枚举常量 + 契约版本登记；不新建大表族（事件落库归 [API-02](API-02.md)）。

## 视角清单（11 视角逐条）
1. 产品架构：临床触发的"统一语言"，三卡共用。
2. 产品体验：N·A（契约层）。
3. 系统与数据架构：契约稳定、可版本化；触发分类 O(1)。
4. 临床医疗安全：触发点语义清晰避免错触发；高危触发走 [OPT-03](OPT-03.md) 分级。
5. 知识与数据治理：N·A（契约不持有知识）。
6. 安全合规与监管：触发审计可按 hook 类型聚合（[BASE-04](../D0/BASE-04.md)）。
7. 集团化与多租户治理：契约与租户无关；上下文携 `OrgContext`。
8. 集成与互操作：★对齐 CDS Hooks 行业风格，便于第三方对接（[EMBED-01](EMBED-01.md)）。
9. 运维 / SRE / 国产化：契约版本可灰度。
10. 质量与真实性审计：★三处引用同一契约、无分叉；契约测试守约。
11. AI / 模型治理与可降级：N·A（契约确定性）。

## 适用不变量
- 命中核心约束：**核心 §10 集成边界** · **§1.4 统一入参** · **铁律 #1（单一归属）**。
- 本卡落点：6 触发点统一事件契约，三卡单一引用不分叉。

## 验收 + 验证
- [ ] AC-1（FR-1/2/3）：6 触发点请求/响应契约完整、契约测试通过。
- [ ] AC-2（FR-4）：[API-02](API-02.md)/[API-07](API-07.md)/[EMBED-01](EMBED-01.md) 引用同一契约（无重复定义）。
- [ ] AC-3（FR-5）：契约版本化 + 兼容策略验证。
- 关联 A1–A9 剧本：A4 触发点。
- T-GATE：后端真实性门禁全绿（契约单一源 / 无分叉）。
- B0 验收：契约纯确定性，与模型无关。

## 完工证据
- 代码 permalink：CdsHook 契约 + 6 触发点枚举 + 三卡引用。
- 测试：契约测试 / 引用一致性 / 版本兼容。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
