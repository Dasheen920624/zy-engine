# 后端开发提示词模板

请基于以下资料实现指定后端任务。开始前按最小阅读策略执行：

- 必读：`medkernel-mvp/docs/00_总入口与AI接手导航.md`
- 只读任务行：`medkernel-mvp/docs/02_任务台账.md`
- 后端任务补读：`medkernel-mvp/docs/06_后端开发规范.md`
- 涉及 DDL / 持久化 / 开发库 / 生产库时再补读：`medkernel-mvp/docs/数据库Provider与离线AI开发约定.md`
- 涉及架构 Provider 或跨模块时再补读：`medkernel-mvp/docs/05_架构总图与服务边界.md`

## 模块

【填写：配置包 / 组织模型 / 来源追溯 / 路径引擎 / 规则引擎 / 图谱引擎 / Dify 工作流 / 标准化中心 / 适配器中心 / 审计运维 / 安全权限】

## 任务编号

【填写任务编号，如 PKG-001、ORG-001、PROV-001、TERM-001、RULE-001】

## 输入资料

- 最小入口：`medkernel-mvp/docs/00_总入口与AI接手导航.md`
- 任务台账：`medkernel-mvp/docs/02_任务台账.md`
- 业务核查（医学/医保/质控任务才读）：`medkernel-mvp/docs/产品功能业务核查与开工清单.md`
- 产品总纲（范围不清时才读）：`medkernel-mvp/docs/产品化方案与AI开发编排.md`
- OpenAPI：`ai-dev-input/02_api_contracts/engines.openapi.yaml`
- JSON Schema：`ai-dev-input/03_data_models/...`
- DDL：`ai-dev-input/04_database/oracle/core_ddl.sql`、`ai-dev-input/04_database/dm/core_ddl.sql`、`ai-dev-input/04_database/postgres/core_ddl.sql`、`ai-dev-input/04_database/local/h2_core_ddl.sql`
- 样例数据：`ai-dev-input/06_samples/...`
- 测试用例：`ai-dev-input/07_tests/...`

## 开发要求

1. 严格遵守模块边界。
2. 所有接口返回统一 `ApiResult`。
3. 所有调用链透传 `traceId`。
4. 所有核心配置支持版本、状态、审核、发布、回滚和审计。
5. 生产库与开发库必须分离：Oracle 是当前生产权威库；达梦、PostgreSQL、KingbaseES 是生产交付兼容库；LOCAL_H2_FILE 只作为 AI/离线开发本地文件库。
6. DB-only 模式必须可运行，不能强依赖 Neo4j 或 Dify。
7. 数据库访问需考虑 Oracle、达梦、PostgreSQL/Kingbase 兼容；无生产库环境时必须走 LOCAL_H2_FILE 开发库验证。
8. 不允许把医院差异、规则、路径、字典硬编码在业务代码中。
9. 新增外部依赖必须说明降级策略。
10. 新增关键操作必须写审计或说明暂不写的原因。
11. 涉及医学、医保、质控依据的配置，必须支持来源追溯；缺来源、来源过期或来源未审核时不得发布。
12. 涉及表结构、索引、约束、迁移脚本、持久化 SQL 或落库行为时，必须同步 Oracle/达梦/PostgreSQL-Kingbase/LOCAL_H2_FILE DDL；有生产库环境时执行对应生产库 smoke，无生产库环境时至少执行 LOCAL_H2_FILE 开发库验证，不能只用内存/JUnit 验收。
13. 新增或修改表、字段、索引、约束、迁移脚本时，必须补齐中文表/字段备注；H2 开发库备注不能替代生产库备注。
14. 公共 Controller、Service、Provider、复杂规则、发布/回滚/同步、医学/医保/质控判断必须有简洁意图注释，避免无意义逐行注释。
15. 客户可见、医学/医保/质控、数据库持久化、前端页面、发布同步、安全权限功能必须创建 `ai-dev-input/13_feature_acceptance/` 验收记录；未达 GOLD 的功能进入专业优化队列。
16. 补充契约测试和必要集成测试。

## 必须输出

1. 任务理解和边界。
2. 目标角色、业务闭环和客户验收故事线。
3. 修改文件列表。
4. 代码实现。
5. 测试用例。
6. 文档和样例更新。
7. claim/git 状态同步：claim_id、是否已推送、开发前 git 状态、心跳、任务台账状态。
8. review 和功能验收：review_id、open_findings、feature_acceptance_id、质量等级、优化队列状态。
9. 表/字段备注与必要代码注释说明。
10. 本地启动和验证命令。
11. 风险与未完成项。

## 验证要求

提交前必须执行：

```powershell
.\medkernel-mvp\scripts\check-ai-collaboration.ps1
.\medkernel-mvp\scripts\run-tests.ps1
.\medkernel-mvp\scripts\build.ps1
git diff --check
```

若任务涉及落库，还必须先区分生产库和开发库。无 Oracle/内网环境时执行：

```powershell
.\medkernel-mvp\scripts\detect-db-env.ps1 -BootstrapLocal
.\medkernel-mvp\scripts\start-local-db.ps1
```

有 Oracle 生产权威库时执行：

```powershell
.\medkernel-mvp\scripts\run-oracle-ddl.ps1
.\medkernel-mvp\scripts\run-oracle-org-smoke.ps1
```

Oracle 脚本会自动读取仓库根目录 `.env.oracle.local`。`.env.oracle.local.example` 已提交用于说明连接目标；真实 `.env.oracle.local` 仅为本地忽略文件，不允许提交。若无法连接生产库，必须说明已完成的 LOCAL_H2_FILE 开发库验证、未验证原因、影响范围和补验计划。

若命令失败，必须说明失败原因、影响范围和修复计划。

## 提交要求

1. 先运行 `git status -sb` 确认工作树。
2. 运行 `.\medkernel-mvp\scripts\check-ai-collaboration.ps1` 检查 active claim、pending review 和功能验收队列。
3. 确认已创建并推送 `ai-dev-input/10_task_claims/active/<claim_id>.md`，并把开发前 `git status -sb` 写入 claim。
4. 自主开发时确认已创建或更新 `ai-dev-input/12_autonomous_runs/active/<run_id>.md`。
5. 完成开发后创建 `ai-dev-input/11_ai_reviews/pending/<review_id>.md`，通过质量评审和整改复评。
6. 需要功能验收时，创建或更新 `ai-dev-input/13_feature_acceptance/<status>/<feature_acceptance_id>.md`。
7. 只有 `review_status=APPROVED` 且 `open_findings=0` 后，才能正式提交业务代码；需要客户验收的功能还必须有功能验收结论。
8. 只暂存本任务相关文件。
9. 每完成一个明确任务并通过质量门禁后，必须提交一次，不得把已完成任务长期留在本地未提交状态。
10. 提交信息使用中文短句，例如：

```text
增加配置包发布模型
补充组织上下文接口
增加医嘱标准化映射
```

11. 提交成功后必须立即推送到远端当前分支，确保其它 AI 可以拉取最新项目。
12. 最终回复必须包含改动摘要、验证结果、claim_id、review_id、feature_acceptance_id、open_findings、run_id、提交 hash、推送分支；如未提交或未推送，必须说明原因、影响和替代交接方式。
