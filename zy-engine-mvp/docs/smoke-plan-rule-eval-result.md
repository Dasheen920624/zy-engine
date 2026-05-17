# RE_RULE_EVAL_RESULT Smoke 测试计划

## 1. 测试目标

验证 `re_rule_eval_result` 表的 DDL 正确性、Repository 持久化层功能以及跨实例查询能力。

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
| TC-001 | 执行 DDL 脚本创建 re_rule_eval_result 表 | 表创建成功，无语法错误 |
| TC-002 | 验证字段类型和约束 | 所有字段类型正确，主键/非空约束生效 |
| TC-003 | 验证索引创建 | 5 个索引存在（eval_id、rule、patient、org、time） |

### 3.2 Repository CRUD 操作 (TC-004 ~ TC-010)

| 编号 | 用例 | 操作 | 预期结果 |
|------|------|------|----------|
| TC-004 | 新增评估结果 | save() | 记录插入成功，ID 和 evalId 自动生成 |
| TC-005 | 根据 evalId 查询 | findByEvalId() | 返回正确记录列表 |
| TC-006 | 根据 evalId + ruleCode 查询 | findByEvalIdAndRuleCode() | 返回单条记录 |
| TC-007 | 根据患者+就诊查询 | findByPatientAndEncounter() | 返回按时间倒序的记录 |
| TC-008 | 根据组织上下文查询 | findByOrgContext() | 返回指定组织的记录 |
| TC-009 | 查询命中的结果 | findHitResults() | 只返回 hit_flag=1 的记录 |
| TC-010 | 统计数量 | countByTenantAndRule() | 返回正确数量 |

### 3.3 跨实例查询 (TC-011 ~ TC-013)

| 编号 | 用例 | 操作 | 预期结果 |
|------|------|------|----------|
| TC-011 | 不同租户数据隔离 | 跨租户查询 | 只返回指定租户的数据 |
| TC-012 | 不同医院数据隔离 | 跨医院查询 | 只返回指定医院的数据 |
| TC-013 | 不同科室数据隔离 | 跨科室查询 | 只返回指定科室的数据 |

### 3.4 数据清理 (TC-014)

| 编号 | 用例 | 操作 | 预期结果 |
|------|------|------|----------|
| TC-014 | 删除历史数据 | deleteBeforeTime() | 删除指定时间之前的数据 |

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

3. 执行规则评估
   └─ POST /rules/evaluate → 执行成功
   └─ 验证返回结果包含 evalId

4. 查询评估结果
   └─ GET /rules/eval-results/{evalId} → 返回结果列表
   └─ 验证列表包含刚执行的规则结果

5. 跨实例查询
   └─ GET /rules/eval-results?tenantId=xxx&hospitalCode=yyy → 返回过滤结果
   └─ 验证只返回指定组织的数据

6. 统计查询
   └─ GET /rules/eval-results/count?tenantId=xxx&ruleCode=yyy → 返回数量
   └─ 验证数量正确
```

### 4.3 示例测试数据

```json
{
  "ruleCode": "AMI_RISK_001",
  "ruleVersion": "1.0.0",
  "patientId": "P_TEST_001",
  "encounterId": "E_TEST_001",
  "tenantId": "TENANT_001",
  "hospitalCode": "HOSP_001",
  "departmentCode": "DEPT_001"
}
```

## 5. 验收标准

### 5.1 开发库 (LOCAL_H2_FILE)
- [ ] 所有 TC-001 ~ TC-014 通过
- [ ] 无数据库连接错误
- [ ] 无 SQL 语法错误

### 5.2 生产库 (Oracle/DM/PostgreSQL)
- [ ] DDL 执行无错误
- [ ] CRUD 操作正常
- [ ] 中文字段存储和查询正确
- [ ] JSON 字段序列化/反序列化正确
- [ ] 跨实例查询性能可接受

## 6. 执行命令

```powershell
# 开发库验证
.\zy-engine-mvp\scripts\run-rule-eval-result-smoke.ps1 -DbType LOCAL_H2_FILE

# 生产库验证 (有环境时)
.\zy-engine-mvp\scripts\run-rule-eval-result-smoke.ps1 -DbType ORACLE
.\zy-engine-mvp\scripts\run-rule-eval-result-smoke.ps1 -DbType DM
.\zy-engine-mvp\scripts\run-rule-eval-result-smoke.ps1 -DbType POSTGRES
```

## 7. 风险和注意事项

1. **无生产库时**：必须先通过 LOCAL_H2_FILE 验证，生产库 smoke 标为 SKIPPED
2. **JSON 字段**：Oracle 中 CLOB 存储 JSON 需验证序列化正确性
3. **中文字符**：确保数据库字符集支持中文存储
4. **性能测试**：跨实例查询可能涉及大量数据，需关注查询性能
5. **数据清理**：定期清理历史数据，避免表膨胀

## 8. 测试报告模板

```
=== RE_RULE_EVAL_RESULT Smoke Test Report ===
Date: YYYY-MM-DD
Environment: LOCAL_H2_FILE | ORACLE | DM | POSTGRES

[TC-001] DDL 创建表: PASS | FAIL
[TC-002] 字段约束验证: PASS | FAIL
[TC-003] 索引创建验证: PASS | FAIL
[TC-004] 新增评估结果: PASS | FAIL
[TC-005] evalId 查询: PASS | FAIL
[TC-006] evalId+ruleCode 查询: PASS | FAIL
[TC-007] 患者+就诊查询: PASS | FAIL
[TC-008] 组织上下文查询: PASS | FAIL
[TC-009] 命中结果查询: PASS | FAIL
[TC-010] 统计数量: PASS | FAIL
[TC-011] 租户数据隔离: PASS | FAIL
[TC-012] 医院数据隔离: PASS | FAIL
[TC-013] 科室数据隔离: PASS | FAIL
[TC-014] 删除历史数据: PASS | FAIL

Result: ALL PASSED | SOME FAILED | SKIPPED
Notes: [备注信息]
```
