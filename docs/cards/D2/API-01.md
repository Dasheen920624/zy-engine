# API-01 · 标准上下文 API

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D2 域简报](_brief.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §1.4 API 统一输入（L132）· §1.4.1 业务切片统一口径（L152）· §1.5.2 当前/后续 API 清单·上下文行（L191）· §7.4 标准临床模型（L1378，消费）。

## 身份
- 卡 ID：API-01（= backlog 任务 ID）
- 域：D2 试点准备
- 关联场景：横切（S2 院内系统接入 / S8 临床嵌入运行的上下文输入底座；所有引擎的统一只读入参）
- 依赖卡：[SYS-01](../D0/SYS-01.md)（12 标准对象 + `ClinicalEventContext`）· [BASE-03](../D0/BASE-03.md)（ApiResult/ProblemDetail/Record DTO）· [BASE-01](../D0/BASE-01.md)（OrgContext/包版本快照）· [OBS-01](../D0/OBS-01.md)（traceId/diagnose）
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标

提供**标准上下文快照 API**：把患者 / 就诊 / 诊断 / 医嘱 / 报告 / 组织 / 包版本一次组装为**请求级不可变的标准上下文快照**，作为规则 / 路径 / CDSS / 质控所有引擎的**统一只读输入**；缺失字段与编码映射状态**诚实暴露**，不静默填默认、不伪造。本卡只立"读取与组装"的上下文契约，不做临床判断。

## 现状（搬迁时核查 2026-05-30，以 `medkernel-backend/src` 为准）

实质基础已齐，本卡＝**契约化 / 真实化 / 补全**而非从零：

- 已有 `engine/context/`：`ContextSnapshot{Controller/Service/Request/Filter/Response/Status/Summary/Resources/Repository}`，端点 `POST /api/v1/engine/context/snapshots` + `GET /{id}` + `GET /{id}/diagnose` + `GET`（分页）；`ContextSnapshotControllerSecurityTest`/`ContextSnapshotTraceEndToEndTest` 在。
- 已有 `CanonicalResource(+Type/Repository)` 与 `canonical/` 12 子类型、`PackageVersionPort`(+Lenient 适配)、`ContextValidator`、`MissingFieldEntry`、`ContextIdempotencyKey`、`TerminologyMappingPort`、`QualityStatus`。
- **补全点**：对齐详规 §1.4 的 12 字段统一入参与 §1.5.2 的"标准上下文 ID + 缺失字段 + 映射状态"输出口径；包版本快照一致性、幂等、组织作用域、缺字段诚实暴露的契约固化与测试补全。

## 功能要求（原子可测条目）

- [ ] **FR-1 快照组装**：`POST /api/v1/engine/context/snapshots` 接收详规 §1.4 统一入参（`request_id`/`trace_id`/租户六层/`user_id`/`role_codes`/`patient_id`/`encounter_id`/`package_version`）+ 临床切片（诊断/医嘱/检验/检查/组织），组装为标准上下文快照，返回 `{snapshotId, 缺失字段, 映射状态}`。
- [ ] **FR-2 只读消费 12 标准对象**：快照内容**引用** [SYS-01](../D0/SYS-01.md) 的 12 类 `CanonicalResource`，**不重定义模型**；快照是只读组合，非新权威实体。
- [ ] **FR-3 包版本快照绑定**：每个快照绑定生效 `packageVersion`（经 `PackageVersionPort`）；同一快照内多次解析配置/知识返回**同一版本**（呼应核心 §7 唯一权威知识、[BASE-01](../D0/BASE-01.md) 请求快照）。
- [ ] **FR-4 缺失/映射诚实暴露**：缺必填字段 → `MissingFieldEntry` 逐条列出；编码未映射 → 映射状态标记（指向 [TERM-01](../_index.md) 字典卡），**不静默填默认、不伪造映射**。
- [ ] **FR-5 幂等**：`(tenant_id, request_id)` 幂等键（`ContextIdempotencyKey`），重复请求返回**同一** `snapshotId`，不重复落库。
- [ ] **FR-6 组织作用域 + 校验**：快照经 `OrgContext` 行级作用域（[BASE-01](../D0/BASE-01.md)）；`ContextValidator` 校验必填/类型；越权/缺上下文返回 `ProblemDetail`，不静默放行。
- [ ] **FR-7 解释追溯**：`GET /{snapshotId}/diagnose` 返回快照组装来源/缺失/映射诊断（专家模式，**不向客户面暴露裸 JSON/trace**，核心 §14）。

## 接口契约 / 页面契约

### 接口契约（引擎/API 卡）
- 端点：`POST /api/v1/engine/context/snapshots`（创建）· `GET /api/v1/engine/context/snapshots/{snapshotId}` · `GET .../{snapshotId}/diagnose`（专家模式解释）· `GET .../snapshots`（分页列表，复用 [API-13](../D0/API-13.md)）。
- DTO：`ContextSnapshotRequest`（Record + Bean Validation，§1.4 统一入参）→ `ContextSnapshotResponse {snapshotId, resources(12 类引用), missingFields[], mappingStatus, packageVersion, qualityStatus, traceId}`（核心 #7）。
- 响应信封：`ApiResult`；缺上下文/越权/校验失败用 `ProblemDetail`（[BASE-03](../D0/BASE-03.md)）。
- 状态机：N·A —— 上下文快照是**临床事实只读投影**（带 `QualityStatus` 质量状态，非"配置/变更/待办/告警"四类资产业务状态机）；快照不可变，无状态流转。
- 幂等 / 错误码 / traceId：`(tenant_id, request_id)` 幂等；缺上下文 `CONTEXT_MISSING`、越权 `ORG_SCOPE_DENIED`、校验失败 `CONTEXT_VALIDATION_FAILED`；全链路带 traceId（[OBS-01](../D0/OBS-01.md) 同源）。

### 页面契约（页面卡）
N·A —— 本卡无页面。快照被 D3 患者主索引/临床提醒页消费；`diagnose` 仅专家模式抽屉。

## 数据与迁移
- 表族：`context_snapshot`（快照头：患者/就诊/组织/包版本/质量/trace）· `context_snapshot_resource`（快照 ↔ `canonical_resource` 引用）· `context_idempotency_key`（幂等）· `canonical_resource`（[SYS-01](../D0/SYS-01.md) 立，本卡读）。
- 主键：ULID；唯一约束：`(tenant_id, request_id)` 幂等、`snapshot_id`；索引：`patient_id`、`encounter_id`、`org_path`、`package_version`、`event_time`。
- 组织字段：`tenant_id` + `org_path` + 审计字段（[BASE-01](../D0/BASE-01.md) 规约、[BASE-04](../D0/BASE-04.md) 留痕）。
- 5 方言迁移：h2/postgres/oracle/dm/kingbase 一致 + 中文注释 + 高基数索引（核心 §12；现状已有 context 迁移，本卡补齐快照引用/幂等约束）。

## 视角清单（11 视角逐条）
1. **产品架构**：★上下文快照 = 所有引擎的**统一只读输入单一口径**；各引擎禁各自组装上下文，杜绝"同一患者多版上下文"漂移。
2. **产品体验**：N·A —— 本卡无页面；快照驱动的患者页/提醒在 D3 呈现，`diagnose` 仅专家模式。
3. **系统与数据架构**：★主战场 —— 快照请求级不可变 + 包版本绑定 + 幂等；10 万级患者/就诊，`(patient,encounter)` 解析 P95 达标；关系库权威，缓存仅投影。
4. **临床医疗安全**：缺字段/未映射**诚实暴露**防"按缺数据误判"；高危近似编码不静默确认（判别归 [TERM-01](../_index.md)）；快照强绑 `patient_id` 防跨患者串数据。
5. **知识与数据治理**：`packageVersion` 快照保证同一快照内知识/配置版本一致（核心 §7）；编码字段挂标准字典映射锚点（消费 [TERM-01](../_index.md)）。
6. **安全合规与监管**：患者敏感字段脱敏 + 字段级加密 + 访问审计（核心 §8 / #8）；快照只载当前场景必要字段，禁默认透传整份病历。
7. **集团化与多租户治理**：快照经 `OrgContext` 作用域，跨租户/越权默认 0 行 + 审计（[BASE-01](../D0/BASE-01.md)）；快照带组织维支撑灰度/责任归属。
8. **集成与互操作**：外部系统经适配器映射入快照（[INTEG-01](../_index.md)）；[OPT-01](OPT-01.md) FHIR 门面与本卡**共用同一 `CanonicalResource`**，不另造模型。
9. **运维 / SRE / 国产化**：5 方言；高基数快照表索引/分区；支持在线/异步/批量入口（[SYS-05](../D0/SYS-05.md)）；内外网双形态下权威源均在院内关系库。
10. **质量与真实性审计**：无 mock 患者、无写死病种/编码（核心 #18）；缺失**诚实拒绝不 fallback**（铁律 #1/#2）；门禁校验快照表带 `tenant_id+org_path`。
11. **AI / 模型治理与可降级**：快照是 AI 增强的 **B0 输入底座**；无模型时规则/路径/CDSS 消费同一快照（核心 §11 / 铁律 #4）；本卡纯确定性、无模型依赖。

## 适用不变量
- 命中核心约束：**§7 包版本请求快照（唯一权威知识落点）** · **#5 关系库权威** · **#7 Record DTO + Bean Validation** · **§9 组织作用域** · **#8 敏感字段脱敏加密** · **§10 与 FHIR 门面共用 CanonicalResource**。
- 本卡落点：以一个**不可变上下文快照 + 包版本绑定 + 缺失/映射诚实暴露**，把"任意引擎要消费的患者上下文"在入口一次性钉死，下游引擎零散组装、零版本漂移。

## 验收 + 验证
- [ ] **AC-1（FR-1/2）**：构造一次含患者/就诊/诊断/医嘱/报告/组织的请求 → 返回 `snapshotId`，快照内 12 标准对象引用正确、可往返读出。
- [ ] **AC-2（FR-3）**：同一快照内两次解析同一配置/知识键 → 返回**同一** `packageVersion`（即使期间有新版本激活）。
- [ ] **AC-3（FR-4）**：构造缺"诊断"且含未映射本地检验码的请求 → 响应 `missingFields` 列出诊断、`mappingStatus` 标未映射，**未填任何默认值**。
- [ ] **AC-4（FR-5）**：同 `(tenant, request_id)` 重复 POST → 返回同一 `snapshotId`，库中不新增第二条。
- [ ] **AC-5（FR-6）**：跨组织患者请求 → `ORG_SCOPE_DENIED` + 审计；无租户上下文请求 → `CONTEXT_MISSING`（中文 + traceId），未落默认数据。
- 关联 A1–A9 剧本：A2 院内接入（外部数据入标准上下文）、A3 临床运行（快照驱动规则/路径/CDSS）。
- T-GATE：前后端真实性门禁全绿（无 mock 患者/无写死编码/无伪造映射；迁移 5 方言一致）。
- B0 验收：本卡纯确定性、无模型依赖，**天然 B0**（关闭全部模型后行为不变）。

## 完工证据
- 代码 permalink：`ContextSnapshotController/Service` · `ContextSnapshotRequest/Response`（§1.4 入参/输出）· `PackageVersionPort`（版本快照）· `ContextValidator` + `MissingFieldEntry`（诚实暴露）· `ContextIdempotencyKey` · `context_snapshot*` 迁移（×5 方言）。
- 测试：快照组装往返测试 + 包版本一致性测试 + 缺失/映射诚实暴露测试 + 幂等测试 + 跨组织拒绝（`ContextSnapshotControllerSecurityTest`）+ trace 端到端（`ContextSnapshotTraceEndToEndTest`）。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。

## 大卡工序（3d，后端单层，可单 PR；如拆分）
- PR1：§1.4 统一入参 DTO + 快照组装 + 12 对象引用 + 包版本绑定 + 5 方言迁移补齐 → AC-1/2。
- PR2：缺失/映射诚实暴露 + 幂等 + 组织作用域拒绝 + diagnose 解释 + 安全/trace 测试 → AC-3/4/5。
