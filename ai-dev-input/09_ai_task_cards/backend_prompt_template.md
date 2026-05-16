# 后端开发提示词模板

请基于以下资料实现指定后端任务。开始前必须先阅读：

- `zy-engine-mvp/docs/AI接手执行手册.md`
- `zy-engine-mvp/docs/顶级多角色评审与AI并行开发总控.md`
- `zy-engine-mvp/docs/产品化方案与AI开发编排.md`
- `zy-engine-mvp/docs/全功能蓝图与并行开发计划.md`
- `ai-dev-input/09_ai_task_cards/ai_system_prompt.md`

## 模块

【填写：配置包 / 组织模型 / 来源追溯 / 路径引擎 / 规则引擎 / 图谱引擎 / Dify 工作流 / 标准化中心 / 适配器中心 / 审计运维 / 安全权限】

## 任务编号

【填写任务编号，如 PKG-001、ORG-001、PROV-001、TERM-001、RULE-001】

## 输入资料

- 产品总纲：`zy-engine-mvp/docs/产品化方案与AI开发编排.md`
- 全功能蓝图：`zy-engine-mvp/docs/全功能蓝图与并行开发计划.md`
- 顶级总控：`zy-engine-mvp/docs/顶级多角色评审与AI并行开发总控.md`
- 接手手册：`zy-engine-mvp/docs/AI接手执行手册.md`
- OpenAPI：`ai-dev-input/02_api_contracts/engines.openapi.yaml`
- JSON Schema：`ai-dev-input/03_data_models/...`
- DDL：`ai-dev-input/04_database/oracle/core_ddl.sql`、`ai-dev-input/04_database/dm/core_ddl.sql`
- 样例数据：`ai-dev-input/06_samples/...`
- 测试用例：`ai-dev-input/07_tests/...`

## 开发要求

1. 严格遵守模块边界。
2. 所有接口返回统一 `ApiResult`。
3. 所有调用链透传 `traceId`。
4. 所有核心配置支持版本、状态、审核、发布、回滚和审计。
5. Oracle/关系型数据库是主数据源；Neo4j、Dify、第三方系统是 Provider 或 Adapter。
6. DB-only 模式必须可运行，不能强依赖 Neo4j 或 Dify。
7. 数据库访问需考虑 Oracle 和达梦兼容。
8. 不允许把医院差异、规则、路径、字典硬编码在业务代码中。
9. 新增外部依赖必须说明降级策略。
10. 新增关键操作必须写审计或说明暂不写的原因。
11. 涉及医学、医保、质控依据的配置，必须支持来源追溯；缺来源、来源过期或来源未审核时不得发布。
12. 涉及 Oracle 表结构、索引、约束、迁移脚本、持久化 SQL 或落库行为时，必须同步真实 Oracle 并执行 Oracle 版本 smoke，不能只用内存/JUnit 验收。
13. 补充契约测试和必要集成测试。

## 必须输出

1. 任务理解和边界。
2. 修改文件列表。
3. 代码实现。
4. 测试用例。
5. 文档和样例更新。
6. 本地启动和验证命令。
7. 风险与未完成项。

## 验证要求

提交前必须执行：

```powershell
.\zy-engine-mvp\scripts\run-tests.ps1
.\zy-engine-mvp\scripts\build.ps1
git diff --check
```

若任务涉及 Oracle 落库，还必须执行：

```powershell
.\zy-engine-mvp\scripts\run-oracle-ddl.ps1
.\zy-engine-mvp\scripts\run-oracle-org-smoke.ps1
```

Oracle 脚本会自动读取仓库根目录 `.env.oracle.local`。`.env.oracle.local.example` 已提交用于说明连接目标；真实 `.env.oracle.local` 仅为本地忽略文件，不允许提交。若无法连接 Oracle，必须说明未验证原因、影响范围和补验计划。

若命令失败，必须说明失败原因、影响范围和修复计划。

## 提交要求

1. 先运行 `git status -sb` 确认工作树。
2. 只暂存本任务相关文件。
3. 每完成一个明确任务，必须提交一次，不得把已完成任务长期留在本地未提交状态。
4. 提交信息使用中文短句，例如：

```text
增加配置包发布模型
补充组织上下文接口
增加医嘱标准化映射
```

5. 提交成功后必须立即推送到远端当前分支，确保其它 AI 可以拉取最新项目。
6. 最终回复必须包含改动摘要、验证结果、提交 hash、推送分支；如未提交或未推送，必须说明原因、影响和替代交接方式。
