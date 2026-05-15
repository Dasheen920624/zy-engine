# 后端开发提示词模板

请基于以下资料实现指定模块。

## 模块

【填写：路径引擎 / 规则引擎 / 图谱引擎 / 字典映射 / 适配器中心 / Dify适配】

## 任务编号

【填写任务编号，如 PE-001】

## 输入资料

- OpenAPI：`ai-dev-input/02_api_contracts/engines.openapi.yaml`
- JSON Schema：`ai-dev-input/03_data_models/...`
- DDL：`ai-dev-input/04_database/oracle/core_ddl.sql`、`ai-dev-input/04_database/dm/core_ddl.sql`
- 样例数据：`ai-dev-input/06_samples/...`
- 测试用例：`ai-dev-input/07_tests/...`

## 开发要求

1. 严格遵守模块边界。
2. 所有接口返回统一ApiResult。
3. 所有调用链透传trace_id。
4. 所有配置支持版本和状态。
5. 数据库访问需兼容Oracle和达梦。
6. 不允许把规则、路径、字典硬编码在业务代码中。
7. 补充单元测试和必要集成测试。

## 输出要求

1. 修改文件列表。
2. 代码实现。
3. 单元测试。
4. 数据库脚本或迁移说明。
5. 本地启动和验证命令。
6. 风险与未完成项。

