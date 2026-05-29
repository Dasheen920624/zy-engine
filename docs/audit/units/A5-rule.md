# A5 规则引擎 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：RULE-01 / API-05 = done
> **审计结论：✅ 达标（真实性审计已确认 DSL 真引擎，本轮补全维度）。0C / 1H / 2M。**

## 1. 概览
- 后端 `engine/rule`（33 文件）：`RuleDslEvaluator` / `RuleEngineService` / `RuleProgressor` 等
- 前端 `tenant/RuleDefinitions.tsx`(disable=1/fallback=15) + `clinical/RuleValidate.tsx`(disable=1)
- 测试 4 文件

## 2. 十维度要点
- **业务正确性 ✅**：`RuleDslEvaluator` 完整 `when/all/any/leaf` 条件树 + 10 确定性算子 + BigDecimal 数值比较 + then 动作解析与最高严重度计算（真实性审计 §4 确认真引擎）。仿真执行复用同一 evaluator 并写 `rule_execution_log`（rule 12 处"仿真"均为合法 Javadoc，非造假）。
- **医疗安全合规 ✅**：`RuleRiskLevel` HIGH/CRITICAL 触发 `requiresPhysicianConfirmation=true`，影响仿真/真实执行最高严重度；高风险规则发布门禁。
- **多租户隔离 ✅**：@DataScope；`findByRuleIdAndTenantId` 等全租户过滤。
- **审计证据链 ✅**：定义/版本/测试/仿真/发布/执行写审计 + 状态历史；执行日志记录真实/仿真。
- **五方言 ✅**：rule_definition/version/execution_log/test_case 五方言齐全。
- **代码净化 ✅**：后端嗅探仅合法"仿真"注释；无 Math.random/写死。
- **错误处理 ✅**：DSL 缺字段产生未命中而非抛异常（便于发布门禁定位），ENG-RULE 错误码。
- **可观测性 🟡**：traceId + 执行日志；缺规则执行延时/命中率指标。
- **测试 🟡 A5-M-01**：4 测试覆盖 DSL/版本；建议补高风险发布强制审核、跨租户用例。
- **契约一致 🟡 A5-H-01（前端）**：`RuleDefinitions.tsx` disable=1 + fallback=15，疑有写死规则种子/沙箱兜底（与 C2 一并整改）；`RuleValidate.tsx` 较轻（catch=2）。

## 4. Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| High | A5-H-01 | 前端 RuleDefinitions eslint-disable + fallback=15 疑写死种子/兜底 | `RuleDefinitions.tsx`（C2 详查）|
| Medium | A5-M-01 | 缺高风险发布审核 / 跨租户用例 | rule 测试 |
| Medium | A5-M-02 | 缺规则执行延时/命中率指标 | service |

合计：C0 H1 M2 L0

## 6. 总评
RULE-01 / API-05 **后端名副其实且质量高**（真 DSL 引擎，CDSS/评估均复用），是平台确定性内核。前端 RuleDefinitions 归入 C2 整改。可进验收。
