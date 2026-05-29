# A3 知识资产与版本引擎 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：KNOW-01/02 / API-03 = done
> **审计结论：✅ 后端达标（含宪法 §14 旧版隔离红线真实）。3 项已修复确认 + 旧版隔离不变量真实。0 Critical / 0 High / 3 Medium。**

## 1. 概览
- 后端 `engine/knowledge`（39 文件）：`KnowledgeIdentityService` / `KnowledgeVersionService` / `KnowledgeExportService`
- 测试 6 文件；迁移含 knowledge_identity / knowledge_asset_version / source_fragment 等
- 前端：知识审核走 `quality/AiReview.tsx`（嗅探干净）

## 2. 十维度要点
- **业务正确性 ✅**：来源登记/解析/片段锚点/版本登记真实。`KnowledgeIdentityService:209` 片段 `contentHash=sha256(textExcerpt)` 真实计算 + `findBySourceVersionIdAndContentHash` 唯一查重（§5.2 B1 已修复确认）。
- **医疗安全合规 ✅ 红线达标**：`KnowledgeVersionService` 旧版隔离真实——activate 原子 `目标→ACTIVE、旧 ACTIVE→SUPERSEDED` 写 supersession（99-108），withdraw `ACTIVE→WITHDRAWN`（28），**关键不变量"同 identity 同时刻 ACTIVE ≤ 1"**（33）；`findActiveByIdentity` 只取 ACTIVE。满足宪法 §1#14 / §10「旧版退出新临床决策」。
- **多租户隔离 ✅**：3 个 @DataScope 控制器；版本/身份/片段查询全租户过滤 + 悲观锁 `findByTenantIdAndIdForUpdate`（§5.3 已确认）。
- **审计证据链 ✅**：登记/激活/替换/隔离写审计 + supersession 链；hash 碰撞阻断留痕。
- **五方言 ✅**：相关 V?? 五方言齐全（SHA-256 列 64 长）。
- **代码净化 ✅**：嗅探 0 可疑。
- **错误处理 🟡 A3-M-01**：`KnowledgeIdentityService:170` contentHash 为空时兜底 `sha256(versionNo + "_" + 时间戳)`——非内容指纹，同文档两次登记得不同"内容哈希"（§5.2 残留点）。
- **可观测性 🟡**：StateTransitionRecorder 在版本状态转换调用；缺版本激活/替换计数指标。
- **测试 🟡 A3-M-02**：6 测试覆盖版本状态机、hash 查重；建议补"旧版被 CDSS/规则查询排除"的端到端用例（隔离不变量的下游验证）。
- **契约一致 ✅**：AiReview 前端嗅探干净。

## 3. 角色视角
- 合规审计：版本唯一 + supersession 链 + hash 防重，知识可信版本治理扎实 ✅

## 4. Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| Medium | A3-M-01 | contentHash 空时用 versionNo+时间戳兜底，非内容指纹 | `KnowledgeIdentityService.java:170` |
| Medium | A3-M-02 | 缺"旧版被下游查询排除"的端到端用例 | knowledge 测试 |
| Medium | A3-M-03 | 版本激活/替换缺 Micrometer 指标 | service |

合计：C0 H0 M3 L0

## 5. 改造
- A3-M-01：contentHash 必填或强制由内容计算，禁用时间戳合成（约 1h）。
- A3-M-02：补端到端用例断言 SUPERSEDED/WITHDRAWN 版本不出现在 ACTIVE 查询（约 1.5h）。

## 6. 总评
KNOW-01/02 / API-03 **名副其实**。旧版隔离红线（宪法 §14）真实达标，是知识治理的关键合规点。可进入验收，建议补 A3-M-01/M-02。
