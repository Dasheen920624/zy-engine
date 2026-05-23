# 测试数据工厂 - 数据集索引

## 目录结构

| 目录 | 说明 | 数据集 |
|------|------|--------|
| `patient/` | 患者/病历数据 | AMI-PATIENT-001 |
| `order/` | 医嘱数据 | AMI-ORDERS-001 |
| `insurance/` | 医保数据 | INSURANCE-CLAIMS-001 |
| `pathway/` | 路径候选数据 | PATHWAY-CANDIDATES-001 |
| `config/` | 配置包数据 | CONFIG-PACKAGES-001 |
| `security/` | 权限角色数据 | SECURITY-PERMS-001 |
| `edge-cases/` | 异常边界数据 | ERROR-SCENARIOS-001 |

## 使用方式

1. 数据工厂脚本 `run-test-data-factory.ps1` 自动加载这些数据集
2. 每个 JSON 文件可独立使用，也可组合使用
3. `patient_id` 字段用于跨数据集关联

## 数据集规范

- 每个 JSON 文件包含 `dataset_id`、`description`、`category`、`version`、`records`
- `records` 数组中的每条记录必须有唯一 `id`
- 跨数据集引用使用 `patient_id` 等外键字段
- 所有时间字段使用 ISO 8601 格式
