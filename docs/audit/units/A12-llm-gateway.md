# A12 模型能力网关 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude
> backlog 自报状态：LLM-01 / LLM-02 / API-12 / DEGRADE-01 = done
> **审计结论：🔴 不达标，名不副实。核心"B0/B1/B2 路由推理"为表演式假实现，且造假贯穿后端+测试+前端三层，含医疗安全 Critical。建议三项 done 全部回退。**

---

## 1. 单元概览

| 项 | 信息 |
|---|---|
| 后端 | `engine/llm`（11 文件）；核心 `ModelGatewayService`(490) / `ModelGatewayController`(92) |
| 迁移 | V18 五方言齐全（model_capability_task / model_capability_policy） |
| 前端 | `pages/advanced/AiWorkflows.tsx`(1058) |
| 测试 | 3 个：ServiceTest / ControllerTest / ControllerSecurityTest |
| 业务目标 | getStatus / submitTask / getTask / retryTask / validatePolicy + 路由 / 脱敏 / 结构化输出 / B0 降级 |

**完成度**：外壳（API/持久化/审计/租户/脱敏）真实；**核心推理能力全假**——B1/B2 不调任何模型，返回写死数据，B2 伪造患者/引文/置信度。

---

## 2. 十维度审计结果

### 2.1 业务正确性 — 🔴 核心造假
- `submitTask` 路由三分支（`ModelGatewayService.java:156/168/186`）：
  - B0(BASEPLAY)：`executeB0Fallback`(450) 写死 switch，合法（B0 本就是确定性基线）。
  - B1(LOCAL_MODEL)：`executeB1LocalInference`(465) 写死 switch，Javadoc 自承"**模拟**本地部署模型"。
  - B2(EXTERNAL_MODEL)：`executeB2ExternalInference`(480) 写死 switch，Javadoc 自承"**模拟**外部大语言模型/Dify"。
- `retryTask:338` 注释明写"**当前所有能力均走 B0 确定性基线**"——坐实 B1/B2 从不真实推理。
- **无任何 HTTP/SDK/Dify 调用代码**，整个网关不连接任何模型。

### 2.2 医疗安全合规 — 🔴 红线
- 🔴 **LLM-CRIT-01**：`:188-193` B2 分支写死 `modelMode="B2"`、`modelVersion="MedKernel-Cognitive-LLM-v2"`、`sourceCitations="[\"急性脑梗死规范化溶栓指南(2025版)§4.2\"]"`、`confidence=0.96`，与真实输入无关；`executeB2ExternalInference:482` 更编造患者 `"李建国"/68岁` + 体征禁忌。**医疗 AI 编造引文/置信度/患者数据**，直接违反宪法 #9（AI 内容须如实标识，此处是伪造来源）、#13（可插拔模型需诚实无模型基线），有患者安全风险。
- 脱敏 `desensitize:395` **真实**（手机号 402 保留前 3 后 4、身份证 405 保留前 6 后 4），较真实性审计 B7 已修。但仅覆盖手机+身份证，**未脱敏患者姓名/病历号/就诊号/住址**（个保法覆盖不全）→ 见 LLM-M-03。

### 2.3 多租户隔离 — ✅
- Controller 类级 `@DataScope(requireTenant=true)`（25）。
- `getTask:301` / `retryTask:334` 显式跨租户校验，越权抛 `TENANT_FORBIDDEN`。
- `requireCurrentTenant:379` 缺租户即抛；Policy 查询 `findByTenantIdAndCapabilityCode`。

### 2.4 审计与证据链 — ✅（优于多数模块）
- 主任务经 `isolatedAudit.publishInNewTx`（264）留痕；失败经 `publishFailureAudit:387`（`AuditEvent.failure` + errorCode）留痕。
- ⚠️ 瑕疵：`retryTask:347` 用普通 `auditPublisher` 而非 isolated（与本模块其余不一致）。
- ⚠️ 审计内容本身记录了**虚假**的 mode=B2/fallback 信息（审计如实记录了假数据，审计机制真但被审计对象假）。

### 2.5 五方言一致性 — ✅（文件齐全）
V18 五方言均存在。逐字段比对待后续。

### 2.6 代码净化 — 🔴
- 🟠 **LLM-M-01**：`:136` `FORCE_FAIL_SCHEMA_` 魔法字符串测试钩子混入**生产**代码路径。
- 🟠 **LLM-M-02**：`:231` `System.err.println("【大模型网关警告】...")` 生产代码直接打印 stderr（违反净化门禁，应走 logger）。
- 路由分支写死推理数据（见 2.1）。

### 2.7 错误处理与降级 — 🟡
- `ENG_LLM_001..004` 声明并使用；ProblemDetail 统一。
- 降级"链"真实存在（catch→B0，217-232），但因 B1/B2 本就是假，"降级"只是从一组写死数据切到另一组写死数据。
- 🟠 **LLM-H-01**：`validateSchema:440` 仅按字符串 `contains` 判断字段存在，非真 JSON Schema 校验；结构化输出"合规"形同虚设。
- `validatePolicy:358` 校验较浅（能力码白名单 + schema 以 `{`/`[` 开头），但非假。

### 2.8 可观测性 — 🟡
- traceId 贯穿、timeCost 计时（234）；但用 `System.err` 而非 Micrometer/logger，无结构化指标（调用量/降级率/延时分布）。

### 2.9 测试覆盖与有效性 — 🔴 把假固化为绿
- 🔴 **LLM-H-02**：`ModelGatewayServiceTest.java:127` `submitTask_externalModelRoute_..._ReturnsB2Success` **断言** `modelMode=="B2"`(141)、`modelVersion=="MedKernel-Cognitive-LLM-v2"`(143)、`sourceCitations.contains("溶栓指南")`(144)——**把伪造的模型版本与编造引文断言为"正确行为"**。测试不仅没发现造假，反而将其固化为回归基线，是"全绿"假象的根源（呼应总计划 D2）。
- 无"无 provider 时应诚实标 B0、不得出 B2 元数据"的负向用例。

### 2.10 前后端契约一致 — 🔴 前端系统性假闭环
- 🔴 **LLM-CRIT-02**：`AiWorkflows.tsx` 每个 catch 块伪造成功：
  - `:324-386` 推理失败→造 `mockRes`（`Math.random` taskId/traceId）+ `message.success("[仿真推理成功]...")`
  - `:401-415` 重试失败→造 `mockRetry` + `message.success("[仿真重试成功]...走入 B0 基线通道并结案")`
  - `:254-265` 策略发布失败→`message.success("[仿真模式] 策略...已物理就绪")`
- 🟠 **LLM-H-03**：`:48` 注释公开承认"**防 ESLint no-page-mock，采用驼峰法规避**"——明文绕过防造假门禁（坐实总计划 R1 根因）。
- `:190` "合并 API 数据与本地仿真数据"，真假混展。

---

## 3. 七角色视角评估

| 角色 | 评估 |
|---|---|
| 合规审计 | 🔴 审计虽留痕，但记录的是伪造的 B2/引文，导出"证据"将包含编造内容，合规风险极高 |
| 临床医生 | 🔴 若任何上层（CDSS/知识/规则生成）消费本网关 B2 输出，将得到编造的临床引文与患者数据 |
| 信息科主任 | B0 无模型基线确实可跑（可作为唯一诚实价值），但 B1/B2 路由配置是摆设 |

---

## 4. Findings 汇总

| Severity | ID | 一句话 | 位置 |
|---|---|---|---|
| Critical | LLM-CRIT-01 | B2 写死并编造引文/置信度/患者数据，违反宪法#9 | `ModelGatewayService.java:188-193,480-488` |
| Critical | LLM-CRIT-02 | 前端 catch 块系统性伪造推理/重试/发布成功 | `AiWorkflows.tsx:254-265,324-386,401-415` |
| High | LLM-H-01 | validateSchema 仅字符串 contains，非真校验 | `ModelGatewayService.java:440` |
| High | LLM-H-02 | 单测断言伪造引文/模型版本为"正确"，固化假绿 | `ModelGatewayServiceTest.java:141-144` |
| High | LLM-H-03 | 前端公开承认驼峰命名规避 no-page-mock 门禁 | `AiWorkflows.tsx:48` |
| Medium | LLM-M-01 | FORCE_FAIL_SCHEMA_ 测试钩子混入生产 | `ModelGatewayService.java:136` |
| Medium | LLM-M-02 | System.err.println 进生产代码 | `ModelGatewayService.java:231` |
| Medium | LLM-M-03 | 脱敏仅覆盖手机+身份证，漏患者姓名/病历号 | `ModelGatewayService.java:395-408` |
| Low | LLM-M-04 | retryTask 用普通 auditPublisher 非 isolated | `ModelGatewayService.java:347` |

合计：Critical 2 · High 3 · Medium 3 · Low 1

---

## 5. 改造方案（按优先级）

### LLM-CRIT-01 B1/B2 诚实化（核心）
- **问题**：B1/B2 假冒模型推理并编造临床内容。
- **改造**：二选一并落到 §10「大模型只能是可插拔增强层」：
  - **方案 A（推荐，符合 B0 验收）**：移除 `executeB1LocalInference`/`executeB2ExternalInference` 的写死返回；当未配置真实 provider/Dify 时，路由**一律诚实降级到 B0**，`modelMode="B0"`、`fallbackUsed=true`、`fallbackReason="未接入外部/本地模型，按确定性基线执行"`，**禁止输出任何 B1/B2 元数据、引文、置信度、患者数据**。
  - **方案 B**：接入真实 provider 抽象（`ModelProviderPort` + Ollama/Dify Adapter），有则真调、无则走 A。引文/置信度只能来自真实模型输出，不得写死。
- **工作量**：方案 A 约 3h；方案 B 约 2–3 天（含 Adapter + 集成测试）。
- **验证**：无 provider 时 submitTask 任意能力均返回 mode=B0、citations=[]、confidence=null；新增负向用例断言"绝不出现 MedKernel-Cognitive-LLM-v2 / 编造引文"。

### LLM-CRIT-02 前端去除 catch 伪造（核心）
- **改造**：删除 `:324-386`/`:401-415`/`:254-265` 的 mock 伪造与 `setSandboxResult(mock...)`；失败时显示真实错误 + 六态错误态；沙箱推理只渲染后端真实返回。
- **工作量**：约 3h。
- **验证**：断网/后端 500 时前端显示错误态，绝不弹"仿真推理成功"。

### LLM-H-02 重写测试（否则改完又被假绿掩盖）
- **改造**：删除断言伪造引文/版本的用例；改为断言"无 provider→B0 诚实降级、无编造元数据"；schema 校验改用真 JSON 解析后断言。
- **工作量**：约 2h。

### LLM-H-01 真 Schema 校验
- **改造**：`validateSchema` 用 Jackson 解析输出 JSON，按 expectedSchema 的 required 字段集做存在性 + 类型校验，失败抛 `ENG_LLM_002`。
- **工作量**：约 1.5h。

### LLM-H-03 / LLM-M-01 / M-02 门禁与净化
- 删 `AiWorkflows.tsx:48` 规避注释，恢复 no-page-mock（随 CRIT-02 清存量）；
- 移除 `FORCE_FAIL_SCHEMA_` 生产钩子（测试改用 Mockito 注入故障）；
- `System.err.println` 改 `org.slf4j.Logger.warn`。
- **工作量**：合计约 1.5h。

### LLM-M-03 脱敏补全
- **改造**：扩展 `desensitize` 覆盖患者姓名（结合上下文字段）、病历号/就诊号、住址、银行卡；或对接统一脱敏组件。中文场景慎用 `\b`。
- **工作量**：约 2h。

---

## 6. 优化建议（非阻塞）
- 抽象 `ModelProviderPort` 让 B0/B1/B2 与 provider 解耦，便于内网（Ollama）/外网（Dify/OpenAI 兼容）切换，满足宪法 §10 内外网双形态。
- 网关调用量/降级率/延时接 Micrometer，供 Provider 状态页与质控看板下钻。

---

## 7. 总评
- **done 名副其实性**：LLM-01 / LLM-02 / DEGRADE-01 **名不副实**，应回退为 in_progress。外壳（API/租户/审计/脱敏正则）真实可用，但"模型能力"这一核心价值是表演式假实现。
- **可否进入 GA 验收**：否。LLM-CRIT-01/02 是患者安全 + 假闭环红线，必须先返工。
- **最危险点**：编造的临床引文/置信度/患者数据若被上层引擎消费会污染临床决策；审计还会把伪造内容固化成"证据"。**建议优先于其它单元修复本单元 CRIT-01。**
- **与 backlog 联动**：v4.40 曾把本模块改回 done、v4.41 又标 DEGRADE-01 done、#128 称"B0/B1/B2 路由开发"——均与代码事实不符，建议据实订正台账。
