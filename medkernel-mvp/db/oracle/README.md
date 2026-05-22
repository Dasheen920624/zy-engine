# Oracle 建表说明

## 首发基线

`clean_init_all.sql` 是 Oracle 首发干净初始化脚本，仅用于首次发布、测试环境重置，或明确要求“全部清空后重建”的场景。

执行效果：

- 删除当前 schema 下全部表、索引、视图、序列、触发器、过程、包、类型等对象。
- 重建核心引擎、数据治理、MPI、规则评估、安全认证、SSO、用户同步、审计链和业务扩展表。
- 写入首发初始化数据。
- 写入 `schema_version` 基线记录。
- 自动补齐表和字段中文备注，并校验缺失数量必须为 0。

生产环境只有在明确接受清库时才能执行该脚本。

## 后续迭代

正常版本升级不再执行 `clean_init_all.sql`，而是在 `migrations/` 目录追加增量脚本：

```text
migrations/V0_3_1__add_xxx.sql
migrations/V0_3_2__alter_xxx.sql
migrations/V0_4_0__release_xxx.sql
```

迁移约定：

- 每个脚本只描述从上一个版本升级到当前版本的差异。
- 每个新增表和新增字段必须有中文 `COMMENT ON`。
- 每个脚本执行完成后写入 `schema_version`，记录版本、脚本名、类型和说明。
- 迁移脚本不得默认清空业务数据；需要清理数据时必须在脚本头部写明影响范围。
- Oracle、达梦、PostgreSQL 分目录维护各自方言，不复用会改变语义的 SQL。

## 核心脚本

`medkernel_core_ddl_with_comments.sql` 会创建组织模型、路径引擎、规则引擎、图谱引擎、字典映射、适配器中心和审计日志的核心表，并写入表备注和字段备注。

为避免 SQLPlus 客户端字符集不一致导致中文备注乱码，推荐通过 `../../scripts/run-oracle-ddl.ps1` 执行。该脚本会在建表后执行 `medkernel_comments_unistr.sql`，使用 Oracle `UNISTR` 方式覆盖写入中文表备注和字段备注。

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
