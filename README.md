# 集团化医疗智能引擎平台

本仓库用于管理“专科诊疗路径智能管理平台 / 医疗智能引擎平台”的产品方案、AI 研发输入包、后端 MVP、数据库脚本、接口示例、测试和交接文档。

项目目标是把路径引擎、规则引擎、图谱引擎、Dify/AI 工作流、字典标准化、适配器中心、质控审计能力产品化，支持单医院、集团化医院、多院区、卫生所/站点等多组织形态部署。

## 最高优先级入口

后续任何 AI 或开发者接手前，必须按顺序阅读：

1. [AI接手执行手册](zy-engine-mvp/docs/AI接手执行手册.md)
2. [产品化方案与AI开发编排](zy-engine-mvp/docs/产品化方案与AI开发编排.md)
3. [后端 MVP README](zy-engine-mvp/README.md)
4. [AI研发输入包 README](ai-dev-input/README.md)
5. [AI开发系统提示词](ai-dev-input/09_ai_task_cards/ai_system_prompt.md)

其中 `产品化方案与AI开发编排.md` 是产品和架构总纲，`AI接手执行手册.md` 是后续 AI 的执行入口。

## 当前产品原则

- Oracle/关系型数据库是配置、版本、审计和运行记录的主数据源。
- Neo4j 是图谱查询投影，不是唯一主数据源。
- Dify 是 AI 工作流执行目标，不保存核心业务状态。
- DB-only 测试环境必须完整可运行，不能强依赖 Neo4j 或 Dify。
- 医院差异必须通过组织范围、配置包、字典映射、适配器绑定和 Provider 实现。
- 路径、规则、图谱、Dify 模板、字典、适配器都必须版本化、可审核、可发布、可回滚。
- 临床建议必须可解释，并由医生最终确认。

## 目录说明

- [zy-engine-mvp](zy-engine-mvp/README.md)：后端 MVP 工程，包含 Java/Spring Boot 代码、测试、脚本、运行说明。
- [ai-dev-input](ai-dev-input/README.md)：AI 可执行规格包，包含 OpenAPI、JSON Schema、DDL、样例数据、测试矩阵和任务卡。
- [zy-engine-mvp/docs](zy-engine-mvp/docs)：产品化总纲、AI 接手手册、API 示例、编码规范、研发任务拆解。
- [ai-dev-input/09_ai_task_cards](ai-dev-input/09_ai_task_cards)：AI 系统提示词、后端提示词模板、任务卡模板、历史 P0 任务。
- [项目总控计划.md](项目总控计划.md)：早期总控计划，后续以 `zy-engine-mvp/docs` 下的最新总纲为准。
- 早期产品方案和原型 HTML：用于理解业务背景，不能替代当前开发约定。

## 当前后端能力

后端工程位于：

```text
zy-engine-mvp/
```

已具备：

- JDK 1.8 兼容。
- DB-only / 内存态测试运行。
- Oracle 可选持久化。
- Provider 运行状态接口：`GET /api/system/providers`。
- AMI/STEMI 候选推荐、医生确认入径、节点流转。
- 路径版本发布、diff、回滚。
- 路径变异、节点完成率、节点滞留时长、质控聚合指标。
- 规则 DSL、规则包导入、包级 review、批量发布、执行日志和统计。
- 图谱版本、节点、边、证据导入与查询，Neo4j 不可用时降级。
- Dify 工作流模板、参数映射、重试、降级输出、调用统计。
- 字典映射和适配器 Mock。
- 审计日志查询与汇总。

常用命令：

```powershell
cd zy-engine-mvp
.\scripts\run-tests.cmd
.\scripts\build.cmd
.\scripts\start-memory.cmd
.\scripts\verify-encoding.cmd
```

健康检查：

```text
http://localhost:18080/zy-engine/api/health
http://localhost:18080/zy-engine/api/system/providers
```

## 后续开发主线

当前后续优先级以 [产品化方案与AI开发编排](zy-engine-mvp/docs/产品化方案与AI开发编排.md) 为准。近期优先任务池：

1. `PKG-001` 配置包统一模型。
2. `ORG-001` 集团/医院/院区/站点/科室组织模型。
3. `TERM-001` 医嘱标准化和未映射治理。
4. `RULE-001` 第三方规则引擎 API，支持病历质控、医保质控、医嘱安全拦截。
5. `GRAPH-001` 图谱包发布与 Oracle 到 Neo4j 同步。
6. `DIFY-001` Dify 契约绑定和调用回放。

## 每批开发硬性流程

后续 AI 开发必须遵守：

1. 先读 `AI接手执行手册.md` 和 `产品化方案与AI开发编排.md`。
2. 运行 `git status -sb`，确认工作树状态。
3. 明确任务编号、边界、验收标准。
4. 实现代码时同步更新测试、样例、API 示例和文档。
5. 执行：

```powershell
.\zy-engine-mvp\scripts\run-tests.ps1
.\zy-engine-mvp\scripts\build.ps1
git diff --check
```

6. 只提交本任务相关文件。
7. 最终回复必须说明改动、验证结果、未覆盖风险。

## 重要红线

- 不允许硬编码单医院逻辑。
- 不允许把 Neo4j/Dify 做成测试环境强依赖。
- 不允许外部系统传任意 SQL/Cypher 到核心引擎执行。
- 不允许配置静默覆盖已发布版本。
- 不允许关键医疗建议绕过医生确认。
- 不允许发布、回滚、同步绕过审计。
