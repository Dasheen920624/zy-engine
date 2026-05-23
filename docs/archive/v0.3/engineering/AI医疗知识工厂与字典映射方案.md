# AI 医疗知识工厂与字典映射方案

## 1. 文档定位

本文把客户提出的两个关键问题正式纳入项目方案：

- 规则、路径、图谱和字典不能靠实施人员从零手工配置，系统需要持续吸收国内外权威医疗知识，并自动生成可审核的配置资产。
- 院内字典和标准字典映射工作量大，且不同医院是否具备 Dify/大模型能力不一致，系统必须能在有 AI、无 AI、离线和外网基准包多种模式下工作。

本文是以下模块的设计入口：

```text
AI 医疗知识工厂
来源追溯中心
标准化中心
字典映射审核台
规则/图谱/路径候选生成
Dify/AI Provider
配置包导出导入
```

核心结论：

```text
AI 不是医学事实源，也不是自动发布者。
AI 是自动化研究员、配置工程师和质检员。
所有 AI 产物必须经过来源追溯、自动校验和专业人员审核后，才能成为可发布配置。
规则引擎不能依赖实时 AI、Dify 或 Neo4j 才能完成核心判定。
```

## 2. 总体目标

外网基准版本负责持续沉淀全球和国内权威医疗知识，形成可版本化的产品基线。院内系统负责导入、dry-run、审核本地覆盖并激活使用。

```text
外网 AI 医疗知识工厂
  -> 权威来源接入
  -> AI 抽取、映射、生成
  -> 证据追溯和质量校验
  -> 专家审核
  -> 基准配置包发布
  -> 离线导出
  -> 院内系统导入
  -> 院内 dry-run
  -> 院内激活
```

平台要支持四种落地模式：

| 模式 | 医院能力 | 系统策略 |
|---|---|---|
| A 外网基准增强 | 外网基准版可接入强 AI | 在外网持续生成标准知识包、同义词包、候选规则、图谱和映射索引，导出给院内 |
| B 院内 Dify 增强 | 院内可接 Dify 或大模型 | 院内标准化中心通过 Provider 调用 Dify 生成候选映射，结果仍需回写主库审核 |
| C 院内无 Dify | 院内不能接大模型 | 使用外网预生成包、本地规则、同义词、向量索引和人工审核完成映射 |
| D 完全本地封闭 | 不能外传字典，也不能接 AI | 使用标准包和人工审核，后续可平滑切换到本地模型或 Dify Provider |

规则引擎在四种模式下都必须保留同一套核心能力：

```text
规则包导入
规则包 review/publish/rollback
规则 DSL 确定性执行
标准化结果引用
simulate/evaluate/batch
结果回查
执行日志
来源追溯
组织隔离
审计
```

Dify/大模型只增强以下环节：

```text
候选规则生成
规则解释润色
复杂来源摘要
低置信度复核
人工审核辅助
字典候选映射排序
```

不能把一次实时 Dify/大模型输出作为医嘱拦截、医保质控、病历质控或路径入径的最终命中结论。

## 3. 客户未显式提出但必须设计的能力

### 3.1 来源授权和版权治理

不同标准、指南、药品库、知识库的授权条件不同。系统不能只保存“抓到了什么”，还要保存“能不能用、能不能分发、能不能导出到院内”。

必须记录：

```text
source_code
publisher
license_type
license_scope
redistribution_allowed
commercial_use_allowed
export_allowed
expiry_date
```

授权不明确的内容只能进入内部研究态，不能进入可交付基准包。

### 3.2 脱敏和数据边界

院内真实患者数据不能进入外网 AI。字典治理允许导出的内容必须只包含术语、编码、规格、剂型、项目属性等主数据，不包含患者姓名、证件号、病历全文、检查报告全文等隐私。

导出未映射字典时必须经过脱敏策略：

```text
允许：院内字典编码、名称、类别、规格、单位、剂型、厂家、收费分类
禁止：患者标识、就诊号、完整病历文本、完整医嘱流水、医生个人敏感信息
```

### 3.3 责任边界

AI 生成的映射、规则、图谱和解释不能绕过人工审核。系统要清楚标识：

```text
AI_PROPOSED     AI 候选
AUTO_VALIDATED  自动校验通过
DOMAIN_REVIEW   专家审核中
APPROVED        专家批准
PUBLISHED       发布为配置包
ACTIVE          院内激活
```

高风险资产必须双人审核：

- 医嘱拦截。
- 用药禁忌。
- 医保拒付风险。
- 路径入径条件。
- 病历质控强制整改项。

### 3.4 变化影响分析

医学标准、医保政策、药品说明书和医院字典都会变化。系统必须支持从一个来源版本或一个标准码反查影响：

```text
来源变更 -> 影响哪些标准概念
标准概念变更 -> 影响哪些映射
映射变更 -> 影响哪些规则、路径、图谱、Dify 模板
规则变更 -> 影响哪些组织和运行场景
```

### 3.5 质量评价和回归测试

知识包不能只看“生成了多少”，必须看质量：

```text
映射准确率
未映射率
多候选冲突率
人工驳回率
规则编译通过率
规则样例通过率
来源缺失率
来源过期率
发布后回滚率
```

## 4. 权威来源接入

来源接入不应散落在代码里，应建立 `Source Registry`。

### 4.1 来源类型

```text
STANDARD_TERMINOLOGY   标准术语，如 ICD、SNOMED CT、LOINC
INSURANCE_POLICY       医保政策、医保编码、支付限制
DRUG_LABEL             药品说明书、药品监管数据
CLINICAL_GUIDELINE     临床指南、专家共识、临床路径
QUALITY_POLICY         病历质控、医疗质量控制政策
HOSPITAL_DICTIONARY    院内诊断、医嘱、检验、检查、收费等字典
VENDOR_INTERFACE_DOC   HIS/EMR/LIS/PACS/医保接口字段说明
```

### 4.2 来源元数据

```text
source_code
source_name
source_type
publisher
region
language
release_version
release_date
effective_date
expiry_date
authority_level
license_scope
fetch_method
raw_hash
parsed_hash
review_status
```

### 4.3 解析产物

原始来源解析后生成：

```text
SourceDocument      来源文档
EvidenceSpan        引用片段
SourceTable         表格结构
SourceConcept       来源中抽取的候选概念
SourceChangeSet     新旧来源版本差异
```

任何规则、映射、图谱关系都必须能反查到 `EvidenceSpan` 或人工审核记录。

## 5. AI 医疗知识工厂

### 5.1 流水线

```text
来源发现
-> 更新检测
-> 文档解析
-> 医疗概念抽取
-> 标准概念归并
-> 同义词和别名生成
-> 候选映射生成
-> 规则候选生成
-> 图谱断言生成
-> 测试样例生成
-> 自动质检
-> 专家审核
-> 发布基准知识包
```

### 5.2 AI 角色

| AI 角色 | 职责 | 输出 |
|---|---|---|
| Research Agent | 阅读指南、政策、说明书和标准源 | 结构化摘要、概念、证据片段 |
| Terminology Agent | 生成标准概念、同义词、别名和映射候选 | terminology candidates |
| Rule Agent | 从来源生成规则草案和 DSL 候选 | rule draft、test cases |
| Graph Agent | 生成疾病、药品、检查、规则和证据关系 | graph assertions |
| Critic Agent | 查找冲突、缺来源、过期来源、规则不可执行等问题 | validation findings |
| Package Agent | 汇总生成配置包 manifest、diff 和说明 | package draft |

### 5.3 模型网关

不要把系统绑死到某一个模型或 Dify。系统内部应统一通过 `Model Gateway` 调用：

```text
research_deep_model     复杂医学资料理解和规则推理
extract_fast_model      大批量结构化抽取
embedding_model         术语召回、相似匹配、同义词聚类
rerank_model            候选标准码排序
critic_model            规则审查、证据完整性审查
dify_workflow_provider  院内 Dify 工作流
local_model_provider    本地大模型或私有化模型
```

每次 AI 调用必须记录：

```text
model_provider
model_name
model_version
prompt_version
input_hash
output_hash
temperature
tool_calls
evidence_ids
review_status
```

## 6. 院内字典映射工作流

### 6.1 映射对象

第一阶段优先覆盖：

```text
DIAGNOSIS       诊断
ORDER           医嘱
DRUG            药品
LAB_ITEM        检验项目
EXAM_ITEM       检查项目
PROCEDURE       手术/操作
BILLING_ITEM    收费/医保项目
DEPARTMENT      科室
DOCUMENT_TYPE   病历文书类型
```

### 6.2 映射流程

```text
院内字典导入
-> 字段清洗和归一
-> 候选标准项召回
-> 多策略评分
-> AI/Dify 辅助判断
-> 置信度分层
-> 专业人员审核
-> 发布映射包
-> 运行时标准化
-> 未映射项持续治理
```

### 6.3 候选生成策略

同一个映射任务应综合多路候选：

```text
exact_match              精确名称和编码匹配
alias_match              同义词、商品名、简称、别名
pinyin_match             拼音、首字母、常见缩写
component_match          药品成分、剂型、规格、厂家
semantic_vector_match    向量相似度召回
policy_code_match        医保编码、收费分类、限制支付口径
source_evidence_match    来源文档证据匹配
ai_judgement             AI 或 Dify 对候选解释和排序
```

### 6.4 置信度分层

```text
score >= 0.95
  可建议自动通过，但高风险类别仍需人工审核。

0.75 <= score < 0.95
  进入人工确认队列。

score < 0.75
  进入未映射治理队列。

多候选接近
  标记 CONFLICT，必须人工选择。
```

### 6.5 映射结果结构

```text
mapping_id
hospital_code
dictionary_type
local_code
local_name
local_spec
standard_system
standard_code
standard_name
mapping_method
confidence_score
candidate_rank
evidence_ids
ai_explanation
review_status
reviewed_by
reviewed_time
package_code
package_version
```

## 7. Dify 和无 Dify 兼容

### 7.1 Provider 抽象

标准化中心内部不直接依赖 Dify，而是定义候选生成 Provider：

```text
TerminologyCandidateProvider
```

实现包括：

```text
RuleBasedProvider       内置规则、同义词、标准包索引
VectorProvider          向量召回和 rerank
DifyProvider            调用院内 Dify 工作流
ExternalAiPackageProvider  使用外网基准版预生成成果
ManualProvider          人工维护候选和确认结果
LocalModelProvider      院内私有化模型，预留
```

### 7.2 院内有 Dify

院内 Dify 只作为候选生成器，不能保存正式映射主数据。

```text
标准化中心
  -> DifyProvider
  -> Dify workflow
  -> 返回候选标准项、解释、置信度
  -> 写回 Oracle 草稿
  -> 专家审核
  -> 发布映射包
```

Dify 输入必须是脱敏字典项：

```json
{
  "dictionary_type": "DRUG",
  "local_code": "HIS_DRUG_001",
  "local_name": "拜阿司匹灵",
  "spec": "100mg",
  "dosage_form": "肠溶片",
  "candidate_standards": []
}
```

Dify 输出必须结构化：

```json
{
  "candidates": [
    {
      "standard_system": "NATIONAL_DRUG",
      "standard_code": "STD_DRUG_ASPIRIN_ENTERIC",
      "standard_name": "阿司匹林肠溶片",
      "confidence_score": 0.94,
      "reason": "商品名、成分、剂型和规格一致"
    }
  ],
  "warnings": []
}
```

### 7.3 院内无 Dify

院内无 Dify 时，仍使用同一套流程：

```text
导入外网基准知识包
-> 本地规则、同义词、向量索引生成候选
-> 低置信度进入人工审核
-> 未映射项导出为脱敏治理包
-> 外网基准版二次生成增量包
-> 院内导入、审核、激活
```

这样不会因为没有大模型而导致产品不可用，只是自动化程度降低。

对规则引擎而言，院内无 Dify 不是降级失败状态，而是必须支持的基线运行状态。系统只是不执行 AI 增强复核，仍应返回确定性规则命中、证据来源、建议动作、缺失事实、标准化差异、traceId 和审计记录。

### 7.4 完全本地封闭

如果医院不允许任何字典外传：

```text
外网仅提供标准知识包
院内只做本地匹配和人工审核
未映射项留在院内治理
未来可增加 LocalModelProvider 或院内 DifyProvider
```

业务 API 和配置包生命周期不变。

## 8. 发布包设计

新增上游总包类型：

```text
medical_knowledge_package
```

包结构：

```text
manifest.json
source_snapshot/
terminology_package/
mapping_candidate_package/
rule_candidate_package/
graph_candidate_package/
pathway_candidate_package/
workflow_template_package/
validation_cases/
review_records/
license_manifest.json
import_policy.json
```

院内导入时拆分为既有资产：

```text
terminology package
rule package
graph package
pathway package
workflow package
adapter package
```

导入规则：

- 不允许覆盖已发布同版本包。
- 必须校验 hash 和签名。
- 必须显示 diff 和影响范围。
- 必须先 dry-run，再激活。
- 必须保留院内本地覆盖配置。
- 必须可回滚。

## 9. 审核工作台

前端需要新增“AI 候选配置审核台”，重点不是聊天，而是审配置。

页面能力：

- AI 候选字典映射列表。
- 未映射治理队列。
- 多候选冲突处理。
- 映射详情和证据卡片。
- 规则候选和 DSL 预览。
- 图谱关系候选和来源证据。
- 旧版本 diff 和影响范围。
- 测试样例 dry-run。
- 通过、修改后通过、驳回、要求补证据。

不同角色审核边界：

| 角色 | 审核重点 |
|---|---|
| 临床专家 | 诊断、路径、病历质控、临床规则 |
| 药师 | 药品、用药禁忌、适应症、剂量、相互作用 |
| 检验/检查专家 | 检验检查项目、单位、参考范围、项目归类 |
| 医保办 | 医保编码、限定支付、收费项目、拒付风险 |
| 信息科/实施 | 院内编码、接口字段、适配器口径、导入发布 |

## 10. 运行时标准化

规则、路径、质控和图谱查询不能直接依赖院内原始文本。运行时必须先标准化：

```http
POST /api/terminology/normalize
```

返回至少包含：

```text
local_code
local_name
standard_system
standard_code
standard_name
mapping_status
mapping_version
confidence_score
review_status
warnings
```

未映射时必须返回：

```text
UNMAPPED_TERM
PENDING_MAPPING
```

规则引擎不能静默忽略未映射项，必须在结果、日志和治理队列中体现。

规则引擎运行时不得要求实时调用 Dify 或大模型来完成标准化后判断。若启用了 Dify/AI 增强，只能作为补充说明、候选排序或复核建议，并且必须在响应中标识增强结果是否可用、是否超时、是否降级。

## 11. 数据模型建议

第一阶段可以按以下表或等价实体推进：

```text
SOURCE_REGISTRY              来源注册
SOURCE_DOCUMENT              来源文档
SOURCE_EVIDENCE_SPAN         来源引用片段
AI_KNOWLEDGE_JOB             AI 知识生产任务
AI_KNOWLEDGE_ARTIFACT        AI 生成资产
AI_MODEL_CALL_LOG            AI/Dify 调用记录
TM_STANDARD_CONCEPT          标准概念
TM_STANDARD_ALIAS            标准同义词/别名
TM_LOCAL_DICTIONARY_IMPORT   院内字典导入批次
TM_MAPPING_CANDIDATE         映射候选
TM_MAPPING_REVIEW            映射审核记录
TM_MAPPING_PACKAGE           映射发布包
TM_UNMAPPED_QUEUE            未映射治理队列
```

## 12. 任务拆分

建议新增任务线：

| 任务 | 名称 | 目标 |
|---|---|---|
| AIK-001 | AI 医疗知识工厂总包模型 | 定义 source、artifact、job、model call、package manifest |
| SRC-001 | 来源注册与授权治理 | 来源元数据、授权、证据片段、影响分析 |
| TERM-AI-001 | AI 字典候选生成 | 候选生成 Provider、置信度、冲突和未映射队列 |
| TERM-AI-002 | 院内 Dify 映射 Provider | Dify 契约、超时降级、调用回放、结构化输出 |
| TERM-AI-003 | 无 Dify 离线映射模式 | 外网基准包、本地规则、人工审核闭环 |
| RULE-AI-001 | AI 规则候选生成 | 规则草案、DSL 编译、测试样例、证据绑定 |
| RULE-CORE-001 | 规则引擎 DB-only 兼容 | 无 Neo4j、无 Dify、无大模型时规则执行、审计、dry-run、结果回查完整可用 |
| GRAPH-AI-001 | AI 图谱断言生成 | 图谱关系候选、来源证据、冲突审查 |
| PKG-AI-001 | 医疗知识包导出导入 | 外网基准包导出、院内导入、hash、签名、dry-run |
| FE-AI-001 | AI 候选配置审核台 | 字典映射、规则、图谱、证据、diff 和审核 |
| OPS-AI-001 | 持续更新和模型评估 | 定时更新、质量指标、模型效果对比、回滚 |

第一阶段优先顺序：

```text
SRC-001
-> TERM-AI-001
-> TERM-AI-003
-> PKG-AI-001
-> FE-AI-001
-> TERM-AI-002
```

理由：先让无 Dify 的院内系统也能用外网基准包完成字典治理，再逐步增强 Dify 和规则/图谱自动生成。

## 13. 验收口径

客户演示时至少要跑通：

1. 外网基准版生成一批药品、诊断、检验标准同义词和映射候选。
2. 院内导入 HIS 字典，系统自动给出候选标准码和置信度。
3. 对低置信度、多候选冲突、未映射项进入人工审核队列。
4. 专家审核后发布映射包。
5. 规则引擎运行时展示标准化前后差异。
6. 没有 Dify 时流程仍可完成。
7. 有 Dify 时展示 Dify 生成候选、调用回放、超时降级和人工审核。
8. 导出未映射脱敏治理包，在外网基准版生成增量包，再导回院内。
9. 所有映射、规则和图谱候选都能查看来源、模型、证据和审核记录。

## 14. 安全红线

- 外网 AI 不接收院内真实患者隐私。
- AI 产物不能自动激活。
- Dify 不能保存配置主数据。
- 未经授权的来源不能进入交付包。
- 缺来源、来源过期或来源未审核的高风险资产不能发布。
- 未映射项不能被规则引擎静默忽略。
- 院内本地覆盖配置不能被基准包静默覆盖。
- 模型输出必须结构化、可追溯、可复核。

## 15. 与现有架构的关系

本文不替代现有模块，而是作为上游能力增强：

```text
AI 医疗知识工厂 -> 生成候选资产
来源追溯中心 -> 证明资产依据
标准化中心 -> 管理字典映射和未映射治理
配置包中心 -> 发布、导出、导入、回滚
规则/路径/图谱/Dify -> 使用审核后的配置资产
前端平台 -> 提供审核、diff、dry-run 和追溯界面
```

后续实现必须继续遵守：

- Oracle/关系型数据库是配置主数据源。
- Neo4j 是图谱投影。
- Dify 是工作流执行目标，不是主数据源。
- DB-only 模式必须可验收。
- 所有资产必须支持组织范围、版本、审核、发布、导出、导入和回滚。
