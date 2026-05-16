# 医疗智能引擎产品化方案与 AI 开发编排

## 1. 文档定位

本文是后续多 AI、多开发者共同参与本项目的总纲文档。任何新增能力、重构、测试、部署和验收都应先对齐本文约定，再进入具体任务卡。

如果是刚接手项目的 AI，先读更短的执行入口：

```text
zy-engine-mvp/docs/AI接手执行手册.md
```

前端配置、功能演示、规则校验和可视化验收的详细方案见：

```text
zy-engine-mvp/docs/前端配置平台规划与开发验证.md
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
+ 配置发布与运维审计中心
+ 前端配置与演示校验平台
```

核心原则：

- Oracle/关系型数据库是配置、版本、审计和业务运行记录的主数据源。
- Neo4j 是图谱查询投影，不是唯一主数据源。
- Dify 是 AI 工作流执行目标，不保存核心业务状态。
- HIS、EMR、LIS、PACS、医保等院内系统是外部数据源，必须通过适配器接入。
- 路径、规则、图谱、Dify 模板、字典、适配器均是可版本化、可发布、可回滚的配置资产。
- 没有 Neo4j、没有 Dify 时，DB-only 模式必须仍可完成核心业务验收。
- 前端必须让配置、演示、规则校验、质控运营和运维追溯可视化，不能只依赖接口文档或 Postman 验收。

## 3. 世界级参考标准

后续设计不机械照搬外部标准，但原则上向这些成熟做法靠拢：

- HL7 FHIR：医疗数据交换资源模型和 REST API 思路。参考：`https://hl7.org/fhir/overview.html`
- SMART on FHIR：医疗应用和 EHR 安全启动、授权、上下文传递。参考：`https://build.fhir.org/ig/HL7/smart-app-launch/branches/main/conformance.html`
- CDS Hooks：在医嘱、病历、开单等工作流节点嵌入临床决策支持。参考：`https://cds-hooks.hl7.org/1.0/`
- HL7 CQL：临床质量和决策支持表达语言。参考：`https://cql.hl7.org/`
- IHE Profiles：用角色、事务、内容约束描述真实医疗集成场景。参考：`https://www.ihe.net/resources/profiles/`
- SNOMED CT：临床术语标准化思路。参考：`https://www.nlm.nih.gov/healthit/snomedct/snomed_overview.html`
- LOINC：检验、观测、文书编码标准化思路。参考：`https://loinc.org/`
- ISO 13485：医疗软件质量管理、设计控制、验证确认和变更控制思路。参考：`https://www.iso.org/iso-13485-medical-devices.html`
- OWASP ASVS：Web/API 安全验证要求。参考：`https://owasp.org/www-project-application-security-verification-standard/`
- NIST CSF：网络安全风险管理框架。参考：`https://www.nist.gov/publications/nist-cybersecurity-framework-csf-20`

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

### 8.6 前端配置与演示平台

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

- 属于哪个业务场景？
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

### 11.4 每批代码 Definition of Done

每批 AI 开发完成前必须满足：

- `git status -sb` 确认范围清晰。
- 只提交本任务相关文件。
- `scripts/run-tests.ps1` 通过。
- `scripts/build.ps1` 通过。
- `git diff --check` 通过。
- 新增接口有契约测试。
- 新增配置有样例 JSON 或 API 示例。
- README 或 docs 已更新。
- 不引入 Neo4j/Dify 强依赖。
- 不硬编码医院逻辑。

## 12. 当前优先任务池

### PKG-001 配置包统一模型

目标：统一路径、规则、图谱、Dify、字典、适配器的包生命周期。

当前进展：

- 第一批已落地配置包内存态模型、导入、列表、详情、review、hash 校验、publish、export、审计、样例和契约测试。
- 第二批已接入组织目录：配置包增加 `tenant_id`，列表可按组织范围过滤，review/publish 会校验 `scope_level/scope_code` 是否存在；`PLATFORM/DEFAULT` 仍作为系统内置默认基线。
- 后续继续补 Oracle/达梦表、跨环境导入导出、发布包回滚、同步任务和前端配置包中心。

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
- 后续继续补组织目录 Oracle 持久化、配置包组织继承/覆盖计算、路径/规则/质控接口组织隔离。

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

### RULE-001 第三方规则引擎 API

目标：支持病历质控、医保质控、医嘱安全拦截等第三方独立调用。

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

## 14. 后续维护方式

本文是活文档。每完成一个里程碑，应更新：

- 当前能力。
- 已完成任务编号。
- 下一批优先任务。
- 新增风险和边界。
- 验收口径变化。

更新本文属于 `DOC-xxx` 任务；若本文和代码行为冲突，以代码测试结果为准，但必须立即补正文档。
