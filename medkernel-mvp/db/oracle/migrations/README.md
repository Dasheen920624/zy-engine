# Oracle 增量迁移目录

本目录用于首发之后的 Oracle 数据库结构迭代。

命名格式：

```text
V主版本_次版本_修订号__中文或英文说明.sql
```

示例：

```text
V0_3_1__add_rule_publish_audit.sql
```

执行约定：

- 首发干净建库使用上级目录的 `clean_init_all.sql`。
- 后续升级只追加增量脚本，不修改已发布脚本。
- 新增表和新增字段必须写中文备注。
- 脚本末尾写入 `schema_version`，并校验中文备注缺失数量为 0。
