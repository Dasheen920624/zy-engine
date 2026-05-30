# EVID-01 · 证据链

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D5 域简报](_brief.md)。
> 迁移来源（覆盖矩阵锚点）：详规 S14 用户权限与合规 · 核心 §6 安全合规 · 详规 证据链与签名。

## 身份
- 卡 ID：EVID-01（引擎卡；`EvidenceSnapshot`/证据链单一归属）
- 域：D5 合规运维
- 关联场景：S14 用户、权限与合规
- 依赖卡：[BASE-04](../D0/BASE-04.md) 审计 · [SYS-06](SYS-06.md) 证据框架 · [SVC-COMPLIANCE-02](SVC-COMPLIANCE-02.md) 导出 · [BASE-05](../D0/BASE-05.md) 方言
- 工作量：4d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把证据链做成 **B0 真实**：对关键合规事件生成**真实文件证据 + 国密签名 + 可验签**，大导出返回**真实文件 URI**，证据不可篡改、可追溯，**绝不伪造签名哈希/假文件**。

## 现状（搬迁时核查 2026-05-30，以 `medkernel-backend` 为准）
已有实质基础：`com/medkernel/compliance/evidence/` 下 `EvidenceController` + `EvidenceService` + `EvidenceSnapshot(+Repository)` + `EvidenceCreateDto` + `EvidenceResponse` + `EvidenceVerifyResult`。本卡＝把"真实文件 + 国密签名 + 验签 + 大导出真实 URI"框架化/补全，非从零。

## 功能要求（原子可测条目）
- [ ] FR-1 证据生成：对合规事件生成证据快照（`EvidenceSnapshot`），含真实文件内容。
- [ ] FR-2 国密签名：证据用国密算法签名，签名与摘要落库、可独立验证。
- [ ] FR-3 验签：`EvidenceVerifyResult` 验签真实，篡改即失败、不放过。
- [ ] FR-4 大导出：大证据包导出返回**真实文件 URI**（对象存储），不伪造下载。
- [ ] FR-5 不可篡改：证据一经签名不可改，改动留新版本 + 审计。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
- 端点：`POST /api/v1/compliance/evidence`（生成）· `GET .../evidence/{id}`（取）· `POST .../evidence/{id}:verify`（验签）· `POST .../evidence:export`（大导出真实 URI）
- DTO：`EvidenceCreateDto` → `EvidenceResponse` / `EvidenceVerifyResult`；信封 `ApiResult`/`ProblemDetail`
- 状态机：配置类（生成→已签名→已归档）；trace（[OBS-01](../D0/OBS-01.md)）
- 幂等 / 错误码：证据幂等键；验签失败明确错误码

## 数据与迁移
- 表族：`evidence_snapshot`（摘要 + 签名 + 文件 URI + 组织字段 + 版本 + 审计）；文件落对象存储；五方言（[BASE-05](../D0/BASE-05.md)）
- 唯一约束：证据 ID + 签名唯一；索引：来源事件/时间

## 视角清单（11 视角逐条）
1. 产品架构：合规证据的"出厂 + 验真"底座。
2. 产品体验：N·A（页面在 [AUDITLOG-01](AUDITLOG-01.md)）。
3. 系统与数据架构：大文件异步导出真实 URI；验签 O(1)；P95 生成 ≤2s。
4. 临床医疗安全：N·A（合规证据，不触临床）。
5. 知识与数据治理：证据快照绑定来源版本可追溯。
6. 安全合规与监管：★国密签名 + 验签 + 不可篡改 + 真实文件，满足等保/监管。
7. 集团化与多租户治理：证据按租户隔离，跨租户不可验/取。
8. 集成与互操作：导出标准格式供外部验签。
9. 运维 / SRE / 国产化：★国密算法国产化；对象存储离线可用。
10. 质量与真实性审计：★绝不伪造签名哈希/假文件；篡改验签必败。
11. AI / 模型治理与可降级：N·A（确定性证据）。

## 适用不变量
- 命中核心约束：**核心 §6 安全合规** · **铁律 #1/#2 真实性（不伪造签名/文件）** · **§9 多租户隔离** · **§国产化**。
- 本卡落点：真实文件 + 国密签名 + 验签 + 真实导出 URI 的证据链。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：证据含真实文件、国密签名落库。
- [ ] AC-2（FR-3/5）：验签真实、篡改必败、不可改。
- [ ] AC-3（FR-4）：大导出返回真实文件 URI（非伪造）。
- 关联 A1–A9 剧本：A9 证据导出。
- T-GATE：后端真实性门禁全绿（无伪造签名/文件）。
- B0 验收：证据/签名纯确定性，关模型可用。

## 完工证据
- 代码 permalink：`compliance/evidence` 国密签名 + 验签 + 真实导出。
- 测试：签名/验签/篡改必败/真实 URI 导出 + 安全测试。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
