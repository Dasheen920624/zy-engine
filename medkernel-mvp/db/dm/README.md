# 达梦 DM 建表说明

> 适用 **达梦 DM 7 / DM 8**。语法整体接近 Oracle，但项目内 DDL 已统一使用 `BIGINT` / `VARCHAR` 等通用类型，避免方言锁死。

## 文件

| 文件 | 用途 |
|---|---|
| `medkernel_core_ddl.sql` | 核心建表（幂等 `IF NOT EXISTS` 风格 + 索引）|
| `medkernel_comments.sql` | 中文表备注 + 字段备注（DM UTF8 库可直接写中文，不需 UNISTR）|
| `medkernel_org_context_migration.sql` | 升级老库：补齐 PE_VARIATION_RECORD / RE_RULE_EXEC_LOG / ENGINE_AUDIT_LOG 的组织字段 |

> 字段层与索引与 Oracle 等价（见 `ai-dev-input/04_database/dm/core_ddl.sql`）。

## 推荐执行方式

不要把数据库密码写入代码库。请在可访问数据库的机器上设置环境变量后执行：

```powershell
$env:MEDKERNEL_DB_DIALECT='dm'
$env:MEDKERNEL_DB_CONNECT='192.168.4.30:5236'
$env:MEDKERNEL_DB_USERNAME='MEDKERNEL'
$env:MEDKERNEL_DB_PASSWORD='数据库密码'
# 批次 3 提供 scripts/run-dm-ddl.ps1，本批次 README 提供手工执行方式
```

或在达梦客户端 disql / DM 管理工具中按顺序执行：

```text
1) medkernel_core_ddl.sql
2) medkernel_org_context_migration.sql   （旧库升级才需要）
3) medkernel_comments.sql
```

## 字符集

库必须为 **UTF8**：

```sql
-- 建库时（dminit 时）：CHARSET=1 即 UTF-8
-- 验证：
SELECT PARA_VALUE FROM V$DM_INI WHERE PARA_NAME = 'CHARSET';
-- 期望返回 1
```

JDBC URL 必须显式时区，避免时间偏差：

```text
jdbc:dm://host:5236?clobAsString=true&useServerPrepStmts=true&serverTimezone=Asia/Shanghai
```

## 验证 SQL

```sql
-- 表是否齐全
SELECT TABLE_NAME FROM USER_TABLES
 WHERE TABLE_NAME IN (
   'ORG_UNIT', 'PE_PATHWAY_DEF', 'PE_PATHWAY_VERSION', 'PE_PATIENT_INSTANCE',
   'PE_PATIENT_NODE_STATE', 'PE_PATIENT_TASK_STATE', 'PE_VARIATION_RECORD',
   'RE_RULE_DEF', 'RE_RULE_EXEC_LOG', 'TM_STANDARD_CONCEPT', 'TM_CONCEPT_MAPPING',
   'ADP_ADAPTER_DEF', 'ADP_QUERY_DEF', 'GE_GRAPH_VERSION', 'ENGINE_AUDIT_LOG'
 )
 ORDER BY TABLE_NAME;

-- 表备注
SELECT TABLE_NAME, COMMENTS FROM USER_TAB_COMMENTS
 WHERE TABLE_NAME LIKE 'PE_%' OR TABLE_NAME LIKE 'RE_%' OR TABLE_NAME = 'ORG_UNIT';

-- 组织字段是否齐全
SELECT COLUMN_NAME FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'RE_RULE_EXEC_LOG'
   AND COLUMN_NAME IN ('TENANT_ID', 'HOSPITAL_CODE', 'SCOPE_LEVEL', 'SCOPE_CODE', 'ORG_SOURCE')
 ORDER BY COLUMN_NAME;
```

## 与 Oracle 的差异

| 维度 | Oracle | DM 8 | 项目处理 |
|---|---|---|---|
| 数据类型 | NUMBER / VARCHAR2 | BIGINT / VARCHAR | DM DDL 统一用通用类型 |
| 备注 | NLS 客户端字符集敏感 | UTF8 库直接写中文 | DM 不需 UNISTR |
| 序列 | SEQUENCE + 触发器 | 同 Oracle | 项目 ID 应用层生成，不用 |
| DUAL | 必须 | 同 Oracle | mapper 屏蔽 |
| 分页 | OFFSET ... FETCH | LIMIT/OFFSET 也支持 | 见 `PaginationDialect` |
| timezone | NLS_TIMESTAMP_TZ_FORMAT | JDBC `serverTimezone` 参数 | URL 加 `serverTimezone=Asia/Shanghai` |

详见 [`../../docs/08_国产化兼容性规约.md`](../../docs/08_国产化兼容性规约.md) §5。

## 批次 3 计划

- `scripts/run-dm-ddl.ps1` / `.cmd` 自动化建表
- `scripts/run-dm-org-smoke.ps1` 真实落库 smoke
- `application-dm.yml` profile
