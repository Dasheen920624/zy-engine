# Feature Acceptance: TEST-007 测试数据工厂和验收数据集

fa_id: FA-TEST-007-S01
task_id: TEST-007
feature: 测试数据工厂和验收数据集
grade: SILVER
status: PENDING

## 验收检查

| # | 检查项 | 结果 | 证据 |
|---|--------|------|------|
| 1 | 数据集 JSON 格式正确 | PASS | 7 个类别数据集文件 |
| 2 | 数据集可复用 | PASS | patient_id 跨数据集关联 |
| 3 | 数据工厂脚本可执行 | PASS | run-test-data-factory.ps1 |
| 4 | 验收重放脚本可执行 | PASS | run-acceptance-replay.ps1 |
| 5 | 异常场景覆盖 | PASS | error-scenarios.json |
| 6 | 权限验证覆盖 | PASS | permissions.json |
| 7 | build 通过 | PASS | mvn compile 成功 |

## Evidence

```
ai-dev-input/07_tests/datasets/patient/ami-patient.json
ai-dev-input/07_tests/datasets/order/ami-orders.json
ai-dev-input/07_tests/datasets/insurance/insurance-claims.json
ai-dev-input/07_tests/datasets/pathway/pathway-candidates.json
ai-dev-input/07_tests/datasets/config/config-packages.json
ai-dev-input/07_tests/datasets/security/permissions.json
ai-dev-input/07_tests/datasets/edge-cases/error-scenarios.json
medkernel-mvp/scripts/run-test-data-factory.ps1
medkernel-mvp/scripts/run-acceptance-replay.ps1
```
