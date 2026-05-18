# 医疗智能引擎产品化方案与 AI 开发编排

## 1. 文档定位

本文是后续多 AI、多开发者共同参与本项目的总纲文档。任何新增能力、重构、测试、部署和验收都应先对齐本文约定，再进入具体任务卡。

原有产品能力的统一详细实施说明见：

```text
zy-engine-mvp/docs/原有产品能力详细设计与落地实施方案.md
```

如果是刚接手项目的 AI，先读更短的执行入口：

```text
zy-engine-mvp/docs/AI接手执行手册.md
```

用户授权 AI 自主开发、连续执行或“直到没有额度”时，必须遵守：

```text
zy-engine-mvp/docs/AI自主开发运行守则.md
```

顶级多角色评审、优化决策、并行开发泳道、不同能力 AI 执行边界和产品级验收剧本见：

```text
zy-engine-mvp/docs/顶级多角色评审与AI并行开发总控.md
```

业务功能、客户验收故事线和 AI 开工优先级的最终核查见：

```text
zy-engine-mvp/docs/产品功能业务核查与开工清单.md
```

多 AI 同时开发前，必须先按以下机制认领任务：

```text
zy-engine-mvp/docs/AI任务认领与并行开发机制.md
```

多 AI 开发完成后，必须先按以下机制完成质量评审、问题整改和放行：

```text
zy-engine-mvp/docs/AI开发质量门禁与评审整改机制.md
```

无公司内网 Oracle 的 AI 开发环境，必须按以下约定识别数据库并启用本地文件库：

```text
zy-engine-mvp/docs/数据库Provider与离线AI开发约定.md
```

前端配置、功能演示、规则校验和可视化验收的详细方案见：

```text
zy-engine-mvp/docs/前端配置平台规划与开发验证.md
```

全功能蓝图、来源追溯平台、规则医学文献来源和并行开发计划见：

```text
zy-engine-mvp/docs/全功能蓝图与并行开发计划.md
```

外网 AI 基准知识包、院内字典映射、Dify/无 Dify 兼容和专家审核闭环见：

```text
zy-engine-mvp/docs/AI医疗知识工厂与字典映射方案.md
```

项目目标不是交付单医院定制代码，而是建设可在集团化医院、单体医院、多院区、基层卫生所等多组织形态落地的医疗智能引擎平台。

## 2. 产品定位

产品定位：

```text
集团化医疗智能引擎平台
= 路径引擎
+ 规则引擎
+ 图谱引擎
+ 质控引擎
+ Dify/AI 工作流引擎
+ 医疗数据标准化中心
+ AI 医疗知识工厂
+ 配置发布与运维审计中心
+ 前端配置与演示校验平台
```

核心原则：

- Oracle/关系型数据库是配置、版本、审计和业务运行记录的主数据源。
- AI 离线开发可使用 `LOCAL_H2_FILE` 本地文件数据库完成等价开发验证，但 Oracle 仍是生产权威结构和最终真实落库验收目标。
- Neo4j 是图谱查询投影，不是唯一主数据源。
- Dify 是 AI 工作流执行目标，不保存核心业务状态。
- HIS、EMR、LIS、PACS、医保等院内系统是外部数据源，必须通过适配器接入。
- 路径、规则、图谱、Dify 模板、字典、适配器均是可版本化、可发布、可回滚的配置资产。
- 规则、知识、图谱证据、Dify 解释、字典映射、适配器口径和质控指标必须可追溯来源；缺来源、来源过期或来源未审核的医学/医保/质控配置不得发布。
- 外网 AI 基准版可以利用大模型持续生成医疗知识、字典映射、规则和图谱候选，但 AI 产物必须经过来源追溯、自动校验和专业人员审核后才能进入发布包。
- 院内是否具备 Dify/大模型能力不能影响核心标准化流程；Dify、外网基准包、本地规则、向量索引和人工审核都必须通过 Provider 方式接入。
- 规则引擎必须优先保证无 Neo4j、无 Dify、无大模型场景下的完整可用性；Dify、大模型和 Neo4j 只作为候选生成、解释增强、证据补全和更高准确率复核的可降级增强能力。
- 没有 Neo4j、没有 Dify 时，DB-only 模式必须仍可完成核心业务验收。
- 前端必须让配置、演示、规则校验、质控运营和运维追溯可视化，不能只依赖接口文档或 Postman 验收。

## 3. 世界级参考标准

后续设计不机械照搬外部标准，但原则上向这些成熟做法靠拢：

- HL7 FHIR：医疗数据交换资源模型和 REST API 思路。参考：`https://hl7.org/fhir/overview.html`
- SMART on FHIR：医疗应用和 EHR 安全启动、授权、上下文传递。参考：`https://hl7.org/fhir/smart-app-launch/`
- CDS Hooks：在医嘱、病历、开单等工作流节点嵌入临床决策支持。参考：`https://hl7.github.io/cds-hooks-hl7-site/2.0/`
- HL7 CQL：临床质量和决策支持表达语言。参考：`https://cql.hl7.org/`
- IHE Profiles：用角色、事务、内容约束描述真实医疗集成场景。参考：`https://www.ihe.net/resources/profiles/`
- SNOMED CT：临床术语标准化思路。参考：`https://www.nlm.nih.gov/healthit/snomedct/snomed_overview.html`
- LOINC：检验、观测、文书编码标准化思路。参考：`https://loinc.org/`
- ISO 13485：医疗软件质量管理、设计控制、验证确认和变更控制思路。参考：`https://www.iso.org/iso-13485-medical-devices.html`
- OWASP ASVS：Web/API 安全验证要求。参考：`https://owasp.org/www-project-application-security-verification-standard/`
- NIST CSF 2.0：网络安全风险管理框架。参考：`https://www.nist.gov/cyberframework`

## 4. 组织模型约定

必须支持集团化医院和多层级医疗组织：

```text
集团 group
  -> 医院 hospital
    -> 院区 campus
      -> 卫生所/站点 site
        -> 科室 department
```

所有配置、运行实例、日志、审计、指标后续必须逐步补齐以下组织维度：

```text
tenant_id
group_code
hospital_code
campus_code
site_code
department_code
```

配置选择规则采用最精确优先：

```text
科室/站点配置 > 院区配置 > 医院配置 > 集团配置 > 系统内置默认（产品基线配置）
```

AI 开发约定：

- 不允许把某家医院、某个院区、某个系统厂商逻辑硬编码进核心引擎。
- 医院差异必须沉淀为配置包、字典映射、适配器绑定或 Provider。
- 新增查询类接口时，要预留组织范围过滤能力。

## 5. 主数据与一致性原则

Oracle/关系型数据库是唯一配置主数据源。

```text
测试环境 Oracle
  -> 导出不可变配置发布包
  -> 内网环境 Oracle 导入
  -> dry-run 校验
  -> 投影到 Neo4j / 绑定到 Dify
  -> 激活版本指针
```

一致性原则：

- 业务人员不直接改 Neo4j 或 Dify 并把它当正式配置源。
- Neo4j 只承载图谱查询投影。
- Dify 只承载工作流执行。
- 正式配置、版本、hash、审批、激活状态、审计都在 Oracle。
- 发布包必须不可变，同一 `package_code + package_version` 不允许被静默覆盖。

## 6. 配置包生命周期

所有配置资产统一包化：

- 路径包：pathway package
- 规则包：rule package
- 图谱包：graph package
- Dify 契约包：workflow package
- 字典包：terminology package
- 适配器包：adapter package

统一状态：

```text
DRAFT -> REVIEWED -> PUBLISHED -> SYNCED -> ACTIVE -> RETIRED
```

发布包必须包含：

```text
package_code
package_version
scope_level
scope_code
base_version
target_version
content_hash
manifest
diff
full_snapshot
created_by
reviewed_by
approved_by
published_time
```

为什么同时需要 `diff` 和 `full_snapshot`：

- `diff` 用于配置 review 和影响分析。
- `full_snapshot` 用于灾备、跨环境导入、重建和 hash 校验。

## 7. 目标系统架构

```text
接入层
  API Gateway / 内网鉴权 / 第三方系统调用 / 管理后台 / 演示校验工作台

应用层
  路径引擎
  规则引擎
  图谱服务
  Dify 工作流服务
  质控服务
  配置发布服务
  标准化服务
  审计运维服务

Provider 层
  DB Graph Provider
  Neo4j Graph Provider
  Local Workflow Provider
  Dify Workflow Provider
  HIS/EMR/LIS/PACS/医保 Adapter Provider

数据层
  Oracle 主数据
  H2 本地文件库（AI/离线开发 Provider）
  Neo4j 图谱投影
  发布包文件
  日志与指标存储
```

当前 MVP 已开始具备 Provider 状态探测：

```http
GET /api/system/providers
```

后续 Provider 必须逐步拆成清晰接口，而不是在业务服务里直接硬编码外部依赖。

## 8. 模块边界

### 8.1 路径引擎

职责：

- 候选路径识别。
- 医生确认入径。
- 节点流转。
- 节点任务状态。
- 路径变异记录。
- 路径版本发布、回滚、diff。

边界：

- 不直接访问院内 HIS/EMR 数据库。
- 不直接执行 Dify prompt。
- 不直接写 Neo4j Cypher。

### 8.2 规则引擎

职责：

- 确定性规则执行。
- 路径入径规则。
- 病历质控规则。
- 医保质控规则。
- 医嘱安全拦截。
- 规则包 review、publish、rollback。
- 执行日志和命中率统计。

兼容性硬要求：

- 规则导入、发布、版本选择、模拟执行、第三方调用、批量评估、结果回查、执行日志、审计、来源追溯和组织隔离必须在 DB-only 模式完整可用。
- 规则条件必须优先基于标准化后的结构化事实、标准码、配置包和关系型数据库快照执行，不能把 Neo4j 查询、Dify 调用或大模型回答作为必需判断步骤。
- Neo4j 可用于图谱证据召回和候选补充；不可用时必须回退到 DB 图谱快照、已发布知识包或内置启发式，规则引擎不能失败。
- Dify/大模型可用于规则候选生成、复杂语义解释、人工审核辅助、低置信度复核和结果说明增强；不可用时必须返回确定性规则结果和可审计的降级说明。
- 高风险场景如医嘱安全拦截、医保质控、病历质控和路径入径，最终命中结论必须来自已审核发布的规则包，不能来自一次实时大模型生成。

后续必须支持第三方独立调用：

```http
POST /api/rule-engine/evaluate
POST /api/rule-engine/batch-evaluate
GET  /api/rule-engine/results/{resultId}
```

规则场景：

- `PATHWAY_ENTRY`
- `EMR_QC`
- `INSURANCE_QC`
- `ORDER_SAFETY`
- `DRUG_INDICATION`
- `EXAM_RATIONALITY`

### 8.3 图谱引擎

职责：

- 候选疾病召回。
- 证据查询。
- 图谱版本管理。
- 图谱节点、边、证据包导入。
- Oracle 图谱包同步 Neo4j。

边界：

- Oracle 保存图谱主数据。
- Neo4j 是查询投影。
- Neo4j 不可用时必须回退到 DB 图谱或内置启发式。

### 8.4 Dify/AI 工作流引擎

职责：

- 工作流契约管理。
- 参数映射。
- mock/degraded output。
- 真实 Dify 调用。
- 超时、重试、降级。
- 调用统计和回放。

边界：

- Dify 不保存路径状态。
- 测试环境只需要工作流契约和降级输出。
- 内网环境通过 binding 绑定真实 Dify app。

### 8.5 标准化中心

职责：

- 诊断标准化。
- 医嘱标准化。
- 检查检验标准化。
- 手术标准化。
- 收费项目标准化。
- 病历文书类型标准化。
- 未映射治理。
- 字典版本影响分析。

规则和路径禁止直接依赖院内原文。

错误示例：

```text
医嘱名称 = 阿司匹林肠溶片100mg
```

正确示例：

```text
standard_order_code = ASPIRIN
order_class = ANTIPLATELET
dose_group = LOW_DOSE
```

### 8.6 AI 医疗知识工厂

职责：

- 外网基准版持续接入权威医疗标准、医保政策、药品说明书、临床指南、专家共识和院内脱敏字典治理包。
- AI 生成标准概念、同义词、候选映射、规则草案、图谱断言、测试样例和配置包说明。
- 通过来源追溯、授权检查、自动质检、冲突检测和专家审核，把 AI 候选资产转为可发布配置包。
- 为无 Dify 院内环境预生成标准知识包、同义词索引、映射候选和增量治理包。
- 为有 Dify 或本地大模型的院内环境提供统一 Model Gateway 和 Provider 契约。

边界：

- AI 不是医学事实源，不能直接发布或激活临床、医保、质控高风险配置。
- Dify/大模型只生成候选和解释，不保存正式配置主数据。
- 外网 AI 不接收院内真实患者隐私；院内字典外传必须经过脱敏和授权策略。
- 未经授权、无来源、来源过期或未审核的知识不得进入交付包。

### 8.7 前端配置与演示平台

职责：

- 配置包、路径、规则、图谱、Dify、字典、适配器的可视化配置。
- 客户演示和 UAT 场景库。
- 规则校验 dry-run。
- 病历质控、医保质控、医嘱安全的可视化输入和结果展示。
- 发布 review、diff、dry-run、publish、sync、rollback 的操作入口。
- Provider 状态、同步任务、审计日志、质控指标看板。

边界：

- 前端不保存核心业务状态。
- 前端权限控制只改善体验，真正权限必须由后端校验。
- 演示和 dry-run 默认不写正式患者路径状态。
- 前端不展示数据库密码、Dify API Key 或患者完整隐私明文。

关键要求：

- 组织上下文、配置版本、规则包版本、字典版本必须在界面可见。
- 规则命中结果必须显示命中条件、证据、建议动作、traceId。
- 病历质控和医保质控结果必须可导出报告，便于客户验收和医学/医保复核。
- DB-only 环境也必须可以完成前端演示闭环。

## 9. 产品生命周期

### 9.1 产品设计

每个需求必须回答：

- 服务哪个用户角色？
- 属于哪个业务场景？
- 是否落在首批客户验收故事线内？
- 是否影响患者安全、医保合规、医疗质量？
- 是否需要医生确认？
- 是否需要组织隔离？
- 是否需要版本、审核、发布、回滚？
- 是否依赖外部 Provider？
- 是否需要配置界面、演示界面或规则校验工作台？
- DB-only 环境如何验收？

### 9.2 架构设计

每个技术方案必须说明：

- 主数据存在哪里。
- Provider 如何切换。
- 外部服务不可用时如何降级。
- 如何审计。
- 如何按组织隔离。
- 如何测试。
- 如何回滚。

### 9.3 开发实现

每批代码必须包含：

- 接口或服务实现。
- 输入校验。
- 契约测试。
- 样例 JSON 或 API 示例。
- README 或 docs 更新。
- smoke 脚本或验证说明。

### 9.4 测试验证

测试分层：

- 单元测试：规则 DSL、配置校验、diff、映射函数。
- 契约测试：MockMvc 覆盖 API。
- 集成测试：Oracle/达梦、Neo4j、Dify Provider。
- DB-only 验收：无 Neo4j、无 Dify 仍可完成核心流程。
- 回归测试：AMI 样例端到端。
- 安全测试：鉴权、越权、敏感信息、日志脱敏。

### 9.5 验收发布

产品级验收不是接口能通，而是：

- 单数据库环境可完整验收。
- 多医院、多院区、卫生所能隔离运行。
- 配置能跨环境导出导入。
- Neo4j/Dify 不可用时核心业务不崩。
- 前端可完成路径推荐、规则校验、病历质控、医保质控、医嘱安全的可视化演示。
- 院内医嘱、诊断、收费变化可治理。
- 第三方系统能独立调用规则引擎。
- 每次发布有 diff、hash、审批、回滚。
- 所有质控结果可追溯到规则版本、数据版本、字典版本。

### 9.6 运维运营

必须逐步补齐：

- `/api/system/providers`
- 配置发布状态看板。
- 同步任务看板。
- 规则命中率。
- Dify 降级率。
- 图谱召回率。
- 接口错误率。
- 审计日志查询。
- 离线部署包。
- 初始化和升级脚本。

## 10. 路线图

### P0：产品化底座

目标：让 DB-only 测试环境可完整验收，并为多医院适配打底。

任务：

- 组织模型和组织上下文。
- 配置包统一模型。
- 前端信息架构、高保真原型和演示脚本。
- 功能演示与规则校验工作台。
- 来源文档、引用片段、资产绑定和发布来源完整性检查。
- Provider 状态和 DB-only 运行模式。
- 字典标准化中心增强。
- 规则引擎第三方调用接口。
- 审计和 trace 全链路。

### P1：跨院适配

任务：

- 医嘱、诊断、检查、检验、收费映射。
- 未映射治理清单。
- 适配器按医院/院区/站点绑定。
- 病历质控规则包。
- 医保质控规则包。
- 配置继承与覆盖。

### P2：内网增强

任务：

- Oracle 图谱包同步 Neo4j。
- Dify 契约绑定真实 Dify app。
- Provider dry-run。
- 同步任务状态。
- 回滚和重试。
- 调用回放。

### P3：生产运维

任务：

- RBAC 权限。
- 接口签名和 IP 白名单。
- 脱敏日志。
- 运维看板。
- 集团/医院/院区质控指标。
- 离线部署包。
- 灾备和升级脚本。

## 11. AI 开发编排

### 11.1 AI 角色

后续多 AI 可按角色分工：

- 产品架构 AI：维护本文、拆路线图、定义验收。
- 后端实现 AI：实现 Java/Spring 接口、服务、测试。
- 数据库 AI：维护 Oracle/达梦 DDL、迁移、索引、注释。
- 集成 AI：实现 Provider、适配器、Neo4j/Dify 同步。
- QA AI：维护契约测试、集成测试、回归测试矩阵。
- 前端 AI：维护 React/TypeScript 管理台、演示校验工作台、E2E 和可访问性测试。
- 文档 AI：维护 README、API 示例、部署手册、验收手册。

### 11.2 任务命名

任务编号约定：

```text
ORG-xxx     组织模型与租户隔离
PKG-xxx     配置包和发布生命周期
TERM-xxx    标准化和字典治理
RULE-xxx    规则引擎
PATH-xxx    路径引擎
GRAPH-xxx   图谱引擎
DIFY-xxx    Dify/AI 工作流
ADAPT-xxx   第三方适配器
QC-xxx      质控指标
PROV-xxx    来源、证据、引用和可追溯性
FE-xxx      前端配置、演示校验和可视化验收
AUDIT-xxx   审计日志
OPS-xxx     运维部署
SEC-xxx     安全权限
TEST-xxx    测试与验收
DOC-xxx     文档
```

### 11.3 任务卡模板

每张任务卡必须包含：

```text
claim_id：
任务编号：
业务目标：
适用组织范围：
输入配置/样例：
接口变更：
前端页面变更：
数据模型变更：
Provider 影响：
审计要求：
降级策略：
验收标准：
测试要求：
文档要求：
风险与边界：
```

### 11.4 自主运行机制

当用户没有指定具体任务或授权 AI 连续开发时，AI 必须按 `AI自主开发运行守则.md` 运行。

自主运行记录目录：

```text
ai-dev-input/12_autonomous_runs/active
ai-dev-input/12_autonomous_runs/archive
```

执行规则：

- 先检查 `11_ai_reviews`，优先修复 `CHANGES_REQUESTED` 的 P0/P1/P2 问题。
- 再检查 `10_task_claims`，避免领取冲突写入范围。
- 没有明确任务时，按总控最高优先级和当前优先任务池选择小切片。
- 每轮自主运行必须留下 run log，记录任务选择、claim、review、验证、风险和下一步。
- 剩余额度不足时停止开新任务，优先更新 claim/review/run log 交接。
- 遇到生产凭据、真实患者数据、不可逆迁移、医学责任或架构分歧时停止并说明风险。

### 11.5 任务认领机制

多 AI 并行开发统一使用 Git claim 文件做任务锁。

认领目录：

```text
ai-dev-input/10_task_claims/active
ai-dev-input/10_task_claims/blocked
ai-dev-input/10_task_claims/archive
```

执行规则：

- 每个 AI 开发前必须创建并推送 `active/<claim_id>.md`。
- `claim_id` 必须是小切片，例如 `RULE-008-S01`，不能只写大任务编号。
- claim 必须声明 `write_scope`、`forbidden_scope`、`verification` 和 `handoff`。
- 未成功推送 claim 前，只能阅读和规划，不能改业务文件。
- active claim 的写入范围不得重叠；共享文件必须声明具体修改段落。
- 长任务超过 60 分钟应更新 `last_heartbeat`。
- 完成后必须把 claim 归档到 `archive/YYYYMMDD/`。
- 冲突、阻塞、交接按 `AI任务认领与并行开发机制.md` 执行。

### 11.6 质量门禁机制

多 AI 并行开发统一使用 `ai-dev-input/11_ai_reviews` 作为质量评审和整改闭环目录。

评审目录：

```text
ai-dev-input/11_ai_reviews/pending
ai-dev-input/11_ai_reviews/changes_requested
ai-dev-input/11_ai_reviews/approved
ai-dev-input/11_ai_reviews/archive
```

执行规则：

- 开发完成后，Builder AI 必须先把 claim 改为 `SELF_CHECK`，再创建 review。
- Reviewer AI 按需求、架构、医疗安全、数据库一致性、代码质量、测试、安全、前端体验和运维维度审查。
- 发现问题时，review 进入 `CHANGES_REQUESTED`，claim 进入 `CHANGES_REQUESTED` 或 `FIXING`。
- P0/P1/P2 开放问题未关闭时，业务代码不得正式提交、合并、发布或归档完成。
- `review_status=APPROVED` 且 `open_findings=0` 后，claim 才能进入 `READY_TO_SUBMIT`。
- 高风险任务不得由 Builder AI 自行批准；医学、医保、质控、权限、安全、数据库、发布链路必须有独立评审。
- 认领文件、心跳和评审记录可以同步到 `main`；业务代码进入主版本必须先通过质量门禁。

详细流程以 `AI开发质量门禁与评审整改机制.md` 为准。

### 11.7 数据库环境识别

每个 AI 开发前必须执行：

```powershell
.\zy-engine-mvp\scripts\detect-db-env.ps1 -BootstrapLocal
```

约定：

- `recommended_mode=ORACLE`：优先使用 Oracle，涉及 DDL/持久化时跑 Oracle DDL 和 Oracle smoke。
- `recommended_mode=LOCAL_H2`：使用 `.\zy-engine-mvp\scripts\start-local-db.ps1` 启动本地 H2 文件库，端口 `18082`。
- `IN_MEMORY_DEMO` 只用于快速演示，不作为持久化验收。
- 生产库和开发库必须分离：Oracle 是当前生产权威库；达梦、PostgreSQL、KingbaseES 是生产兼容交付库；LOCAL_H2_FILE 只作为开发本地文件库。
- 无生产库环境的 AI 修改表结构时，必须同步维护 Oracle、达梦、PostgreSQL-Kingbase、LOCAL_H2_FILE 结构文件。
- 生产库 smoke 可由有内网环境的 AI 或集成 AI 最终补跑，但无生产库 AI 不能跳过 LOCAL_H2_FILE 开发库验证。

### 11.8 每批代码 Definition of Done

每批 AI 开发完成前必须满足：

- `git status -sb` 确认范围清晰。
- 已创建并推送当前任务 claim。
- 自主运行时已创建或更新 run log。
- 已说明目标角色、业务闭环和客户验收故事线。
- 只提交本任务相关文件。
- `scripts/run-tests.ps1` 通过。
- `scripts/build.ps1` 通过。
- `git diff --check` 通过。
- 已创建质量评审记录。
- `review_status=APPROVED`。
- `open_findings=0`。
- 每完成一个明确任务，必须提交并推送到远端当前分支，保证其它 AI 可以拉取最新项目。
- 新增接口有契约测试。
- 新增配置有样例 JSON 或 API 示例。
- README 或 docs 已更新。
- 不引入 Neo4j/Dify 强依赖。
- 不硬编码医院逻辑。
- claim 和 review 已更新并归档；若未完成，状态必须是 `BLOCKED`、`HANDOFF` 或 `CHANGES_REQUESTED` 且写清原因。
- 已说明数据库模式；无 Oracle 时已用 `LOCAL_H2_FILE` 验证并标注 Oracle smoke 待集成环境补跑。
- 自主运行结束时已写明 `next_action` 和停机原因。

## 12. 当前优先任务池

### PKG-001 配置包统一模型

目标：统一路径、规则、图谱、Dify、字典、适配器的包生命周期。

当前进展：

- 第一批已落地配置包内存态模型、导入、列表、详情、review、hash 校验、publish、export、审计、样例和契约测试。
- 第二批已接入组织目录：配置包增加 `tenant_id`，列表可按组织范围过滤，review/publish 会校验 `scope_level/scope_code` 是否存在；`PLATFORM/DEFAULT` 仍作为系统内置默认基线。
- 后续继续补生产库（Oracle/达梦/PostgreSQL-Kingbase）表、开发库 LOCAL_H2_FILE 等价链路、跨环境导入导出、发布包回滚、同步任务和前端配置包中心。

产出：

- 配置包数据模型。
- 发布包 manifest。
- 包 review 接口。
- 包导出/导入接口。
- hash 校验。

### ORG-001 组织模型

目标：支持集团、医院、院区、卫生所/站点、科室。

当前进展：

- 第一批已落地组织上下文解析接口 `GET /api/system/org-context`，支持 Header/Query 传入 `tenant_id/group_code/hospital_code/campus_code/site_code/department_code`。
- 默认上下文兼容当前历史 `default/ZYHOSPITAL`，并返回配置继承顺序：科室、站点、院区、医院、集团、系统内置默认（产品基线配置）。
- 第二批已落地组织目录导入、列表、详情、树形回查和 `ORG_UNIT` DDL；`PLATFORM` 不可导入为真实组织。
- 第三批新增 `OrganizationContextService.resolveWithBody`，Header/Query/Body 三方合并（Body 优先），首先织入 `/api/rule-engine/*`：评估记录与审计写 `tenant_id/group_code/hospital_code/campus_code/site_code/department_code/scope_level/scope_code/org_source`，`GET /api/rule-engine/results` 支持按多个组织维度过滤。
- 第四批已织入路径运行、质控和审计查询：患者入径实例落组织字段，路径实例、变异、质控指标和审计日志支持 `tenantId/groupCode/hospitalCode/campusCode/siteCode/departmentCode/scopeLevel/scopeCode` 过滤。
- 第五批已织入规则配置管理：同一 `rule_code/version_no` 可在不同组织范围独立导入、发布、回查和模拟，执行日志支持组织过滤；第三方规则引擎在无专属规则时回退 legacy 基线规则。
- 后续继续补组织目录 Oracle 持久化、配置包组织继承/覆盖计算、路径配置/图谱配置/Dify 模板/适配器绑定组织隔离。

产出：

- 组织表。
- 组织上下文解析。
- 配置 scope 字段。
- 基础查询过滤。

### TERM-001 医嘱标准化

目标：解决院内医嘱名称、编码、规格变化导致规则不可用的问题。

产出：

- 医嘱映射导入。
- 医嘱标准化接口。
- 未映射医嘱清单。
- 字典版本 impact report。

### AIK-001 AI 医疗知识工厂

目标：在外网基准版本中持续接入国内外权威医疗知识，利用 AI 生成可审核的标准概念、同义词、字典映射、规则、图谱和路径候选，再打包导出给院内系统使用。

产出：

- 来源注册、授权和证据片段数据模型。
- AI 知识生产任务、模型调用记录和候选资产模型。
- 医疗知识总包 `medical_knowledge_package` manifest。
- AI 候选资产的自动质检、冲突检测和专家审核状态。
- 外网基准包到院内配置包的导出、导入、dry-run 和激活流程。

### TERM-AI-001 AI 字典候选生成与 Dify 兼容

目标：让院内字典和标准字典映射不依赖单一 AI 能力；有 Dify 时调用 Dify Provider，无 Dify 时使用外网基准包、本地规则、同义词索引、向量召回和人工审核。

产出：

- `TerminologyCandidateProvider` 抽象。
- `RuleBasedProvider`、`ExternalAiPackageProvider`、`DifyProvider`、`ManualProvider` 设计和降级策略。
- 候选映射置信度、冲突、多候选和未映射治理队列。
- 映射审核、发布、回滚和影响分析流程。
- 字典脱敏治理包导出和外网增量映射包导入流程。

### RULE-CORE-001 规则引擎 DB-only 兼容加固

目标：确保规则引擎在无 Neo4j、无 Dify、无大模型的院内环境中仍具备最高程度可用性，只有在追求更高召回率、解释质量或低置信度复核时才使用 Dify/AI 增强。

产出：

- DB-only 规则执行能力矩阵：导入、review、publish、simulate、evaluate、batch、results、audit、source trace、terminology normalize。
- 规则执行 Provider 边界：核心 DSL evaluator、terminology、source、audit、result store 必须本地可用；Graph/Dify/AI Provider 必须可降级。
- 无 Neo4j/无 Dify 测试样例和验收剧本。
- Dify/AI 增强场景清单：候选规则生成、解释增强、复杂证据补全、人工审核辅助、低置信度复核。
- 发布阻断规则：实时 AI 输出不得直接成为高风险规则命中结论。

### PROV-001 来源追溯底座

目标：让规则、知识、路径、图谱、Dify、字典、适配器和质控结论都能查到来源、引用位置、版本、审批人和适用组织范围。

产出：

- 来源文档、引用片段、资产绑定、来源审核和运行证据数据模型。
- Oracle/达梦 DDL、迁移脚本、索引和中文备注。
- 来源样例 JSON 和 API 契约草案。
- 配置包 review 的 `source_review` 检查结果。
- 缺来源、过期来源、未审核来源的发布阻断规则。
- 来源影响分析：来源变更时反查规则、路径、图谱、Dify、字典、适配器和配置包。

### RULE-001 第三方规则引擎 API

目标：支持病历质控、医保质控、医嘱安全拦截等第三方独立调用。

当前进展：

- 第一批已落地同步接口 `POST /api/rule-engine/evaluate`：支持 `scenario_code`、`rule_package_code/rule_package_version`、`rule_codes` 白名单与显式 `patient_context`，未命中规则时返回 `NO_RULES_MATCHED` 警告而非异常。
- 规则可声明 `scenario_codes` 数组同时归属多个场景；老规则按 `rule_type`（如 `SAFETY_BLOCK`→`ORDER_SAFETY`、`TIME_LIMIT_QC`→`EMR_QC`）自动推断兼容。
- 返回标准 `results[]`，包含 `rule_code/rule_name/rule_type/scenario_code/package_code/package_version/version_no/hit/severity/message/actions/evidence/missing_facts`；同时写入 `RULE_ENGINE/EVALUATE_SCENARIO` 审计与每条规则的执行日志。
- 第三方调用入口与 `/api/rules/*` 配置管理与内部演示接口解耦，便于后续灰度、限流和审计治理。
- 第二批已落地批量同步 `POST /api/rule-engine/batch-evaluate` 与结果回查 `GET /api/rule-engine/results`、`GET /api/rule-engine/results/{resultId}`：批量调用返回共享 `batch_id` 和每条独立 `result_id`，所有评估都落入容量 500 的内存环形缓冲，列表支持 `scenarioCode/packageCode/batchId/source/patientId/encounterId/limit/offset` 过滤并仅返回摘要字段。
- 第二批写入 `RULE_ENGINE/BATCH_EVALUATE_SCENARIO` 审计，详情接口包含完整 `results/warnings`，列表接口剔除大字段；Oracle/达梦持久化与异步入口留给第三批。
- ORG-001 第三批已把组织上下文织入 `/rule-engine/evaluate` 与 `/batch-evaluate`：通过 `OrganizationContextService.resolveWithBody` Header/Query/Body 三方合并（Body 优先），评估记录与审计明细均落 `tenant_id/group_code/hospital_code/campus_code/site_code/department_code/scope_level/scope_code/org_source`；列表查询新增 `tenantId/groupCode/hospitalCode/campusCode/siteCode/departmentCode/scopeLevel/scopeCode` 过滤，集团化医院/多院区聚合复盘开箱可用。
- DB-ORG-001 已补齐 Oracle/达梦表结构与应用写入：`PE_VARIATION_RECORD`、`RE_RULE_EXEC_LOG`、`ENGINE_AUDIT_LOG` 增加组织字段和索引，Oracle 迁移脚本 `zyengine_org_context_migration.sql` 可重复执行，`run-oracle-org-smoke.cmd` 可通过 API + SQLPlus 校验规则定义、规则执行日志、审计日志真实落 Oracle。

产出：

- `/api/rule-engine/evaluate`
- `/api/rule-engine/batch-evaluate`
- 标准结果模型。
- 场景码。
- 执行日志。

### GRAPH-001 图谱包发布与同步

目标：Oracle 保存图谱主数据，Neo4j 作为投影。

产出：

- 图谱包 review。
- 图谱 active version。
- Neo4j dry-run。
- Neo4j idempotent sync。
- 回滚 active 指针。

### DIFY-001 Dify 契约绑定

目标：测试环境维护契约，内网环境绑定真实 Dify app。

产出：

- workflow contract。
- environment binding。
- binding health。
- 调用回放。

### FE-001 前端信息架构与高保真原型

目标：把管理台、配置中心、演示校验、发布 review 和质控运维的页面结构先定下来。

产出：

- 页面清单。
- 导航结构。
- 高保真原型。
- AMI 演示脚本。
- 病历质控、医保质控、医嘱安全演示脚本。
- 空态、加载态、错误态说明。

### FE-002 前端工程脚手架

目标：建立 React + TypeScript + Ant Design/ProComponents 管理台工程。

产出：

- 路由、Layout、菜单。
- API client 和统一错误处理。
- Provider 状态页。
- mock 数据。
- 基础构建和测试脚本。

### FE-003 功能演示与规则校验工作台

目标：让质控办、医保办、临床专家、实施人员可以不用 Postman，直接在界面运行路径推荐、规则校验、病历质控、医保质控和医嘱安全 dry-run。

产出：

- 演示场景库。
- 患者上下文构建器。
- 规则包和版本选择。
- `EMR_QC`、`INSURANCE_QC`、`ORDER_SAFETY` 运行入口。
- 命中条件、证据、建议动作、标准化差异、traceId 展示。
- 演示报告导出。

## 13. 质量和安全红线

- 临床建议必须可解释，并且必须由医生最终确认。
- 医保质控和病历质控结果必须保留规则版本和输入快照。
- 不允许日志输出数据库密码、Dify API Key、患者完整隐私明文。
- 不允许外部系统直接传任意 SQL/Cypher 到核心引擎执行。
- 不允许外部 Provider 不可用导致路径核心状态不可用。
- 不允许配置静默覆盖已发布版本。
- 不允许绕过审计执行发布、回滚、同步。
- 不允许缺来源、来源过期或来源未审核的医学/医保/质控配置进入发布和激活。

## 14. 后续维护方式

本文是活文档。每完成一个里程碑，应更新：

- 当前能力。
- 已完成任务编号。
- 下一批优先任务。
- 新增风险和边界。
- 验收口径变化。

更新本文属于 `DOC-xxx` 任务；若本文和代码行为冲突，以代码测试结果为准，但必须立即补正文档。
