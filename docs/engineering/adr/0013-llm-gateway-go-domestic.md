# ADR-0013：LLM Gateway 去 Dify 化策略 — 直连国产大模型 + Ollama 本地化

- 状态：Accepted
- 日期：2026-05-21
- 决策人：架构组（Claude Opus 4.7 + 用户）
- 关联：[COMPREHENSIVE_REVIEW.md §11](../../COMPREHENSIVE_REVIEW.md) + [DEPLOYMENT_DUAL_MODE.md §3.7](../../DEPLOYMENT_DUAL_MODE.md) + [PR-V3-DIFY-DOMESTIC](../02_任务台账.md)

---

## 1. 背景

### 1.1 原 Dify 路径的问题

v0.2-demo 阶段，AI 能力调用走 Dify Workflow：
```
MedKernel Backend → Dify Workflow → 大模型（默认 OpenAI）
```

**3 类严重问题**：

| 问题 | 影响 |
|---|---|
| **数据出境** | Dify Cloud 默认境外节点（虚拟 IP 在新加坡 / 美国），患者数据走 Dify 即跨境，触发《数据出境安全评估办法》100 万人评估门槛 |
| **运维负担** | Dify 私有化部署需独立 Python 服务（Flask + Celery + Redis + Postgres），医院 IT 多维护一个栈 |
| **抽象冗余** | Dify 的"工作流"对 80% 业务过度设计（医学解释、规则归纳、文本提取这些场景**单次 LLM 调用即可**）|

### 1.2 国产大模型现状（2026-05）

| 厂商 | 模型 | OpenAI 兼容协议 | 数据合规 | 推荐场景 |
|---|---|:---:|:---:|---|
| 阿里通义 | qwen-max / qwen-plus / qwen2.5-72b | ✅ DashScope 兼容模式 | ✅ 境内 | 主推 |
| DeepSeek | deepseek-chat / deepseek-coder / deepseek-r1 | ✅ 原生 OpenAI 协议 | ✅ 境内 | 主推（性价比） |
| 月之暗面 Kimi | moonshot-v1-8k/32k/128k | ✅ 原生 | ✅ 境内 | 长上下文 |
| 智谱 | glm-4 / glm-4-plus / glm-4-air | ✅ 原生 | ✅ 境内 | 主推 |
| 百度文心 | ernie-4.0-turbo | ❌ 百度专有 | ✅ 境内 | 备用 |
| 字节豆包 | doubao-1.5-pro / doubao-pro-32k | ✅ 火山引擎 ark 兼容 | ✅ 境内 | 备用 |
| MiniMax | abab6.5-chat | ✅ | ✅ 境内 | 备用 |
| 零一万物 Yi | yi-large | ✅ | ✅ 境内 | 备用 |
| 阶跃 | step-2 | ✅ | ✅ 境内 | 备用 |
| 百川 | baichuan4 | ✅ | ✅ 境内 | 备用 |
| 商汤 | sense-chat | ✅ | ✅ 境内 | 备用 |

**结论**：**80% 国产大模型支持 OpenAI 兼容协议**，一个 Provider 实现即可适配。

### 1.3 内网部署的"本地 LLM"需求

部分三甲医院 / 国央企医院的真实诉求：**LLM 也要内网部署，不出院**。

主流本地化方案：
- **Ollama**（推荐）—— 轻量、易部署、支持 Qwen2/Llama3/DeepSeek-V2/GLM-4 等开源模型，OpenAI 兼容 API
- **vLLM**（中型）—— Python 服务，吞吐量高，适合医院集中 GPU 服务器
- **Xinference**（备用）—— 国产 OSS，支持模型多

---

## 2. 决策

### 2.1 三层 Provider 策略

```
┌────────────────────────────────────────────────────────────┐
│ ModelGatewayService                                        │
│   降级链：QIANWEN → DEEPSEEK → OLLAMA_LOCAL → LOCAL_RULE   │
└─────────┬──────────────┬──────────────┬──────────────┬─────┘
          │              │              │              │
          ↓              ↓              ↓              ↓
    [Cloud LLM]    [Cloud LLM]    [Local LLM]   [Rule Engine]
  OpenAICompatible OpenAICompatible  Ollama       LocalModel
   (Qianwen)        (DeepSeek)     (qwen2:7b)   (硬编码兜底)
```

### 2.2 Provider 实现路径

| Provider | 类型 | 实现成本 | 部署形态 | 默认状态 |
|---|---|---|---|:---:|
| **`OpenAICompatibleProvider`**（核心） | 配置驱动 | 高（一次写，多次用）| 内网 + 外网 | ✅ 启用 |
| **`OllamaProvider`** | 配置驱动 | 中 | 内网首选 | 🟡 配置启用 |
| **`LocalModelProvider`**（保留） | 硬编码 | 已有 | 全场景兜底 | ✅ 启用 |
| **`DifyModelProvider`**（保留） | 已有 | 已有 | 复杂工作流场景 | 🔴 默认禁用 |
| `WenxinNativeProvider`（次优先） | 单独实现 | 低 | 备用 | 🔴 V0.3 不做 |

### 2.3 配置驱动模型

`application.yml` 新增段：

```yaml
medkernel:
  model-gateway:
    enabled: true
    default-timeout-ms: 8000
    max-retry-count: 1
    # 降级链：按调用类型配置 Provider 顺序
    degradation-chains:
      RESEARCH: "QIANWEN,DEEPSEEK,OLLAMA_LOCAL,LOCAL"
      EXTRACT: "DEEPSEEK,QIANWEN,OLLAMA_LOCAL,LOCAL"
      EMBEDDING: "QIANWEN,LOCAL"
      RERANK: "LOCAL"
      CRITIC: "DEEPSEEK,QIANWEN,LOCAL"
      WORKFLOW: "DIFY,LOCAL"   # 仅 WORKFLOW 才走 Dify
    # 多 Provider 配置（启动时由 LlmProviderFactory 注册为多个 ModelProvider Bean）
    providers:
      QIANWEN:
        type: openai-compatible
        base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
        api-key: ${QIANWEN_API_KEY:}
        model: qwen-plus
        timeout-ms: 8000
        max-tokens: 2000
        temperature: 0.3
        enabled: false   # 需要 API Key 才启用
      DEEPSEEK:
        type: openai-compatible
        base-url: https://api.deepseek.com/v1
        api-key: ${DEEPSEEK_API_KEY:}
        model: deepseek-chat
        timeout-ms: 8000
        max-tokens: 2000
        temperature: 0.3
        enabled: false
      KIMI:
        type: openai-compatible
        base-url: https://api.moonshot.cn/v1
        api-key: ${KIMI_API_KEY:}
        model: moonshot-v1-32k
        enabled: false
      ZHIPU:
        type: openai-compatible
        base-url: https://open.bigmodel.cn/api/paas/v4
        api-key: ${ZHIPU_API_KEY:}
        model: glm-4-plus
        enabled: false
      OLLAMA_LOCAL:
        type: ollama
        base-url: http://localhost:11434
        model: qwen2:7b
        timeout-ms: 30000    # 本地推理慢
        enabled: false       # 内网医院手动启用
```

### 2.4 业务行为约定

| 调用类型 | 推荐 Provider 优先级 | 理由 |
|---|---|---|
| RESEARCH（医学知识研究） | 通义 > DeepSeek > Ollama > Local | 通义对中文医学知识理解最好 |
| EXTRACT（病历信息抽取） | DeepSeek > 通义 > Ollama > Local | DeepSeek 性价比高 |
| EMBEDDING（文本向量化） | 通义 text-embedding-v3 > Local | 国产唯一稳定方案 |
| RERANK（重排序） | Local（暂无国产推荐 API） | 等 v0.4 接入 BGE-reranker |
| CRITIC（医学审稿/质疑） | DeepSeek > 通义 > Local | DeepSeek 逻辑推理强 |
| WORKFLOW（多步流程） | Dify > Local | 真正需要工作流编排时才用 |

### 2.5 LLM Provider 与"医学知识审核"的关系

按 [PR-V3-DIFY-DOMESTIC](../02_任务台账.md)：

- AI 生产的所有 KnowledgeAsset **必须经过医生审核**（不变量 R5：医学内容必须有来源）
- 任何 LLM 输出都打 `provider=XXX / model=XXX / fallback=true|false / trace_id=XXX` 元数据
- 患者数据传输到任何 LLM 前，前端必须显示 "本次推荐由 X 模型生成，调用记录可在 /aik/sources 查询" banner
- 如果 LLM 是境外（默认禁用，但管理员可强制开 `WORKFLOW: "DIFY"`），banner 升级为 "⚠️ 涉及数据出境调用，已记录审计"

---

## 3. 实施计划

### 3.1 v0.3 完成（PR-V3-LLM-GATEWAY，本 PR）

- [x] ADR-0013（本文）
- [ ] `OpenAICompatibleProvider.java` — 单一类实现 OpenAI Chat Completions 协议
- [ ] `OllamaProvider.java` — 单一类实现 Ollama Chat API（兼容 OpenAI 但有 `/api/chat` endpoint 差异）
- [ ] `LlmProviderFactory.java` — 启动时根据 application.yml 配置注册 N 个 ModelProvider Bean
- [ ] `LlmProviderConfig.java` — 单个 Provider 配置数据类
- [ ] `LlmGatewayProperties.java` — 全部 LLM Gateway 配置（providers Map）
- [ ] 改 `ModelGatewayService` 默认降级链：`DIFY,LOCAL` → 见 §2.3
- [ ] application.yml 加 §2.3 中的全部默认配置
- [ ] 加单测：`OpenAICompatibleProviderTest` + `OllamaProviderTest`
- [ ] 文档：本 ADR + 更新 [COMPREHENSIVE_REVIEW.md §11](../../COMPREHENSIVE_REVIEW.md) + 更新 02_任务台账

### 3.2 v0.3 后续 PR（独立任务）

| PR | 内容 | 估时 |
|---|---|---|
| **PR-V3-LLM-CONSOLE** | 前端「模型网关控制台」`/admin/llm-gateway` — 配 Provider / 看调用日志 / 切换默认模型 | 3 天 |
| **PR-V3-LLM-PROMPT** | PromptTemplate 中心化管理 + 版本化 + 灰度发布 | 5 天 |
| **PR-V3-LLM-COST** | Token 用量统计 + 月度成本报告（按租户）| 3 天 |
| **PR-V3-LLM-SAFETY** | 输入/输出安全过滤（敏感词 / 个人信息 / 越狱攻击 / Prompt Injection 防御） | 5 天 |
| **PR-V3-WENXIN** | 文心一言 / ERNIE 原生 Provider（百度专有协议，不支持 OpenAI 兼容） | 2 天 |

### 3.3 v1.0 后续 PR

| PR | 内容 |
|---|---|
| PR-V1.0-LLM-VLLM | vLLM Provider（医院私有 GPU 集群部署） |
| PR-V1.0-LLM-RAG | RAG 检索增强（向量库 + Reranker，对接现有 graph 引擎） |
| PR-V1.0-LLM-FT | 医院私有模型微调流水线（医学知识库 + LoRA） |

---

## 4. 影响

### 4.1 对客户的影响

| 客户类型 | 部署形态 | 推荐 LLM 配置 |
|---|---|---|
| 三甲医院（数据极敏感） | 内网 + 信创 | OLLAMA_LOCAL（本地 GPU + Qwen2-72B） |
| 三甲医院（一般） | 内网 | QIANWEN + DEEPSEEK（公网 API，VPN 出口） |
| 集团医院 | 内网双活 | OLLAMA_LOCAL + 国产 API 降级 |
| 中小医院 / SaaS | 外网 | QIANWEN + DEEPSEEK + KIMI + ZHIPU |
| 国央企（涉密） | 内网 + 信创 + 涉密网 | OLLAMA_LOCAL only（禁止公网调用） |

### 4.2 对 Dify 的处置

- **Dify Provider 保留**（向后兼容已有的 DifyWorkflowTemplate 用户）
- **默认禁用**（application.yml 默认 `medkernel.dify.enabled=false`）
- **仅 `WORKFLOW` 调用类型走 Dify**（真正需要多步工作流的场景）
- **建议**：未来 v1.0 后用 LangChain4j / 自实现 PromptChain 替换 Dify workflow

### 4.3 对成本的影响

| 维度 | 走 Dify Cloud | 走 OpenAICompatibleProvider 直连国产 | 节省 |
|---|---|---|---|
| API 调用单价 | Dify 加价 20-30% | 0 加价（直接付国产厂商） | 20-30% |
| 运维负担 | Dify 私有化部署需 1-2 人维护 | 0 额外人力（在主项目内） | 40-80 万 / 年 |
| 数据出境合规 | 必须做评估 30-60 天 + ¥10-30 万 | 0（直连国内）| ¥10-30 万 + 项目延期成本 |

### 4.4 风险

- 配置错误的 baseUrl/apiKey → 启动时 Provider 标 NOT_READY，降级链自动跳过；不影响系统启动
- 国产大模型 API 变更协议 → 一个文件修复（OpenAICompatibleProvider），影响范围可控
- 本地 Ollama 推理慢 → timeout-ms 配 30s，且 RESEARCH/EXTRACT 优先走 Cloud，OLLAMA 仅在 Cloud 全失败时降级

---

## 5. 不变量

本 ADR 落地后，新增以下不变量（写入 PRODUCT_ARCHITECTURE_FINAL §9）：

| # | 不变量 | 检测方式 |
|---|---|---|
| A6 | **不允许在业务代码中直接 new RestTemplate 调 LLM**，必须走 `ModelGatewayService.invoke()` | `verify-pr.ps1` 加 grep |
| A7 | **不允许把 LLM apiKey 写到 application.yml**（必须用环境变量 ${XXX_API_KEY:}） | `verify-pr.ps1` 加 grep |
| A8 | **不允许在前端代码中直接调 LLM 厂商 endpoint**（必须经后端 ModelGateway） | ESLint 规则 |
| A9 | **Dify enable 默认为 false**，启用必须有 ADR 说明业务必要性 | `verify-pr.ps1` 加 application.yml 检查 |

---

## 6. 拒绝的替代方案

| 方案 | 拒绝理由 |
|---|---|
| 每个国产大模型一个 Provider 类 | 重复代码；OpenAI 兼容协议覆盖 80%，没必要 |
| 完全去 Dify（删除 DifyModelProvider） | 破坏向后兼容；WORKFLOW 调用类型还需要 |
| 直接对接 OpenAI/Claude/Gemini 境外大模型 | 数据出境合规风险，医疗场景禁止 |
| 用 LangChain4j 框架替代手写 | 框架重，启动慢；当前用例简单单次调用不需要 |
| 等到 v1.0 再做 | 当前 Dify 强依赖直接影响数据合规和销售，必须 v0.3 立即做 |

---

**End of ADR-0013.**
