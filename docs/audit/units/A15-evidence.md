# A15 证据链引擎 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：EVID-01 = done
> **审计结论：后端 ✅ 达标（导出已修 + 验签真实）；前端 Provenance ⚠️ 仍有假闭环。0C / 1H / 2M。**

## 1. 概览
- 后端 `compliance/evidence`（7 文件）：`EvidenceService`(导出/验签) / Controller / EvidenceSnapshot
- 前端 `advanced/Provenance.tsx`(嗅探 disable=1 / rand=5 / fallback=11)
- 测试 2 文件

## 2. 十维度要点
- **业务正确性 ✅**：`EvidenceService:175-191` 导出 = 稳定排序 + `evidenceId:payloadHash` 拼接 + 整体 SHA-256 归档指纹（**§5.2 B8 假哈希已修复确认**，不再是 UUID 充哈希）。
- **医疗安全合规 ✅**：`verifyEvidence:138` 双向验签——重算 SHA-256 比对，篡改时 `:149` 经隔离子事务发 `outcome=FAILED` 高危入侵审计（§5.3 确认真实）。
- **多租户隔离 ✅**：2 @DataScope 控制器；`:127/:142` 显式跨租户校验。
- **审计证据链 ✅**：入库/验签/导出留痕；篡改 FAILED 审计走 isolated。
- **五方言 ✅**：V21 五方言齐全。
- **代码净化 ✅ 后端**：嗅探 0；前端见 2.10。
- **错误处理 ✅**：ENG-EVID 错误码 + ProblemDetail。
- **可观测性 🟡**：traceId 贯穿；缺验签失败率指标。
- **测试 🟡 A15-M-01**：2 测试偏少，建议补篡改检测、跨租户、导出归档指纹稳定性用例。
- **契约一致 🔴 A15-H-01（前端）**：`Provenance.tsx` 仍是真实性审计 F1/F2 同款——写死病案/手敲假 SHA-256 当真证据展示、自校验沙箱用写死 hash 比实时摘要（打开即误报"篡改"）。disable=1/rand=5/fallback=11 佐证未清。

## 3. 角色视角
- 合规审计：后端导出/验签可信 ✅；但前端"来源追溯"控制台展示伪造证据，会污染对外合规演示。

## 4. Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| High | A15-H-01 | 前端 Provenance 写死病案+假 SHA-256 当真证据，自校验沙箱误报 | `Provenance.tsx`（参 真实性审计 F1/F2）|
| Medium | A15-M-01 | 后端验签/导出测试偏少（仅 2） | evidence 测试 |
| Medium | A15-M-02 | 缺验签失败率指标 | EvidenceService |

合计：C0 H1 M2 L0

## 5. 改造
- A15-H-01：删 Provenance 写死证据链与假 hash；展示仅来自后端真实 evidence；自校验沙箱用后端 payloadHash 与前端 Web Crypto 实算比对（同源）。约 3h。
- A15-M-01：补篡改检测/跨租户/导出指纹稳定性用例。约 2h。

## 6. 总评
EVID-01 **后端名副其实**（导出 + 验签真实，较真实性审计已实质修复）；**前端 Provenance 仍需返工**（H-01）。后端可进验收，前端阻塞。
