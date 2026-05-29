# C6 高级工具前端页 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：高级工具（来源追溯/图谱/AI工作流/国产化自检/开发者控制台）
> **审计结论：⚠️ 2 页假闭环（Provenance/AiWorkflows），3 页干净。2C / 0H / 1M。**

## 页面清单与嗅探
| 页面 | disable | rand | fallback | 判定 |
|---|---|---|---|---|
| AiWorkflows.tsx | 0 | 2 | 41 | 🔴 假闭环（A12-CRIT-02，catch 伪造推理/重试/发布 + 公开承认驼峰规避门禁）|
| Provenance.tsx | 1 | 5 | 11 | 🔴 假闭环（A15-H-01，写死病案+假 SHA-256+自校验误报）|
| GraphExplore.tsx | 0 | 0 | 0 | ✅ 干净 |
| DomesticCheck.tsx | 0 | 0 | 0 | ✅ 干净 |
| DevConsole.tsx | 0 | 0 | 0 | ✅ 干净 |

## 十维度要点（前端侧）
- **医疗安全/契约 🔴**：AiWorkflows（fallback=41 全区最高）catch 伪造 B2 推理 + `:48` 公开承认"驼峰法规避 no-page-mock"；Provenance 写死证据链 + 假哈希自校验误报（见 A12/A15）。
- **图谱投影 ✅**：GraphExplore 干净（图作为投影查询，宪法 §10）。
- **代码净化 🟡**：AiWorkflows 用 camelCase 绕门禁（未 disable 仍假，C1-H-01 佐证）。

## Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| Critical | C6-CRIT-01 | AiWorkflows 假闭环 + 公开承认绕门禁（=A12-CRIT-02）| `AiWorkflows.tsx:48,324-415` |
| Critical | C6-CRIT-02 | Provenance 写死证据 + 假哈希自校验误报（=A15-H-01）| `Provenance.tsx` |
| Medium | C6-M-01 | GraphExplore 需确认图谱仅投影、权威源在关系库 | `GraphExplore.tsx` |

合计：C2 H0 M1（与 A12/A15 重叠，整改合并）

## 改造
- 随 A12/A15 整改 AiWorkflows/Provenance；GraphExplore/DomesticCheck/DevConsole 保持。约 1 天。

## 总评
高级工具区两极分化：AiWorkflows/Provenance 是假闭环典型（且 AiWorkflows 是"绕门禁"实证），其余 3 页干净。整改与 A12/A15 联动。
