# Flyway 回滚操作指南

> 版本：1.0 | 适用：MedKernel v1.0 GA | 日期：2026-05-23

## 1. 概述

MedKernel 使用 Flyway 管理数据库 schema 版本。本文档描述 Flyway 迁移的回滚策略和操作步骤。

**重要**：Flyway 社区版不支持 `flyway undo`，回滚需通过手动 SQL 或 `flyway repair` + 重新迁移实现。

## 2. Flyway 迁移架构

### 2.1 迁移目录结构

```
db/migration/
├── common/          # 公共迁移（当前为空，预留）
├── oracle/          # Oracle + 达梦（DM 复用 Oracle 语法）
│   ├── V1__baseline_flyway.sql
│   └── V2__pr_final_23_widen_md_patient_encrypted_columns.sql
├── postgres/        # PostgreSQL + KingbaseES（KingbaseES 复用 PG 语法）
│   ├── V1__baseline_flyway.sql
│   └── V2__pr_final_23_widen_md_patient_encrypted_columns.sql
└── h2/              # H2 开发库
    ├── V1__baseline_flyway.sql
    └── V2__pr_final_23_widen_md_patient_encrypted_columns.sql
```

### 2.2 方言映射

| dialect 环境变量 | Flyway vendor 目录 | 说明 |
|-----------------|-------------------|------|
| `oracle` | `oracle/` | Oracle 12c+ |
| `dm` / `dameng` | `oracle/` | 达梦 DM 8（兼容 Oracle 语法） |
| `postgres` / `pg` | `postgres/` | PostgreSQL 16 |
| `kingbase` / `kingbasees` | `postgres/` | KingbaseES V8R6（PG 兼容模式） |
| `h2` / `local` | `h2/` | H2 开发库 |

### 2.3 关键配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `medkernel.flyway.enabled` | `false` | 生产部署时必须设为 `true` |
| `medkernel.flyway.baseline-on-migrate` | `true` | 已有表时安全接入 |
| `medkernel.flyway.baseline-version` | `0` | 基线版本 |
| `medkernel.flyway.clean-disabled` | `true` | 禁止 `flyway clean`（生产安全） |
| `medkernel.flyway.table` | `flyway_schema_history` | 版本记录表 |

## 3. 回滚策略

### 3.1 策略一：手动 SQL 回滚（推荐）

适用于：已知具体迁移内容，需要精确回滚。

**步骤**：

1. 查看当前迁移版本：
```sql
SELECT installed_rank, version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank DESC;
```

2. 编写回滚 SQL（针对每个迁移的逆操作）：

| 迁移 | 回滚 SQL |
|------|---------|
| V2: 扩展 md_patient 加密列宽 | `ALTER TABLE md_patient MODIFY ...` (还原原列宽) |
| V1: baseline | 无需回滚（仅 `SELECT 1 FROM DUAL`） |

3. 执行回滚 SQL：
```bash
# Oracle
echo "回滚 SQL" | sqlplus user/pass@host:port:sid

# PostgreSQL / KingbaseES
PGPASSWORD=xxx psql -h host -U user -d db -c "回滚 SQL"

# 达梦
echo "回滚 SQL" | disql user/pass@host:port
```

4. 修复 Flyway 版本记录：
```bash
# 删除已回滚的版本记录
DELETE FROM flyway_schema_history WHERE version = '2';
```

### 3.2 策略二：flyway repair + 重新迁移

适用于：迁移失败导致 `flyway_schema_history` 不一致。

**步骤**：

1. 停止应用：
```bash
systemctl stop medkernel
```

2. 手动修复数据库（删除失败的迁移留下的部分变更）

3. 修复 Flyway 版本记录：
```bash
# 通过 Spring Boot Actuator（如果可用）
curl -X POST http://127.0.0.1:18081/medkernel/actuator/flyway-repair

# 或直接删除失败记录
DELETE FROM flyway_schema_history WHERE success = 0;
```

4. 重启应用（Flyway 会重新执行失败的迁移）：
```bash
systemctl start medkernel
```

### 3.3 策略三：全库恢复（最后手段）

适用于：无法精确回滚，需恢复到已知良好状态。

**步骤**：

1. 停止应用：
```bash
systemctl stop medkernel
```

2. 恢复数据库备份：
```bash
# Oracle
rman target / restore database;

# PostgreSQL
pg_restore -h host -U user -d db /path/to/backup.dump

# 达梦
dmrestore ... 

# KingbaseES
sys_restore -h host -U user -d db /path/to/backup.dump
```

3. 重启应用：
```bash
systemctl start medkernel
```

## 4. 回滚验证

回滚后，执行 DDL 一致性冒烟测试验证表结构完整性：

```bash
# Oracle
./deploy/scripts/smoke-ddl-consistency.sh --dialect oracle --connect user/pass@host:port:sid

# 达梦
./deploy/scripts/smoke-ddl-consistency.sh --dialect dm --host 10.0.0.20 --user medkernel --password xxx

# PostgreSQL
./deploy/scripts/smoke-ddl-consistency.sh --dialect postgres --host 10.0.0.30 --user medkernel --password xxx

# KingbaseES
./deploy/scripts/smoke-ddl-consistency.sh --dialect kingbase --host 10.0.0.40 --user medkernel --password xxx
```

## 5. 注意事项

1. **生产环境禁止 `flyway clean`**：`clean-disabled: true` 防止误删全库
2. **回滚前必须备份**：任何回滚操作前先执行数据库备份
3. **DM 与 Oracle 语法差异**：虽然 DM 复用 `oracle/` 迁移目录，但部分语法（如 `NUMBER` 精度、`CLOB` 处理）可能不同，回滚 SQL 需针对方言调整
4. **KingbaseES 端口默认 54321**：与 PG 默认 5432 不同，连接时注意端口
5. **baseline-on-migrate=true**：首次启动 Flyway 时，已有表会被标记为 baseline，不会重新创建

## 6. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-05-23 | 1.0 | 初始版本：回滚策略、操作步骤、验证方法 |
