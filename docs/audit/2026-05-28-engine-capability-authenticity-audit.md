# MedKernel 引擎能力真实性代码核查报告

> 日期：2026-05-28 · 审计人：Claude · 范围：E2–E4 已标 `done` 引擎与对应前端控制台
> 触发：核查台账"全部 done / 100% / 测试全绿"声明与实际代码是否相符
> 结论摘要：台账"100% done"**不可信**——多个旗舰"done"引擎含真造假闭环;部分模块（如规则引擎）为扎实真实现。

---

## 1. 背景与目的

台账（`docs/backlog.md` v4.38）将 E1–E4 几乎全部标记 `done`，并在修订记录中反复声明"100% 补全""WOW 级""482 个 JUnit 用例全绿"。宪法（`docs/CONSTITUTION.md`）明令**禁止业务 mock 假闭环、裸 Map、单病种硬编码、业务模块直连模型/Dify**。本次核查目的：核实"done"声明是否属实，定位假闭环与夸大表述，为 E5 验收门禁提供真实靶清单。

## 2. 方法与覆盖范围

- **逐行深读**（后端）：`TerminologyService`、`KnowledgeIdentityService`、`KnowledgeVersionService`、`IntegrationService`、`ModelGatewayService`、`EvidenceService`(export)、`RuleDslEvaluator`；对应测试 `TerminologyServiceTest`、`KnowledgeEngineTest`。
- **逐行深读**（前端）：`advanced/Provenance.tsx`、`tenant/AdapterHub.tsx`、`eslint-rules/no-page-mock.js`。
- **针对性嗅探**（grep 全 `src/main/java`）：`Math.random`、裸 `Map<String,Object>`、`模拟/仿真/演示/占位`、TODO/placeholder、硬编码业务 JSON、UUID 充哈希、写死分值/恒真注释。
- **局限**：`PathwayEngineService`、`RecommendationEngineService`、`EvaluationEngineService`(903 行)、`PackageEngineService`、`FollowupEngineService`、`EmbedEngineService`、`LargeListEngineService` 仅做嗅探级核查（未逐行深读）。嗅探未发现造假标记，但**不构成 100% 真实性担保**。

## 3. 总判定

"全部 done / 100% / 测试全绿"不可信。根本问题有三：
1. **门禁失效**：防假闭环的 `no-page-mock` lint 只拦 SHOUTY_CASE 命名，camelCase 一律放行，假闭环全面回潮。
2. **"双轨仿真"成默认模式**：后端故障→前端 `catch` 伪造成功；后端无 provider→LLM 编造 B2；无导出→返回假哈希。直接违反宪法"禁止业务 mock 假闭环"。
3. **测试策略漏洞**：全 mock 单元测试，把假实现/弱算法固化为"绿"。

## 4. 确认真实可用（平衡说明）

- **RULE-01** `RuleDslEvaluator`：完整 `when`/`all`/`any`/`leaf` 条件树 + 10 个确定性算子 + `BigDecimal` 数值比较 + `then` 动作解析与最高严重度计算。真引擎。
- **#122 KNOW 版本状态机** `activate`/`withdraw`：悲观锁（`findByTenantIdAndIdForUpdate`）原子切换 ACTIVE，写 supersession 链。真。
- **#122 TERM** `confirmCandidate` / 映射包 `build`·`publish`·`rollback`：状态机与 SUPERSEDED 处理真实。真。
- **EVID `verifyEvidence`**（SHA-256 重算比对 + 失败入侵审计）、**INTEG `hmacSha256` 签名计算**：算法真实。
- PATH / CDSS / EVAL / PKG / FOLLOW / EMBED / list：嗅探无造假标记（未深读）。

## 5. 发现清单（按危害排序）

| ID | 级 | 模块 | 一句话 |
|---|---|---|---|
| B8 | 高 | EVID-01 | 证据大导出是空操作，返回假哈希串 |
| B7 | 高 | LLM-01 | "B2 推理"不调模型，B0 写死结果贴 B2 标签 + 编造引文 |
| B4 | 高 | INTEG-01 | 适配器 Ping 用随机数造 RTT，无真握手 |
| B5 | 高 | INTEG-01 | 死信重试掷骰子 70% 假成功，从不真投递 |
| B1 | 高 | KNOW-01 | "片段 SHA-256 锚点去重"无任何哈希计算 |
| F1 | 高 | 前端 EVID | Provenance 默认把写死病案+假哈希当真证据展示 |
| F2 | 高 | 前端 EVID | "防篡改沙箱"假哈希比真摘要，一打开即误报"篡改" |
| F4 | 高 | 前端 INTEG | AdapterHub 每个动作 catch 伪造成功；导出弹写死假哈希 |
| R1 | 高 | 门禁 | `no-page-mock` 只拦 SHOUTY_CASE，camelCase 全放行 |
| B2 | 中 | TERM-01 | `calculateSimilarity` 非 LCS，字符命中比，临床误配 |
| B6 | 中 | INTEG-01 | `testWebhookSignature` 返回裸 `Map<String,Object>` |
| B3 | 中 | KNOW-02 | `createDraftVersion` 哈希去重无锁无 DB 唯一约束 |
| F3 | 中 | 前端 EVID | 伪造 KPI + 标"实时后端读取"的写死审计流 |
| F5 | 中 | 前端 | `defaultLocal`/仿真兜底铺在 8+ 页面 |
| B9 | 低 | KNOW | `KnowledgeExportService` 导出计数/result_uri 占位（注释诚实） |

### 高危发现详情

**B8 · EVID-01 证据链大导出是空操作**
`compliance/evidence/service/EvidenceService.java:172` `exportEvidences()` 仅 `archiveZipHash = "sha256-archive-" + UUID + "-proof"`（非任何数据的真实摘要），写一条"导出成功"审计后返回。**无打包、无归档、无文件**。前端 `Provenance.tsx` 的"下载加密防伪证据包"（`useExportEvidences` → 本方法）随后显示"防伪盖章成功"并下载一个仅含该假哈希的 `.txt`。端到端纯假闭环，却对外宣称"可信证据导出"。

**B7 · LLM-01 模型网关编造推理结果**
`engine/llm/ModelGatewayService.java:143` 非 BASEPLAY 分支（即所谓 B1/B2 模型辅助）**不调用任何模型**：硬编码 `modelMode="B2"`、`modelVersion="MedKernel-Cognitive-LLM-v2"`、`confidence=0.92`、`sourceCitations="临床高血压指南 §3.2"`，真实 `outputContent = executeB0Fallback()`（行 159）。`executeB0Fallback`（行 405）对任意输入都返回写死的"高血压"JSON。审计因此记录"B2 推理 @0.92 + 引文"全为假。另：`FORCE_TIMEOUT`/`FORCE_FAIL_SCHEMA` 魔法字符串测试钩子混入生产代码；`validateSchema` 仅字符串 `contains`；`desensitize` 把手机号替换成固定 `138****8888`（不保留真实前后缀）。医疗 AI 编造引文/置信有患者安全风险。

**B4/B5 · INTEG-01 集成总线掷骰子**
`engine/integration/service/IntegrationService.java:124` `pingAdapter` 用 `2 + Math.random()*13` 造握手 RTT，并写死 `missingRate:0.02,termMappingRate:0.97` 质量报告，**无真实连接**。`:286` `retryMessage` `Math.random() > 0.3` 掷骰子 70% 标 SUCCESS，**从不真投递**。适配器健康、互联互通自检、死信重试全部为假。

**B1 · KNOW-01 片段 SHA-256 锚点去重不存在**
`engine/knowledge/KnowledgeIdentityService.java:194` `createFragment` 的 Javadoc 称"计算 textExcerpt 的 SHA-256 摘要作为锚点保护"，但方法体**无任何哈希计算**；去重靠 `textExcerpt.equals()` 明文比对 +（sourceVersionId, anchorPath）自然键。`SourceFragment` 实体**无 hash 字段**。另：`:170` `registerSourceVersion` 当 contentHash 为空时兜底 `sha256(versionNo + "_" + 时间戳)`——非内容指纹，同文档两次登记得到不同"内容哈希"。

**F1/F2/F4 · 前端系统性假闭环**
- `advanced/Provenance.tsx:67`/`:404`：`strokeEvidenceChain`/`amiEvidenceChain` 写死卒中/心梗病案（含编造患者、体征、**手敲假 SHA-256**），默认 `searchTraceId="tr-stk-proof-009"` 进页面即把假证据当真展示，配文"已通过国家数字防伪审计校对"。
- `:587`：自校验沙箱拿写死假 hash 与实时算出的真 SHA-256 比对——永不相等，打开演示节点立即误报"🚨 检测到物理数据篡改"。WOW 特性是坏的。
- `tenant/AdapterHub.tsx:483`：`handleExportLogsCertificate` 不调后端，直接弹写死假哈希 `sha256-4c74026f...` 并称"可用于互联互通测评与评级审计"；各动作 `catch` 块（ping/签名/重试/切换）一律伪造成功（`mockRtt`、`mockSign`、"[仿真模式]成功"）。

**R1 · 防假闭环门禁失效（系统性根因）**
`frontend/eslint-rules/no-page-mock.js:20` 规则仅在 `VariableDeclarator` 命名匹配 `^[A-Z][A-Z0-9_]*$`（SHOUTY_CASE）且初始化为对象数组时报错。`defaultLocalAdapters`、`strokeEvidenceChain` 等 camelCase 命名**直接绕过**；代码注释明写"规范命名规避 no-page-mock 常量报错"。故 BASE-09"锁假闭环不回潮"实际失效。`仿真模式/兜底/defaultLocal/fallback` 至少出现在 8 个页面（CdssFatigue、PatientPathways、ConfigPackages、AiWorkflows、EmbedLaunch、AdapterHub、Provenance 等）。

## 6. 整改建议（分批，按宪法与多 PR 风格）

**PR-A 诚实化（先行，本报告 + 台账回退已含）**
- 据实订正 `docs/backlog.md`：将 KNOW-01/KNOW-02/TERM-01/LLM-01/EVID-01/INTEG-01 从 `done` 回退 `in_progress`。
- E5 验收门禁（QA-07 代码净化、QA-04 B0 验收、QA-08 第三方对接）显式把上述假闭环列为必打靶。

**PR-B 修门禁（防回潮，优先）**
- 重写 `no-page-mock`：不再以命名大小写为判据，改为检测"页面内大体量对象数组字面量""`catch` 块内 `message.success`/伪造成功""`api?.length>0 ? api : 写死` 兜底"等模式；camelCase 同样拦截。
- 加门禁后预期会爆出 8+ 页面的存量假闭环，逐页改为六态占位 + 真接口。

**PR-C 修最危险的后端假闭环**
- B8：`exportEvidences` 接真实打包（GA-ENG-PKG-01 通道）或在无导出能力时明确返回"未就绪"，禁止返回假哈希。
- B7：LLM 网关无 provider 时必须诚实标 B0，禁止伪造 B2 元数据与引文；魔法字符串测试钩子移出生产。
- B5/B4：死信重试与适配器 Ping 接真实投递/连接，或明确标注为"未接入"状态，不得用 `Math.random` 伪造结果。
- B1：补 `SourceFragment.contentHash` 列 + 五方言迁移，`createFragment` 真算 SHA-256；`registerSourceVersion` 禁止用时间戳合成"内容哈希"。

**PR-D 修数据质量与契约**
- B2：替换字符命中比为真编辑距离/真 LCS，或抬高阈值并补医学语义约束，杜绝"肌钙蛋白T→血红蛋白"类误配。
- B6：`testWebhookSignature` 改用 Record DTO，消除裸 Map。
- B3：`createDraftVersion` 加悲观锁或 DB `content_hash` 唯一约束。

## 7. 与 backlog 任务映射 / 状态回退

| backlog 任务 | 原状态 | 回退后 | 依据 |
|---|---|---|---|
| GA-ENG-KNOW-01 | done | in_progress | B1、B9 |
| GA-ENG-KNOW-02 | done | in_progress | B3 |
| GA-ENG-TERM-01 | done | in_progress | B2 |
| GA-ENG-LLM-01 | done | in_progress | B7 |
| GA-ENG-EVID-01 | done | in_progress | B8、F1–F3 |
| GA-ENG-INTEG-01 | done | in_progress | B4、B5、B6、F4 |

> 说明：上述任务**含真实部分**（见 §4），回退为 `in_progress` 表示"已开工但未真正完成"，并非全盘推倒。BASE-09（门禁/净化）因 R1 门禁失效与前端假闭环回潮，建议在 PR-B 一并修复（未单独回退，留待 QA-07 验收把关）。

## 8. 修订记录

| 版本 | 日期 | 审计人 | 主要内容 |
|---|---|---|---|
| 1.0 | 2026-05-28 | Claude | 首次核查：E2–E4 引擎真实性审计，15 项发现（9 后端 + 5 前端 + 1 门禁），6 个引擎任务回退 in_progress |

---

**End of audit report.**
