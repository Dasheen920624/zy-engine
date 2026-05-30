# API-03 · 标准知识资产 API

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D2 域简报](_brief.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §1.5.2 当前/后续 API 清单·知识行（L185）· §8.6 生成流水线（L1720，客户面）· 落地规划 §6.1 总流程（L285）· 核心 §7 唯一权威知识。

## 身份
- 卡 ID：API-03（= backlog 任务 ID）
- 域：D2 试点准备
- 关联场景：S3 AI 知识工厂（客户面 API）
- 依赖卡：[KNOW-01](KNOW-01.md)（来源/资产/引用/分级）· [KNOW-02](KNOW-02.md)（候选/审核/去重）· [SYS-08](SYS-08.md)（版本替换）· [BASE-03](../D0/BASE-03.md)（API 契约）· [API-13](../D0/API-13.md)（大列表分页 + 异步导出）· [OBS-01](../D0/OBS-01.md)（traceId）
- 工作量：4d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
提供知识资产的**统一 REST 客户面**：来源登记 / 解析 / 引用 / 版本（草稿→审核→替换）/ 历史重放 / 分页筛选 / **异步导出**。本卡只立**API 契约**，能力在 [KNOW-01](KNOW-01.md)/[KNOW-02](KNOW-02.md)/[SYS-08](SYS-08.md)；统一入参/信封复用 [BASE-03](../D0/BASE-03.md)。

## 现状（搬迁时核查 2026-05-30，以 `medkernel-backend/src` 为准）
`engine/knowledge` **控制器与异步导出已建**，本卡＝**契约化/补全/对齐 12 字段统一入参**：
- 已有：`KnowledgeIdentityController`、`KnowledgeVersionController`（`activate`/`withdraw`）、`KnowledgeExportController` + `KnowledgeExportService`/`KnowledgeExportJob`(+`Repository`/`AsyncConfig`/`ExportType`/`ExportStatus`)、`KnowledgeIdentityFilter`、安全测试。
- 缺口（本卡补）：① 端点**统一 12 字段入参**（详规 §1.4）+ `ApiResult`/`ProblemDetail` 信封一致；② **历史重放**端点（绑当时资产包版本 + 患者快照）；③ 候选/审核端点对齐 [KNOW-02](KNOW-02.md)；④ 大列表统一走 [API-13](../D0/API-13.md) 分页、导出走异步任务。

## 功能要求（原子可测条目）
- [ ] **FR-1 来源/资产 CRUD**：`POST /sources`、`POST /sources/{id}/versions`、`GET/POST /identities`、`GET /identities/{id}`、`GET /identities/{id}/citations`。
- [ ] **FR-2 版本与替换**：`POST /identities/{id}/versions/{vid}/submit|activate|withdraw`（替换委托 [SYS-08](SYS-08.md)）、`GET /identities/{id}/lineage`。
- [ ] **FR-3 候选/审核**：`GET /identities/{id}/candidates`、`POST /candidates/{id}/review`、`GET /candidates/{id}/diff`（委托 [KNOW-02](KNOW-02.md)）。
- [ ] **FR-4 历史重放**：`GET /identities/{id}/versions/{vid}/replay`（绑定当时资产版本，标"历史版本"，不混入新决策）。
- [ ] **FR-5 大列表 + 异步导出**：列表统一服务端分页/筛选（[API-13](../D0/API-13.md)）；导出走 `KnowledgeExportJob` 异步任务（提交→轮询→下载）。
- [ ] **FR-6 统一入参/信封**：所有端点统一 12 字段入参（request_id/trace_id/租户六层/user/role/package_version 等）+ `ApiResult`/`ProblemDetail`（[BASE-03](../D0/BASE-03.md)）。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
- 端点：`/api/v1/engine/knowledge/**`（sources、identities、versions、candidates、lineage、replay、exports）。
- DTO：复用 `SourceRegisterRequest`/`DraftVersionCreateRequest`/`KnowledgeIdentityFilter`/`KnowledgeExportJob`；新增 `KnowledgeReplayResponse`。
- 响应信封：`ApiResult` / `ProblemDetail`（[BASE-03](../D0/BASE-03.md)）；大列表 `PageResult`（[API-13](../D0/API-13.md)）。
- 状态机：知识版本状态机（[KNOW-01](KNOW-01.md)/[SYS-08](SYS-08.md)）；导出任务走核心 §3 待办/任务态。
- 幂等 / 错误码 / traceId：写操作幂等键（[BASE-03](../D0/BASE-03.md)）；无来源激活 → `KNOWLEDGE_CITATION_REQUIRED`；越权发布 → `FORBIDDEN`；全链路 traceId（[OBS-01](../D0/OBS-01.md)）。
### 页面契约（页面卡）
N·A —— 本卡无页面。被 D4 AI 知识审核页 / D6 来源追溯页消费。

## 数据与迁移
- 无独立表族——复用 [KNOW-01](KNOW-01.md)/[KNOW-02](KNOW-02.md)/[SYS-08](SYS-08.md) 表族 + `knowledge_export_job`（已有）。
- 不落新库（API 契约卡）；导出任务表已存在。

## 视角清单（11 视角逐条）
1. **产品架构**：知识能力的统一对外契约口，前端/外部一处对接。
2. **产品体验**：N·A —— 由 D4/D6 页面消费；大列表分页 + 异步导出进度。
3. **系统与数据架构**：★统一入参/信封/分页；导出异步不阻塞；P95 列表 ≤1s；10 万级资产分页稳定（[API-13](../D0/API-13.md)）。
4. **临床医疗安全**：无来源断言不可激活；历史重放标"历史版本"不混入新决策。
5. **知识与数据治理**：版本/替换/lineage/导出全可溯。
6. **安全合规与监管**：发布/替换/导出留审计（[BASE-04](../D0/BASE-04.md)）；导出证据供监管。
7. **集团化与多租户治理**：端点按 `OrgContext` 作用域过滤（[BASE-01](../D0/BASE-01.md)）；跨租户默认 0。
8. **集成与互操作**：外部系统经本 API 读知识，不直连库。
9. **运维 / SRE / 国产化**：异步导出可重试/断点；大导出离线友好。
10. **质量与真实性审计**：无伪造分页计数/导出哈希；端点真实连引擎（铁律 #1）。
11. **AI / 模型治理与可降级**：候选/审核端点呈现"AI 候选不执行"；关模型仅候选端点空，CRUD/版本/导出不受影响。

## 适用不变量
- 命中核心约束：**§1.4 统一入参** · **§7 唯一权威 + 来源可溯** · **#14 候选不执行** · **依赖 [API-13](../D0/API-13.md) 大列表 / [BASE-03](../D0/BASE-03.md) 契约**。
- 本卡落点：把知识引擎能力以**统一契约**对外，不在 API 层写业务规则。

## 验收 + 验证
- [ ] **AC-1（FR-1/2）**：建来源→建资产带引用→提交→激活（替换走 [SYS-08](SYS-08.md)）→ lineage 可查；统一信封返回。
- [ ] **AC-2（FR-4）**：重放旧版 → 返回当时绑定内容并标"历史版本"。
- [ ] **AC-3（FR-5）**：10 万级资产列表分页/筛选稳定；提交异步导出 → 轮询完成 → 下载，进度真实。
- [ ] **AC-4（FR-6）**：缺统一入参字段 → `ProblemDetail` 校验错误；越权访问跨租户资产 → 0 + 审计。
- 关联 A1–A9 剧本：A2 知识沉淀、A4 替换。
- T-GATE：前后端真实性门禁全绿（无伪造分页/导出）。
- B0 验收：CRUD/版本/导出纯确定性，**天然 B0**。

## 完工证据
- 代码 permalink：`/api/v1/engine/knowledge/**` 端点统一入参/信封 + `KnowledgeReplayResponse` + 分页/异步导出对齐。
- 测试：契约测试（入参/信封）+ 安全测试（越权/租户隔离）+ 分页/异步导出 E2E + 重放标识测试。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
