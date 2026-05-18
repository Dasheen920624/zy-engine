# P0开发任务清单

> 说明：本文是早期 MVP 阶段的历史任务清单，用于理解项目起点。后续产品化开发请以 `medkernel-mvp/docs/00_总入口与AI接手导航.md` 和 `medkernel-mvp/docs/02_任务台账.md` 为准；产品化总纲仅在范围不清时按需查阅。若任务编号或验收口径冲突，以 `00_总入口与AI接手导航.md` 的硬门禁和 `02_任务台账.md` 的状态为准。

## 公共基础

### COMMON-001 统一ApiResult、错误码、trace_id

输入：
- `02_api_contracts/engines.openapi.yaml`

要求：
- 实现统一返回对象。
- 实现trace_id生成和透传。
- 定义错误码：SUCCESS、VALIDATION_ERROR、DATA_MISSING、CONFIG_NOT_FOUND、ENGINE_TIMEOUT、ADAPTER_TIMEOUT、DIFY_TIMEOUT、DB_ERROR、UNKNOWN_ERROR。

验收：
- 所有接口响应包含success、code、message、trace_id。

### COMMON-002 数据库方言适配骨架

输入：
- `04_database/oracle/core_ddl.sql`
- `04_database/dm/core_ddl.sql`

要求：
- 支持db.dialect=oracle|dm。
- ID由应用层生成。
- DAO层不直接硬编码分页方言。

验收：
- 生产库 Oracle/达梦/PostgreSQL-Kingbase DDL 与开发库 LOCAL_H2_FILE DDL 均可初始化或有明确补验计划。

## 路径引擎

### PE-001 路径配置导入与发布

输入：
- `03_data_models/pathway_config.schema.json`
- `06_samples/sample_ami_pathway.json`

要求：
- 支持创建路径草稿。
- 支持发布路径版本。
- config_json保存原始配置。
- pathway_code、version_no唯一。

验收：
- AMI路径可导入并发布。

### PE-002 患者候选路径识别

输入：
- `03_data_models/patient_context.schema.json`
- `06_samples/sample_patient_context_ami.json`

要求：
- 路径引擎调用规则引擎和图谱引擎。
- 生成RecommendationCard。
- Dify不可用时可降级。

验收：
- AMI样例返回AMI_STEMI候选，score>=85。

### PE-003 医生确认入径与节点流转

要求：
- POST /api/patient-pathways/admit 创建实例。
- 同一encounter_id + pathway_code只能有一个ACTIVE实例。
- 创建第一个节点状态。
- 支持完成节点并流转下一节点。

验收：
- AMI样例可创建ACTIVE实例。

## 规则引擎

### RE-001 规则DSL解析与模拟执行

输入：
- `03_data_models/rule_dsl.schema.json`
- `06_samples/sample_ami_rules.json`

要求：
- 支持all、any、exists、not_exists、field、contains_any、in、duration_minutes_between、gt。
- 支持规则模拟接口。

验收：
- R_AMI_STEMI_CANDIDATE对AMI样例命中。

### RE-002 规则执行日志

要求：
- 每次执行写入re_rule_exec_log。
- 记录trace_id、rule_code、hit_flag、elapsed_ms、input_snapshot、output_snapshot。

验收：
- 执行规则后可查询日志。

## 图谱引擎

### GE-001 图谱候选疾病召回

输入：
- `06_samples/sample_graph_nodes_edges.json`

要求：
- 业务系统调用固定API，不传任意Cypher。
- 返回候选疾病、raw_graph_score、matched_relations、evidence_refs。

验收：
- 胸痛+ST段抬高召回AMI_STEMI。

### GE-002 证据查询

要求：
- 输入target_code返回证据列表。
- 支持graph_version。

验收：
- AMI_REPERFUSION_EVAL返回EV_AMI_001。

## Dify适配

### DIFY-001 Workflow统一调用

要求：
- 支持workflow_code、workflow_version、trace_id、inputs。
- 支持超时和降级。
- 输出JSON结构校验。

验收：
- 模拟Dify超时时返回DIFY_TIMEOUT，不影响路径候选识别。
