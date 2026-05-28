-- MedKernel v1.0 GA · GA-SVC-PILOT-01 租户定制与客户成功多维生命周期（H2）

CREATE TABLE IF NOT EXISTS tenant_branding (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(64)  NOT NULL,
    hospital_name       VARCHAR(128) NOT NULL,
    logo_url            VARCHAR(512) NULL,
    theme_color         VARCHAR(32)  NULL,
    expert_mode         BOOLEAN      NOT NULL DEFAULT FALSE,
    custom_branding_json VARCHAR(4000) NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    CONSTRAINT uk_tenant_branding UNIQUE (tenant_id)
);

CREATE TABLE IF NOT EXISTS tenant_success_plan (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(64)  NOT NULL,
    current_stage       VARCHAR(32)  NOT NULL,
    health_score        INT          NOT NULL DEFAULT 80,
    activated_modules   VARCHAR(512) NULL,
    activated_pathways  VARCHAR(512) NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    CONSTRAINT uk_tenant_success_plan UNIQUE (tenant_id)
);

COMMENT ON TABLE tenant_branding IS '租户定制 brand 品牌信息表';
COMMENT ON COLUMN tenant_branding.id IS '自增物理主键';
COMMENT ON COLUMN tenant_branding.tenant_id IS '租户 ID';
COMMENT ON COLUMN tenant_branding.hospital_name IS '医院物理名称';
COMMENT ON COLUMN tenant_branding.logo_url IS '医院定制 Logo URL';
COMMENT ON COLUMN tenant_branding.theme_color IS '平台定制 UI 主题色';
COMMENT ON COLUMN tenant_branding.expert_mode IS '是否开启专家模式';
COMMENT ON COLUMN tenant_branding.custom_branding_json IS '其他扩展样式的品牌 JSON 配置';
COMMENT ON COLUMN tenant_branding.created_at IS '创建时间点';
COMMENT ON COLUMN tenant_branding.created_by IS '创建人';
COMMENT ON COLUMN tenant_branding.updated_at IS '最后更新时间点';
COMMENT ON COLUMN tenant_branding.updated_by IS '最后更新人';

COMMENT ON TABLE tenant_success_plan IS '租户客户成功多维生命周期计划表';
COMMENT ON COLUMN tenant_success_plan.id IS '自增物理主键';
COMMENT ON COLUMN tenant_success_plan.tenant_id IS '租户 ID';
COMMENT ON COLUMN tenant_success_plan.current_stage IS '当前生命周期阶段 (PREPARATION, PILOT, ACCEPTANCE, PROMOTION, RUNNING, RENEWAL)';
COMMENT ON COLUMN tenant_success_plan.health_score IS '多维治理健康度评分';
COMMENT ON COLUMN tenant_success_plan.activated_modules IS '已激活系统服务模块逗号分隔列表';
COMMENT ON COLUMN tenant_success_plan.activated_pathways IS '已激活临床专病包逗号分隔列表';
COMMENT ON COLUMN tenant_success_plan.created_at IS '创建时间点';
COMMENT ON COLUMN tenant_success_plan.created_by IS '创建人';
COMMENT ON COLUMN tenant_success_plan.updated_at IS '最后更新时间点';
COMMENT ON COLUMN tenant_success_plan.updated_by IS '最后更新人';
