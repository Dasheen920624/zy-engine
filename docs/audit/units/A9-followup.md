# A9 随访引擎 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：FOLLOW-01 / API-09 = done
> **审计结论：✅ 后端达标 / 前端轻度（Followup.tsx 有 rand+fallback）。0C / 1H / 1M。**

## 概览
`engine/followup`(19)：FollowupEngineService（计划生成/任务/问卷/异常回院/结果回流）；测试 3；前端 `clinical/Followup.tsx`(disable=1/rand=4/fallback=4)。

## 十维度要点
- **业务正确性 ✅ 后端**：`generatePlan:50` 真实计划生成；任务分发、问卷回传、异常回院上报闭环；嗅探后端 0 可疑。
- **医疗安全合规 ✅**：异常回院上报 + traceId 审计追溯；不自动写医嘱。
- **多租户隔离 ✅**：@DataScope；`followup.read/write` 权限；ENG-FOLLOW-001..005 错误码。
- **审计/五方言/净化 ✅**：V16（followup_plan/task/questionnaire/event）五方言齐全；后端嗅探 0。
- **测试 🟡**：3 测试；建议补跨租户、异常回院用例。
- **契约一致 🟡 A9-H-01（前端）**：`Followup.tsx` disable=1/rand=4/fallback=4，疑有 Math.random 兜底 ID + 仿真兜底（与 C3 一并整改，严重度参照 A6/A7 之间）。

## Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| High | A9-H-01 | 前端 Followup eslint-disable + rand=4 + fallback，疑兜底伪造 | `Followup.tsx`（C3 详查）|
| Medium | A9-M-01 | 缺跨租户 / 异常回院用例 + 指标 | followup |

合计：C0 H1 M1 L0

## 总评
FOLLOW-01 / API-09 **后端名副其实**；前端 Followup 归入 C3 整改。后端可进验收。
