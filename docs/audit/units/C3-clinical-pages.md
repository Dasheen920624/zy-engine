# C3 临床运行前端页 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：E6 CLINICAL-01/02/03 = done
> **审计结论：⚠️ 重灾区（临床最敏感）。8 页中 4 页假闭环，含 A7 的 2 个医疗安全 Critical。2C / 3H / 1M。**

## 页面清单与嗅探
| 页面 | disable | rand | fallback | 判定 |
|---|---|---|---|---|
| CdssFatigue.tsx | 1 | 4 | 6 | 🔴 伪造药理卡 + 署名造假（A7-CRIT-01/02）|
| PatientPathways.tsx | 1 | 4 | 7 | 🟠 写死种子 + mock fallback（A6-H-01/02）|
| EmbedLaunch.tsx | 1 | 0 | 10 | 🟠 仿真兜底（A11-H-01）|
| Followup.tsx | 1 | 4 | 4 | 🟠 rand 兜底（A9-H-01）|
| RuleValidate.tsx | 1 | 0 | 1 | 🟡 轻度 |
| Mpi.tsx | 0 | 0 | 0 | ✅ 干净（catch=1 真报错）|
| Notifications.tsx | 0 | 0 | 0 | ✅ 干净 |
| WorkflowTodos.tsx | 0 | 1 | 0 | ✅ 基本干净 |

## 十维度要点（前端侧）
- **医疗安全合规 🔴 最严重**：CdssFatigue 伪造临床药理卡展示给医师 + 反馈署名硬编码 PHYS-1002（A7-CRIT-01/02）——临床最敏感页的红线。
- **业务正确性/契约 🟠**：PatientPathways/EmbedLaunch/Followup 兜底伪造（见各后端单元报告），但多数 catch 诚实报错（优于 C2）。
- **诚实样板 ✅**：Mpi/Notifications/WorkflowTodos 干净。

## Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| Critical | C3-CRIT-01 | CdssFatigue 伪造药理卡（=A7-CRIT-01）| `CdssFatigue.tsx:172-189` |
| Critical | C3-CRIT-02 | CdssFatigue 反馈署名造假（=A7-CRIT-02）| `CdssFatigue.tsx:204` |
| High | C3-H-01 | PatientPathways 写死种子 + mock fallback（=A6-H-01/02）| `PatientPathways.tsx` |
| High | C3-H-02 | EmbedLaunch 仿真兜底（=A11-H-01）| `EmbedLaunch.tsx` |
| High | C3-H-03 | Followup rand 兜底（=A9-H-01）| `Followup.tsx` |
| Medium | C3-M-01 | 5 页 eslint-disable 待清理 | clinical/* |

合计：C2 H3 M1（与 A7/A6/A9/A11 重叠，整改合并）

## 改造
- **优先 C3-CRIT-01/02**（临床安全红线）：删伪造卡 + 署名取真实用户（见 A7 §5）。
- PatientPathways/EmbedLaunch/Followup 删兜底伪造，参照 Mpi/Notifications 诚实样板。约 2 天。

## 总评
临床运行区是**医疗安全风险最高的前端区**，CdssFatigue 两个 Critical 必须最优先修复。Mpi/Notifications/WorkflowTodos 是诚实样板。
