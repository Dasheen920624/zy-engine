# A4 字典映射引擎 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：TERM-01 / API-04 = done
> **审计结论：✅ 达标（上次 B2 字符命中比已修为真 LCS）。0C / 0H / 2M。**

## 概览
`engine/terminology`(21)：TerminologyService（未映射发现/候选推荐/确认/映射包发布）；测试 3。

## 十维度要点
- **业务正确性 ✅**：`TerminologyService:383` 扫描未映射本地词条，`:412` `calculateSimilarity` 推荐候选；`:451-472` **真 LCS 动态规划**（dp[m][n] + 经典公式 `2*LCS/(len1+len2)`）——**§5.4 / 真实性审计 B2「字符命中比非 LCS」已修复确认**。confirmCandidate 状态机 + 映射包 build/publish/rollback（§5.3 真）。
- **医疗安全合规 🟡 A4-M-01**：LCS 对中文医学术语仍有语义局限（"肌钙蛋白T"vs"血红蛋白"字面 LCS 可能偏高），建议候选默认 Pending 人工确认（已是）+ 抬阈值 + 医学语义约束，避免误推。AI 候选应标识。
- **多租户隔离 ✅**：@DataScope + 租户过滤；Pending 候选原地幂等更新避免唯一键碰撞（§5.3）。
- **审计/五方言/净化 ✅**：映射发布留痕；terminology 表五方言齐全；嗅探 0。
- **测试 🟡 A4-M-02**：3 测试；建议补 LCS 边界（高相似误配防护）、跨租户用例。
- **契约一致 ✅**：`tenant/TerminologyMapping.tsx` 仅 disable=1，无 fallback/rand（BASE-08 真接入样板页，基本干净）。

## Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| Medium | A4-M-01 | LCS 对中文医学术语语义局限，需阈值+语义约束防误推 | `TerminologyService.java:451` |
| Medium | A4-M-02 | 缺高相似误配防护 / 跨租户用例 | terminology 测试 |

合计：C0 H0 M2 L0

## 总评
TERM-01 / API-04 **名副其实**（相似度已从假算法修为真 LCS）。可进验收，建议 A4-M-01 医学语义加固。
