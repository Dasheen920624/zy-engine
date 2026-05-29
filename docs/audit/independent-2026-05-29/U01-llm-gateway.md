# U01 · LLM 模型能力网关 — 独立深度真实性审计

> 审计日期：2026-05-29 · 审计员视角：资深医疗系统审计专家（架构 + 临床）
> 取证方式：从零、逐行、只读源码；不采信任何既有审计结论
> 对照权威：README → docs/CONSTITUTION.md（第 18 条）→ MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md §1.6 / §7.12
> backlog 自报：GA-ENG-API-12 / GA-ENG-LLM-01 / GA-ENG-LLM-02 / GA-ENG-DEGRADE-01 均标 `done`

---

## 1. 单元概览

### 1.1 代码路径与关键类

| 层 | 文件 | 关键行 | 角色 |
|---|---|---|---|
| Controller | `medkernel-backend/.../engine/llm/ModelGatewayController.java` | L24 `@RequestMapping("/api/v1/model-capabilities")`、L25 `@DataScope(requireTenant=true)`、L40/52/64/76/88 `@PreAuthorize` | 5 端点：status / tasks(POST) / tasks/{id}(GET) / tasks/{id}/retry / policies/validate |
| Service | `medkernel-backend/.../engine/llm/ModelGatewayService.java` | L45-49 能力码常量、L102 submitTask、L132-142 路由恒 B0、**L460-468 executeB0Fallback 写死病种**、L325-348 desensitize、L372-387 validateSchema | 网关核心逻辑 |
| Entity | `ModelCapabilityTask.java`(L13)、`ModelCapabilityPolicy.java`(L13、L18 注释) | — | 两实体均含 tenant_id |
| Repository | `ModelCapabilityTaskRepository.java`(L19)、`ModelCapabilityPolicyRepository.java`(L20) | — | 仅查询；**policyRepo 无 save** |
| DTO | `ModelTaskRequest/Response`、`ModelCapabilityStatusResponse`、`ModelPolicyValidate{Request,Response}` | — | Response 含 spec 要求的 model_mode/version/prompt_version/source_citations/confidence/risk_level/fallback_used/trace_id |
| 迁移 | `db/migration/{postgres,oracle,dm,kingbase,h2}/V18__model_gateway_api.sql` | 各 69 行（h2 43 行，无 COMMENT 为 house style） | 两表 DDL |
| 测试 | `ModelGatewayServiceTest`(9 例)、`ModelGatewayControllerTest`(4 例)、`ModelGatewayControllerSecurityTest`(4 例) | 共 17 例 | — |
| 前端 | `frontend/src/pages/advanced/AiWorkflows.tsx`(888 行)、`frontend/src/shared/api/hooks.ts` L1905-2014 | — | 配置矩阵 + 推理沙箱 |

### 1.2 业务目标完成度（对照 §7.12 / §1.6.1-1.6.2）

| 能力 | 规范要求 | 实际 | 评级 |
|---|---|---|---|
| 5 个模型接入 API | status/tasks/tasks{id}/retry/validate | 5 端点齐全、DTO/校验/错误码/traceId/审计齐全 | ✅ 形态达标 |
| provider 无关契约 | 统一 DTO + **适配 SPI 固定** | DTO 有；**SPI/适配接口完全不存在** | ⚠️ 半成品 |
| 真实 provider 接入(B1/B2) | 本地/外部模型 | **完全未接入**（恒 B0） | 诚实缺失（代码注释如实声明） |
| B0 诚实降级 | 不伪造模型名/置信度/引文/患者 | modelVersion/confidence/citations 诚实；**但 outputContent 写死病种假候选** | ❌ 假闭环 |
| 能力码产真实候选 | rule.draft/pathway.draft/knowledge.extract 等 | **写死高血压/I10.xx02 等常量当候选** | ❌ 违宪第 18 条 |
| 路由策略可配置/继承/回滚(GA-LLM-02) | 平台→集团→医院→科室→场景继承、可配可回滚 | **policy 表无写入路径；无继承；validate 不落库** | ❌ 缺失 |
| 数据最小化/脱敏(GA-LLM-03) | 字段白名单+脱敏+审批+阻断 | 正则脱敏真实；**无字段白名单/审批编号/越权阻断** | ⚠️ 部分 |
| 故障处理 | 超时/拒答/限流/结构失败/安全阻断均记录 | 仅 DISABLED + Schema 失败；**timeout(003)/敏感字段(005) 错误码定义但从不使用** | ⚠️ 部分 |

---

## 2. 十维度逐项结果

### ① 业务正确性 — 不达标

- **F-C1（Critical）**：`executeB0Fallback` 用 `switch` 对 4 个能力码写死单病种常量当"候选数据"返回：`ModelGatewayService.java:460-468`。如 `knowledge.extract` 恒返回 `{"entity":"高血压","degree":"III级","risk":"高危"}`、`terminology.map` 恒返回 `{"standard_code":"I10.xx02",...}`、`rule.draft`/`pathway.draft` 同样写死。Javadoc(L457-459) 自称"提供 100% 格式合法且中文化的**物理候选数据**"——即把写死常量包装成业务产出。命中判伪铁律"写死 switch/常量当真实结果"，且违反规范禁止"单病种硬编码"(spec L370)。无论用户输入什么病历，B0 都吐同一句高血压，是彻底的假产出。
- **F-H1（High）**：`knowledge.discovery / cdss.explain / quality.semantic-check / followup.draft` 四能力码落 `default` 分支(L466)，返回 `{"result":"确定性基线回退数据","capability":"..."}`——纯占位字符串，对这些能力 B0 等于无任何确定性基线实现（spec §1.6 B0 要求"已发布知识检索、确定性规则"等真实能力，此处空转）。
- **F-M1（Medium）**：路由策略取值 `BASEPLAY / LOCAL_MODEL / EXTERNAL_MODEL`（`ModelCapabilityPolicy.java:18`、`ModelGatewayService.java:443-451`）与规范定义 `DISABLED / LOCAL_ONLY / APPROVED_EXTERNAL / LOCAL_THEN_EXTERNAL`(spec L1534) 完全不一致；`BASEPLAY` 疑为 `BASELINE` 拼写错误（DDL 注释 L67 写 `BASELINE基线B0` 但 DEFAULT 字面量是 `'BASEPLAY'`，自相矛盾）。

### ② 医疗安全合规 — 部分达标（核心降级诚实，但写死候选触红线）

- 诚实面（达标）：provider 缺位时 `modelVersion="B0-Deterministic-Baseline"`、`confidence=null`、`sourceCitations="[]"`、`riskLevel="LOW"`、`fallbackUsed=true`(L134-140)；`baselineReason`(L441-453) 如实写"未接入本地/外部模型"。未伪造模型名/置信度/引文/患者，符合 §7.12.4 医疗安全组与宪法第 18 条的诚实降级要求。
- **F-C1 复用（Critical）**：但 §7.12.4 医疗安全组明令"无来源生成…均被阻断"。`executeB0Fallback` 输出的高血压候选 `sourceCitations="[]"`（无来源）却作为正式 `outputContent` 返回并落库，等于"无来源即生成医疗候选"。虽 confidence=null，但前端沙箱将其当"结构化输出内容"高亮展示(AiWorkflows.tsx:760-762)，临床用户可能误认为是真实抽取结果。
- 风险等级恒 `LOW`(L139)：高风险能力（rule.draft 阻断规则、pathway.draft 治疗节点）按 spec §7.12.2 需"双审/专家确认/高风险标识"，此处一律 LOW 且无审核门禁联动（记 High 见 F-H2）。
- **F-H2（High）**：无"AI 内容标识/医师确认才进病历/高风险强审/旧版隔离"任何机制。网关只产 `ModelTaskResponse`，无候选→审核→入库的状态机衔接；riskLevel 未按能力分级，高风险能力（rule/pathway/cdss）无强制双审标记。

### ③ 多租户隔离 — 基本达标（防御深度可加强）

- 写路径：submitTask 落库前 `requireCurrentTenant()`(L103,305-311) 取上下文 tenant，task.tenantId 来自上下文，非用户可控。✅
- 读路径：`taskRepo.findByTaskId(taskId)` **不带 tenant 条件**(`ModelCapabilityTaskRepository.java:19`)，靠 Service 手动 `if(!tenantId.equals(task.tenantId())) throw TENANT_FORBIDDEN`(L227-229、L260-262) 补救。功能正确但属"先查全局再判租户"，非数据层物理隔离。
- **F-M2（Medium）**：跨租户读防护是应用层手动校验而非 Repository 物理 `findByTaskIdAndTenantId`，且**无任何跨租户访问测试**（见维度⑨）。建议改带 tenant 的派生查询，符合"Repository 带 tenant_id"。
- Controller 有 `@DataScope(requireTenant=true)`(L25) 强制租户存在。✅

### ④ 审计证据链 — 达标

- 成功留痕：submitTask 走 `auditPublisher.publish(EXECUTE,...)`(L190-196) AFTER_COMMIT 同事务；含 capabilityCode/mode/fallback/cost。✅
- 失败留痕：DISABLED 阻断(L119)、Schema 失败(L153) 均走 `isolatedAudit.publishInNewTx(AuditEvent.failure(..., code.code(), summary))`(L313-316)，独立子事务，带 error_code，符合"失败走 IsolatedAuditPublisher"铁律。✅
- trace_id：贯穿 task 实体(L180)、响应(L211)、审计上下文。✅
- **F-M3（Medium）**：retryTask(L273) 与其内部调用的 submitTask 会对同一逻辑产生两条 EXECUTE 记录，outcome 语义略冗余；非阻塞。

### ⑤ 五方言一致性 — 达标（含小瑕疵）

- 五份 V18 表结构、列、类型映射等价：postgres `TEXT/BOOLEAN/DOUBLE PRECISION/BIGSERIAL/TIMESTAMPTZ` ↔ oracle/dm `CLOB/NUMBER(1)/NUMBER(8,4)/IDENTITY/TIMESTAMP` ↔ h2 `TEXT/BOOLEAN/DOUBLE/GENERATED IDENTITY`。JSON 字段 `output_content/expected_schema` 在 oracle/dm 正确用 CLOB，postgres/kingbase/h2 用 TEXT。✅
- 唯一约束 `uk_model_task_id`、`uk_model_policy_tenant`、索引 `idx_model_task_tenant` 五方言齐全。✅ 无保留字冲突；长度一致（VARCHAR 64/512/1024/128/255）。✅
- h2 无 COMMENT 是 house style（V15-V18 h2 均 0 条 COMMENT），非缺陷。
- **F-L1（Low）**：dm 方言 `dm/V18__model_gateway_api.sql:69` 注释串入英文 `期待输出匹配 of JSON Schema结构约束`（其余四方言为"期待输出匹配的..."），纯文案瑕疵。
- **F-M4（Medium）**：五方言 `route_strategy` 列 DEFAULT 字面量均为 `'BASEPLAY'`(L35)，但同文件 COMMENT(L67) 声明枚举为 `BASELINE基线B0`——DDL 内部自相矛盾，与 F-M1 同根。

### ⑥ 代码净化 — 不达标

- **F-C1 复用（Critical）**：`executeB0Fallback`(L460-468) 写死常量当结果——嗅探判伪命中。Javadoc 措辞"物理候选数据"(L459)、"100% 格式合法且中文化"(L458) 美化写死行为。
- 未发现：Math.random 造结果、catch 吞错伪造成功、FORCE_* 测试钩子、硬编码身份署名。
- **F-M5（Medium）**：`computeSha256` catch 块 `return "hash-"+UUID...`(L361-363)。SHA-256 在标准 JRE 必然可用，此分支实为死代码；但"UUID 充哈希"属判伪模式，仍应移除避免误导（若可达将污染 input_hash 审计字段）。

### ⑦ 错误处理与降级 — 部分达标

- ErrorCode 体系完整：ENG_LLM_001..005 定义(`ErrorCode.java:107-111`)，含 HTTP 状态与 ErrorClass，经 ApiException→ProblemDetail。✅
- 无 provider 时 B0 降级"真实可用"——**不成立**：降级动作真实触发，但 B0 产物是写死假数据，不构成 spec 要求的"确定性基线真实能力"。诚实性达标（如实标 fallbackUsed/reason），可用性不达标。
- **F-H3（High）**：`ENG_LLM_003`(超时,504) 与 `ENG_LLM_005`(敏感字段未脱敏,400) 已定义但**全代码零引用**（grep 仅命中定义行）。spec §1.6.1 故障处理要求"超时、拒答、限流、结构失败、安全阻断均记录原因"，实际仅覆盖 DISABLED(001) 与 Schema(002)，超时/限流/安全阻断/敏感字段拦截均未实现 → 故障处理矩阵残缺。

### ⑧ 可观测性 — 基本达标

- 日志：Schema 失败 `log.warn`(L151)；trace_id 全链路。✅
- 耗时 `time_cost_ms` 真实计算(L107,159) 并落库 + 响应。✅
- 前端看板"最近一次推理用时/路由模式/降级状态"取自后端真实返回(AiWorkflows.tsx:365,379,397)，未写死指标。✅
- 缺：无 metrics 计数器（调用量/降级率/失败率）暴露；属增强项非阻塞。

### ⑨ 测试覆盖与有效性 — 不达标（固化假绿）

- **F-H4（High）**：`ModelGatewayServiceTest.java:99` `assertTrue(resp.outputContent().contains("高血压"))` 将写死病种数据断言为"正确"，把 F-C1 假闭环固化为通过用例——典型"写死掉真实实现固化假绿"。`submitTask_BaseplayStrategy`(L84-105) 整例都在验证写死输出。
- 正向覆盖：DISABLED 阻断(L68-81)、B0 降级(L84-105)、LOCAL/EXTERNAL 诚实降级不伪造(L108-163)、Schema 满足/不满足(L166-202)、validate 合法/非法(L205-224)。降级诚实性反例断言（不出现 `MedKernel-Local-Cognitive-v1`/`溶栓指南`/`李建国` L129-130,156-158）质量较高。✅ 部分
- 权限：Controller 安全测试覆盖 无 auth→401、write 角色→200、只读角色→403、缺租户→400(`...SecurityTest.java:50-94`)。✅
- **F-H5（High｜关键测试缺失）**：**无跨租户隔离测试**——getTask/retryTask 的 `TENANT_FORBIDDEN` 分支(Service L227-229,260-262) 零覆盖，即多租户最关键安全路径未被任何用例验证。
- 缺失：并发/幂等无测试；脱敏 MASK_ALL 分支(L340-345 中文姓名/病历号)无测试；getStatus 已配置策略分支(L77-86)与 DISABLED 状态分支无测试；retryTask 无测试。
- Controller 测试 mock 掉整个 Service(L37 `@MockBean`)，故 L86/L109 出现 `"B2","Med-LLM",0.95` 等仅为 mock 桩数据（不影响真实逻辑，但说明 Controller 层未做真实集成验证）。

### ⑩ 前后端契约一致 — 基本达标（含 enum 偏差）

- DTO↔type：`ModelTaskResponse`(后端)↔`ModelTaskResponse`(hooks.ts:1921-1935) 字段逐一对应。✅ 但前端 `confidence: number`(L1929) 与后端 `Double`(可空) 不符——provider 缺位时 confidence=null，前端类型未标 `| null`，沙箱直接渲染 `{sandboxResult.confidence}`(AiWorkflows.tsx:777) 会显示 `null`。记 F-M6。
- 路径↔queryKey：`/model-capabilities/*` 五端点与 hooks 完全对应(hooks.ts:1951-2014)。✅
- 错误码处理：前端 `err?.response?.data?.message` 兜底展示(AiWorkflows.tsx:255,305,319)——按 message 而非 code 分支，未针对 ENG-LLM-00x 差异化处理；粗粒度但可用。
- **F-H6（High）**：前端**策略配置不落库**。`handleSavePolicy`(AiWorkflows.tsx:215-257) 调 validate 后仅 `setLocalPolicies` 写 React state，注释如实声明"本地预演生效，未落库持久化"(L113,236,246)。即整个"路由策略配置矩阵"是无后端持久化的前端演示——GA-ENG-LLM-02"可配置、可回滚"在前后端均无实现。诚实（有声明）但功能缺失。
- **F-M6（Medium）**：前端 enum 含 `BASEPLAY`(hooks.ts:1907、AiWorkflows.tsx:130,327,464,854) 沿用后端拼写错误；`confidence: number` 漏 null。
- **F-M7（Medium）**：AiWorkflows.tsx:670 当 `isB0Active=false` 时 Timeline 写死展示"智能模型通道运行 (B2)"绿勾。因 `isB0Active` 在无 sandboxResult 时退回看 `routeStrategy`(L325-327)，用户选 EXTERNAL_MODEL 且未运行时会误显示"B2 通道运行"，而后端实际恒 B0。属前端展示与后端契约不符（轻度，运行后即被真实返回纠正）。

---

## 3. Findings 汇总表

| Severity | ID | 一句话 | file:line |
|---|---|---|---|
| Critical | F-C1 | B0 兜底用 switch 写死单病种常量（高血压/I10.xx02）当真实候选返回并落库展示，违宪第18条假闭环 | `ModelGatewayService.java:460-468` |
| High | F-H1 | 4 能力码（discovery/cdss/quality/followup）B0 落 default 仅返回占位字符串，无确定性基线实现 | `ModelGatewayService.java:466` |
| High | F-H2 | 无 AI 标识/医师确认入库/高风险强审/旧版隔离；riskLevel 恒 LOW 未按能力分级 | `ModelGatewayService.java:139` |
| High | F-H3 | ENG_LLM_003(超时)/005(敏感字段) 定义却零引用；超时/限流/安全阻断故障处理未实现 | `ErrorCode.java:109,111` |
| High | F-H4 | 测试断言写死"高血压"输出为正确，固化假闭环为绿灯 | `ModelGatewayServiceTest.java:99` |
| High | F-H5 | 跨租户隔离（getTask/retryTask 的 TENANT_FORBIDDEN）零测试覆盖 | `ModelGatewayService.java:227-229,260-262` |
| High | F-H6 | 路由策略配置前后端均不落库（policyRepo 无 save；前端仅本地 state），GA-LLM-02 可配可回滚缺失 | `AiWorkflows.tsx:215-257` / `ModelCapabilityPolicyRepository.java` |
| Medium | F-M1 | 路由枚举 BASEPLAY/LOCAL_MODEL/EXTERNAL_MODEL 与规范 DISABLED/LOCAL_ONLY/APPROVED_EXTERNAL/LOCAL_THEN_EXTERNAL 不一致 | `ModelCapabilityPolicy.java:18` |
| Medium | F-M2 | 跨租户读靠应用层手动判断而非 Repository 物理 tenant 过滤 | `ModelCapabilityTaskRepository.java:19` |
| Medium | F-M3 | retry 与内部 submit 对同一逻辑产生双 EXECUTE 审计，语义冗余 | `ModelGatewayService.java:273,190` |
| Medium | F-M4 | 五方言 DDL route_strategy DEFAULT 'BASEPLAY' 与同文件 COMMENT 'BASELINE' 自相矛盾 | `postgres/V18__model_gateway_api.sql:35,67` |
| Medium | F-M5 | computeSha256 catch 块以 UUID 充哈希（死代码但属判伪模式） | `ModelGatewayService.java:361-363` |
| Medium | F-M6 | 前端 enum 沿用 BASEPLAY 拼写错误；confidence 类型漏 null | `hooks.ts:1907,1929` |
| Medium | F-M7 | 前端 Timeline 未运行时写死"智能模型通道运行(B2)"绿勾，与后端恒 B0 不符 | `AiWorkflows.tsx:670,325-327` |
| Low | F-L1 | dm 方言注释串入英文 "of" | `dm/V18__model_gateway_api.sql:69` |
| (达标) | — | 审计证据链（成功/失败/trace_id）、租户写隔离、五方言结构、ErrorCode 体系、降级诚实性 | — |

合计：**Critical 1 · High 6 · Medium 7 · Low 1**

---

## 4. 改造建议（C/H 项）

### F-C1 · B0 兜底写死单病种常量

- 问题：`executeB0Fallback` switch 写死高血压/I10.xx02 等当候选返回，违宪第 18 条（写死结果假闭环）+ 违 spec 禁止单病种硬编码 + 违医疗安全组"无来源不生成"。
- 位置：`ModelGatewayService.java:460-468`。
- 影响：临床用户在沙箱/任务结果看到与输入无关的固定病种"抽取结果"，可能误用；审计 output_content 落入虚假医疗内容。
- 改动建议：B0 不得产出任何"看似真实"的医疗候选。两条路线择一：①B0 对增强类能力（extract/draft/map/discovery）**明确返回不可增强**：`outputContent` 置空或返回结构化"B0 无模型，本能力需人工/确定性流程处理"提示，`status=DEGRADED`，给可跳转的人工流程入口；②若 B0 要给确定性结果，必须接真实确定性引擎（如 terminology.map 走已落库字典映射查询、knowledge.discovery 走已发布知识检索），结果带真实 source。绝不用 switch 常量。
- 工作量：8h（含与各引擎确定性查询对接调研）。
- 验证：用例改为断言"B0 输出不含任何病种字面量 + status=DEGRADED + outputContent 为空或人工提示"；新增"不同输入得到不同/或一致拒绝增强"测试。

### F-H1 · 4 能力码 B0 占位

- 问题：discovery/cdss/quality/followup 落 default 返回 `确定性基线回退数据` 占位串。
- 位置：`ModelGatewayService.java:466`。
- 影响：这些能力 B0 等于完全空实现，spec B0 行声明能力未兑现。
- 改动建议：随 F-C1 一并处理——统一改为"明确不可增强"诚实响应，移除 default 占位串。
- 工作量：含于 F-C1。
- 验证：断言 default 分支不返回任何伪结果。

### F-H2 · 医疗安全门禁缺失

- 问题：无 AI 标识/入库需医师确认/高风险强审/riskLevel 分级。
- 位置：`ModelGatewayService.java:139`（riskLevel 恒 LOW）。
- 影响：违 spec §7.12.2/§7.12.4 医疗安全组；高风险能力（rule/pathway/cdss）输出无双审标记。
- 改动建议：按能力码建立 riskLevel 映射（rule.draft/pathway.draft/cdss.explain=HIGH 需双审，extract/map=MEDIUM）；ModelTaskResponse 增 `requiresReview`/`aiGenerated` 标识；候选入库链路（与规则/路径/知识引擎衔接）必须经审核状态机，禁止直接生效。
- 工作量：6h（本单元侧）+ 跨单元联调。
- 验证：高风险能力用例断言 riskLevel=HIGH 且 requiresReview=true。

### F-H3 · 故障处理矩阵残缺 + 死错误码

- 问题：ENG_LLM_003/005 零引用；超时/限流/安全阻断未实现。
- 位置：`ErrorCode.java:109,111`。
- 影响：spec §1.6.1 故障处理与 §7.12.3 调用控制（超时/限额/敏感词/熔断）未落地。
- 改动建议：接入 provider 时落实超时(→003)、敏感字段检测无脱敏策略时(→005) 阻断并审计；当前阶段至少对 NONE 脱敏 + 含敏感数据组合给 005 拦截。
- 工作量：4h（当前阶段）；provider 接入后再补超时/限流/熔断。
- 验证：敏感输入 + NONE 策略断言抛 005 并 isolatedAudit 留痕。

### F-H4 · 测试固化假绿

- 问题：断言写死"高血压"为正确输出。
- 位置：`ModelGatewayServiceTest.java:99`。
- 影响：掩盖 F-C1，使 CI 绿灯失去意义。
- 改动建议：随 F-C1 重写该用例（见 F-C1 验证）。
- 工作量：1h。
- 验证：删除 `contains("高血压")` 类断言。

### F-H5 · 跨租户隔离零测试

- 问题：getTask/retryTask 的 TENANT_FORBIDDEN 分支无测试。
- 位置：`ModelGatewayService.java:227-229,260-262`。
- 影响：多租户最关键安全路径未验证，回归风险高。
- 改动建议：新增用例——tenant-A 提交任务，切 tenant-B 上下文调 getTask/retryTask 断言抛 TENANT_FORBIDDEN。
- 工作量：1.5h。
- 验证：两条新用例通过。

### F-H6 · 策略配置不落库

- 问题：policyRepo 无 save；前端 handleSavePolicy 仅写本地 state；validate 不持久化。
- 位置：`AiWorkflows.tsx:215-257`、`ModelCapabilityPolicyRepository.java`、`ModelGatewayService.java:284-301`。
- 影响：GA-ENG-LLM-02"院内/获批外部/禁用可配置、可校验、可回滚"前后端均无实现；§7.12.3 平台→集团→医院→科室→场景策略继承完全缺失。
- 改动建议：新增 `POST/PUT /api/v1/model-capabilities/policies` 落库端点（带 tenant/能力码 upsert + 审计 + 版本）；实现策略继承解析（默认→组织层级覆盖）；前端 save 改调真实落库端点而非本地 state。
- 工作量：12h。
- 验证：策略落库后 getStatus 反映真实配置；继承覆盖用例；跨租户策略隔离用例。

---

## 5. 总评

**done 是否名副其实：否。** 四个自报 `done` 的 backlog 项中：
- GA-ENG-API-12（API/迁移/审计/B0 降级）：API 形态、五方言迁移、审计链、错误码体系确实**真实落地且质量不低**（审计证据链、租户写隔离、Schema 真实 JSON 校验、脱敏正则均为实做），可算"骨架达标"；但 B0 降级的**产物是写死假数据**，"降级"动作真而"基线能力"伪。
- GA-ENG-LLM-01（provider 无关契约）：DTO 契约在、能力码在，但**适配 SPI 完全不存在**，provider 无关只做了一半。
- GA-ENG-LLM-02（路由/继承/开关，可配可回滚）：**实质未实现**——策略表无写入、无组织继承、前端仅本地预演。名实严重不符。
- GA-ENG-DEGRADE-01（降级链主链路可运行）：降级触发与诚实标识达标，但因 B0 产物造假 + 4 能力空实现，"主链路仍可运行"对模型增强类能力不成立。

**可否真实验收：否。** 阻塞项（须先清）：①F-C1 移除写死病种假候选（违宪硬约束，最高优先）；②F-H6 补策略落库与继承（GA-LLM-02 名实不符）；③F-H1 清除占位串；④F-H5 补跨租户测试；⑤F-H2 医疗安全门禁。

**值得肯定（独立复核确认非造假的部分）：** 代码作者在 provider 缺位的诚实性上做得到位——modelVersion/confidence/citations 不伪造、注释明确声明"未接入 provider""未落库持久化"、Schema 校验是真实 Jackson 解析而非字符串 contains、失败走 IsolatedAuditPublisher。问题集中在"B0 兜底用写死常量假装有产出"这一处假闭环，及策略持久化/SPI/故障矩阵的功能缺失，而非系统性造假。

**建议 backlog 状态重置：** GA-ENG-API-12 → `partial`（骨架真、B0 假）；GA-ENG-LLM-01 → `partial`（缺 SPI）；GA-ENG-LLM-02 → `todo`（实质未做）；GA-ENG-DEGRADE-01 → `partial`（诚实但能力空）。
