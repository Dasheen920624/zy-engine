# MedKernel · 运维手册

> 状态：骨架占位 · 待 E4 嵌入与模型上线后正式填充
> 适用：v1.0 GA · 院方信息科 / 乙方 SRE / 国产化驻场运维

---

## 1. 文档定位

本手册面向**日常运维与值守人员**，覆盖系统上线后的可用性、安全性、可追溯性维护。

运维范围：

- 服务健康度与告警值守（Prometheus / Actuator / Grafana）
- 备份恢复演练（联动 [runbooks/backup-restore.md](runbooks/backup-restore.md)）
- 升级与回滚（联动 [runbooks/upgrade-rollback.md](runbooks/upgrade-rollback.md)）
- 性能压测与基线维护（联动 [performance/README.md](performance/README.md)）
- Provider/模型健康度与降级值守
- 审计链验签与证据导出
- 国产化 profile（govcloud）巡检

---

## 2. 启用阶段

| 阶段 | 触发条件 |
|---|---|
| 骨架占位（当前） | E0/E1 完成 |
| 正式填充 | E4 嵌入与模型上线 + Provider/模型审计链路验收 |
| 内容来源 | 引用 [详细规范](../MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md) 中合规运维子包；不另起方案 |

---

## 3. 当前已具备的运维入口

- 健康检查：`/actuator/health`
- 指标：`/actuator/prometheus`
- 备份脚本：`deploy/docker/scripts/backup.sh` + `restore.sh`（SHA-256 摘要校验）
- 国产化配置：`application-govcloud.yml`

---

## 4. 数据库运维注意（Oracle 标识符大小写）

业务表由 Flyway 迁移脚本以**不加双引号**方式创建，Oracle 按默认规则折叠为**大写**（如 `PLATFORM_CREDENTIAL`、`ORG_UNIT`）；运行时 Spring Data JDBC 已通过 `JdbcIdentifierPolicyConfig`（`forceQuote=false`）对齐，查询正常。

但 Flyway 版本历史表 `flyway_schema_history` 由 **Flyway 自身**创建并**强制加双引号**，在 Oracle 中以**小写、区分大小写**存储，与业务表（大写）不同。这是 Flyway 跨库的固定行为，**无害**（Flyway 读写口径一致，不影响迁移）。

**DBA 手动排查注意**：Oracle 下查询该表必须加双引号 + 小写，否则报 `ORA-00942`：

```sql
-- ✅ 正确（加双引号 + 小写）
SELECT * FROM "flyway_schema_history" ORDER BY installed_rank;

-- ❌ 错误：未加引号会被当成大写 FLYWAY_SCHEMA_HISTORY，ORA-00942 表不存在
SELECT * FROM flyway_schema_history;
```

PostgreSQL / Kingbase / 达梦 / H2 不受影响（不区分大小写或同为小写），仅 Oracle 需注意此差异。

---

## 5. 关联文档

- [产品宪法](../CONSTITUTION.md)
- [基础底座与引擎服务能力总览](../MEDKERNEL_FOUNDATION_AND_SERVICES.md)
- [备份恢复 Runbook](runbooks/backup-restore.md)
- [升级回滚 Runbook](runbooks/upgrade-rollback.md)
- [性能压测目录](performance/README.md)
