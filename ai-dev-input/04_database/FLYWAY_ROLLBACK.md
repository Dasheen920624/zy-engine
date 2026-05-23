# Flyway Rollback 策略

> 版本：1.0 · 2026-05-24
> 任务：GA-DB-01 多方言 smoke 矩阵和 Flyway rollback 证据

## 1. 概述

MedKernel 使用 Flyway 管理数据库 schema 版本。由于使用 Flyway Community 版（不支持 `flyway undo`），rollback 采用手动策略。

## 2. 支持的方言

| 方言 | Flyway locations | DDL 目录 | 兼容说明 |
|---|---|---|---|
| Oracle | `db/migration/{common,oracle}` | `ai-dev-input/04_database/oracle/` | `NUMBER(20)`, `VARCHAR2`, `CLOB` |
| DM8+ (达梦) | `db/migration/{common,dm}` | `ai-dev-input/04_database/dm/` | 兼容 Oracle，使用 `BIGINT`, `VARCHAR`, `CLOB` |
| PostgreSQL | `db/migration/{common,postgres}` | `ai-dev-input/04_database/postgres/` | `BIGINT`, `VARCHAR`, `TEXT`, `COMMENT ON` |
| KingbaseES (人大金仓) | `db/migration/{common,kingbase}` | `ai-dev-input/04_database/postgres/` | 兼容 PostgreSQL |
| H2 (开发/测试) | `db/migration/{common,h2}` | `ai-dev-input/04_database/local/` | Oracle 兼容模式 |

## 3. 当前迁移版本

| 版本 | 描述 | Oracle | DM | PostgreSQL | KingbaseES | H2 |
|---|---|---|---|---|---|---|
| V1 | baseline 占位 | `SELECT 1 FROM DUAL` | `SELECT 1 FROM DUAL` | `SELECT 1` | `SELECT 1` | `SELECT 1` |
| V2 | 扩展 md_patient 加密字段列宽 | `MODIFY (col VARCHAR2(N))` | `MODIFY (col VARCHAR(N))` | `ALTER COLUMN ... TYPE VARCHAR(N)` | `ALTER COLUMN ... TYPE VARCHAR(N)` | `ALTER COLUMN ... VARCHAR(N)` |

## 4. Rollback 策略

### 4.1 原则

- **优先回滚应用**：数据库 schema 变更应向后兼容，允许旧版本应用在新 schema 上运行
- **最小化破坏性变更**：新增列/表是安全的，删除/重命名列/表是破坏性的
- **手动 undo 脚本**：每个破坏性迁移必须有对应的手动 undo 脚本

### 4.2 Rollback 步骤

#### 场景 A：应用回滚（数据库不变）

1. 停止新版本应用
2. 部署旧版本应用
3. 验证旧版本应用可正常连接数据库

**适用条件**：schema 变更是向后兼容的（如新增列、新增表）

#### 场景 B：数据库回滚（需要 undo）

1. 停止应用
2. 执行手动 undo SQL（见下方）
3. 更新 `flyway_schema_history` 表（删除已回滚版本的记录）
4. 部署旧版本应用
5. 验证应用可正常连接数据库

**适用条件**：schema 变更是破坏性的（如删除列、修改列类型导致数据截断）

### 4.3 手动 Undo SQL

#### V2 Undo：恢复 md_patient 加密字段原始列宽

| 方言 | Undo SQL |
|---|---|
| Oracle | `ALTER TABLE md_patient MODIFY (patient_name VARCHAR2(64)); ALTER TABLE md_patient MODIFY (id_card_no VARCHAR2(64)); ALTER TABLE md_patient MODIFY (phone VARCHAR2(32)); ALTER TABLE md_patient MODIFY (address VARCHAR2(256));` |
| DM | `ALTER TABLE md_patient MODIFY (patient_name VARCHAR(64)); ALTER TABLE md_patient MODIFY (id_card_no VARCHAR(64)); ALTER TABLE md_patient MODIFY (phone VARCHAR(32)); ALTER TABLE md_patient MODIFY (address VARCHAR(256));` |
| PostgreSQL | `ALTER TABLE md_patient ALTER COLUMN patient_name TYPE VARCHAR(64); ALTER TABLE md_patient ALTER COLUMN id_card_no TYPE VARCHAR(64); ALTER TABLE md_patient ALTER COLUMN phone TYPE VARCHAR(32); ALTER TABLE md_patient ALTER COLUMN address TYPE VARCHAR(256);` |
| KingbaseES | 同 PostgreSQL |
| H2 | `ALTER TABLE md_patient ALTER COLUMN patient_name VARCHAR(64); ALTER TABLE md_patient ALTER COLUMN id_card_no VARCHAR(64); ALTER TABLE md_patient ALTER COLUMN phone VARCHAR(32); ALTER TABLE md_patient ALTER COLUMN address VARCHAR(256);` |

**注意**：V2 undo 需要先解密数据（如果已使用 SM4 加密），否则列宽缩小会导致密文截断。

#### V1 Undo：无（baseline 占位，无需回滚）

### 4.4 flyway_schema_history 维护

回滚后需手动删除对应版本记录：

```sql
-- 回滚 V2 后执行
DELETE FROM flyway_schema_history WHERE version = '2';
```

## 5. 升级前检查清单

- [ ] 确认当前 Flyway 版本号（`SELECT * FROM flyway_schema_history ORDER BY installed_rank`）
- [ ] 备份数据库
- [ ] 确认 undo SQL 已准备
- [ ] 在测试环境验证迁移和回滚
- [ ] 确认应用兼容新旧 schema

## 6. 禁止模式

| 模式 | 原因 |
|---|---|
| `ON DUPLICATE KEY UPDATE` | MySQL 专有语法，Oracle/DM/PG/Kingbase/H2 均不支持 |
| 直接修改 `flyway_schema_history` | 除非回滚操作，否则禁止手动修改 |
| 跳过版本号 | Flyway 版本号必须连续 |
| 破坏性变更无 undo | 每个破坏性迁移必须有对应的手动 undo SQL |
