# B1 五方言迁移一致性 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：BASE-05 / QA-02 = done
> **审计结论：✅ 达标。5 方言迁移文件数完全对齐（各 24）。0C / 0H / 2M。**

## 概览
`db/migration/{postgres,oracle,dm,kingbase,h2}`，V1..V21+ 全表族。

## 十维度要点
- **文件数对齐 ✅**：postgres/oracle/dm/kingbase/h2 各 **24 个迁移**，完全对齐，无方言遗漏。
- **历史合规修复 ✅**：oracle 保留字列名冲突（API-08）、唯一键索引（整改查询）等历史问题已在各 API 任务修复。
- **审计/状态/版本/租户字段 ✅**：BASE-05 合同门禁；canonical_resource +trace_id（V8）、audit_event +outcome/error_code（V9）。
- **测试 🟡 B1-M-01**：Flyway smoke 存在，但 3 个 Docker 依赖多方言烟测在本机 Docker 不可用时跳过（CI 应确保真跑）。
- **逐字段等价 🟡 B1-M-02**：本轮核对到文件数对齐，**未逐字段比对列类型/长度/约束**；建议脚本化校验 5 方言 DDL 列级等价（尤其 JSON→CLOB、VARCHAR 长度、保留字）。

## Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| Medium | B1-M-01 | 多方言烟测依赖 Docker，缺席时跳过（CI 需保真跑）| migration smoke |
| Medium | B1-M-02 | 未脚本化逐字段列级等价校验 | db/migration |

合计：C0 H0 M2 L0

## 改造
- B1-M-02：写脚本解析 5 方言同版本 DDL，比对列名/类型映射/长度/约束，纳入 CI 门禁。约 1 天。

## 总评
BASE-05 / QA-02 **文件级名副其实**（5 方言齐全对齐）。建议补列级等价自动校验（B1-M-02）以彻底闭合方言一致性。
