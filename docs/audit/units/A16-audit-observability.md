# A16 审计与可观测性底座 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：BASE-04 / OBS-01 = done
> **审计结论：✅ 达标。全平台审计/状态历史/traceId 基础设施真实。0C / 0H / 2M。**

## 概览
`compliance/audit`(AuditController/AuditEvent) + `shared/audit`(AuditEventPublisher/IsolatedAuditPublisher/persistence) + `shared/observability`(StateTransitionRecorder/MdcEnrichmentFilter/TraceIdPropagator/BusinessMetrics/PayloadStoragePort/DiagnoseResponseAssembler) + `shared/trace`。

## 十维度要点
- **业务正确性 ✅**：`StateTransitionRecorder`（同事务写历史 + 兜底）、`PayloadStoragePort`（InMemory 默认 + @ConditionalOnMissingBean 三层让位）、`DiagnoseResponse` 聚合、`MdcEnrichmentFilter` + `TraceIdPropagator`（OBS-01 全套，被各引擎真实调用）。
- **审计 ✅ 关键**：`AuditEvent.of/failure` + `IsolatedAuditPublisher.publishInNewTx`（PROPAGATION_REQUIRES_NEW）——失败审计不丢的全平台机制；audit_event 含 outcome/error_code/trace_id（V9）。
- **多租户/五方言 ✅**：audit_event / state_transition_history（V8）五方言齐全。
- **代码净化 ✅**：嗅探 0。
- **可观测性 ✅**：BusinessMetrics 存在（W1-G6 helpers）；但各业务引擎对其调用不均（多数引擎缺自定义指标——已在各单元记为 Medium）。
- **测试 🟡 A16-M-01**：建议补 IsolatedAuditPublisher 在父事务回滚时审计仍存的专项用例。
- **审计发布器一致性 🟡 A16-M-02**：CDSS/EVAL/PKG 等用普通 AuditEventPublisher 而非 Isolated，失败路径审计不统一（跨单元共性，建议统一规范）。

## Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| Medium | A16-M-01 | 缺父事务回滚时 Isolated 审计仍存的专项用例 | audit 测试 |
| Medium | A16-M-02 | 多引擎未统一用 IsolatedAuditPublisher + 失败审计（跨单元共性） | 各 engine service |

合计：C0 H0 M2 L0

## 总评
BASE-04 / OBS-01 **名副其实**，是审计/可观测性基础设施，质量高。建议推动 A16-M-02 全平台审计发布器统一规范（影响 CDSS/EVAL/PKG/PATH 多单元）。
