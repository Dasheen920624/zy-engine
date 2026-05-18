# CFG_CONFIG_PACKAGE Smoke 测试计划

## 1. 测试目标

验证 `cfg_config_package` 表的 DDL 正确性、Repository 持久化层功能以及 Service 层的数据库优先查询逻辑。

## 2. 测试环境

| 环境 | 数据库 | 说明 |
|------|--------|------|
| 开发库 | LOCAL_H2_FILE | 必须验证，无条件执行 |
| 生产库 | Oracle | 有环境时验证，无环境时标为 SKIPPED |
| 生产库 | DM (达梦) | 有环境时验证，无环境时标为 SKIPPED |
| 生产库 | PostgreSQL/Kingbase | 有环境时验证，无环境时标为 SKIPPED |

## 3. 测试用例

### 3.1 DDL 验证 (TC-001 ~ TC-003)

| 编号 | 用例 | 预期结果 |
|------|------|----------|
| TC-001 | 执行 DDL 脚本创建 cfg_config_package 表 | 表创建成功，无语法错误 |
| TC-002 | 验证字段类型和约束 | 所有字段类型正确，主键/非空约束生效 |
| TC-003 | 验证索引创建 | UK_CONFIG_PACKAGE 唯一索引存在 |

### 3.2 Repository CRUD 操作 (TC-004 ~ TC-008)

| 编号 | 用例 | 操作 | 预期结果 |
|------|------|------|----------|
| TC-004 | 新增配置包 | INSERT | 记录插入成功，ID 自动生成 |
| TC-005 | 查询配置包 | SELECT by code+version | 返回正确记录 |
| TC-006 | 更新配置包 | UPDATE status | 状态更新成功 |
| TC-007 | 按状态查询 | SELECT by status | 返回对应状态的记录列表 |
| TC-008 | 删除配置包 | DELETE | 记录删除成功 |

### 3.3 Service 层集成 (TC-009 ~ TC-011)

| 编号 | 用例 | 操作 | 预期结果 |
|------|------|------|----------|
| TC-009 | 数据库查询优先 | findPackage | 优先从数据库返回，不走内存 |
| TC-010 | 数据库未找到回退 | findPackage (不存在的包) | 回退到内存存储查询 |
| TC-011 | 状态流转 | DRAFT → REVIEWED → PUBLISHED | 状态变更正确持久化 |

## 4. Smoke 脚本设计

### 4.1 脚本参数

```powershell
param(
  [string]$BaseUrl = "http://localhost:18080/zy-engine/api",
  [string]$DbType = "LOCAL_H2_FILE"  # LOCAL_H2_FILE | ORACLE | DM | POSTGRES
)
```

### 4.2 测试流程

```
1. Health Check
   └─ GET /health → success = true

2. DDL 验证 (如果需要)
   └─ 检查表是否存在

3. 新增配置包
   └─ POST /config/packages → 创建成功
   └─ 验证返回的 ID 不为空

4. 查询配置包
   └─ GET /config/packages/{code}/{version} → 返回正确数据
   └─ 验证 packageCode, packageVersion, status 字段

5. 更新状态
   └─ PUT /config/packages/{id}/status → 状态更新成功
   └─ 验证 status 变为 REVIEWED

6. 按状态查询
   └─ GET /config/packages?status=REVIEWED → 返回列表
   └─ 验证列表包含刚更新的记录

7. 删除配置包
   └─ DELETE /config/packages/{id} → 删除成功
   └─ 验证查询返回 404 或空
```

### 4.3 示例测试数据

```json
{
  "packageCode": "SMOKE_TEST_PKG",
  "packageVersion": "1.0.0",
  "packageName": "Smoke 测试配置包",
  "packageType": "PATHWAY",
  "targetObjectType": "DISEASE",
  "targetObjectCode": "AMI",
  "targetObjectName": "急性心肌梗死",
  "status": "DRAFT",
  "description": "Smoke 测试用配置包"
}
```

## 5. 验收标准

### 5.1 开发库 (LOCAL_H2_FILE)
- [ ] 所有 TC-001 ~ TC-011 通过
- [ ] 无数据库连接错误
- [ ] 无 SQL 语法错误

### 5.2 生产库 (Oracle/DM/PostgreSQL)
- [ ] DDL 执行无错误
- [ ] CRUD 操作正常
- [ ] 中文字段存储和查询正确
- [ ] JSON 字段序列化/反序列化正确

## 6. 执行命令

```powershell
# 开发库验证
.\zy-engine-mvp\scripts\run-config-package-smoke.ps1 -DbType LOCAL_H2_FILE

# 生产库验证 (有环境时)
.\zy-engine-mvp\scripts\run-config-package-smoke.ps1 -DbType ORACLE
.\zy-engine-mvp\scripts\run-config-package-smoke.ps1 -DbType DM
.\zy-engine-mvp\scripts\run-config-package-smoke.ps1 -DbType POSTGRES
```

## 7. 风险和注意事项

1. **无生产库时**：必须先通过 LOCAL_H2_FILE 验证，生产库 smoke 标为 SKIPPED
2. **JSON 字段**：Oracle 中 BLOB/CLOB 存储 JSON 需验证序列化正确性
3. **中文字符**：确保数据库字符集支持中文存储
4. **并发测试**：当前 smoke 不包含并发场景，后续可扩展

## 8. 测试报告模板

```
=== CFG_CONFIG_PACKAGE Smoke Test Report ===
Date: YYYY-MM-DD
Environment: LOCAL_H2_FILE | ORACLE | DM | POSTGRES

[TC-001] DDL 创建表: PASS | FAIL
[TC-002] 字段约束验证: PASS | FAIL
[TC-003] 索引创建验证: PASS | FAIL
[TC-004] 新增配置包: PASS | FAIL
[TC-005] 查询配置包: PASS | FAIL
[TC-006] 更新配置包: PASS | FAIL
[TC-007] 按状态查询: PASS | FAIL
[TC-008] 删除配置包: PASS | FAIL
[TC-009] 数据库优先查询: PASS | FAIL
[TC-010] 回退到内存查询: PASS | FAIL
[TC-011] 状态流转验证: PASS | FAIL

Result: ALL PASSED | SOME FAILED | SKIPPED
Notes: [备注信息]
```
