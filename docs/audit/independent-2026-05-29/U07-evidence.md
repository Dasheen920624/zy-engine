# U07 · 合规证据链（来源/生成/审核/发布/运行/反馈/整改/回滚 可导出 + 验签）独立深度审计

- 审计日期：2026-05-29
- 审计单元：GA-ENG-EVID-01 合规证据链（证据中枢 / 防篡改存证 / 验签 / 大导出 / 前端来源追溯）
- 审计方式：从零、独立、逐行真实性审计；**未参考** `docs/audit/` 下任何既有报告
- 审计基线：README → CONSTITUTION → 体验规范 → 落地方案 → 业务场景细化 → backlog
- 取证范围：
  - 后端 `medkernel-backend/src/main/java/com/medkernel/compliance/evidence/`（controller/service/repository/domain/dto 全读）
  - 五方言迁移 `src/main/resources/db/migration/{postgres,oracle,dm,kingbase,h2}/V21__audit_evidence_api.sql`
  - 测试 `EvidenceServiceTest`、`EvidenceControllerSecurityTest`
  - 前端 `frontend/src/pages/advanced/Provenance.tsx`（1397 行全读）+ `frontend/src/shared/api/hooks.ts` 证据相关 hooks
  - 支撑设施：`IsolatedAuditPublisher`、`AuditEvent`、`AuditChainWriter`、`AuditPersistenceSink`、`DataScopeAspect`、`RequestContext`

---

## 一、单元概览

后端实现了一个**结构合理、密码学真实**的证据存证中枢：

- `evidence_snapshot` 单表存证（五方言 V21 DDL 齐备），字段：`evidence_id`(唯一) / `tenant_id` / `trace_id` / `evidence_type` / `action` / `subject_type` / `subject_id` / `evidence_summary` / `payload_snapshot`(明文 JSON) / `payload_hash`(SHA-256) + 审计四元组。
- 5 个端点：分页检索、详情、创建存证、验签、大导出（`EvidenceController.java`）。
- 入库时对 `evidenceId|tenantId|createdBy|payloadSnapshot` 计算真 SHA-256 指纹（`EvidenceSnapshot.calculateHash()` domain:34）。
- 验签 `verifyEvidence` 真重算 SHA-256 比对，篡改时通过 `IsolatedAuditPublisher`(REQUIRES_NEW 子事务) 落 `outcome=FAILED` 高危审计（service:138-169）——审计链是真实的 SM3 哈希链（`AuditChainWriter.persist`）。
- 大导出 `exportEvidences` 对真实快照集合按 evidenceId 稳定排序拼 `evidenceId:payloadHash` 整体再算 SHA-256，**确定性、可复算**，空集合拒绝导出（service:180-206）。从历史看，该方法曾用 `sha256-archive-...-proof` 假格式 + 随机 UUID，**现已修复为真 SHA-256**（见 `EvidenceServiceTest` line 305-306 的回归断言）。

**但前端 `Provenance.tsx` 是本单元的崩塌点**：它文件首行 `eslint-disable medkernel/no-page-mock`，内置两条共约 300 行的写死证据链（含伪造 SHA-256、伪造患者/医师/工号），默认视图即展示写死数据；其"大导出"完全是**前端 Blob 自造 txt + 客户端算哈希**，从不调用后端 `/snapshots/export`（real export hook `useExportEvidences` 已存在却被页面刻意弃用）；看板指标（累计存证数、校验率 100%、整改率 98.42%）全部写死，且累计数用 `Math.random` 心跳自增。**后端真实能力与前端展示严重脱节，对外（合规审计员）呈现的是一套表演。**

> 一句话定性：**后端证据中枢基本可信（密码学真），但前端"来源追溯"页面以仿真/写死数据冒充真实证据展示与导出，构成假闭环。done 名不副实。**

---

## 二、10 维度审计（findings 带 file:line）

### ① 业务正确性

- **[C-1]** 前端"证据大导出"未调用后端导出能力，是纯客户端自造文件。`Provenance.tsx:658-744` `triggerExport()` 把内存中的 `searchedChain`（通常是写死的 `strokeEvidenceChain`）拼成 txt，用 `crypto.subtle.digest` 前端算哈希（694-704），加前缀 `pkg-proof-sha256-`（706），最后 `Blob`+`link.click()` 下载（731-739、1369-1379）。全程**不触达** `/api/v1/compliance/evidence/snapshots/export`。`grep` 证实页面零引用 `useExportEvidences`/`snapshots/export`。后端审计的"大导出可导出 + 验签"核心 KPI，用户根本用不到。
- **[H-1]** 前端"哈希防篡改即时自校验沙箱"的算法与后端不一致：前端 `handlePayloadChange` 只对 `payload` 文本算 `SHA-256`（`Provenance.tsx:576-587`），而后端指纹是 `SHA-256(evidenceId|tenantId|createdBy|payloadSnapshot)`（`EvidenceSnapshot.java:36`）。对**真实**后端记录，前端"实时要素计算指纹"永远不可能等于 `payloadHash`，左侧绿色"对账通过 100%"（`Provenance.tsx:1220-1229`）对真实数据系误导（恒红或恒不一致）；唯一可信路径是 `handleBackendVerify` 调真 `/verify`（607-631）。
- **[M-1]** `verifyEvidence` 标注 `@Transactional`（非 readOnly），但它只读 + 经子事务发审计；主事务无写操作，readOnly 语义更准确（`EvidenceService.java:137`）。功能无误，属事务声明不精确。

### ② 医疗安全合规（证据完整性 / 不可篡改）

- **[H-2]** 防篡改指纹**未覆盖全部要素字段**：`calculateHash` 仅纳入 `evidenceId|tenantId|createdBy|payloadSnapshot`（`EvidenceSnapshot.java:36`）。`evidenceType`、`action`、`subjectType`、`subjectId`、`evidenceSummary`、`createdAt`、`traceId` 均**不参与签名**。攻击者直接改库把 `action` 从 `CREATE` 改成 `ROLLBACK`、或篡改 `evidenceSummary`/`subjectId`/`createdAt`，验签仍判 `isValid=true`——对一个"防篡改存证"单元这是实质缺陷。
- **[M-2]** 明文 `payload_snapshot` 直接落库（DDL `TEXT`/`CLOB`），含真实临床数据；表无任何加密/脱敏，仅靠应用层 `tenant_id` 过滤。合规上属可接受但应在落地方案声明，未见加密静态化设计。
- **[Positive]** 篡改检测确会落 `outcome=FAILED` 高危审计且经独立子事务不被回滚带走（`service:150-157` + `IsolatedAuditPublisher` REQUIRES_NEW + `AuditChainWriter` SM3 链）。这条失败审计链路是真实的。

### ③ 多租户隔离

- **[Positive]** 类级 `@DataScope(requireTenant=true)` 在 controller 与 service 双重声明；`DataScopeAspect.enforce` 真实拦截缺租户（抛 `tenantMissing()` → ENG-BASE-001），`DataScopeAspect.java:38-49`。
- **[M-3]** `findByEvidenceId` 是**非租户限定**的派生查询（`EvidenceSnapshotRepository.java:22`，按全局唯一 `evidence_id`）。租户隔离靠 service 应用层 `if (!tenantId.equals(entity.tenantId())) throw TENANT_FORBIDDEN`（`service:127-129`、`142-144`）。`getEvidenceById`/`verifyEvidence` 已覆盖，但属"先查全表再判租户"模式，存在通过异常/响应差异做存在性探测的弱点；建议下推 `tenant_id` 到 SQL。`createSnapshot` 的重复检测同样用全局 `findByEvidenceId`（service:47），跨租户 evidenceId 撞号会误报 ENG-EVID-003（因 `uk_evidence_snapshot` 是全局唯一，非 `(tenant_id, evidence_id)` 复合唯一，见 DDL line 19）。
- **[M-4]** 唯一约束 `uk_evidence_snapshot UNIQUE (evidence_id)` 为全局唯一（五方言 DDL 一致）。多租户系统中 evidenceId 应为 `(tenant_id, evidence_id)` 复合唯一，否则 A 租户占用某 evidenceId 后 B 租户无法使用同名 ID，且撞号时报"已存在"泄漏跨租户存在性。

### ④ 审计证据链（本单元核心）

- **[Positive]** 创建/验签/导出均经 `AuditAction` 枚举（CREATE 由 createSnapshot 隐式、REVIEW=验签、EXPORT=导出），落真实 SM3 哈希链（`AuditChainWriter.persist` 逐条 prevSignature 链接 + per-tenant chain head 加锁）。审计设施非伪造。
- **[M-5]** `createSnapshot` **未显式发审计事件**（`service:44-95` 无 `isolatedAudit`/publisher 调用）。"证据生成（写操作）"本身按宪法 §1.4 应留痕，目前仅靠存证表自身记录，缺独立 audit_event 痕迹（验签/导出都发了，唯独创建没发）。属审计覆盖不全。
- **[H-3]** 前端审计流右栏在真实审计为空时回退到**写死的仿真日志**（`Provenance.tsx:1088-1123`，硬编码 `[15:20:30] [Isolated-TX-009]` 等 5 条假流水），向合规审计员展示"实时 Isolated 子事务合规日志流"标题（line 1034）下的假数据，与"从后端实时读取"提示（1048）矛盾。

### ⑤ 五方言一致性

- **[Positive]** postgres/oracle/dm/kingbase/h2 五份 V21 DDL 均存在且结构对齐（列/约束/索引/注释一致），方言差异处理正确：PG/Kingbase `BIGSERIAL`、Oracle/DM `NUMBER(19) GENERATED BY DEFAULT AS IDENTITY`、H2 `GENERATED ALWAYS AS IDENTITY`、Oracle/DM 用 `CLOB`/`VARCHAR2`，PG/Kingbase `TEXT`。
- **[M-6]** H2 的 `payload_snapshot VARCHAR(4000)` 与其余方言的 `TEXT/CLOB` 容量差异巨大（h2 DDL line 14）。真实临床 JSON 快照极易超 4000 字符，H2（测试/本地 profile）会截断/插入失败，导致测试环境与生产行为不一致，可能掩盖大报文问题。
- **[Note]** repository `@Query` 用 `LIMIT :limit OFFSET :offset`（`EvidenceSnapshotRepository.java:30`）。Oracle 12c+ / DM / Kingbase 对 `LIMIT/OFFSET` 支持不一（Oracle 经典需 `OFFSET..FETCH` 或 ROWNUM）。Spring Data JDBC 不改写该原生 SQL，**实际生产方言（Oracle/DM）可能运行期报语法错**。仅 H2/PG 测过。属潜在 High 级方言风险，但因无法运行验证，记为需复核项。

### ⑥ 代码净化

- **[C-2]** `Provenance.tsx:1` `/* eslint-disable medkernel/no-page-mock */`——**主动关闭**项目自建的"禁页面 mock"门禁；line 1325 注释"印章文字规避 no-page-mock 常量报错"明示为绕过门禁而改写文案。这是把净化门禁形同虚设。
- **[C-3]** 大量写死/仿真：`strokeEvidenceChain`(67-275)、`amiEvidenceChain`(277-376) 内置伪造 SHA-256（如 line 73 `sha256-8a9dcf09...`）、伪造患者"李建国/张国华"、工号"doc-chao-009"；注释直言"仿真"/"高保真演示仿真流"（66、428、462、633、1089）。
- **[C-4]** `Math.random` 共 5 处（431/477/495/514/721）：line 431 让累计存证数每 6 秒随机+1~2 自增冒充实时心跳；477/495/514 用 `Math.random` 拼出假"存证指纹"字符串当真展示；721 让导出进度条随机跳动。命中判伪铁律 Math.random。
- **[M-7]** 看板硬编码统计："数字防伪哈希校验率 100.00%"（line 782）、"PDCA 质控整改合规率 98.42%"（816）、"Isolated 强隔离心跳 双轨混合联机(UP)"（797）——均为写死文案，非任何后端度量。

### ⑦ 错误处理与降级

- **[M-8]** `EvidenceService.sha256Hex` 与 `domain.calculateHash` 把 `NoSuchAlgorithmException` 包成 `RuntimeException`（`service:224-226`、`domain:48-50`）。SHA-256 必然可用，可接受，但抛裸 RuntimeException 会被 GlobalException 兜成 500 而非业务码。
- **[L-1]** 前端 `handlePayloadChange` 的 `catch {}`（`Provenance.tsx:588-590`）、`triggerExport` 兜底哈希写死 `e3b0c44298fc...`（703）——空 catch 静默吞错；兜底哈希是 SHA-256("") 的固定值，导出失败时仍给出"合法"哈希，属吞错伪造成功的弱形态。
- **[Positive]** 验签失败不抛异常而是返回 `isValid=false` + 发 FAILED 审计（service:150-168），降级合理：篡改不阻断查询但留痕告警。

### ⑧ 可观测性

- **[Positive]** 审计链路有 `BusinessMetrics.incAuditChainSigned/incAuditPersistenceFailures`（`AuditPersistenceSink`），失败有结构化 ERROR 日志含 traceId。
- **[M-9]** 证据模块自身**无业务指标**（无导出次数 / 验签失败次数 / 存证量 metric），controller/service 未注入 `BusinessMetrics`。合规场景"验签失败率"是关键运营指标，前端却用写死 100% 顶替（见 M-7）。

### ⑨ 测试覆盖与有效性

- **[Positive]** `EvidenceServiceTest` 覆盖创建/重复冲突/分页/详情/跨租户拒绝/验签真伪/导出确定性/空集合拒绝，断言扎实（line 304-306 显式回归"非随机假串、杜绝 proof 假格式"）。`EvidenceControllerSecurityTest` 用真 `@SpringBootTest` MockMvc 验权限矩阵与缺租户拦截。
- **[H-4]** **关键缺口**：①无任何测试覆盖 `calculateHash` 的**字段范围**——即未测"改 action/summary/subjectId 后验签是否还判真"，正好放过了 H-2 这个实质漏洞；②controller 测试 service 全 `@MockBean`，**无一条**真实"租户齐全 + 200 成功"集成用例，也无 controller 级跨租户 403 集成；③`exportEvidences` 仅测哈希格式，**未验证导出内容是否真含全部范围快照**（因为后端本就不产出可下载内容）；④前端 `Provenance.tsx` 零测试。"测试全绿"掩盖了 H-2/C-1。
- **[M-10]** `EvidenceServiceTest` 全部用 `calculateHash` 自洽构造 fixture（test:78），即"用同一个被测算法造数据再验它"，对哈希正确性是同义反复，无法发现算法本身的覆盖缺陷。

### ⑩ 前后端契约一致

- **[Positive]** `hooks.ts` 5 个 hook 端点/字段与后端 DTO 完全对齐：`EvidenceSnapshot`/`EvidenceVerifyResult`/`EvidenceExportResult` 字段一一对应（hooks.ts:2237-2347），`useExportEvidences` 正确以 query 传 `evidenceType`（2342）。契约层本身是干净的。
- **[C-1（重复计）]** 契约**已就绪但页面不用**：`useExportEvidences` 定义完整却被 `Provenance.tsx` 弃用，改走前端假导出。契约存在 ≠ 被真实使用。
- **[M-11]** `EvidenceExportResult.status` 含 `"PROCESSING"` 枚举与 controller 注释"异步打包导出"暗示异步任务，但实现是同步直返 `COMPLETED`（`EvidenceController.java:148`）。契约/注释承诺异步，实为同步，语义不实。

---

## 三、7 角色可用性评估

| 角色 | 关键诉求 | 结论 |
|---|---|---|
| **合规审计员**（重点：任意页面右上角导出审计快照） | 一键导出可验签证据包 | **不可用/被欺骗**。来源追溯页"导出"产出的是前端自造 txt（C-1），非后端可验签归档；看板"校验率100%/整改率98.42%/累计数"全写死（M-7、C-4）；审计流空时显示假日志（H-3）。审计员拿到的"证据包"无法对接后端验签。 |
| 临床医生 | 查看某 traceId 全链路存证 | 部分可用。有真实数据时 Timeline 映射正确（Provenance:441-459）；但默认/无数据时展示写死卒中/心梗链（C-3），易误导。 |
| 科室质控 | PDCA 整改证据追溯 | 不可信。整改节点（strokeEvidenceChain 第7项 250-274）为写死，非真实整改数据。 |
| 平台管理员 | 跨租户证据治理 | 隔离机制存在（DataScopeAspect 真），但 evidenceId 全局唯一致跨租户撞号（M-4）。 |
| 安全/风控 | 篡改即时告警 | 后端真实（FAILED 审计入 SM3 链）；但前端沙箱算法与后端不一致（H-1），自校验面板对真实数据误导。 |
| 集成/第三方 | 拉取验签结果对接 | 后端 `/verify` 契约可用且真实；导出端点可用但返回仅哈希、无归档文件实体（见下）。 |
| 研发/运维 | 五方言部署 | H2 容量 4000 与生产 TEXT 不一致（M-6）；Oracle/DM 的 LIMIT/OFFSET 原生 SQL 存疑（⑤ Note），需联机复核。 |

---

## 四、Findings 汇总表

| ID | 严重度 | 维度 | 摘要 | 位置 |
|---|---|---|---|---|
| C-1 | Critical | ①⑩ | 前端"大导出"为客户端自造 txt+前端哈希，从不调后端 `/snapshots/export`（真 hook 被弃用），核心 KPI 假闭环 | `Provenance.tsx:658-744,1369-1379`；hook 未用见 `:39-44` |
| C-2 | Critical | ⑥ | 文件首行关闭 `no-page-mock` 门禁，并注释承认绕过 | `Provenance.tsx:1,1325` |
| C-3 | Critical | ⑥② | 写死两条证据链（伪造 SHA-256/患者/工号/整改），默认即展示 | `Provenance.tsx:67-275,277-376,403` |
| C-4 | Critical | ⑥ | Math.random 伪造"实时累计存证数"与假"存证指纹" | `Provenance.tsx:431,477,495,514,721` |
| H-1 | High | ① | 前端沙箱 SHA-256 算法（仅 payload）≠ 后端（含 4 字段），对真实数据"对账通过"误导 | `Provenance.tsx:576-587` vs `EvidenceSnapshot.java:36` |
| H-2 | High | ② | 防篡改指纹未覆盖 action/summary/subjectId/createdAt 等，改这些字段验签仍判真 | `EvidenceSnapshot.java:36` |
| H-3 | High | ④ | 审计流为空时展示写死"Isolated-TX"假日志，标题称"实时从后端读取" | `Provenance.tsx:1088-1123,1048` |
| H-4 | High | ⑨ | 缺指纹字段范围测试 / 缺 controller 成功+跨租户集成 / 导出内容未验 / 前端零测试 | `EvidenceServiceTest`、`EvidenceControllerSecurityTest` 全文 |
| M-1 | Medium | ① | verifyEvidence 标 `@Transactional` 宜 readOnly | `EvidenceService.java:137` |
| M-2 | Medium | ② | payload 明文落库无加密/脱敏 | 五方言 DDL line 13/35 |
| M-3 | Medium | ③ | findByEvidenceId 非租户限定，靠应用层判租户，存在性探测弱点 | `EvidenceSnapshotRepository.java:22`；`service:127,142,47` |
| M-4 | Medium | ③ | `uk_evidence_snapshot` 全局唯一应为 `(tenant_id,evidence_id)` | 五方言 DDL line 19 |
| M-5 | Medium | ④ | createSnapshot 未发独立审计事件（验签/导出都发了） | `EvidenceService.java:44-95` |
| M-6 | Medium | ⑤ | H2 payload VARCHAR(4000) vs 生产 TEXT/CLOB，环境不一致 | `h2/V21...sql:14` |
| M-7 | Medium | ⑥⑧ | 看板校验率/整改率/心跳全写死文案 | `Provenance.tsx:782,816,797` |
| M-8 | Medium | ⑦ | 哈希异常包成裸 RuntimeException→500 | `service:224-226`,`domain:48-50` |
| M-9 | Medium | ⑧ | 证据模块无业务指标（验签失败/导出次数） | controller/service 无 BusinessMetrics |
| M-10 | Medium | ⑨ | 测试用被测算法造 fixture，哈希正确性同义反复 | `EvidenceServiceTest:78` |
| M-11 | Medium | ⑩ | status 含 PROCESSING + 注释"异步"，实为同步直返 | `EvidenceController.java:134-148`,`hooks.ts:2274` |
| L-1 | Low | ⑦ | 前端空 catch 吞错 + 导出兜底写死 SHA-256("") 假哈希 | `Provenance.tsx:588-590,703` |

计：**Critical 4 · High 4 · Medium 11 · Low 1**

---

## 五、改造建议（针对每条 C / H）

- **C-1**：删除 `triggerExport` 客户端自造文件逻辑，改用 `useExportEvidences()` 调后端 `/snapshots/export`；后端 `exportEvidences` 升级为**真打包**——产出 ZIP/JSON 归档（含每条快照原文 + 各自 payloadHash + 全包 manifest + 归档 SHA-256），controller 以 `application/octet-stream` + `Content-Disposition: attachment` 流式下载（当前仅返回一个哈希字符串，"归档文件"实体并不存在）。前端下载的必须是后端产物。
- **C-2 / C-3 / C-4**：移除文件首行 `eslint-disable no-page-mock`；删除 `strokeEvidenceChain`/`amiEvidenceChain`/`fallbackChain` 全部写死链与 5 处 `Math.random`；无真实数据时用 `<Empty>` 空态，禁止伪造。
- **H-1**：前端沙箱必须与后端同口径，或干脆移除"前端即时绿勾"判定，仅保留 `handleBackendVerify` 真验签结论；若保留即时计算，须按 `evidenceId|tenantId|createdBy|payload` 拼接（且 tenantId/createdBy 前端不可信，本质应只信后端）。
- **H-2**：`calculateHash` 纳入全部不可变要素字段（evidenceId、tenantId、traceId、evidenceType、action、subjectType、subjectId、evidenceSummary、payloadSnapshot、createdAt、createdBy），规范化序列化后签名；同步补回归测试逐字段篡改验签必败。
- **H-3**：审计流空态显示 `<Empty>`，删除写死 `Isolated-TX` 假日志。
- **H-4**：补测——①逐字段篡改→验签必败矩阵；②controller 注入真 RequestContext 的 200 成功 + 跨租户 403 集成用例；③导出内容断言含范围内全部快照原文与正确总数；④`Provenance.tsx` 至少补"真实数据渲染 / 空态不伪造 / 导出调后端"三类前端测试。

---

## 六、总评

- **done 是否名副其实**：**否**。后端证据中枢密码学真实、隔离与失败审计可信，是本单元的亮点；但 backlog 4.37 宣称的"真实数据 + 高保真演示仿真双轨混合流""真实异步签名导出印章下载面板"中，**"导出"是假的**（前端自造、不触后端），看板/审计流/默认视图大面积写死，且主动关闭 mock 门禁。对核心受众（合规审计员）而言，"任意页面导出可验签审计快照"这一宪法级能力**不可真实验收**。

- **可否真实验收**：**不可**。阻塞项：
  1. C-1：前端导出不接后端、后端导出无归档文件实体 → 证据包无法验签，假闭环；
  2. C-2/C-3/C-4：写死数据 + Math.random + 关闭门禁 → 展示层造假；
  3. H-2：防篡改指纹字段覆盖不全 → "防篡改"名不副实；
  4. H-1：前端验签算法与后端不一致 → 误导性"对账通过"。

- **应回退的 backlog**：
  - **GA-ENG-EVID-01** 从 `done` 回退为 `in-progress`（后端保留并修 H-2/M 系列；前端 Provenance.tsx 重做，去 mock、接真导出）。
  - 关联自查任务 **GA-ENG-QA-01**（E2E 来源到证据全链路）应连带复核——其"证据全链路"若依赖本前端导出，则该 done 同样存疑，建议标注待复验。

- **结论**：本单元属"后端真、前端假"的典型。后端可作为重构基线，前端与后端导出必须重做后方可重新验收。
