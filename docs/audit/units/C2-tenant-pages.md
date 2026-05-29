# C2 试点准备前端页 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：E6 PILOT-01/02/03 + SVC-INTEGRATION-01 = done
> **审计结论：⚠️ 重灾区。8 页中 4 页系统性假闭环。2C / 2H / 1M。**

## 页面清单与嗅探
| 页面 | disable | rand | fallback | 判定 |
|---|---|---|---|---|
| AdapterHub.tsx | 1 | 3 | 24 | 🔴 假闭环（详见 A14-CRIT-01）|
| ConfigPackages.tsx | 1 | 5 | 31 | 🔴 假闭环（详见 A10-CRIT-01）|
| RuleDefinitions.tsx | 1 | 0 | 15 | 🟠 疑写死规则种子/兜底 |
| PathwayTemplates.tsx | 1 | 0 | 15 | 🟡 沙箱仿真显式标注 + catch 诚实（详见 A6）|
| TerminologyMapping.tsx | 1 | 0 | 0 | ✅ 基本干净（BASE-08 真接入样板）|
| ImplementationGuide.tsx | 0 | 0 | 0 | ✅ 干净 |
| TenantOnboarding.tsx | 0 | 0 | 0 | ✅ 干净（catch=2 真报错）|

## 十维度要点（前端侧）
- **业务正确性/契约 🔴**：AdapterHub、ConfigPackages 与后端真实接口并存但 catch 伪造成功 + 写死数据集（见 A14/A10 报告）；RuleDefinitions fallback=15 疑写死规则种子。
- **代码净化 🔴**：4 页 eslint-disable（C1-H-01 根因）。
- **诚实样板 ✅**：TenantOnboarding/ImplementationGuide/TerminologyMapping 是该区"真接入"参照。

## Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| Critical | C2-CRIT-01 | AdapterHub 假闭环（=A14-CRIT-01）| `AdapterHub.tsx` |
| Critical | C2-CRIT-02 | ConfigPackages 假闭环（=A10-CRIT-01）| `ConfigPackages.tsx` |
| High | C2-H-01 | RuleDefinitions eslint-disable + fallback=15 疑写死种子 | `RuleDefinitions.tsx` |
| High | C2-H-02 | PathwayTemplates 沙箱兜底需确认仿真显式标注边界 | `PathwayTemplates.tsx` |
| Medium | C2-M-01 | 4 页 eslint-disable 待清理 | tenant/* |

合计：C2 H2 M1 L0（注：CRIT 与 A14/A10 重叠，整改合并）

## 改造
- 随 A14/A10 整改 AdapterHub/ConfigPackages；RuleDefinitions 删写死规则种子改后端 query；PathwayTemplates 确保"沙箱仿真"与真实数据明确分区。参照 TenantOnboarding 诚实样板。约 1.5 天。

## 总评
试点准备区是假闭环重灾区（适配器/配置包/规则三页），但已有 3 个诚实样板可参照。整改与 A14/A10/A5 联动。
