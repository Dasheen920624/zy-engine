# PostgreSQL / KingbaseES 建表说明

> 适用 **PostgreSQL 14 / 15 / 16** 与 **KingbaseES V8（PG 兼容模式启动）**。

## 文件

| 文件 | 用途 |
|---|---|
| `medkernel_core_ddl_with_comments.sql` | 核心建表 + 中文 `COMMENT ON`（幂等，含 `IF NOT EXISTS`）|
| `medkernel_org_context_migration.sql` | 升级老库：补齐 PE_VARIATION_RECORD / RE_RULE_EXEC_LOG / ENGINE_AUDIT_LOG 的组织字段与索引 |

> 与 `ai-dev-input/04_database/postgres/core_ddl.sql` 等价，本目录额外提供中文备注与迁移脚本。

## 建库

```bash
# 在 postgres 超级账号下：
sudo -u postgres psql <<'SQL'
CREATE USER medkernel WITH PASSWORD '请改成实际密码';
CREATE DATABASE medkernel
  WITH OWNER = medkernel
       ENCODING = 'UTF8'
       LC_COLLATE = 'zh_CN.UTF-8'
       LC_CTYPE = 'zh_CN.UTF-8'
       TEMPLATE = template0;
GRANT ALL PRIVILEGES ON DATABASE medkernel TO medkernel;
SQL
```

如果系统未生成 `zh_CN.UTF-8` locale：

```bash
sudo localedef -i zh_CN -f UTF-8 zh_CN.UTF-8
# Debian/Ubuntu：sudo dpkg-reconfigure locales 勾选 zh_CN.UTF-8
```

## 执行 DDL

```bash
psql -h <host> -p 5432 -U medkernel -d medkernel \
     -v ON_ERROR_STOP=1 \
     -f medkernel_core_ddl_with_comments.sql

# 升级旧库：
psql -h <host> -p 5432 -U medkernel -d medkernel \
     -v ON_ERROR_STOP=1 \
     -f medkernel_org_context_migration.sql
```

> Windows 下用 `psql.exe`，参数相同。批次 3 提供 `scripts/run-postgres-ddl.ps1` 自动化。

## 应用接入

`application-postgres.yml`（批次 3 提供完整版本）：

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${MEDKERNEL_DB_HOST:localhost}:${MEDKERNEL_DB_PORT:5432}/${MEDKERNEL_DB_NAME:medkernel}?reWriteBatchedInserts=true&prepareThreshold=0
    username: ${MEDKERNEL_DB_USERNAME:medkernel}
    password: ${MEDKERNEL_DB_PASSWORD:}
medkernel:
  db:
    dialect: postgres
```

## KingbaseES

KingbaseES V8 PG 兼容模式启动后，本目录 DDL **可直接使用**：

```bash
# 用 KingbaseES 自带 ksql：
ksql -h <host> -p 54321 -U system -d medkernel \
     -v ON_ERROR_STOP=1 \
     -f medkernel_core_ddl_with_comments.sql
```

注意点：

- 建库时 `ENCODING=UTF8`、`LC_COLLATE='zh_CN.UTF-8'`。
- 启动 mode 必须为 `PG`（默认）；如果是 `Oracle` 模式，请改用 `db/oracle/` 下的脚本。
- JDBC 驱动用 `kingbase8`：

```yaml
spring:
  datasource:
    driver-class-name: com.kingbase8.Driver
    url: jdbc:kingbase8://<host>:54321/medkernel
```

## 验证 SQL

```sql
-- 表是否齐全
SELECT tablename FROM pg_tables
 WHERE schemaname = 'public'
   AND tablename IN (
   'org_unit', 'pe_pathway_def', 'pe_pathway_version', 'pe_patient_instance',
   'pe_patient_node_state', 'pe_patient_task_state', 'pe_variation_record',
   're_rule_def', 're_rule_exec_log', 'tm_standard_concept', 'tm_concept_mapping',
   'adp_adapter_def', 'adp_query_def', 'ge_graph_version', 'engine_audit_log'
 )
 ORDER BY tablename;

-- 表备注
SELECT relname, obj_description(c.oid)
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'public'
   AND relkind = 'r'
   AND (relname LIKE 'pe_%' OR relname LIKE 're_%' OR relname = 'org_unit');

-- 组织字段是否齐全
SELECT column_name FROM information_schema.columns
 WHERE table_name = 're_rule_exec_log'
   AND column_name IN ('tenant_id', 'hospital_code', 'scope_level', 'scope_code', 'org_source')
 ORDER BY column_name;

-- 字符集
SHOW server_encoding;     -- 期望 UTF8
SHOW lc_collate;          -- 期望 zh_CN.UTF-8 或 C.UTF-8
SHOW timezone;            -- 期望 Asia/Shanghai
```

## 性能调优（生产）

`postgresql.conf` 关键参数（按 16GB 内存机型示例）：

```conf
shared_buffers = 4GB
work_mem = 16MB
maintenance_work_mem = 512MB
effective_cache_size = 12GB
wal_level = replica
max_wal_size = 4GB
checkpoint_completion_target = 0.9
timezone = 'Asia/Shanghai'
```

中文全文检索可选 `zhparser` 或 `pg_jieba` 扩展（按需）。

## 与 Oracle / DM 的差异

详见 [`../../../docs/engineering/08_国产化兼容性规约.md`](../../../docs/engineering/08_国产化兼容性规约.md) §5。

## 批次 3 计划

- `scripts/run-postgres-ddl.ps1` / `.cmd` / `.sh` 自动化建表
- `scripts/run-postgres-org-smoke.ps1` 真实落库 smoke
- `application-postgres.yml` profile
- KingbaseES 等价脚本（驱动替换 + ksql）
