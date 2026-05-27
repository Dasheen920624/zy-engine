-- MedKernel v1.0 GA · GA-ENG-API-12 模型能力网关 API（Kingbase）

CREATE TABLE IF NOT EXISTS model_capability_task (
    id                        BIGSERIAL PRIMARY KEY,
    task_id                   VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    capability_code           VARCHAR(64)   NOT NULL,
    input_hash                VARCHAR(64)   NOT NULL,
    input_summary             VARCHAR(512)  NOT NULL,
    output_content            TEXT          NULL,
    model_mode                VARCHAR(32)   NOT NULL,
    model_version             VARCHAR(64)   NULL,
    prompt_version            VARCHAR(64)   NULL,
    source_citations          VARCHAR(1024) NULL,
    confidence                DOUBLE PRECISION NULL,
    risk_level                VARCHAR(32)   NULL,
    fallback_used             BOOLEAN       NOT NULL DEFAULT FALSE,
    fallback_reason           VARCHAR(255)  NULL,
    time_cost_ms              BIGINT        NOT NULL DEFAULT 0,
    status                    VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    trace_id                  VARCHAR(128)  NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    CONSTRAINT uk_model_task_id UNIQUE (task_id)
);

CREATE INDEX idx_model_task_tenant ON model_capability_task (tenant_id, capability_code);

CREATE TABLE IF NOT EXISTS model_capability_policy (
    id                        BIGSERIAL PRIMARY KEY,
    tenant_id                 VARCHAR(64)   NOT NULL,
    capability_code           VARCHAR(64)   NOT NULL,
    route_strategy            VARCHAR(32)   NOT NULL DEFAULT 'BASEPLAY',
    desensitize_strategy      VARCHAR(64)   NOT NULL DEFAULT 'DEFAULT',
    expected_schema           TEXT          NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    CONSTRAINT uk_model_policy_tenant UNIQUE (tenant_id, capability_code)
);

COMMENT ON TABLE model_capability_task IS '模型网关调用任务表';
COMMENT ON COLUMN model_capability_task.task_id IS '任务ID';
COMMENT ON COLUMN model_capability_task.tenant_id IS '租户ID';
COMMENT ON COLUMN model_capability_task.capability_code IS '能力标识代码';
COMMENT ON COLUMN model_capability_task.input_hash IS '原始输入内容哈希';
COMMENT ON COLUMN model_capability_task.input_summary IS '脱敏后的输入内容摘要';
COMMENT ON COLUMN model_capability_task.output_content IS '模型推理或基线返回的结构化或自由文本输出内容';
COMMENT ON COLUMN model_capability_task.model_mode IS '运行模式(B0无模型,B1模型辅助,B2探索模式)';
COMMENT ON COLUMN model_capability_task.model_version IS '调用的模型名称及版本';
COMMENT ON COLUMN model_capability_task.prompt_version IS '调用的提示词版本';
COMMENT ON COLUMN model_capability_task.source_citations IS '模型生成候选引用的文献或事实文献来源';
COMMENT ON COLUMN model_capability_task.confidence IS '输出结果的可信度/置信度评分';
COMMENT ON COLUMN model_capability_task.risk_level IS '输出结果的医疗安全风险级别(LOW,MEDIUM,HIGH)';
COMMENT ON COLUMN model_capability_task.fallback_used IS '是否使用了B0无模型基线路径回退降级';
COMMENT ON COLUMN model_capability_task.fallback_reason IS '降级回退具体触发原因';
COMMENT ON COLUMN model_capability_task.time_cost_ms IS '模型推理或处理耗时（毫秒）';
COMMENT ON COLUMN model_capability_task.status IS '任务流转状态(PENDING,RUNNING,SUCCESS,FAILED,DEGRADED)';
COMMENT ON COLUMN model_capability_task.trace_id IS '追踪ID';

COMMENT ON TABLE model_capability_policy IS '场景模型路由与脱敏策略配置表';
COMMENT ON COLUMN model_capability_policy.tenant_id IS '租户ID';
COMMENT ON COLUMN model_capability_policy.capability_code IS '能力标识代码';
COMMENT ON COLUMN model_capability_policy.route_strategy IS '模型路由策略(DISABLED禁用,BASELINE基线B0,LOCAL_MODEL本地,EXTERNAL_MODEL外部)';
COMMENT ON COLUMN model_capability_policy.desensitize_strategy IS '数据脱敏策略代码';
COMMENT ON COLUMN model_capability_policy.expected_schema IS '期待输出匹配的JSON Schema结构约束';
