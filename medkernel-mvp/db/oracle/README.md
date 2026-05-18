# Oracle建表说明

建表脚本：

```text
medkernel_core_ddl_with_comments.sql
```

该脚本会创建组织模型、路径引擎、规则引擎、图谱引擎、字典映射、适配器中心和审计日志的核心表，并写入表备注和字段备注。

为避免 SQLPlus 客户端字符集不一致导致中文备注乱码，推荐通过 `../../scripts/run-oracle-ddl.ps1` 执行。该脚本会在建表后执行 `medkernel_comments_unistr.sql`，使用 Oracle `UNISTR` 方式覆盖写入中文表备注和字段备注。

脚本特性：

- 表已存在时跳过创建。
- 不执行 `DROP TABLE`。
- 不删除已有数据。
- 创建核心索引。
- 创建 `COMMENT ON TABLE` 和 `COMMENT ON COLUMN`。

## 推荐执行方式

不要把数据库密码写入代码库。请在可访问数据库的机器上设置环境变量后执行：

```powershell
$env:MEDKERNEL_DB_CONNECT='//192.168.4.25:1521/ORCL'
$env:MEDKERNEL_DB_USERNAME='MEDKERNEL'
$env:MEDKERNEL_DB_PASSWORD='数据库密码'
.\scripts\run-oracle-ddl.ps1
```

## 验证SQL

```sql
SELECT table_name, comments
  FROM user_tab_comments
 WHERE table_name LIKE 'PE_%'
    OR table_name LIKE 'RE_%'
    OR table_name LIKE 'TM_%'
    OR table_name LIKE 'ADP_%'
    OR table_name LIKE 'GE_%'
    OR table_name = 'ORG_UNIT'
    OR table_name = 'ENGINE_AUDIT_LOG'
 ORDER BY table_name;
```
