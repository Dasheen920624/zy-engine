-- MedKernel v1.0 GA · GA-ENG-INTEG-01 适配器健康状态扩展（Kingbase）
-- 引入诚实自检状态 NOT_CONNECTED（配置合法但未接入真实连接器、外部可达性未知）
-- 与 MISCONFIGURED（配置非法），取代"配置合法即 HEALTHY"的失真语义；
-- 保留 HEALTHY/UNHEALTHY 供接入真实外部连接器（INTEG-02 / QA-08）后使用。

ALTER TABLE integration_adapter DROP CONSTRAINT ck_integration_adapter_health;
ALTER TABLE integration_adapter ADD CONSTRAINT ck_integration_adapter_health
    CHECK (health_status IN ('HEALTHY','UNHEALTHY','NOT_CONNECTED','MISCONFIGURED'));
