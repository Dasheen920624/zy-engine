# AI 研发输入包

本目录是给 AI 编码工具和研发团队使用的可执行规格包。它提供 OpenAPI、JSON Schema、DDL、样例数据、测试矩阵和任务卡模板。

后续开发的最终约定不只在本目录内。接手 AI 必须先阅读：

1. `../zy-engine-mvp/docs/AI接手执行手册.md`
2. `../zy-engine-mvp/docs/产品化方案与AI开发编排.md`
3. `09_ai_task_cards/ai_system_prompt.md`

## 目录内容

1. `02_api_contracts/engines.openapi.yaml`
   路径引擎、规则引擎、图谱引擎、Dify 适配、字典映射、适配器中心的历史 OpenAPI 契约。

2. `03_data_models/*.schema.json`
   临床事件、患者上下文、路径配置、规则 DSL、图谱查询、推荐卡片等 JSON Schema。

3. `04_database/oracle/core_ddl.sql`
   Oracle 核心表结构。

4. `04_database/dm/core_ddl.sql`
   达梦数据库核心表结构。

5. `06_samples/*.json`
   AMI/STEMI 样例路径、规则、患者上下文、图谱、Dify、字典、适配器数据。

6. `07_tests/*`
   测试矩阵和可执行验收用例定义。

7. `09_ai_task_cards/*`
   AI 系统提示词、后端提示词模板、任务卡模板和历史任务清单。

## 当前开发使用顺序

建议按以下顺序交给 AI 编码工具或研发团队：

1. 阅读 `../zy-engine-mvp/docs/AI接手执行手册.md`。
2. 阅读 `../zy-engine-mvp/docs/产品化方案与AI开发编排.md`。
3. 阅读 `09_ai_task_cards/ai_system_prompt.md`。
4. 根据用户需求或总纲中的当前优先任务池选择任务编号。
5. 使用 `09_ai_task_cards/task_card_template.md` 补全任务卡。
6. 开发时同步修改后端代码、测试、样例、API 示例和文档。
7. 使用 `../zy-engine-mvp/scripts/run-tests.ps1`、`../zy-engine-mvp/scripts/build.ps1`、`git diff --check` 验证。

## 当前优先任务池

以 `../zy-engine-mvp/docs/产品化方案与AI开发编排.md` 为准，当前优先：

- `PKG-001` 配置包统一模型。
- `ORG-001` 集团/医院/院区/站点/科室组织模型。
- `TERM-001` 医嘱标准化和未映射治理。
- `RULE-001` 第三方规则引擎 API。
- `GRAPH-001` 图谱包发布与 Oracle 到 Neo4j 同步。
- `DIFY-001` Dify 契约绑定和调用回放。

## 关键边界

- 路径引擎保存患者路径状态。
- 规则引擎执行确定性判断和质控规则。
- 图谱引擎封装 DB/Neo4j 查询，不暴露任意 Cypher 执行。
- Dify 只负责编排工具和生成解释，不保存核心状态。
- Oracle/关系型数据库是配置、版本、审计和业务运行记录的主数据源。
- Neo4j 和 Dify 必须是可插拔 Provider，不能成为 DB-only 测试环境强依赖。
- 所有临床建议必须由医生确认。
- 所有核心配置必须支持版本、审核、发布、审计和回滚。
- 医院差异必须通过组织范围、配置包、字典映射和适配器绑定实现。

## 历史资料说明

本目录内部分 P0/P1 文档是早期 MVP 阶段资料。若旧文档和最新产品化总纲冲突，以：

```text
../zy-engine-mvp/docs/产品化方案与AI开发编排.md
../zy-engine-mvp/docs/AI接手执行手册.md
```

为准。
