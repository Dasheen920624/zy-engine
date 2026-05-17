# AI 研发输入包

本目录是给 AI 编码工具和研发团队使用的可执行规格包。它提供 OpenAPI、JSON Schema、DDL、样例数据、测试矩阵和任务卡模板。

后续开发的最终约定不只在本目录内。接手 AI 必须先阅读 **唯一总入口**：

```text
../zy-engine-mvp/docs/00_总入口与AI接手导航.md
```

总入口会按需把你路由到下列详情文档：

1. `../zy-engine-mvp/docs/01_多角色诉求矩阵.md`
2. `../zy-engine-mvp/docs/02_任务台账.md`
3. `../zy-engine-mvp/docs/03_AI能力分级与并行冲突规约.md`
4. `../zy-engine-mvp/docs/04_客户验收剧本与报告模板.md`
5. `../zy-engine-mvp/docs/AI接手执行手册.md`
6. `../zy-engine-mvp/docs/产品化方案与AI开发编排.md`
7. `../zy-engine-mvp/docs/顶级多角色评审与AI并行开发总控.md`
8. `../zy-engine-mvp/docs/全功能蓝图与并行开发计划.md`
9. `../zy-engine-mvp/docs/前端配置平台规划与开发验证.md`
10. `../zy-engine-mvp/docs/前端产品交互与视觉规范.md`
11. `../zy-engine-mvp/docs/AI自主开发运行守则.md`
12. `../zy-engine-mvp/docs/AI任务认领与并行开发机制.md`
13. `../zy-engine-mvp/docs/AI开发质量门禁与评审整改机制.md`
14. `../zy-engine-mvp/docs/数据库Provider与离线AI开发约定.md`
15. `../zy-engine-mvp/docs/产品功能业务核查与开工清单.md`
16. `../zy-engine-mvp/docs/AI医疗知识工厂与字典映射方案.md`
17. `09_ai_task_cards/ai_system_prompt.md`

## 目录内容

1. `02_api_contracts/engines.openapi.yaml`
   路径引擎、规则引擎、图谱引擎、Dify 适配、字典映射、适配器中心和配置包的历史 OpenAPI 契约。

2. `03_data_models/*.schema.json`
   临床事件、患者上下文、路径配置、规则 DSL、图谱查询、推荐卡片等 JSON Schema。

3. `04_database/oracle/core_ddl.sql`
   Oracle 核心表结构。

4. `04_database/dm/core_ddl.sql`
   达梦数据库核心表结构。

5. `04_database/local/h2_core_ddl.sql`
   AI/离线开发用 H2 本地文件数据库结构；Oracle/达梦仍是交付权威结构。

6. `06_samples/*.json`
   AMI/STEMI 样例路径、规则、患者上下文、组织上下文、组织目录、图谱、Dify、字典、适配器和配置包数据。
   其中 `sample_config_package_source_blocked.json` 用于演示配置包 `source_review` 阻断发布。

7. `07_tests/*`
   测试矩阵和可执行验收用例定义。

8. `09_ai_task_cards/*`
   AI 系统提示词、后端提示词模板、任务卡模板和历史任务清单。

9. `10_task_claims/*`
   多 AI 并行开发的任务认领、阻塞和归档记录；未成功推送 active claim 前不能修改业务代码。

10. `11_ai_reviews/*`
    多 AI 开发质量评审、质控问题、整改复评和放行记录；未通过 review 的业务代码不能正式提交。

11. `12_autonomous_runs/*`
    AI 自主开发运行记录，记录任务选择、claim/review 列表、验证结果、停机原因和交接信息。

## 当前开发使用顺序

建议按以下顺序交给 AI 编码工具或研发团队：

1. 阅读 `../zy-engine-mvp/docs/AI接手执行手册.md`。
2. 阅读 `../zy-engine-mvp/docs/AI自主开发运行守则.md`。
3. 阅读 `../zy-engine-mvp/docs/AI任务认领与并行开发机制.md`。
4. 阅读 `../zy-engine-mvp/docs/AI开发质量门禁与评审整改机制.md`。
5. 阅读 `../zy-engine-mvp/docs/数据库Provider与离线AI开发约定.md`。
6. 阅读 `../zy-engine-mvp/docs/产品化方案与AI开发编排.md`。
7. 阅读 `../zy-engine-mvp/docs/产品功能业务核查与开工清单.md`。
8. 阅读 `../zy-engine-mvp/docs/全功能蓝图与并行开发计划.md`。
9. 若涉及前端，阅读 `../zy-engine-mvp/docs/前端配置平台规划与开发验证.md`。
10. 阅读 `09_ai_task_cards/ai_system_prompt.md`。
11. 执行 `../zy-engine-mvp/scripts/detect-db-env.ps1 -BootstrapLocal`，确认使用 Oracle 或 LOCAL_H2。
12. 自主开发时，在 `12_autonomous_runs/active/<run_id>.md` 创建运行记录。
13. 根据用户需求、业务核查清单或总纲中的当前优先任务池选择任务编号并拆出 `claim_id`。
14. 使用 `09_ai_task_cards/task_card_template.md` 补全任务卡。
15. 在 `10_task_claims/active/<claim_id>.md` 创建、提交并推送任务认领文件。
16. 开发时同步修改后端代码、测试、样例、API 示例和文档。
17. 使用 `../zy-engine-mvp/scripts/run-tests.ps1`、`../zy-engine-mvp/scripts/build.ps1`、`git diff --check` 验证。
18. 在 `11_ai_reviews/pending/<review_id>.md` 创建评审记录，完成自检、AI 评审、整改和复评。
19. 无 Oracle 环境时使用 LOCAL_H2 模式跑对应 smoke；有 Oracle 环境时补 Oracle DDL 和 Oracle smoke。
20. `review_status=APPROVED` 且 `open_findings=0` 后，才能正式提交业务代码。
21. 完成后归档 claim 到 `10_task_claims/archive/YYYYMMDD/`，归档 review 到 `11_ai_reviews/archive/YYYYMMDD/`；自主运行结束时归档 run log。

## 当前优先任务池

以 `../zy-engine-mvp/docs/产品化方案与AI开发编排.md` 为准，当前优先：

- `PKG-001` 配置包统一模型（第一批已补 `sample_config_package.json` 和后端契约测试）。
- `ORG-001` 集团/医院/院区/站点/科室组织模型（已补 `sample_org_context.json`、`sample_org_units.json`、`/api/system/org-context` 和 `/api/organizations`）。
- `PROV-001` 来源追溯底座（来源文档、引用片段、资产绑定、来源审核、运行证据链）。
- `TERM-001` 医嘱标准化和未映射治理。
- `RULE-001` 第三方规则引擎 API（第一批已补 `sample_rule_engine_scenarios.json` 与 `POST /api/rule-engine/evaluate`）。
- `GRAPH-001` 图谱包发布与 Oracle 到 Neo4j 同步。
- `DIFY-001` Dify 契约绑定和调用回放。
- `FE-001` 前端信息架构、高保真原型和演示脚本。
- `FE-002` 前端工程脚手架。
- `FE-003` 功能演示与规则校验工作台。

## 关键边界

- 路径引擎保存患者路径状态。
- 规则引擎执行确定性判断和质控规则。
- 图谱引擎封装 DB/Neo4j 查询，不暴露任意 Cypher 执行。
- Dify 只负责编排工具和生成解释，不保存核心状态。
- Oracle/关系型数据库是配置、版本、审计和业务运行记录的主数据源。
- Neo4j 和 Dify 必须是可插拔 Provider，不能成为 DB-only 测试环境强依赖。
- 所有临床建议必须由医生确认。
- 所有核心配置必须支持版本、审核、发布、审计和回滚。
- 所有规则、知识、图谱证据、Dify 解释、字典映射、适配器口径和质控结论必须支持来源追溯。
- 医院差异必须通过组织范围、配置包、字典映射和适配器绑定实现。

## 历史资料说明

本目录内部分 P0/P1 文档是早期 MVP 阶段资料。若旧文档和最新产品化总纲冲突，以：

```text
../zy-engine-mvp/docs/产品化方案与AI开发编排.md
../zy-engine-mvp/docs/产品功能业务核查与开工清单.md
../zy-engine-mvp/docs/AI接手执行手册.md
../zy-engine-mvp/docs/AI自主开发运行守则.md
../zy-engine-mvp/docs/AI任务认领与并行开发机制.md
../zy-engine-mvp/docs/AI开发质量门禁与评审整改机制.md
../zy-engine-mvp/docs/数据库Provider与离线AI开发约定.md
../zy-engine-mvp/docs/前端配置平台规划与开发验证.md
```

为准。
