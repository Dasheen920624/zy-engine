# 长期内存

## 项目信息

- **项目名称**: medkernel - 专科诊疗路径智能管理平台
- **技术栈**: Java Spring Boot + Neo4j + Dify + 多数据库支持
- **数据库**: Oracle, DM (达梦), PostgreSQL, H2 (本地开发)
- **开发规范**: claim-based工作流 (claim→implement→self-check→review→push)

## 数据库架构

### 支持的数据库方言
1. **Oracle**: 生产环境，使用UNISTR编码存储中文
2. **DM (达梦)**: 国产化替代，支持UTF-8中文
3. **PostgreSQL**: 开源替代，标准SQL语法
4. **H2**: 本地开发，仅包含核心表

### 表结构统计
- Oracle/DM/PG: 40张表（包含所有模块）
- H2: 27张表（仅核心模块）

### 核心模块表
- **路径引擎**: pe_pathway_def, pe_pathway_version, pe_patient_instance, pe_patient_node_state, pe_patient_task_state, pe_variation_record, pe_recommendation_record
- **规则引擎**: re_rule_def, re_rule_exec_log
- **字典映射**: tm_standard_concept, tm_concept_mapping, tm_unmapped_queue
- **适配器**: adp_adapter_def, adp_query_def
- **图谱引擎**: ge_graph_version
- **审计日志**: engine_audit_log
- **来源追溯**: src_document, src_citation, src_asset_binding, src_review_record, src_runtime_evidence
- **配置包**: cfg_config_package
- **工作流**: wf_todo_task, wf_approval_action, wf_approval_rule
- **通知中心**: NOTIFY_NOTIFICATION, NOTIFY_TEMPLATE, NOTIFY_CHANNEL_CONFIG, NOTIFY_SUBSCRIPTION, NOTIFY_DELIVERY_LOG
- **安全模块**: sec_tenant, sec_user, sec_role, sec_permission, sec_user_role, sec_role_permission, sec_user_org_scope, sec_auth_audit_log, sec_identity_provider, sec_identity_binding, sec_user_sync_job, sec_user_sync_detail
- **患者主索引**: mpi_patient_identity, mpi_visit_identity, mpi_identity_conflict

## 开发任务状态

### MPI-001 患者主索引和就诊标识治理
- **状态**: 已完成
- **完成时间**: 2026-05-20
- **主要成果**:
  1. 创建3张新表DDL（4方言）：mpi_patient_identity, mpi_visit_identity, mpi_identity_conflict
  2. 创建3个实体类：PatientIdentity, VisitIdentity, IdentityConflict
  3. 创建MpiPersistenceService：支持患者标识、就诊标识、标识冲突的CRUD操作和冲突检测
  4. 创建MpiService：支持患者标识管理、就诊标识管理、冲突处理、适配器同步
  5. 创建MpiController：提供REST API（标识管理、冲突处理、适配器同步）
  6. 创建单元测试MpiServiceTest：覆盖核心业务逻辑

### SEC-006 院内用户体系同步
- **状态**: 已完成
- **完成时间**: 2026-05-20
- **主要成果**:
  1. 创建4张新表DDL（4方言）：sec_identity_provider, sec_identity_binding, sec_user_sync_job, sec_user_sync_detail
  2. 创建4个实体类：IdentityProvider, IdentityBinding, UserSyncJob, UserSyncDetail
  3. 扩展SecurityPersistenceService：添加IdentityProvider/IdentityBinding/SyncJob/SyncDetail CRUD方法
  4. 扩展AdapterHubService：添加HIS/EMR/OA用户同步适配器定义及Mock数据
  5. 创建UserSyncService：支持全量/增量/手动同步，通过适配器框架拉取外部用户
  6. 创建UserSyncController：提供REST API（身份源CRUD、同步触发、任务查询）

### REFIT-004 多数据库持久化和中文注释统一补齐
- **状态**: 已完成
- **完成时间**: 2026-05-20
- **主要成果**:
  1. 创建H2中文注释文件（118条注释）
  2. 创建PostgreSQL DDL文件（4个文件）
  3. 修复表结构一致性（添加pe_recommendation_record表）
  4. 验证所有数据库方言表结构一致

### PR-V2-07 路径模板编辑器（含 X6 画布）
- **状态**: 已完成
- **完成时间**: 2026-05-20
- **主要成果**:
  1. 接管 TraeAI-1 遗留实现，验证 6 DoD 全部通过
  2. 修复 UnsavedChangesGuard.tsx 缺少 React import 的 TS 错误
  3. 创建 FA-PR-V2-07-S01 验收证据（BRONZE 级）
- **核心文件**: PathwayEditor/index.tsx, PathwayEditorHeader.tsx, StageTree.tsx, NodePropertyPanel.tsx, UnsavedChangesGuard.tsx, PathwayCanvas/PathwayCanvas.tsx

### REFIT-007 已实现功能验收证据补齐和专业优化池
- **状态**: 已完成
- **完成时间**: 2026-05-20
- **主要成果**:
  1. 创建3个自动化脚本：验收证据填充（45后端+43前端）、优化池生成
  2. 88个FA文件填充真实代码数据（API端点、代码目录、DDL表、测试文件）
  3. 质量评估：59 BRONZE、31 REJECTED（0 GOLD/SILVER）
  4. 优化池83个发现：1 P0、44 P1、38 P2
- **验收脚本**: `medkernel-mvp/scripts/fill-acceptance-evidence.py`, `fill-remaining-evidence.py`, `update-optimization-pool.py`

## 工具和脚本

### 转换工具
- `/tmp/convert_comments.py`: Oracle UNISTR编码到H2中文注释转换
- `/tmp/convert_oracle_to_pg.py`: Oracle到PostgreSQL语法转换

### 比较工具
- `/tmp/compare_tables_v3.py`: 多数据库表结构比较

### 验收证据工具
- `medkernel-mvp/scripts/fill-acceptance-evidence.py`: 核心后端45个任务验收证据自动生成
- `medkernel-mvp/scripts/fill-remaining-evidence.py`: 前端/V2/文档/架构43个任务验收证据生成
- `medkernel-mvp/scripts/update-optimization-pool.py`: 从Findings生成优化任务池

## 注意事项

1. **中文编码**: Oracle使用UNISTR编码，需要转换为UTF-8
2. **数据类型差异**: NUMBER→BIGINT, VARCHAR2→VARCHAR, CLOB→TEXT
3. **H2限制**: 本地开发仅包含核心表，缺少安全模块和通知模块
4. **索引一致性**: 需要验证所有数据库方言的索引和约束完全一致