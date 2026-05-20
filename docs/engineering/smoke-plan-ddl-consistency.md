# DDL 一致性 Smoke 计划

## 目标

验证 Oracle / DM(DM8) / PostgreSQL / LOCAL_H2_FILE 四方言的表结构存在性。

## 使用方法

每方言执行对应段的 SELECT COUNT(*) 语句，确认返回值 >= 0（表存在且可查询）。

---

## 1. Oracle

```sql
SELECT 'org_unit' AS tbl, COUNT(*) AS cnt FROM org_unit WHERE 1=0
UNION ALL SELECT 'pe_pathway_def', COUNT(*) FROM pe_pathway_def WHERE 1=0
UNION ALL SELECT 'pe_pathway_version', COUNT(*) FROM pe_pathway_version WHERE 1=0
UNION ALL SELECT 'pe_patient_instance', COUNT(*) FROM pe_patient_instance WHERE 1=0
UNION ALL SELECT 'pe_patient_node_state', COUNT(*) FROM pe_patient_node_state WHERE 1=0
UNION ALL SELECT 'pe_patient_task_state', COUNT(*) FROM pe_patient_task_state WHERE 1=0
UNION ALL SELECT 'pe_variation_record', COUNT(*) FROM pe_variation_record WHERE 1=0
UNION ALL SELECT 'pe_recommendation_record', COUNT(*) FROM pe_recommendation_record WHERE 1=0
UNION ALL SELECT 're_rule_def', COUNT(*) FROM re_rule_def WHERE 1=0
UNION ALL SELECT 're_rule_exec_log', COUNT(*) FROM re_rule_exec_log WHERE 1=0
UNION ALL SELECT 'tm_standard_concept', COUNT(*) FROM tm_standard_concept WHERE 1=0
UNION ALL SELECT 'tm_concept_mapping', COUNT(*) FROM tm_concept_mapping WHERE 1=0
UNION ALL SELECT 'adp_adapter_def', COUNT(*) FROM adp_adapter_def WHERE 1=0
UNION ALL SELECT 'adp_query_def', COUNT(*) FROM adp_query_def WHERE 1=0
UNION ALL SELECT 'ge_graph_version', COUNT(*) FROM ge_graph_version WHERE 1=0
UNION ALL SELECT 'engine_audit_log', COUNT(*) FROM engine_audit_log WHERE 1=0
UNION ALL SELECT 'src_document', COUNT(*) FROM src_document WHERE 1=0
UNION ALL SELECT 'src_citation', COUNT(*) FROM src_citation WHERE 1=0
UNION ALL SELECT 'src_asset_binding', COUNT(*) FROM src_asset_binding WHERE 1=0
UNION ALL SELECT 'src_review_record', COUNT(*) FROM src_review_record WHERE 1=0
UNION ALL SELECT 'src_runtime_evidence', COUNT(*) FROM src_runtime_evidence WHERE 1=0
UNION ALL SELECT 'cfg_config_package', COUNT(*) FROM cfg_config_package WHERE 1=0
UNION ALL SELECT 'tm_unmapped_queue', COUNT(*) FROM tm_unmapped_queue WHERE 1=0
UNION ALL SELECT 'src_dify_template', COUNT(*) FROM src_dify_template WHERE 1=0;
```

## 2. DM (达梦)

```sql
SELECT 'org_unit' AS tbl, COUNT(*) AS cnt FROM org_unit WHERE 1=0
UNION ALL SELECT 'pe_pathway_def', COUNT(*) FROM pe_pathway_def WHERE 1=0
UNION ALL SELECT 'pe_pathway_version', COUNT(*) FROM pe_pathway_version WHERE 1=0
UNION ALL SELECT 'pe_patient_instance', COUNT(*) FROM pe_patient_instance WHERE 1=0
UNION ALL SELECT 'pe_patient_node_state', COUNT(*) FROM pe_patient_node_state WHERE 1=0
UNION ALL SELECT 'pe_patient_task_state', COUNT(*) FROM pe_patient_task_state WHERE 1=0
UNION ALL SELECT 'pe_variation_record', COUNT(*) FROM pe_variation_record WHERE 1=0
UNION ALL SELECT 'pe_recommendation_record', COUNT(*) FROM pe_recommendation_record WHERE 1=0
UNION ALL SELECT 're_rule_def', COUNT(*) FROM re_rule_def WHERE 1=0
UNION ALL SELECT 're_rule_exec_log', COUNT(*) FROM re_rule_exec_log WHERE 1=0
UNION ALL SELECT 'tm_standard_concept', COUNT(*) FROM tm_standard_concept WHERE 1=0
UNION ALL SELECT 'tm_concept_mapping', COUNT(*) FROM tm_concept_mapping WHERE 1=0
UNION ALL SELECT 'adp_adapter_def', COUNT(*) FROM adp_adapter_def WHERE 1=0
UNION ALL SELECT 'adp_query_def', COUNT(*) FROM adp_query_def WHERE 1=0
UNION ALL SELECT 'ge_graph_version', COUNT(*) FROM ge_graph_version WHERE 1=0
UNION ALL SELECT 'engine_audit_log', COUNT(*) FROM engine_audit_log WHERE 1=0
UNION ALL SELECT 'src_document', COUNT(*) FROM src_document WHERE 1=0
UNION ALL SELECT 'src_citation', COUNT(*) FROM src_citation WHERE 1=0
UNION ALL SELECT 'src_asset_binding', COUNT(*) FROM src_asset_binding WHERE 1=0
UNION ALL SELECT 'src_review_record', COUNT(*) FROM src_review_record WHERE 1=0
UNION ALL SELECT 'src_runtime_evidence', COUNT(*) FROM src_runtime_evidence WHERE 1=0
UNION ALL SELECT 'cfg_config_package', COUNT(*) FROM cfg_config_package WHERE 1=0
UNION ALL SELECT 'tm_unmapped_queue', COUNT(*) FROM tm_unmapped_queue WHERE 1=0
UNION ALL SELECT 'src_dify_template', COUNT(*) FROM src_dify_template WHERE 1=0;
```

## 3. PostgreSQL

```sql
SELECT 'org_unit' AS tbl, COUNT(*) AS cnt FROM org_unit WHERE 1=0
UNION ALL SELECT 'pe_pathway_def', COUNT(*) FROM pe_pathway_def WHERE 1=0
UNION ALL SELECT 'pe_pathway_version', COUNT(*) FROM pe_pathway_version WHERE 1=0
UNION ALL SELECT 'pe_patient_instance', COUNT(*) FROM pe_patient_instance WHERE 1=0
UNION ALL SELECT 'pe_patient_node_state', COUNT(*) FROM pe_patient_node_state WHERE 1=0
UNION ALL SELECT 'pe_patient_task_state', COUNT(*) FROM pe_patient_task_state WHERE 1=0
UNION ALL SELECT 'pe_variation_record', COUNT(*) FROM pe_variation_record WHERE 1=0
UNION ALL SELECT 'pe_recommendation_record', COUNT(*) FROM pe_recommendation_record WHERE 1=0
UNION ALL SELECT 're_rule_def', COUNT(*) FROM re_rule_def WHERE 1=0
UNION ALL SELECT 're_rule_exec_log', COUNT(*) FROM re_rule_exec_log WHERE 1=0
UNION ALL SELECT 'tm_standard_concept', COUNT(*) FROM tm_standard_concept WHERE 1=0
UNION ALL SELECT 'tm_concept_mapping', COUNT(*) FROM tm_concept_mapping WHERE 1=0
UNION ALL SELECT 'adp_adapter_def', COUNT(*) FROM adp_adapter_def WHERE 1=0
UNION ALL SELECT 'adp_query_def', COUNT(*) FROM adp_query_def WHERE 1=0
UNION ALL SELECT 'ge_graph_version', COUNT(*) FROM ge_graph_version WHERE 1=0
UNION ALL SELECT 'engine_audit_log', COUNT(*) FROM engine_audit_log WHERE 1=0
UNION ALL SELECT 'src_document', COUNT(*) FROM src_document WHERE 1=0
UNION ALL SELECT 'src_citation', COUNT(*) FROM src_citation WHERE 1=0
UNION ALL SELECT 'src_asset_binding', COUNT(*) FROM src_asset_binding WHERE 1=0
UNION ALL SELECT 'src_review_record', COUNT(*) FROM src_review_record WHERE 1=0
UNION ALL SELECT 'src_runtime_evidence', COUNT(*) FROM src_runtime_evidence WHERE 1=0
UNION ALL SELECT 'cfg_config_package', COUNT(*) FROM cfg_config_package WHERE 1=0
UNION ALL SELECT 'tm_unmapped_queue', COUNT(*) FROM tm_unmapped_queue WHERE 1=0
UNION ALL SELECT 'src_dify_template', COUNT(*) FROM src_dify_template WHERE 1=0;
```

## 4. LOCAL_H2_FILE (开发库)

```sql
SELECT 'org_unit' AS tbl, COUNT(*) AS cnt FROM org_unit WHERE 1=0
UNION ALL SELECT 'pe_pathway_def', COUNT(*) FROM pe_pathway_def WHERE 1=0
UNION ALL SELECT 'pe_pathway_version', COUNT(*) FROM pe_pathway_version WHERE 1=0
UNION ALL SELECT 'pe_patient_instance', COUNT(*) FROM pe_patient_instance WHERE 1=0
UNION ALL SELECT 'pe_patient_node_state', COUNT(*) FROM pe_patient_node_state WHERE 1=0
UNION ALL SELECT 'pe_patient_task_state', COUNT(*) FROM pe_patient_task_state WHERE 1=0
UNION ALL SELECT 'pe_variation_record', COUNT(*) FROM pe_variation_record WHERE 1=0
UNION ALL SELECT 'pe_recommendation_record', COUNT(*) FROM pe_recommendation_record WHERE 1=0
UNION ALL SELECT 're_rule_def', COUNT(*) FROM re_rule_def WHERE 1=0
UNION ALL SELECT 're_rule_exec_log', COUNT(*) FROM re_rule_exec_log WHERE 1=0
UNION ALL SELECT 'tm_standard_concept', COUNT(*) FROM tm_standard_concept WHERE 1=0
UNION ALL SELECT 'tm_concept_mapping', COUNT(*) FROM tm_concept_mapping WHERE 1=0
UNION ALL SELECT 'adp_adapter_def', COUNT(*) FROM adp_adapter_def WHERE 1=0
UNION ALL SELECT 'adp_query_def', COUNT(*) FROM adp_query_def WHERE 1=0
UNION ALL SELECT 'ge_graph_version', COUNT(*) FROM ge_graph_version WHERE 1=0
UNION ALL SELECT 'engine_audit_log', COUNT(*) FROM engine_audit_log WHERE 1=0
UNION ALL SELECT 'src_document', COUNT(*) FROM src_document WHERE 1=0
UNION ALL SELECT 'src_citation', COUNT(*) FROM src_citation WHERE 1=0
UNION ALL SELECT 'src_asset_binding', COUNT(*) FROM src_asset_binding WHERE 1=0
UNION ALL SELECT 'src_review_record', COUNT(*) FROM src_review_record WHERE 1=0
UNION ALL SELECT 'src_runtime_evidence', COUNT(*) FROM src_runtime_evidence WHERE 1=0
UNION ALL SELECT 'cfg_config_package', COUNT(*) FROM cfg_config_package WHERE 1=0
UNION ALL SELECT 'tm_unmapped_queue', COUNT(*) FROM tm_unmapped_queue WHERE 1=0
UNION ALL SELECT 'src_dify_template', COUNT(*) FROM src_dify_template WHERE 1=0
UNION ALL SELECT 'pe_recommendation_record (local-only)', COUNT(*) FROM pe_recommendation_record WHERE 1=0;
```

## 5. 预期结果

每方言返回 24 行（LOCAL_H2_FILE 多 1 行 pe_recommendation_record），所有 cnt = 0。

## 6. 表清单（25 张表）

| # | 表名 | 所属模块 | Oracle | DM | PG | H2 |
|---|------|---------|--------|----|----|-----|
| 1 | org_unit | 组织目录 | ✓ | ✓ | ✓ | ✓ |
| 2 | pe_pathway_def | 路径引擎 | ✓ | ✓ | ✓ | ✓ |
| 3 | pe_pathway_version | 路径引擎 | ✓ | ✓ | ✓ | ✓ |
| 4 | pe_patient_instance | 路径引擎 | ✓ | ✓ | ✓ | ✓ |
| 5 | pe_patient_node_state | 路径引擎 | ✓ | ✓ | ✓ | ✓ |
| 6 | pe_patient_task_state | 路径引擎 | ✓ | ✓ | ✓ | ✓ |
| 7 | pe_variation_record | 路径引擎 | ✓ | ✓ | ✓ | ✓ |
| 8 | pe_recommendation_record | 推荐引擎 | ✓ | ✓ | ✓ | ✓ |
| 9 | re_rule_def | 规则引擎 | ✓ | ✓ | ✓ | ✓ |
| 10 | re_rule_exec_log | 规则引擎 | ✓ | ✓ | ✓ | ✓ |
| 11 | tm_standard_concept | 术语服务 | ✓ | ✓ | ✓ | ✓ |
| 12 | tm_concept_mapping | 术语服务 | ✓ | ✓ | ✓ | ✓ |
| 13 | adp_adapter_def | 适配器 | ✓ | ✓ | ✓ | ✓ |
| 14 | adp_query_def | 适配器 | ✓ | ✓ | ✓ | ✓ |
| 15 | ge_graph_version | 图谱引擎 | ✓ | ✓ | ✓ | ✓ |
| 16 | engine_audit_log | 审计 | ✓ | ✓ | ✓ | ✓ |
| 17 | src_document | 来源追溯 | ✓ | ✓ | ✓ | ✓ |
| 18 | src_citation | 来源追溯 | ✓ | ✓ | ✓ | ✓ |
| 19 | src_asset_binding | 来源追溯 | ✓ | ✓ | ✓ | ✓ |
| 20 | src_review_record | 来源追溯 | ✓ | ✓ | ✓ | ✓ |
| 21 | src_runtime_evidence | 来源追溯 | ✓ | ✓ | ✓ | ✓ |
| 22 | cfg_config_package | 配置包 | ✓ | ✓ | ✓ | ✓ |
| 23 | tm_unmapped_queue | 术语服务 | ✓ | ✓ | ✓ | ✓ |
| 24 | src_dify_template | Dify 模板 | ✓ | ✓ | ✓ | ✓ |
