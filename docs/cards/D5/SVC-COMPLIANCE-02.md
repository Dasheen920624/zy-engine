# SVC-COMPLIANCE-02 · 审计运维服务包

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D5 域简报](_brief.md)。
> 迁移来源（覆盖矩阵锚点）：详规 S14 用户权限与合规 · 详规 审计/Provider/备份 · 核心 §运维。

## 身份
- 卡 ID：SVC-COMPLIANCE-02（服务包卡；审计查询/Provider 状态单一归属）
- 域：D5 合规运维
- 关联场景：S14 用户、权限与合规
- 依赖卡：[BASE-04](../D0/BASE-04.md) 审计骨干 · [EVID-01](EVID-01.md) 证据 · [CONFIG-01](../D0/CONFIG-01.md) 配置 · 页 [AUDITLOG-01](AUDITLOG-01.md)/[PROVIDER-01](PROVIDER-01.md)
- 工作量：4d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把**审计 + 证据包 + Provider/模型状态 + 备份恢复 + 离线许可**编排为审计运维服务包：审计可查可导（带证据）、Provider/模型**无连接诚实显示 `NOT_CONNECTED`**、备份恢复与离线许可真实可演示，**绝不伪造连接/状态**。

## 现状（搬迁时核查 2026-05-30，以 `medkernel-backend` 为准）
部分已有：`com/medkernel/compliance/audit/` 有 `AuditController` + `AuditEvent`（建在 [BASE-04](../D0/BASE-04.md) 骨干）；证据[EVID-01](EVID-01.md) 已成型。本卡＝补审计查询/导出（带证据）+ **Provider/模型状态 + 备份恢复 + 离线许可**（前端 `SystemProviders.tsx` 仅壳，后端待建为主）。

## 功能要求（原子可测条目）
- [ ] FR-1 审计查询：按条件查审计事件（`AuditEvent`，[API-13](../D0/API-13.md) 分页），真实。
- [ ] FR-2 审计导出：导出审计 + 证据（[EVID-01](EVID-01.md) 签名），导出审批（[SYS-06](SYS-06.md)）。
- [ ] FR-3 Provider 状态：Provider/模型连接状态真实，无连接标 `NOT_CONNECTED`、不伪造。
- [ ] FR-4 备份恢复：备份/恢复任务真实可演示，状态可查。
- [ ] FR-5 离线许可：国产化离线许可校验真实，过期/无效诚实拒绝。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
- 端点：`GET /api/v1/compliance/audit-events` · `POST .../audit:export` · `GET .../providers/status` · `POST .../backups` · `GET .../license/status`
- DTO：审计/Provider 状态/备份/许可 Record；信封 `ApiResult`/`ProblemDetail`
- 状态机：备份（变更类：进行中→成功/失败）；Provider（告警类：连接/断连）
- 幂等 / traceId：备份/导出幂等；trace（[OBS-01](../D0/OBS-01.md)）

## 数据与迁移
- 复用 `audit_event` 表（[BASE-04](../D0/BASE-04.md)）+ Provider 状态/备份记录/许可表；五方言（[BASE-05](../D0/BASE-05.md)）

## 视角清单（11 视角逐条）
1. 产品架构：审计与运维的服务编排枢纽。
2. 产品体验：审计可查、Provider 状态直观（页 [AUDITLOG-01](AUDITLOG-01.md)/[PROVIDER-01](PROVIDER-01.md)）。
3. 系统与数据架构：审计大表分页/检索；P95 ≤1s；备份异步。
4. 临床医疗安全：N·A（运维）。
5. 知识与数据治理：审计/证据可追溯。
6. 安全合规与监管：★审计不可篡改、导出带证据签名（[EVID-01](EVID-01.md)）。
7. 集团化与多租户治理：审计按租户隔离查询。
8. 集成与互操作：★Provider/模型状态真实，无连接 `NOT_CONNECTED`。
9. 运维 / SRE / 国产化：★备份恢复 + 离线许可国产化真实可跑。
10. 质量与真实性审计：★绝不伪造连接/状态/备份成功；无连接诚实标记。
11. AI / 模型治理与可降级：模型 Provider 不可用诚实 `NOT_CONNECTED`/`MODEL_DISABLED`。

## 适用不变量
- 命中核心约束：**铁律 #1/#2 真实性（不伪造连接）** · **核心 §6 审计** · **§运维/国产化** · **§9 隔离**。
- 本卡落点：审计查询/导出 + Provider 状态 + 备份/许可编排，无连接诚实降级。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：审计可查；导出带证据签名 + 审批。
- [ ] AC-2（FR-3）：Provider 无连接诚实标 `NOT_CONNECTED`、不伪造。
- [ ] AC-3（FR-4/5）：备份恢复/离线许可真实可演示。
- 关联 A1–A9 剧本：A9 审计运维。
- T-GATE：后端真实性门禁全绿（无伪造连接/备份）。
- B0 验收：审计/证据确定性；Provider 不可用诚实降级。

## 完工证据
- 代码 permalink：审计查询/导出 + Provider 状态 + 备份/许可。
- 测试：审计查/导/证据 + Provider NOT_CONNECTED + 备份 + 许可。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
