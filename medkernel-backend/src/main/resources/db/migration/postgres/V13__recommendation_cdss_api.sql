-- MedKernel v1.0 GA · GA-ENG-API-07 推荐/CDSS API（PostgreSQL）

CREATE TABLE IF NOT EXISTS recommendation_trigger (
    id                  BIGSERIAL PRIMARY KEY,
    trigger_id          VARCHAR(64)  NOT NULL,
    tenant_id           VARCHAR(64)  NOT NULL,
    trigger_code        VARCHAR(128) NOT NULL,
    trigger_type        VARCHAR(64)  NOT NULL,
    source_event_id     VARCHAR(64)  NULL,
    context_snapshot_id VARCHAR(64)  NULL,
    patient_id          VARCHAR(128) NULL,
    encounter_id        VARCHAR(128) NULL,
    patient_pathway_id  VARCHAR(64)  NULL,
    scenario_code       VARCHAR(128) NOT NULL,
    package_version     VARCHAR(64)  NULL,
    input_digest        VARCHAR(128) NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'RECEIVED',
    error_code          VARCHAR(64)  NULL,
    occurred_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id            VARCHAR(128) NULL,
    CONSTRAINT uk_rec_trigger_id UNIQUE (trigger_id),
    CONSTRAINT uk_rec_trigger_tenant_code UNIQUE (tenant_id, trigger_code),
    CONSTRAINT ck_rec_trigger_status CHECK (status IN ('RECEIVED','EVALUATED','NO_CARD','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_rec_trigger_tenant_time ON recommendation_trigger (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_trigger_patient     ON recommendation_trigger (tenant_id, patient_id, encounter_id, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_trigger_status      ON recommendation_trigger (tenant_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_trigger_scenario    ON recommendation_trigger (tenant_id, scenario_code, created_at);

CREATE TABLE IF NOT EXISTS recommendation_card (
    id                              BIGSERIAL PRIMARY KEY,
    card_id                         VARCHAR(64)   NOT NULL,
    tenant_id                       VARCHAR(64)   NOT NULL,
    trigger_id                      VARCHAR(64)   NOT NULL,
    card_code                       VARCHAR(128)  NOT NULL,
    card_type                       VARCHAR(32)   NOT NULL,
    title                           VARCHAR(256)  NOT NULL,
    summary                         VARCHAR(1024) NOT NULL,
    suggested_action                VARCHAR(1024) NOT NULL,
    risk_level                      VARCHAR(32)   NOT NULL,
    interrupt_level                 VARCHAR(32)   NOT NULL,
    status                          VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    requires_physician_confirmation BOOLEAN       NOT NULL DEFAULT FALSE,
    ai_generated                    BOOLEAN       NOT NULL DEFAULT FALSE,
    source_summary                  VARCHAR(1024) NOT NULL,
    explanation_json                TEXT          NULL,
    fatigue_key                     VARCHAR(256)  NULL,
    expires_at                      TIMESTAMPTZ   NULL,
    created_at                      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by                      VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by                      VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id                        VARCHAR(128)  NULL,
    CONSTRAINT uk_rec_card_id UNIQUE (card_id),
    CONSTRAINT uk_rec_card_trigger_code UNIQUE (tenant_id, trigger_id, card_code),
    CONSTRAINT ck_rec_card_type CHECK (card_type IN (
        'MEDICATION','EXAM','LAB','PATHWAY','RISK','KNOWLEDGE','QUALITY','NURSING','FOLLOWUP'
    )),
    CONSTRAINT ck_rec_card_risk CHECK (risk_level IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_rec_card_interrupt CHECK (interrupt_level IN (
        'SILENT','INFO','WEAK_INTERRUPTIVE','STRONG_INTERRUPTIVE'
    )),
    CONSTRAINT ck_rec_card_status CHECK (status IN (
        'PENDING','VIEWED','ACCEPTED','REJECTED','DEFERRED','DISMISSED','SUPPRESSED','EXPIRED'
    )),
    CONSTRAINT ck_rec_card_physician_confirmation CHECK (requires_physician_confirmation IN (TRUE, FALSE)),
    CONSTRAINT ck_rec_card_ai_generated CHECK (ai_generated IN (TRUE, FALSE))
);

CREATE INDEX IF NOT EXISTS idx_rec_card_trigger       ON recommendation_card (tenant_id, trigger_id);
CREATE INDEX IF NOT EXISTS idx_rec_card_tenant_status ON recommendation_card (tenant_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_card_risk          ON recommendation_card (tenant_id, risk_level, interrupt_level, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_card_fatigue       ON recommendation_card (tenant_id, fatigue_key, created_at);

CREATE TABLE IF NOT EXISTS recommendation_source (
    id               BIGSERIAL PRIMARY KEY,
    source_id        VARCHAR(64)   NOT NULL,
    tenant_id        VARCHAR(64)   NOT NULL,
    card_id          VARCHAR(64)   NOT NULL,
    source_type      VARCHAR(32)   NOT NULL,
    source_ref_id    VARCHAR(128)  NULL,
    source_version   VARCHAR(128)  NULL,
    source_title     VARCHAR(256)  NOT NULL,
    citation_locator VARCHAR(256)  NULL,
    source_hash      VARCHAR(128)  NULL,
    summary          VARCHAR(1024) NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by       VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id         VARCHAR(128)  NULL,
    CONSTRAINT uk_rec_source_id UNIQUE (source_id),
    CONSTRAINT ck_rec_source_type CHECK (source_type IN (
        'RULE','PATHWAY','KNOWLEDGE','CONTEXT','TERMINOLOGY','MANUAL'
    ))
);

CREATE INDEX IF NOT EXISTS idx_rec_source_card ON recommendation_source (tenant_id, card_id, source_type);

CREATE TABLE IF NOT EXISTS recommendation_feedback (
    id            BIGSERIAL PRIMARY KEY,
    feedback_id   VARCHAR(64)   NOT NULL,
    tenant_id     VARCHAR(64)   NOT NULL,
    card_id       VARCHAR(64)   NOT NULL,
    feedback_type VARCHAR(32)   NOT NULL,
    reason_code   VARCHAR(128)  NULL,
    reason_text   VARCHAR(1024) NULL,
    operator_id   VARCHAR(64)   NOT NULL,
    operator_role VARCHAR(128)  NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id      VARCHAR(128)  NULL,
    CONSTRAINT uk_rec_feedback_id UNIQUE (feedback_id),
    CONSTRAINT ck_rec_feedback_type CHECK (feedback_type IN (
        'VIEW_SOURCE','ACCEPT','REJECT','DEFER','DISMISS'
    ))
);

CREATE INDEX IF NOT EXISTS idx_rec_feedback_card_time ON recommendation_feedback (tenant_id, card_id, created_at);

CREATE TABLE IF NOT EXISTS recommendation_fatigue_signal (
    id                BIGSERIAL PRIMARY KEY,
    signal_id         VARCHAR(64)  NOT NULL,
    tenant_id         VARCHAR(64)  NOT NULL,
    trigger_id        VARCHAR(64)  NULL,
    card_id           VARCHAR(64)  NULL,
    fatigue_key       VARCHAR(256) NULL,
    patient_id        VARCHAR(128) NULL,
    encounter_id      VARCHAR(128) NULL,
    operator_id       VARCHAR(64)  NULL,
    signal_type       VARCHAR(32)  NOT NULL,
    occurrence_count  INT          NOT NULL DEFAULT 1,
    window_started_at TIMESTAMPTZ  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id          VARCHAR(128) NULL,
    CONSTRAINT uk_rec_fatigue_id UNIQUE (signal_id),
    CONSTRAINT ck_rec_fatigue_signal CHECK (signal_type IN (
        'SHOWN','SILENT_RECORDED','VIEWED','ACCEPTED','REJECTED','DEFERRED','DISMISSED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_rec_fatigue_card        ON recommendation_fatigue_signal (tenant_id, card_id, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_fatigue_key         ON recommendation_fatigue_signal (tenant_id, fatigue_key, signal_type, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_fatigue_tenant_time ON recommendation_fatigue_signal (tenant_id, created_at);

-- ===== 表 / 列中文注释（GA-ENG-API-07 推荐/CDSS 模块） =====

COMMENT ON TABLE recommendation_trigger IS '推荐触发事实：一次 CDSS 运行入口（临床事件/上下文/规则/路径/知识等触发）；trigger_id 全局唯一，(tenant_id, trigger_code) 唯一';
COMMENT ON COLUMN recommendation_trigger.trigger_id          IS '触发 ID（业务键，跨租户唯一）';
COMMENT ON COLUMN recommendation_trigger.tenant_id           IS '租户 ID';
COMMENT ON COLUMN recommendation_trigger.trigger_code        IS '触发业务编码（同租户唯一）';
COMMENT ON COLUMN recommendation_trigger.trigger_type        IS '触发源类型：临床事件 / 上下文 / 规则 / 路径 / 知识 / 嵌入等';
COMMENT ON COLUMN recommendation_trigger.source_event_id     IS '上游事件 id（如临床事件 event_id，可空）';
COMMENT ON COLUMN recommendation_trigger.context_snapshot_id IS '关联标准上下文快照 id（可空）';
COMMENT ON COLUMN recommendation_trigger.patient_id          IS '患者 ID（脱敏后或外部映射 id，可空）';
COMMENT ON COLUMN recommendation_trigger.encounter_id        IS '就诊 ID（可空）';
COMMENT ON COLUMN recommendation_trigger.patient_pathway_id  IS '关联患者路径 ID（可空，路径推进触发时填写）';
COMMENT ON COLUMN recommendation_trigger.scenario_code       IS '业务场景编码（如门诊/病房/急诊/嵌入位）';
COMMENT ON COLUMN recommendation_trigger.package_version     IS '包版本快照（知识/配置包版本，可空）';
COMMENT ON COLUMN recommendation_trigger.input_digest        IS '输入摘要（hash 或归一化串，用于去重和回放）';
COMMENT ON COLUMN recommendation_trigger.status              IS '触发状态：RECEIVED 已接收 / EVALUATED 已生成推荐卡 / NO_CARD 无卡 / FAILED 处理失败';
COMMENT ON COLUMN recommendation_trigger.error_code          IS '处理失败时的错误码（成功时为 NULL）';
COMMENT ON COLUMN recommendation_trigger.occurred_at         IS '触发发生时间（上游声明，默认 NOW）';

COMMENT ON TABLE recommendation_card IS '推荐卡片：CDSS 辅助建议或风险提醒的载体；card_id 全局唯一，(tenant_id, trigger_id, card_code) 唯一';
COMMENT ON COLUMN recommendation_card.card_id                         IS '推荐卡 ID（业务键，跨租户唯一）';
COMMENT ON COLUMN recommendation_card.tenant_id                       IS '租户 ID';
COMMENT ON COLUMN recommendation_card.trigger_id                      IS '关联触发 ID → recommendation_trigger.trigger_id';
COMMENT ON COLUMN recommendation_card.card_code                       IS '卡业务编码（同触发下唯一）';
COMMENT ON COLUMN recommendation_card.card_type                       IS '卡业务类型：MEDICATION 用药 / EXAM 检查 / LAB 检验 / PATHWAY 路径 / RISK 风险 / KNOWLEDGE 知识 / QUALITY 质控 / NURSING 护理 / FOLLOWUP 随访';
COMMENT ON COLUMN recommendation_card.title                           IS '卡标题（向医师展示）';
COMMENT ON COLUMN recommendation_card.summary                         IS '卡摘要（建议内容简述）';
COMMENT ON COLUMN recommendation_card.suggested_action                IS '建议动作（具体处置建议）';
COMMENT ON COLUMN recommendation_card.risk_level                      IS '风险级别：LOW 低 / MEDIUM 中 / HIGH 高 / CRITICAL 红线';
COMMENT ON COLUMN recommendation_card.interrupt_level                 IS '打扰级别：SILENT 静默 / INFO 信息 / WEAK_INTERRUPTIVE 弱打断 / STRONG_INTERRUPTIVE 强打断（强打断必须高风险）';
COMMENT ON COLUMN recommendation_card.status                          IS '推荐卡状态机：PENDING 待处理 / VIEWED 已查看依据 / ACCEPTED 已采纳 / REJECTED 不采纳 / DEFERRED 稍后处理 / DISMISSED 关闭忽略 / SUPPRESSED 疲劳治理抑制 / EXPIRED 过期失效';
COMMENT ON COLUMN recommendation_card.requires_physician_confirmation IS '是否需医师确认（高风险/红线卡必须为 TRUE）';
COMMENT ON COLUMN recommendation_card.ai_generated                    IS '是否 AI 候选（AI 候选必须显式标识，不能伪装为人工规则结论）';
COMMENT ON COLUMN recommendation_card.source_summary                  IS '来源摘要（高风险/红线卡必填）';
COMMENT ON COLUMN recommendation_card.explanation_json                IS '可解释性 JSON（链式推理/规则命中详情，可空）';
COMMENT ON COLUMN recommendation_card.fatigue_key                     IS '疲劳治理键（同一患者/场景/卡类型聚合）';
COMMENT ON COLUMN recommendation_card.expires_at                      IS '卡过期时间（过期后禁止反馈）';

COMMENT ON TABLE recommendation_source IS '推荐卡来源解释：实现 100% 可追溯；每张推荐卡至少含一条来源';
COMMENT ON COLUMN recommendation_source.source_id        IS '来源 ID（业务键，跨租户唯一）';
COMMENT ON COLUMN recommendation_source.tenant_id        IS '租户 ID';
COMMENT ON COLUMN recommendation_source.card_id          IS '关联推荐卡 ID → recommendation_card.card_id';
COMMENT ON COLUMN recommendation_source.source_type      IS '来源类型：RULE 规则命中 / PATHWAY 路径节点 / KNOWLEDGE 知识引用 / CONTEXT 上下文事实 / TERMINOLOGY 术语映射 / MANUAL 人工录入';
COMMENT ON COLUMN recommendation_source.source_ref_id    IS '上游来源 id（规则 id / 路径节点 id / 知识资产 id 等）';
COMMENT ON COLUMN recommendation_source.source_version   IS '上游来源版本';
COMMENT ON COLUMN recommendation_source.source_title     IS '来源标题（用于展示）';
COMMENT ON COLUMN recommendation_source.citation_locator IS '引用定位符（章节/段落/字段路径）';
COMMENT ON COLUMN recommendation_source.source_hash      IS '来源 hash（与 source_ref_id/source_version 之一必须存在）';
COMMENT ON COLUMN recommendation_source.summary          IS '来源摘要文本（可空）';

COMMENT ON TABLE recommendation_feedback IS '医师反馈事实：只记录医师处理动作，不写病历或医嘱；feedback_id 全局唯一';
COMMENT ON COLUMN recommendation_feedback.feedback_id   IS '反馈 ID（业务键，跨租户唯一）';
COMMENT ON COLUMN recommendation_feedback.tenant_id     IS '租户 ID';
COMMENT ON COLUMN recommendation_feedback.card_id       IS '关联推荐卡 ID → recommendation_card.card_id';
COMMENT ON COLUMN recommendation_feedback.feedback_type IS '反馈类型：VIEW_SOURCE 查看依据 / ACCEPT 采纳 / REJECT 不采纳 / DEFER 稍后处理 / DISMISS 关闭忽略';
COMMENT ON COLUMN recommendation_feedback.reason_code   IS '原因代码（不采纳/关闭/稍后处理建议填写，便于疲劳治理）';
COMMENT ON COLUMN recommendation_feedback.reason_text   IS '原因说明文本';
COMMENT ON COLUMN recommendation_feedback.operator_id   IS '操作者 user_id';
COMMENT ON COLUMN recommendation_feedback.operator_role IS '操作者角色（前端附带，便于反馈分析）';

COMMENT ON TABLE recommendation_fatigue_signal IS '疲劳治理信号事实：仅事实采集，不做自动屏蔽；signal_id 全局唯一';
COMMENT ON COLUMN recommendation_fatigue_signal.signal_id         IS '信号 ID（业务键，跨租户唯一）';
COMMENT ON COLUMN recommendation_fatigue_signal.tenant_id         IS '租户 ID';
COMMENT ON COLUMN recommendation_fatigue_signal.trigger_id        IS '关联触发 ID（可空）';
COMMENT ON COLUMN recommendation_fatigue_signal.card_id           IS '关联推荐卡 ID（可空）';
COMMENT ON COLUMN recommendation_fatigue_signal.fatigue_key       IS '疲劳治理键（同一患者/场景/卡类型聚合）';
COMMENT ON COLUMN recommendation_fatigue_signal.patient_id        IS '患者 ID（可空）';
COMMENT ON COLUMN recommendation_fatigue_signal.encounter_id      IS '就诊 ID（可空）';
COMMENT ON COLUMN recommendation_fatigue_signal.operator_id       IS '相关操作者 user_id（可空）';
COMMENT ON COLUMN recommendation_fatigue_signal.signal_type       IS '信号类型：SHOWN 已展示 / SILENT_RECORDED 静默试运行 / VIEWED 用户查看 / ACCEPTED 用户采纳 / REJECTED 用户不采纳 / DEFERRED 稍后处理 / DISMISSED 关闭忽略';
COMMENT ON COLUMN recommendation_fatigue_signal.occurrence_count  IS '聚合发生次数（首版默认 1，后续治理引擎可累加）';
COMMENT ON COLUMN recommendation_fatigue_signal.window_started_at IS '聚合窗口开始时间';
