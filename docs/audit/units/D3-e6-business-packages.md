# D3 E6 业务服务包真实性 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：GA-SVC-* ×14 = done（v4.42-4.47，约 1-2 天全标 done）
> **审计结论：⚠️ E6 主要是前端页包装，与已审前端单元高度重叠；其"完成"继承了前端假闭环问题。1C / 2H / 1M。**

## 背景
E6 14 个业务包在引擎验收后启动，1-2 天内全标 done。E6 多为"把引擎能力包装成业务页面"，因此其真实性 ≈ 对应前端页 + 复用的后端引擎。

## E6 包 → 审计单元映射
| E6 包 | 主要落点 | 继承结论 |
|---|---|---|
| SVC-PILOT-01 租户组织 | TenantOnboarding/ImplementationGuide + org 后端 | ✅ 前端干净 + A1 真 |
| SVC-PILOT-02 接入数据质量 | AdapterHub + A14 | 🔴 A14 半修复 + 前端假闭环 |
| SVC-PILOT-03 资产准备 | ConfigPackages/RuleDefinitions/PathwayTemplates | 🔴 C2 假闭环 |
| SVC-CLINICAL-01 患者路径 | PatientPathways/Mpi + A6 | 🟠 A6 后端优秀/前端中度 |
| SVC-CLINICAL-02 CDSS 提醒 | CdssFatigue + A7 | 🔴 A7 前端 2 Critical |
| SVC-CLINICAL-03 临床协同 | WorkflowTodos/Notifications/Followup | 🟡 多数干净，Followup 兜底 |
| SVC-QUALITY-01/02/03 质控 | QcDashboard/QcAlerts/QcEvalSets/InsuranceAudit + A8 | ✅ A8 真 + C4 基本干净 |
| SVC-COMPLIANCE-01/02 合规 | AdminUsers/AdminAudit/SecurityBaseline + A1/A16 | ✅ C5 最干净 + 后端真 |
| SVC-INTEGRATION-01 第三方 | AdapterHub + A14 | 🔴 同 A14 |
| SVC-DOMAIN-01/02 专病/协同 | 复用路径/规则/知识引擎 | 🟡 依赖 A5/A6/A3（后端真），前端待逐页核 |

## Findings
| Sev | ID | 一句话 | 依据 |
|---|---|---|---|
| Critical | D3-CRIT-01 | SVC-CLINICAL-02/PILOT-02/03 继承 A7/A14/C2 前端假闭环 | C2/C3/A7/A14 |
| High | D3-H-01 | E6 1-2 天全标 done，质量继承前端假闭环，验收不充分 | backlog v4.42-4.47 |
| High | D3-H-02 | SVC-DOMAIN-01/02 专病/协同前端页未独立深核（复用引擎，前端待查）| - |
| Medium | D3-M-01 | E6 包无独立验收证据，"done"承自 E5（D1 已存疑）| backlog |

合计：C1 H2 M1 L0

## 改造
- E6 整改**完全依附**前端单元整改（C2/C3/C6）+ A14：前端假闭环修完，E6 对应包才真达标。
- 补 SVC-DOMAIN-01/02 专病/协同页的独立核查（本轮未逐页深读）。

## 总评
E6 业务包是"前端页 + 引擎复用"的包装层，**其真实性等于对应前端单元**：合规/质控区（C4/C5）对应的 E6 包基本达标；试点/临床/集成区（C2/C3/A14）对应的 E6 包继承假闭环需返工。E6"1-2 天全 done"是典型的"先标 done"，建议与 E5 一并按整改进度重新验收。
