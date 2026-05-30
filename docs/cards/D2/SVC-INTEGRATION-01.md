# SVC-INTEGRATION-01 · 第三方业务接口服务包

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D2 域简报](_brief.md)。
> 迁移来源（覆盖矩阵锚点）：落地规划 §11.4 第三方对接验收（L764，消费）· 详规 §1.5.3 第三方对接能力全景（L211）· §S40 医技互认、远程协同与区域共享（L924，区域协同接入侧）· 核心 §10 集成边界。

## 身份
- 卡 ID：SVC-INTEGRATION-01（= backlog 任务 ID）
- 域：D2 试点准备
- 关联场景：S2 院内系统接入（第三方业务接口编排）
- 依赖卡：[INTEG-01](INTEG-01.md)（对接总线）· [INTEG-02](INTEG-02.md)（接口契约模板）· [OPT-01](OPT-01.md)（FHIR 门面）· [API-01](API-01.md)（标准上下文）· [BASE-04](../D0/BASE-04.md)（审计）
- 工作量：5d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
提供**第三方业务接口**服务包：接入管理 + 字段映射 + 健康检查 + FHIR 门面 + 回调 + **区域协同**接入。把对接能力（[INTEG-01](INTEG-01.md) 总线 + [OPT-01](OPT-01.md) FHIR 门面）编排成面向业务的接口服务，为 [ADAPTER-01](ADAPTER-01.md) 适配器中心页供端到端接入管理。

## 现状（搬迁时核查 2026-05-30，以 `medkernel-backend/src` 为准）
`engine/integration` + FHIR 门面（[OPT-01](OPT-01.md)）**已建**，本卡＝**业务接口编排 + 回调/区域协同补全**：
- 已有：`engine/integration`（`IntegrationAdapter`/`MessageLog`/`WebhookConfig` + `IntegrationService`/`Controller`，详见 [INTEG-01](INTEG-01.md)）；FHIR 资源门面（[OPT-01](OPT-01.md)）。
- 缺口（本卡补）：① **接入管理**端到端编排（接入申请→鉴权配置→联调→上线）；② **回调**管理（Webhook 回调注册/重放）；③ **区域协同**接入（医联体/区域平台跨机构来源证据）；④ 健康检查/字段映射复用 [INTEG-01](INTEG-01.md)。

## 功能要求（原子可测条目）
- [ ] **FR-1 接入管理**：第三方接入全生命周期（申请→鉴权→字段映射→联调→上线→下线），状态可见。
- [ ] **FR-2 FHIR + 适配双路**：标准化方经 [OPT-01](OPT-01.md) FHIR 门面接入，院内私有方经 [INTEG-01](INTEG-01.md) 适配器，两路并存。
- [ ] **FR-3 回调管理**：注册/验签/重放业务回调（复用 `IntegrationWebhookConfig`）；失败入死信可重放。
- [ ] **FR-4 区域协同**：医联体/区域平台跨机构来源证据接入，标注来源机构 + 可信分级（[OPT-07](OPT-07.md)）。
- [ ] **FR-5 健康/降级**：接入健康检查；断连 `NOT_CONNECTED`、超时不阻断主流程（核心 §10）。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
- 端点：复用 [INTEG-01](INTEG-01.md) `/engine/integration/**` + 接入管理编排接口（onboarding、callbacks、regional）。
- DTO：复用 `AdapterCreateDto`/`WebhookCreateDto`/`IntegrationMessageLog`；新增 `IntegrationOnboarding`（接入生命周期）· `RegionalSource`（区域来源）。
- 响应信封：`ApiResult` / `ProblemDetail`（[BASE-03](../D0/BASE-03.md)）。
- 状态机：接入生命周期走核心 §3 配置类；回调走待办/死信态。
- 幂等 / 错误码 / traceId：回调按 message_id 幂等；区域来源未分级 → `REGIONAL_SOURCE_UNGRADED`；断连 `NOT_CONNECTED`；traceId（[OBS-01](../D0/OBS-01.md)）。
### 页面契约（页面卡）
N·A —— 本卡为服务包后端。页面在 [ADAPTER-01](ADAPTER-01.md)（适配器中心）。

## 数据与迁移
- 表族（已有）：`integration_adapter`/`webhook_config`/`message_log`（[INTEG-01](INTEG-01.md)）；本卡补 `integration_onboarding`/`regional_source`。
- 主键 ULID；索引：`adapter_code`、`onboarding_status`、`org_path`。
- 5 方言迁移一致 + 中文注释。

## 视角清单（11 视角逐条）
1. **产品架构**：第三方接入的"业务级编排"服务包（FHIR + 适配 + 区域协同）。
2. **产品体验**：N·A —— 适配器中心页（[ADAPTER-01](ADAPTER-01.md)）。
3. **系统与数据架构**：接入幂等/回调死信；健康检查；区域来源可溯。
4. **临床医疗安全**：外部数据经标准上下文不绕引擎直写；超时不阻断主流程。
5. **知识与数据治理**：区域来源经 [OPT-07](OPT-07.md) 分级、可溯。
6. **安全合规与监管**：接入/回调/区域协同留审计（[BASE-04](../D0/BASE-04.md)）；跨机构数据合规。
7. **集团化与多租户治理**：接入按 org 隔离；医联体跨机构受控。
8. **集成与互操作**：★主战场 —— FHIR + 适配双路 + 回调 + 区域协同（核心 §10）。
9. **运维 / SRE / 国产化**：5 方言；区域平台国产协议；内外网。
10. **质量与真实性审计**：接入状态真实、断连不伪造（铁律 #2）。
11. **AI / 模型治理与可降级**：N·A —— 接入编排确定性，无模型。

## 适用不变量
- 命中核心约束：**§10 集成边界 / 不绕引擎 / 不阻断主流程** · **铁律 #2 断连不伪造** · **依赖 [INTEG-01](INTEG-01.md)/[INTEG-02](INTEG-02.md)/[OPT-01](OPT-01.md)**。
- 本卡落点：把第三方接入编排为端到端、双路、可回调、含区域协同的业务接口服务包。

## 验收 + 验证
- [ ] **AC-1（FR-1/2）**：标准方走 FHIR 门面、私有方走适配器接入，生命周期状态可见。
- [ ] **AC-2（FR-3）**：回调注册/验签/失败重放正确。
- [ ] **AC-3（FR-4/5）**：区域来源接入标机构 + 分级；未分级 → `REGIONAL_SOURCE_UNGRADED`；断连 `NOT_CONNECTED` 不阻断主流程。
- 关联 A1–A9 剧本：A1 接入、A6 合规、A9 区域协同。
- T-GATE：真实性门禁全绿。
- B0 验收：确定性接入编排，**天然 B0**。

## 完工证据
- 代码 permalink：`IntegrationOnboarding` + `RegionalSource` + 回调管理 + 双路接入 + 5 方言迁移。
- 测试：接入生命周期测试 + 双路接入测试 + 回调重放测试 + 区域来源分级测试 + 不阻断主流程测试。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
