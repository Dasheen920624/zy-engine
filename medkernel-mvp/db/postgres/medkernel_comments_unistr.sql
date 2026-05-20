-- PostgreSQL 数据库中文注释脚本
-- 适用于 medkernel 核心业务表
-- 执行方式：psql -U 用户名 -d 数据库名 -f medkernel_comments_unistr.sql

-- ============================================================================
-- 路径引擎
-- ============================================================================
COMMENT ON TABLE pe_pathway_def IS '路径引擎-疾病路径主定义表，保存路径编码、名称、专科和病种等基础信息';
COMMENT ON COLUMN pe_pathway_def.id IS '主键ID，由应用层生成';
COMMENT ON COLUMN pe_pathway_def.tenant_id IS '租户ID，单院部署可固定为医院编码';
COMMENT ON COLUMN pe_pathway_def.org_code IS '医疗机构编码';
COMMENT ON COLUMN pe_pathway_def.pathway_code IS '路径编码，如AMI_STEMI';
COMMENT ON COLUMN pe_pathway_def.pathway_name IS '路径名称';
COMMENT ON COLUMN pe_pathway_def.specialty_code IS '专科编码';
COMMENT ON COLUMN pe_pathway_def.disease_code IS '关联疾病编码';
COMMENT ON COLUMN pe_pathway_def.status IS '路径状态：DRAFT/PUBLISHED/DISABLED';

COMMENT ON TABLE pe_pathway_version IS '路径引擎-路径版本表，保存每个路径版本的完整配置JSON和发布审核信息';
COMMENT ON COLUMN pe_pathway_version.pathway_code IS '路径编码';
COMMENT ON COLUMN pe_pathway_version.version_no IS '版本号';
COMMENT ON COLUMN pe_pathway_version.status IS '版本状态：DRAFT/PUBLISHED/RETIRED';
COMMENT ON COLUMN pe_pathway_version.config_json IS '路径配置DSL原文，包含阶段、节点、任务、流转、绑定规则';
COMMENT ON COLUMN pe_pathway_version.effective_time IS '版本生效时间';
COMMENT ON COLUMN pe_pathway_version.retired_time IS '版本停用时间';
COMMENT ON COLUMN pe_pathway_version.approved_by IS '审核发布人';
COMMENT ON COLUMN pe_pathway_version.approved_time IS '审核发布时间';

COMMENT ON TABLE pe_patient_instance IS '路径引擎-患者路径实例表，保存患者入径后的路径状态和当前节点';
COMMENT ON COLUMN pe_patient_instance.patient_id IS '患者ID';
COMMENT ON COLUMN pe_patient_instance.encounter_id IS '就诊ID';
COMMENT ON COLUMN pe_patient_instance.pathway_code IS '路径编码';
COMMENT ON COLUMN pe_patient_instance.version_no IS '患者实例绑定的路径版本';
COMMENT ON COLUMN pe_patient_instance.status IS '实例状态：ACTIVE/EXITED/CLOSED等';
COMMENT ON COLUMN pe_patient_instance.current_node_code IS '当前路径节点编码';
COMMENT ON COLUMN pe_patient_instance.admitted_by IS '确认入径医生或操作员';
COMMENT ON COLUMN pe_patient_instance.admission_time IS '入径时间';
COMMENT ON COLUMN pe_patient_instance.exit_time IS '退出路径时间';
COMMENT ON COLUMN pe_patient_instance.exit_reason IS '退出路径原因';
COMMENT ON COLUMN pe_patient_instance.lock_version IS '乐观锁版本号';

COMMENT ON TABLE pe_patient_node_state IS '路径引擎-患者路径节点状态表，记录每个节点进入、完成、超时等状态';
COMMENT ON COLUMN pe_patient_node_state.instance_id IS '患者路径实例ID';
COMMENT ON COLUMN pe_patient_node_state.node_code IS '节点编码';
COMMENT ON COLUMN pe_patient_node_state.node_name IS '节点名称';
COMMENT ON COLUMN pe_patient_node_state.status IS '节点状态：WAITING/RUNNING/COMPLETED/SKIPPED/TIMEOUT';
COMMENT ON COLUMN pe_patient_node_state.enter_time IS '进入节点时间';
COMMENT ON COLUMN pe_patient_node_state.complete_time IS '完成节点时间';
COMMENT ON COLUMN pe_patient_node_state.timeout_flag IS '是否超时，0否1是';

COMMENT ON TABLE pe_patient_task_state IS '路径引擎-患者节点任务状态表，记录检查、检验、表单、医嘱包、随访等任务完成情况';
COMMENT ON COLUMN pe_patient_task_state.instance_id IS '患者路径实例ID';
COMMENT ON COLUMN pe_patient_task_state.node_code IS '所属节点编码';
COMMENT ON COLUMN pe_patient_task_state.task_code IS '任务编码';
COMMENT ON COLUMN pe_patient_task_state.task_name IS '任务名称';
COMMENT ON COLUMN pe_patient_task_state.task_type IS '任务类型：FORM/LAB/EXAM/ORDER_SET/FOLLOW_UP等';
COMMENT ON COLUMN pe_patient_task_state.status IS '任务状态';
COMMENT ON COLUMN pe_patient_task_state.result_json IS '任务结果JSON快照';

COMMENT ON TABLE pe_variation_record IS '路径引擎-路径变异记录表，记录医生偏离路径、患者原因、资源限制、禁忌证等变异';
COMMENT ON COLUMN pe_variation_record.instance_id IS '患者路径实例ID';
COMMENT ON COLUMN pe_variation_record.variation_type IS '变异类型';
COMMENT ON COLUMN pe_variation_record.reason IS '变异原因说明';
COMMENT ON COLUMN pe_variation_record.operator_id IS '记录人';

COMMENT ON TABLE pe_recommendation_record IS '路径引擎-推荐卡片记录表，保存候选路径、风险预警、治疗建议等推荐结果';
COMMENT ON COLUMN pe_recommendation_record.recommendation_id IS '推荐ID';
COMMENT ON COLUMN pe_recommendation_record.scenario IS '推荐场景：PATHWAY_ENTRY/RISK_ALERT等';
COMMENT ON COLUMN pe_recommendation_record.target_code IS '推荐目标编码';
COMMENT ON COLUMN pe_recommendation_record.score IS '综合评分';
COMMENT ON COLUMN pe_recommendation_record.confidence IS '置信度';
COMMENT ON COLUMN pe_recommendation_record.action_level IS '动作级别：BLOCK/STRONG_ALERT/WEAK_ALERT/INFO';
COMMENT ON COLUMN pe_recommendation_record.card_json IS '推荐卡片完整JSON';
COMMENT ON COLUMN pe_recommendation_record.trace_id IS '调用链追踪ID';

-- ============================================================================
-- 规则引擎
-- ============================================================================
COMMENT ON TABLE re_rule_def IS '规则引擎-规则定义表，保存规则DSL、类型、版本、状态和审核信息';
COMMENT ON COLUMN re_rule_def.rule_code IS '规则编码';
COMMENT ON COLUMN re_rule_def.rule_name IS '规则名称';
COMMENT ON COLUMN re_rule_def.rule_type IS '规则类型：TIME_LIMIT_QC/CONTENT_QC/PATHWAY_NODE/SAFETY等';
COMMENT ON COLUMN re_rule_def.version_no IS '规则版本号';
COMMENT ON COLUMN re_rule_def.status IS '规则状态：DRAFT/PUBLISHED/DISABLED';
COMMENT ON COLUMN re_rule_def.severity IS '默认严重级别';
COMMENT ON COLUMN re_rule_def.rule_json IS '规则DSL原文';

COMMENT ON TABLE re_rule_exec_log IS '规则引擎-规则执行日志表，记录每次规则执行的输入、输出、命中、耗时和异常';
COMMENT ON COLUMN re_rule_exec_log.trace_id IS '调用链追踪ID';
COMMENT ON COLUMN re_rule_exec_log.rule_code IS '规则编码';
COMMENT ON COLUMN re_rule_exec_log.hit_flag IS '是否命中，0否1是';
COMMENT ON COLUMN re_rule_exec_log.message IS '规则结果说明';
COMMENT ON COLUMN re_rule_exec_log.input_snapshot IS '规则输入快照';
COMMENT ON COLUMN re_rule_exec_log.output_snapshot IS '规则输出快照';
COMMENT ON COLUMN re_rule_exec_log.elapsed_ms IS '执行耗时毫秒';
COMMENT ON COLUMN re_rule_exec_log.result_status IS '执行状态：SUCCESS/FAILED/TIMEOUT等';

-- ============================================================================
-- 字典映射
-- ============================================================================
COMMENT ON TABLE tm_standard_concept IS '字典映射-平台标准概念表，保存疾病、检验、检查、药品、文书等统一编码';
COMMENT ON COLUMN tm_standard_concept.concept_code IS '标准概念编码';
COMMENT ON COLUMN tm_standard_concept.concept_name IS '标准概念名称';
COMMENT ON COLUMN tm_standard_concept.concept_type IS '概念类型';
COMMENT ON COLUMN tm_standard_concept.status IS '状态';

COMMENT ON TABLE tm_concept_mapping IS '字典映射-第三方系统编码到平台标准概念的映射表';
COMMENT ON COLUMN tm_concept_mapping.source_system IS '来源系统，如HIS/EMR/LIS/PACS';
COMMENT ON COLUMN tm_concept_mapping.source_code IS '来源系统原始编码';
COMMENT ON COLUMN tm_concept_mapping.source_name IS '来源系统原始名称';
COMMENT ON COLUMN tm_concept_mapping.concept_type IS '概念类型';
COMMENT ON COLUMN tm_concept_mapping.standard_code IS '映射后的平台标准编码';
COMMENT ON COLUMN tm_concept_mapping.mapping_status IS '映射状态：DRAFT/APPROVED/REJECTED';
COMMENT ON COLUMN tm_concept_mapping.confidence IS '映射置信度';

-- ============================================================================
-- 适配器
-- ============================================================================
COMMENT ON TABLE adp_adapter_def IS '适配器中心-第三方系统适配器定义表，保存REST/SQL/WebService等连接配置';
COMMENT ON COLUMN adp_adapter_def.adapter_code IS '适配器编码';
COMMENT ON COLUMN adp_adapter_def.adapter_name IS '适配器名称';
COMMENT ON COLUMN adp_adapter_def.adapter_type IS '适配器类型：REST/SQL/WEBSERVICE/MQ/FILE';
COMMENT ON COLUMN adp_adapter_def.status IS '状态';
COMMENT ON COLUMN adp_adapter_def.config_json IS '连接配置JSON，敏感信息生产环境应加密或外部注入';

COMMENT ON TABLE adp_query_def IS '适配器中心-适配器查询定义表，保存具体查询模板和字段映射';
COMMENT ON COLUMN adp_query_def.adapter_code IS '适配器编码';
COMMENT ON COLUMN adp_query_def.query_code IS '查询编码';
COMMENT ON COLUMN adp_query_def.query_name IS '查询名称';
COMMENT ON COLUMN adp_query_def.query_config IS '查询配置JSON';
COMMENT ON COLUMN adp_query_def.status IS '状态';

-- ============================================================================
-- 图谱
-- ============================================================================
COMMENT ON TABLE ge_graph_version IS '图谱引擎-图谱版本元数据表，记录医学知识图谱版本发布状态';
COMMENT ON COLUMN ge_graph_version.graph_version IS '图谱版本号';
COMMENT ON COLUMN ge_graph_version.status IS '版本状态';
COMMENT ON COLUMN ge_graph_version.description IS '版本说明';
COMMENT ON COLUMN ge_graph_version.published_by IS '发布人';
COMMENT ON COLUMN ge_graph_version.published_time IS '发布时间';

-- ============================================================================
-- 审计
-- ============================================================================
COMMENT ON TABLE engine_audit_log IS '引擎公共审计日志表，记录路径、规则、图谱、Dify等引擎关键操作';
COMMENT ON COLUMN engine_audit_log.trace_id IS '调用链追踪ID';
COMMENT ON COLUMN engine_audit_log.engine_type IS '引擎类型：PATHWAY/RULE/GRAPH/DIFY/TERMINOLOGY/ADAPTER';
COMMENT ON COLUMN engine_audit_log.action_type IS '操作类型';
COMMENT ON COLUMN engine_audit_log.target_type IS '操作对象类型';
COMMENT ON COLUMN engine_audit_log.target_code IS '操作对象编码';
COMMENT ON COLUMN engine_audit_log.patient_id IS '患者ID';
COMMENT ON COLUMN engine_audit_log.encounter_id IS '就诊ID';
COMMENT ON COLUMN engine_audit_log.operator_id IS '操作人';
COMMENT ON COLUMN engine_audit_log.detail_json IS '操作详情JSON';

-- ============================================================================
-- 来源追溯
-- ============================================================================
COMMENT ON TABLE src_document IS '来源追溯-来源文档表';
COMMENT ON COLUMN src_document.document_code IS '文档编码';
COMMENT ON COLUMN src_document.title IS '文档标题';
COMMENT ON COLUMN src_document.source_type IS '来源类型';
COMMENT ON COLUMN src_document.source_uri IS '来源URI';
COMMENT ON COLUMN src_document.publisher IS '发布机构';
COMMENT ON COLUMN src_document.effective_date IS '生效日期';
COMMENT ON COLUMN src_document.expiry_date IS '失效日期';
COMMENT ON COLUMN src_document.review_status IS '审核状态';
COMMENT ON COLUMN src_document.content_hash IS '内容哈希';

COMMENT ON TABLE src_citation IS '来源追溯-引用片段表';
COMMENT ON COLUMN src_citation.citation_code IS '引用编码';
COMMENT ON COLUMN src_citation.document_code IS '关联文档编码';
COMMENT ON COLUMN src_citation.section_code IS '章节编码';
COMMENT ON COLUMN src_citation.clause_no IS '条款号';
COMMENT ON COLUMN src_citation.page_no IS '页码';
COMMENT ON COLUMN src_citation.excerpt_text IS '引用原文';
COMMENT ON COLUMN src_citation.summary_text IS '引用摘要';
COMMENT ON COLUMN src_citation.evidence_level IS '证据等级';

COMMENT ON TABLE src_asset_binding IS '来源追溯-资产绑定表';
COMMENT ON COLUMN src_asset_binding.asset_type IS '资产类型';
COMMENT ON COLUMN src_asset_binding.asset_code IS '资产编码';
COMMENT ON COLUMN src_asset_binding.asset_version IS '资产版本';
COMMENT ON COLUMN src_asset_binding.citation_code IS '引用编码';
COMMENT ON COLUMN src_asset_binding.binding_role IS '绑定角色';

COMMENT ON TABLE src_review_record IS '来源追溯-审核记录表';
COMMENT ON COLUMN src_review_record.review_code IS '审核编码';
COMMENT ON COLUMN src_review_record.target_type IS '目标类型';
COMMENT ON COLUMN src_review_record.target_code IS '目标编码';
COMMENT ON COLUMN src_review_record.target_version IS '目标版本';
COMMENT ON COLUMN src_review_record.missing_count IS '缺失来源数';
COMMENT ON COLUMN src_review_record.expired_count IS '过期来源数';
COMMENT ON COLUMN src_review_record.unreviewed_count IS '未审核来源数';
COMMENT ON COLUMN src_review_record.review_result IS '审核结果';
COMMENT ON COLUMN src_review_record.review_message IS '审核说明';

COMMENT ON TABLE src_runtime_evidence IS '来源追溯-运行时证据表';
COMMENT ON COLUMN src_runtime_evidence.trace_id IS '调用链追踪ID';
COMMENT ON COLUMN src_runtime_evidence.engine_type IS '引擎类型';
COMMENT ON COLUMN src_runtime_evidence.action_type IS '操作类型';
COMMENT ON COLUMN src_runtime_evidence.target_type IS '目标类型';
COMMENT ON COLUMN src_runtime_evidence.target_code IS '目标编码';
COMMENT ON COLUMN src_runtime_evidence.target_version IS '目标版本';
COMMENT ON COLUMN src_runtime_evidence.citation_code IS '引用编码';
COMMENT ON COLUMN src_runtime_evidence.evidence_summary IS '证据摘要';

-- ============================================================================
-- 配置包
-- ============================================================================
COMMENT ON TABLE cfg_config_package IS '配置包表';
COMMENT ON COLUMN cfg_config_package.package_code IS '配置包编码';
COMMENT ON COLUMN cfg_config_package.package_version IS '配置包版本';
COMMENT ON COLUMN cfg_config_package.asset_type IS '资产类型';
COMMENT ON COLUMN cfg_config_package.scope_level IS '作用域层级';
COMMENT ON COLUMN cfg_config_package.scope_code IS '作用域编码';
COMMENT ON COLUMN cfg_config_package.status IS '状态';
COMMENT ON COLUMN cfg_config_package.base_version IS '基准版本';
COMMENT ON COLUMN cfg_config_package.target_version IS '目标版本';
COMMENT ON COLUMN cfg_config_package.content_hash IS '内容哈希';
COMMENT ON COLUMN cfg_config_package.manifest_json IS '清单JSON';
COMMENT ON COLUMN cfg_config_package.diff_json IS '差异JSON';
COMMENT ON COLUMN cfg_config_package.full_snapshot_json IS '完整快照JSON';

-- ============================================================================
-- 组织
-- ============================================================================
COMMENT ON TABLE org_unit IS '组织单元表';
COMMENT ON COLUMN org_unit.tenant_id IS '租户ID';
COMMENT ON COLUMN org_unit.level_code IS '层级编码';
COMMENT ON COLUMN org_unit.org_code IS '组织编码';
COMMENT ON COLUMN org_unit.org_name IS '组织名称';
COMMENT ON COLUMN org_unit.parent_level_code IS '父级层级编码';
COMMENT ON COLUMN org_unit.parent_org_code IS '父级组织编码';
COMMENT ON COLUMN org_unit.status IS '状态';
COMMENT ON COLUMN org_unit.display_order IS '显示顺序';

-- ============================================================================
-- 术语治理
-- ============================================================================
COMMENT ON TABLE tm_unmapped_queue IS '术语治理-未映射队列表';
COMMENT ON COLUMN tm_unmapped_queue.queue_id IS '队列ID';
COMMENT ON COLUMN tm_unmapped_queue.source_system IS '来源系统';
COMMENT ON COLUMN tm_unmapped_queue.source_code IS '来源编码';
COMMENT ON COLUMN tm_unmapped_queue.source_name IS '来源名称';
COMMENT ON COLUMN tm_unmapped_queue.concept_type IS '概念类型';
COMMENT ON COLUMN tm_unmapped_queue.governance_status IS '治理状态';
COMMENT ON COLUMN tm_unmapped_queue.proposed_standard_code IS '建议标准编码';
COMMENT ON COLUMN tm_unmapped_queue.proposed_standard_name IS '建议标准名称';
COMMENT ON COLUMN tm_unmapped_queue.proposed_confidence IS '建议置信度';
COMMENT ON COLUMN tm_unmapped_queue.proposed_mapping_source IS '建议映射来源';
COMMENT ON COLUMN tm_unmapped_queue.occurrence_count IS '出现次数';

-- ============================================================================
-- Dify 工作流模板
-- ============================================================================
COMMENT ON TABLE src_dify_template IS 'Dify工作流模板表';
COMMENT ON COLUMN src_dify_template.workflow_code IS '工作流编码';
COMMENT ON COLUMN src_dify_template.workflow_version IS '工作流版本';
COMMENT ON COLUMN src_dify_template.workflow_name IS '工作流名称';
COMMENT ON COLUMN src_dify_template.description IS '描述';
COMMENT ON COLUMN src_dify_template.dify_app_code IS 'Dify应用编码';
COMMENT ON COLUMN src_dify_template.timeout_ms IS '超时毫秒';
COMMENT ON COLUMN src_dify_template.retry_count IS '重试次数';
COMMENT ON COLUMN src_dify_template.template_json IS '模板JSON';
COMMENT ON COLUMN src_dify_template.reference_document_code IS '参考文档编码';
COMMENT ON COLUMN src_dify_template.reference_binding_type IS '参考绑定类型';
COMMENT ON COLUMN src_dify_template.status IS '状态';

-- ============================================================================
-- 审批工作流
-- ============================================================================
COMMENT ON TABLE wf_todo_task IS '统一待办任务表';
COMMENT ON COLUMN wf_todo_task.task_code IS '任务编码';
COMMENT ON COLUMN wf_todo_task.business_type IS '业务类型';
COMMENT ON COLUMN wf_todo_task.business_code IS '业务编码';
COMMENT ON COLUMN wf_todo_task.business_version IS '业务版本';
COMMENT ON COLUMN wf_todo_task.title IS '任务标题';
COMMENT ON COLUMN wf_todo_task.description IS '任务描述';
COMMENT ON COLUMN wf_todo_task.priority IS '优先级';
COMMENT ON COLUMN wf_todo_task.status IS '状态';
COMMENT ON COLUMN wf_todo_task.assigned_type IS '分配类型';
COMMENT ON COLUMN wf_todo_task.assigned_to IS '分配给';
COMMENT ON COLUMN wf_todo_task.created_by IS '创建人';
COMMENT ON COLUMN wf_todo_task.due_time IS '截止时间';
COMMENT ON COLUMN wf_todo_task.completed_by IS '完成人';
COMMENT ON COLUMN wf_todo_task.completed_time IS '完成时间';
COMMENT ON COLUMN wf_todo_task.completed_comment IS '完成备注';
COMMENT ON COLUMN wf_todo_task.cancelled_by IS '取消人';
COMMENT ON COLUMN wf_todo_task.cancelled_time IS '取消时间';
COMMENT ON COLUMN wf_todo_task.cancel_reason IS '取消原因';

COMMENT ON TABLE wf_approval_action IS '审批动作表';
COMMENT ON COLUMN wf_approval_action.task_id IS '关联任务ID';
COMMENT ON COLUMN wf_approval_action.task_code IS '关联任务编码';
COMMENT ON COLUMN wf_approval_action.action_type IS '动作类型';
COMMENT ON COLUMN wf_approval_action.action_result IS '动作结果';
COMMENT ON COLUMN wf_approval_action.operator_id IS '操作人ID';
COMMENT ON COLUMN wf_approval_action.operator_name IS '操作人姓名';
COMMENT ON COLUMN wf_approval_action.comment IS '审批意见';
COMMENT ON COLUMN wf_approval_action.delegate_to IS '转办给';
COMMENT ON COLUMN wf_approval_action.delegate_to_name IS '转办给姓名';

COMMENT ON TABLE wf_approval_rule IS '审批规则表';
COMMENT ON COLUMN wf_approval_rule.rule_code IS '规则编码';
COMMENT ON COLUMN wf_approval_rule.rule_name IS '规则名称';
COMMENT ON COLUMN wf_approval_rule.business_type IS '业务类型';
COMMENT ON COLUMN wf_approval_rule.approval_type IS '审批类型';
COMMENT ON COLUMN wf_approval_rule.approver_type IS '审批人类型';
COMMENT ON COLUMN wf_approval_rule.approver_value IS '审批人值';
COMMENT ON COLUMN wf_approval_rule.timeout_hours IS '超时小时数';
COMMENT ON COLUMN wf_approval_rule.timeout_action IS '超时动作';
COMMENT ON COLUMN wf_approval_rule.priority IS '优先级';
COMMENT ON COLUMN wf_approval_rule.status IS '状态';
COMMENT ON COLUMN wf_approval_rule.description IS '描述';
