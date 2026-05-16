# 专科诊疗路径三大引擎 MVP

本目录是“专科诊疗路径智能管理平台”的后端最小可运行工程，已按 **JDK 1.8** 兼容方式搭建。当前重点不是一次性做完所有业务，而是把后续开发需要的工程骨架、数据库表、接口链路和中文备注规范先稳住。

## 当前能力

- 路径引擎：路径草稿、版本发布、候选路径识别、医生确认入径、节点流转、节点任务状态、路径变异记录；变异支持跨实例查询和按变异类型/路径/节点/患者四维聚合统计，为质控看板提供基础数据。
- 路径运行组织上下文：`/patient-pathways/admit` 支持 Header/Query/Body 合并组织上下文并写入实例；`/pathway-instances`、`/pathway-variations`、`/quality/metrics`、`/audit-logs` 支持按 tenant/集团/医院/院区/站点/科室/scope 显式过滤。
- 路径配置校验：导入路径时校验路径编码、版本、节点、任务、任务数据源和流转目标，错误配置返回 `VALIDATION_ERROR`。
- 路径配置回查：支持查询路径清单和指定版本配置原文，便于配置页面预览和验收追溯。
- 路径版本差异对比：`GET /pathways/{pathwayCode}/diff?from=1.0.0&to=draft` 输出元数据字段变化、节点增删改、任务与流转目标的增删，配置发布前的 review 可一眼看到变更规模。
- 规则引擎：AMI/STEMI 候选规则、时限质控规则骨架、安全拦截规则骨架；支持规则包导入、包级审核和批量发布；执行日志可按 `ruleCode/traceId/patientId/hit/resultStatus` 过滤查询，最近 500 条保留在内存环形缓冲，并支持 `/exec-logs/summary` 按规则/严重级别/结果状态聚合 hit_rate 与 average_elapsed_ms。
- 第三方规则引擎入口：`POST /api/rule-engine/evaluate` 按 `scenario_code` 路由（`PATHWAY_ENTRY/EMR_QC/INSURANCE_QC/ORDER_SAFETY/DRUG_INDICATION/EXAM_RATIONALITY`），可选 `rule_package_code/rule_package_version/rule_codes` 过滤，输出 `evaluated_count/hit_count/elapsed_ms/results/warnings` 标准信封，未命中规则时返回 `NO_RULES_MATCHED` 警告而非异常，调用入口写入 `RULE_ENGINE/EVALUATE_SCENARIO` 审计。
- 第三方规则引擎批量与回查：`POST /api/rule-engine/batch-evaluate` 共享 scenario 过滤，每条 `items` 返回独立 `result_id` 并写入 `RULE_ENGINE/BATCH_EVALUATE_SCENARIO`；`GET /api/rule-engine/results` 支持 `scenarioCode/packageCode/batchId/source/patientId/encounterId/limit/offset` 过滤返回摘要，`GET /api/rule-engine/results/{resultId}` 返回完整 envelope；评估在内存环形缓冲（容量 500）保留最近调用，未找到 result_id 返回 `VALIDATION_ERROR`。
- 第三方规则引擎组织上下文：`/api/rule-engine/evaluate` 与 `/batch-evaluate` 通过 `OrganizationContextService.resolveWithBody` 合并 Header/Query/Body（Body 优先），评估记录与审计同时记录 `tenant_id/group_code/hospital_code/campus_code/site_code/department_code/scope_level/scope_code/org_source`；`GET /api/rule-engine/results` 新增 `tenantId/groupCode/hospitalCode/campusCode/siteCode/departmentCode/scopeLevel/scopeCode` 过滤项，集团化复盘开箱可用。
- 图谱引擎：候选疾病召回、证据查询、Neo4j 可配置查询和不可用时降级返回；图谱版本/证据/节点/关系均可注册与查询，Neo4j 关闭时 `disease-candidates` 与 `evidence` 接口优先使用已注册节点+关系召回（`graph_source=REGISTERED_FALLBACK`），未注册才回退到内置 AMI 启发式。
- Dify 适配：保留工作流调用入口，支持配置真实 Dify 调用、超时降级、重试和审计记录；工作流模板可导入并绑定 `required_inputs`、`input_mappings`、`input_defaults`、`timeout_ms`、`retry_count`、`degraded_outputs`，调用时按模板抽取上下文、补全缺省入参并在缺失必填字段时返回 `VALIDATION_ERROR`。
- 字典映射：提供第三方系统编码到平台标准概念的标准化接口，未命中项会返回待治理状态；支持映射配置导入、列表与版本回查，校验失败返回 `VALIDATION_ERROR`。
- 适配器中心：提供 REST/SQL/WebService 风格的第三方取数 Mock，返回统一行集和标准码映射结果；支持适配器查询定义导入、列表与单项查询，导入未内置 Mock 的查询会返回 `SUCCESS` 但 `row_count=0`，提示后续接入真实取数。
- 配置包中心：支持配置包导入、列表、详情、review、hash 校验、组织范围校验、发布、导出和审计；hash 不一致或组织范围不存在的包会阻断发布，同一 `tenant_id + package_code + package_version` 内容不同不会静默覆盖。
- 组织上下文：支持 `tenant_id/group_code/hospital_code/campus_code/site_code/department_code` 解析，默认兼容 `default/ZYHOSPITAL`，并返回配置继承顺序。
- 组织目录：支持集团、医院、院区、卫生所/站点、科室导入、列表、详情和树形回查；`PLATFORM` 不可导入为真实组织，只作为系统内置默认基线。
- 路径任务取数：任务配置了 `source.adapter_code/query_code` 时，完成任务会自动调用适配器 Mock 并保存取数结果。
- Oracle 持久化：可选开启，已验证可写入 `ZYENGINE` 用户下的核心表。
- 运行模式探测：`GET /system/providers` 返回数据库、Neo4j、Dify 当前 Provider、ready 状态和降级原因，便于 DB-only 测试环境和内网集成环境使用同一套验收口径。

## 编码约定

所有源码、Markdown 文档、JSON 样例、YAML、SQL 均统一使用 **UTF-8**。Windows PowerShell 5.1 读取中文文件时请显式指定编码：

```powershell
Get-Content .\README.md -Encoding UTF8
```

Oracle 中文表备注和字段备注推荐执行 `db/oracle/zyengine_comments_unistr.sql`，该脚本使用 `UNISTR` 写入中文，可避免客户端字符集不一致导致备注乱码。

详细说明见：

```text
docs/编码与中文备注规范.md
```

产品化总纲、集团化医院支持方案、跨环境一致性、AI 协作开发约定见：

```text
docs/产品化方案与AI开发编排.md
```

前端配置平台、功能演示、规则校验工作台和可视化验收计划见：

```text
docs/前端配置平台规划与开发验证.md
```

其它 AI 接手执行流程见：

```text
docs/AI接手执行手册.md
```

## 推荐命令行

本机已验证可使用 PowerShell 7。后续建议使用 Windows Terminal 打开 PowerShell 7，命令为：

```powershell
pwsh
```

PowerShell 7 与 Windows 自带的 PowerShell 5.1 并存，不会替换系统组件。工程内 `scripts/*.cmd` 入口会自动优先调用 `pwsh`，如果机器未安装 PowerShell 7，则回退到 Windows PowerShell 5.1。

脚本说明见：

```text
scripts/README.md
```

## 技术栈

- JDK 1.8
- Spring Boot 2.7.18
- Maven
- Oracle JDBC `ojdbc8`
- Oracle / 达梦 DDL 预留

## 编译

如果本机 JDK 1.8 访问 Maven 仓库出现证书问题，可使用工程内置的 HTTP Maven 设置：

```powershell
.\scripts\build.cmd
```

JUnit 接口契约测试（`@SpringBootTest` + `MockMvc`）：

```powershell
.\scripts\run-tests.cmd
```

测试覆盖健康检查、规则导入/发布/模拟/执行日志、路径导入/校验/入径/节点完成、字典映射导入与标准化、适配器导入与查询、图谱 Neo4j 不可用时降级返回 AMI_STEMI 候选。CI 已加入 `Run JUnit contract tests` 步骤。

## 启动：内存演示模式

```powershell
.\scripts\start-memory.cmd
```

健康检查：

```text
GET http://localhost:18080/zy-engine/api/health
```

图谱与 Dify 降级验证：

```powershell
.\scripts\run-graph-dify-smoke.cmd
```

字典映射与第三方适配器 Mock 验证：

```powershell
.\scripts\run-terminology-adapter-smoke.cmd
```

字典映射与适配器查询定义配置导入闭环验证：

```powershell
.\scripts\run-config-import-smoke.cmd
```

## 图谱和 Dify 配置

默认内存模式不要求 Neo4j 或 Dify 可用，接口会返回可解释降级结果。需要接入真实服务时通过环境变量开启：

```powershell
$env:ZYENGINE_GRAPH_ENABLED='true'
$env:ZYENGINE_GRAPH_URI='bolt://localhost:7687'
$env:ZYENGINE_GRAPH_USERNAME='neo4j'
$env:ZYENGINE_GRAPH_PASSWORD='图谱密码'
$env:ZYENGINE_GRAPH_DATABASE='neo4j'

$env:ZYENGINE_DIFY_ENABLED='true'
$env:ZYENGINE_DIFY_BASE_URL='https://dify.example.com'
$env:ZYENGINE_DIFY_API_KEY='Dify应用API密钥'
```

## 启动：Oracle 持久化模式

不要把数据库密码写入代码库。请只在运行时通过环境变量传入：

```powershell
$env:ZYENGINE_DB_ENABLED='true'
$env:ZYENGINE_DB_URL='jdbc:oracle:thin:@//192.168.4.25:1521/ORCL'
$env:ZYENGINE_DB_USERNAME='ZYENGINE'
$env:ZYENGINE_DB_PASSWORD='数据库密码'
.\scripts\start-oracle.cmd
```

健康检查：

```text
GET http://localhost:18081/zy-engine/api/health
```

## 初始化 Oracle 表

建表脚本：

```text
db/oracle/zyengine_core_ddl_with_comments.sql
```

推荐执行方式：

```powershell
$env:ZYENGINE_DB_CONNECT='//192.168.4.25:1521/ORCL'
$env:ZYENGINE_DB_USERNAME='ZYENGINE'
$env:ZYENGINE_DB_PASSWORD='数据库密码'
.\scripts\run-oracle-ddl.cmd
```

脚本会创建核心表、索引、表备注和字段备注。表已存在时会跳过建表，不会删除已有数据。为避免中文备注乱码，脚本会在建表后再次执行 `zyengine_comments_unistr.sql` 覆盖写入中文备注。

## 核心表

- `PE_PATHWAY_DEF`：路径主定义表。
- `PE_PATHWAY_VERSION`：路径版本表。
- `PE_PATIENT_INSTANCE`：患者路径实例表。
- `PE_PATIENT_NODE_STATE`：患者节点状态表。
- `PE_PATIENT_TASK_STATE`：患者任务状态表。
- `PE_VARIATION_RECORD`：路径变异记录表。
- `PE_RECOMMENDATION_RECORD`：推荐卡片记录表。
- `ORG_UNIT`：组织目录表。
- `RE_RULE_DEF`：规则定义表。
- `RE_RULE_EXEC_LOG`：规则执行日志表。
- `TM_STANDARD_CONCEPT`：标准术语概念表。
- `TM_CONCEPT_MAPPING`：院内字典映射表。
- `ADP_ADAPTER_DEF`：第三方适配器定义表。
- `ADP_QUERY_DEF`：第三方查询定义表。
- `GE_GRAPH_VERSION`：图谱版本表。
- `ENGINE_AUDIT_LOG`：引擎审计日志表。

## AMI 样例接口

候选路径识别：

```text
POST /zy-engine/api/patient-pathways/candidates
Body: ../ai-dev-input/06_samples/sample_patient_context_ami.json
```

医生确认入径：

```text
POST /zy-engine/api/patient-pathways/admit
```

示例请求：

```json
{
  "patient_id": "P_AMI_001",
  "encounter_id": "E_AMI_001",
  "pathway_code": "AMI_STEMI",
  "version_no": "1.0.0",
  "doctor_id": "D001"
}
```

完成首节点：

```text
POST /zy-engine/api/patient-pathways/{instanceId}/nodes/AMI_CHEST_PAIN_IDENTIFY/complete
```

节点任务与变异记录：

```text
GET  /zy-engine/api/patient-pathways/{instanceId}
GET  /zy-engine/api/patient-pathways/{instanceId}/nodes/{nodeCode}
POST /zy-engine/api/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/complete
POST /zy-engine/api/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/skip
POST /zy-engine/api/patient-pathways/{instanceId}/variations
GET  /zy-engine/api/pathways
GET  /zy-engine/api/pathways/{pathwayCode}?versionNo=1.0.0
GET  /zy-engine/api/pathways/{pathwayCode}/diff?from=1.0.0&to=draft
POST /zy-engine/api/pathways/{pathwayCode}/rollback
POST /zy-engine/api/graph/disease-candidates
POST /zy-engine/api/graph/evidence
POST /zy-engine/api/graph/versions
GET  /zy-engine/api/graph/versions
GET  /zy-engine/api/graph/versions/{graphVersion}
POST /zy-engine/api/graph/versions/{graphVersion}/activate
POST /zy-engine/api/graph/evidences
GET  /zy-engine/api/graph/evidences?graphVersion=&targetCode=&targetType=&evidenceType=&limit=100
GET  /zy-engine/api/graph/evidences/{evidenceId}
POST /zy-engine/api/graph/nodes
GET  /zy-engine/api/graph/nodes?graphVersion=&type=&limit=200
POST /zy-engine/api/graph/edges
GET  /zy-engine/api/graph/edges?graphVersion=&fromCode=&toCode=&relationType=&limit=200
POST /zy-engine/api/terminology/normalize
POST /zy-engine/api/terminology/mappings
GET  /zy-engine/api/terminology/mappings
GET  /zy-engine/api/terminology/mappings/{sourceSystem}/{sourceCode}?conceptType=DIAGNOSIS
POST /zy-engine/api/adapters/query
POST /zy-engine/api/adapters/definitions
GET  /zy-engine/api/adapters/definitions
GET  /zy-engine/api/adapters/definitions/{adapterCode}/{queryCode}
POST /zy-engine/api/dify/workflows/run
POST /zy-engine/api/dify/workflows
GET  /zy-engine/api/dify/workflows
GET  /zy-engine/api/dify/workflows/stats?workflowCode=&workflowVersion=&status=&provider=&patientId=&encounterId=&limit=500
GET  /zy-engine/api/dify/workflows/{workflowCode}?workflowVersion=1.0.0
POST /zy-engine/api/config-packages
POST /zy-engine/api/config-packages/import
GET  /zy-engine/api/config-packages?tenantId=&assetType=&status=&scopeLevel=&scopeCode=
GET  /zy-engine/api/config-packages/{packageCode}/{packageVersion}?tenantId=
GET  /zy-engine/api/config-packages/{packageCode}/{packageVersion}/review?tenantId=
POST /zy-engine/api/config-packages/{packageCode}/{packageVersion}/review?tenantId=
POST /zy-engine/api/config-packages/{packageCode}/{packageVersion}/publish?tenantId=
POST /zy-engine/api/config-packages/{packageCode}/{packageVersion}/export?tenantId=
GET  /zy-engine/api/system/providers
GET  /zy-engine/api/system/org-context
POST /zy-engine/api/organizations
GET  /zy-engine/api/organizations?tenantId=&level=&parentLevel=&parentCode=&status=&limit=
GET  /zy-engine/api/organizations/tree?tenantId=&rootLevel=&rootCode=
GET  /zy-engine/api/organizations/{level}/{code}?tenantId=
GET  /zy-engine/api/rules/exec-logs?ruleCode=&traceId=&patientId=&encounterId=&resultStatus=&hit=&limit=100
GET  /zy-engine/api/rules/exec-logs/summary?ruleCode=&traceId=&patientId=&encounterId=&resultStatus=&hit=
GET  /zy-engine/api/rules/exec-logs/{logId}
GET  /zy-engine/api/rules/packages/{packageCode}/review?packageVersion=2026.05
POST /zy-engine/api/rules/packages/{packageCode}/publish
GET  /zy-engine/api/pathway-variations?pathwayCode=&patientId=&encounterId=&variationType=&nodeCode=&instanceId=&tenantId=&hospitalCode=&scopeLevel=&scopeCode=&limit=100
GET  /zy-engine/api/pathway-variations/summary?pathwayCode=&patientId=&encounterId=&variationType=&nodeCode=&instanceId=&tenantId=&hospitalCode=&scopeLevel=&scopeCode=
GET  /zy-engine/api/pathway-instances?pathwayCode=&status=&patientId=&encounterId=&currentNodeCode=&tenantId=&hospitalCode=&scopeLevel=&scopeCode=&limit=100
GET  /zy-engine/api/pathway-instances/summary?pathwayCode=&status=&patientId=&encounterId=&currentNodeCode=&tenantId=&hospitalCode=&scopeLevel=&scopeCode=
GET  /zy-engine/api/pathway-instances/node-completion?pathwayCode=&status=&patientId=&encounterId=&tenantId=&hospitalCode=&scopeLevel=&scopeCode=
GET  /zy-engine/api/pathway-instances/node-stay-duration?pathwayCode=&status=&patientId=&encounterId=&tenantId=&hospitalCode=&scopeLevel=&scopeCode=
GET  /zy-engine/api/quality/metrics?pathwayCode=&status=&patientId=&encounterId=&currentNodeCode=&workflowCode=&tenantId=&hospitalCode=&scopeLevel=&scopeCode=
GET  /zy-engine/api/audit-logs?engineType=&actionType=&targetType=&targetCode=&patientId=&encounterId=&traceId=&tenantId=&hospitalCode=&scopeLevel=&scopeCode=&limit=100
GET  /zy-engine/api/audit-logs/summary?engineType=&actionType=&targetType=&targetCode=&patientId=&encounterId=&traceId=&tenantId=&hospitalCode=&scopeLevel=&scopeCode=
```

已验证结果：

- 规则执行日志：`GET /rules/exec-logs?ruleCode=R_AMI_STEMI_CANDIDATE&hit=true` 可在 simulate/evaluate 调用后查询命中记录，`GET /rules/exec-logs/{logId}` 可获取详情。
- 规则包审核发布：`sample_ami_rules.json` 已带 `package_code/package_version`，导入后可通过 `GET /rules/packages/PKG_AMI_CORE/review?packageVersion=2026.05` 查看规则数量、状态分布和 DSL 问题，再通过 `POST /rules/packages/PKG_AMI_CORE/publish` 批量发布。
- 路径变异聚合：`GET /pathway-variations?pathwayCode=AMI_STEMI` 返回跨实例变异；`GET /pathway-variations/summary?pathwayCode=AMI_STEMI` 返回按变异类型/路径/节点/患者的计数桶，已被 JUnit 覆盖。
- 路径实例聚合：`GET /pathway-instances?pathwayCode=AMI_STEMI` 跨实例返回；`GET /pathway-instances/summary?pathwayCode=AMI_STEMI` 同时输出 `total/by_pathway_code/by_status/by_current_node/variation_total/variation_by_type`，质控看板可一次拿到在径数与变异数全景。
- 路径组织过滤：`POST /patient-pathways/admit` 写入 `tenantId/hospitalCode/campusCode/departmentCode/scopeLevel/scopeCode/orgSource`；`GET /pathway-instances?hospitalCode=HOSPITAL_ALPHA`、`GET /pathway-variations?scopeLevel=DEPARTMENT&scopeCode=DEPT_ALPHA`、`GET /quality/metrics?hospitalCode=...` 与 `GET /audit-logs?hospitalCode=...` 已覆盖组织过滤契约测试。
- 路径节点完成率：`GET /pathway-instances/node-completion?pathwayCode=AMI_STEMI` 输出每个节点的 `entered/completed/running/waiting/completion_rate` 与节点内任务的 `total/completed/skipped/pending/completion_rate`，搭配实例与变异聚合形成质控看板第一版完整指标。
- 路径节点滞留时长：`GET /pathway-instances/node-stay-duration?pathwayCode=AMI_STEMI` 输出每个节点的 `average_stay_ms/min_stay_ms/max_stay_ms/timeout_count/timeout_rate`，用于发现运行中节点卡点。
- 质控指标聚合：`GET /quality/metrics?pathwayCode=AMI_STEMI` 一次返回实例摘要、变异摘要、节点完成率、节点滞留时长和 Dify 调用统计，作为质控看板后端聚合入口。
- 候选路径：`AMI_STEMI`
- 推荐评分：`90.45`
- 置信度：`HIGH`
- 路径导入校验：错误流转目标会返回 `VALIDATION_ERROR`，不会写入草稿。
- 路径配置回查：`GET /pathways/AMI_STEMI?versionNo=1.0.0` 可返回发布配置原文。
- 路径发布回滚：`POST /pathways/AMI_STEMI/rollback` 可把当前激活发布版本切回 `target_version`，之后未指定版本号的新入径会使用回滚后的激活版本。
- 入径后首节点：`AMI_CHEST_PAIN_IDENTIFY`
- 完成首节点后当前节点：`AMI_REPERFUSION_EVAL`
- 首节点任务：`TASK_ECG` 可初始化并完成，完成时会通过 `ECG_ADAPTER/QUERY_ECG_REPORT` 自动拉取心电图 Mock 数据。
- 路径变异：任务跳过和医生主动记录均可保存原因。
- 图谱降级：`CHEST_PAIN` + `ST_ELEVATION_CONTIGUOUS_LEADS` 可返回 `AMI_STEMI` 和 `EV_AMI_001`；`sample_graph_versions.json` 中包含 2 条版本 + 3 条证据，通过 `POST /graph/versions` 与 `POST /graph/evidences` 导入后 `evidence` 接口会改返回 `REGISTERED_FALLBACK` 证据集。
- Dify 降级：未配置真实 Dify 时返回 `DEGRADED`，不影响路径核心状态。
- Dify 工作流模板：`sample_dify_workflows.json` 可通过 `POST /dify/workflows` 导入，`POST /dify/workflows/run` 调用已注册工作流时会按模板应用 `input_mappings`、补全 `input_defaults`、校验 `required_inputs`，并在降级时返回模板的 `degraded_outputs`；真实 Dify 调用失败时会按 `retry_count` 重试，当前最多 3 次。
- Dify 调用统计：`GET /dify/workflows/stats?workflowCode=WF_AMI_ENTRY_EXPLAIN` 返回 `total_calls/success_calls/degraded_calls/validation_error_calls/average_elapsed_ms`，并按工作流、状态、provider 聚合。
- 配置包生命周期：`sample_config_package.json` 可通过 `POST /config-packages` 导入，`POST /config-packages/{packageCode}/{packageVersion}/review?tenantId=TENANT_DEMO` 会返回 manifest、hash、组织范围、问题列表和 `ready_to_publish`，`POST /config-packages/{packageCode}/{packageVersion}/publish` 要求 `approved_by` 并写入审计，hash 不一致或组织范围不存在会阻断发布。
- 组织上下文：`GET /system/org-context` 默认返回 `default/ZYHOSPITAL` 医院级范围；传入 `X-Tenant-Id`、`X-Group-Code`、`X-Hospital-Code`、`X-Campus-Code`、`X-Site-Code`、`X-Department-Code` 后会返回最精确组织范围和配置继承顺序。
- 组织目录：`sample_org_units.json` 可通过 `POST /organizations` 导入，`GET /organizations/tree?tenantId=TENANT_DEMO` 返回组织树；导入 `PLATFORM` 会返回 `VALIDATION_ERROR`，避免把系统内置默认误建成真实组织。
- 字典映射：`HIS/I21.3/DIAGNOSIS` 可标准化为 `AMI_STEMI`，未知编码会返回 `UNMAPPED` 和 `PENDING_MAPPING`；`sample_dictionary_mappings.json` 可通过 `POST /terminology/mappings` 导入并经 `GET /terminology/mappings` 回查。
- 适配器 Mock：`ECG_ADAPTER`、`LIS_ADAPTER`、`HIS_ADAPTER`、`EMR_WS_ADAPTER` 可返回 AMI 样例第三方数据；`sample_adapter_definitions.json` 可通过 `POST /adapters/definitions` 导入，未内置 Mock 的 `PACS_ADAPTER/QUERY_CHEST_CT` 调用会返回 `SUCCESS` 且 `row_count=0`。
- Oracle 落表：推荐记录、患者路径实例、节点状态、任务状态、变异记录均可写入。
- 审计日志查询：`GET /audit-logs?engineType=PATHWAY` 与 `GET /audit-logs/summary?engineType=PATHWAY` 可查询内存审计环形缓冲；Oracle 开启时仍会同步写入 `ENGINE_AUDIT_LOG`。
- Provider 状态：`GET /system/providers` 可返回 `run_mode=DB_ONLY/HYBRID/FULL_INTEGRATION/IN_MEMORY_DEMO`，并说明 Neo4j/Dify 未配置时的 fallback Provider。

## 当前边界

这是可运行工程骨架，不是最终生产版本。后续开发需要继续补齐：

- 可视化路径配置器、路径 DSL 校验和版本差异对比。
- 功能演示与规则校验工作台，让质控办、医保办、临床专家和实施人员可视化运行 dry-run。
- 规则 DSL 更多操作符、热更新、规则包灰度发布和可视化模拟器。
- 院内字典映射配置界面和第三方适配器 SDK。
- 图谱证据版本管理和图谱发布流程。
- Dify 工作流业务级调用模板管理、真实 Dify 鉴权策略和更细粒度调用监控。
- 登录鉴权、租户隔离、操作审计、监控告警和国产化部署脚本。
