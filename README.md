# 集团化医疗智能引擎平台

本仓库用于管理"专科诊疗路径智能管理平台 / 医疗智能引擎平台"的产品方案、AI 研发输入包、后端 MVP、数据库脚本、接口示例、测试和交接文档。

项目目标是把路径引擎、规则引擎、图谱引擎、Dify/AI 工作流、字典标准化、适配器中心、质控审计能力产品化，支持单医院、集团化医院、多院区、卫生所/站点等多组织形态部署。

## AI 首读（任何能力、任何时刻接手前先读这一份）

> 本仓库面向 **多 AI 并行开发**。接手前请先打开下面 1 份"总入口"文档，按它的导航跳转，不要凭记忆翻阅其它文档。

**唯一首读**：[zy-engine-mvp/docs/00_总入口与AI接手导航.md](zy-engine-mvp/docs/00_总入口与AI接手导航.md)

总入口配套 4 份高密度索引：

1. [01_多角色诉求矩阵](zy-engine-mvp/docs/01_多角色诉求矩阵.md) — 9 角色 × 必看/必交付/否决项/验收口径
2. [02_任务台账](zy-engine-mvp/docs/02_任务台账.md) — 唯一权威任务表，领任务前必读必改
3. [03_AI能力分级与并行冲突规约](zy-engine-mvp/docs/03_AI能力分级与并行冲突规约.md) — 初/中/高级 AI 边界 + 文件冲突仲裁
4. [04_客户验收剧本与报告模板](zy-engine-mvp/docs/04_客户验收剧本与报告模板.md) — 6 类剧本 + 院方签字报告

## 详情文档（按需深入）

**架构与规范（写代码前必读）**

- 架构总图：[05_架构总图与服务边界](zy-engine-mvp/docs/05_架构总图与服务边界.md) — Provider / 跨数据库 / 国产化 / 内网约束
- 后端开发规范：[06_后端开发规范](zy-engine-mvp/docs/06_后端开发规范.md)
- 前端开发规范：[07_前端开发规范](zy-engine-mvp/docs/07_前端开发规范.md)
- 国产化兼容性规约：[08_国产化兼容性规约](zy-engine-mvp/docs/08_国产化兼容性规约.md) — CentOS 7 / 统信 / 麒麟 / 鲲鹏 / 龙芯 / 达梦 / PG / Kingbase
- 内网部署与版本管理：[09_内网部署与版本管理](zy-engine-mvp/docs/09_内网部署与版本管理.md)、[VERSIONING](VERSIONING.md)、[CHANGELOG](CHANGELOG.md)、[deploy/](deploy/README.md)

**产品 / 业务**

- 产品/架构总纲：[产品化方案与AI开发编排](zy-engine-mvp/docs/产品化方案与AI开发编排.md)
- 多角色评审与并行泳道：[顶级多角色评审与AI并行开发总控](zy-engine-mvp/docs/顶级多角色评审与AI并行开发总控.md)
- 全功能蓝图：[全功能蓝图与并行开发计划](zy-engine-mvp/docs/全功能蓝图与并行开发计划.md)
- 产品功能核查：[产品功能业务核查与开工清单](zy-engine-mvp/docs/产品功能业务核查与开工清单.md)
- AI 医疗知识工厂：[AI医疗知识工厂与字典映射方案](zy-engine-mvp/docs/AI医疗知识工厂与字典映射方案.md)
- 前端方案规划：[前端配置平台规划与开发验证](zy-engine-mvp/docs/前端配置平台规划与开发验证.md)
- 前端交互与视觉规范：[前端产品交互与视觉规范](zy-engine-mvp/docs/前端产品交互与视觉规范.md)

**接手与工程**

- 自主开发运行守则：[AI自主开发运行守则](zy-engine-mvp/docs/AI自主开发运行守则.md)
- 任务认领机制：[AI任务认领与并行开发机制](zy-engine-mvp/docs/AI任务认领与并行开发机制.md)
- 质量门禁与评审：[AI开发质量门禁与评审整改机制](zy-engine-mvp/docs/AI开发质量门禁与评审整改机制.md)
- 数据库 Provider 与离线开发：[数据库Provider与离线AI开发约定](zy-engine-mvp/docs/数据库Provider与离线AI开发约定.md)
- 旧入口（仍有效）：[AI接手执行手册](zy-engine-mvp/docs/AI接手执行手册.md)
- 早期评审：[多角色评审与执行计划](zy-engine-mvp/docs/多角色评审与执行计划.md)
- 后端工程：[后端 MVP README](zy-engine-mvp/README.md)
- 前端工程：[前端 README](frontend/README.md)
- 前端高保真原型：[frontend-prototype/index.html](frontend-prototype/index.html)
- 研发输入包：[AI研发输入包 README](ai-dev-input/README.md)
- AI 系统提示词：[AI开发系统提示词](ai-dev-input/09_ai_task_cards/ai_system_prompt.md)

其中 `产品化方案与AI开发编排.md` 是产品和架构总纲，`产品功能业务核查与开工清单.md` 是业务功能、客户验收和 AI 开工优先级的核查入口，`AI接手执行手册.md` 是后续 AI 的执行入口，`前端配置平台规划与开发验证.md` 与 `前端产品交互与视觉规范.md` 是前端配置、演示、规则校验、可视化操作和天蓝白底融合风格的设计入口，`AI医疗知识工厂与字典映射方案.md` 是外网 AI 基准知识包、院内字典映射、Dify/无 Dify 兼容和专家审核闭环的设计入口。

## 当前产品原则

- Oracle/关系型数据库是配置、版本、审计和运行记录的主数据源。
- Neo4j 是图谱查询投影，不是唯一主数据源。
- Dify 是 AI 工作流执行目标，不保存核心业务状态。
- DB-only 测试环境必须完整可运行，不能强依赖 Neo4j 或 Dify。
- 规则引擎必须在无 Neo4j、无 Dify、无大模型的场景下保持最高程度兼容；规则执行、标准化、审计、dry-run、第三方调用和结果回查必须优先依赖本地配置包和关系型数据库完成。
- AI 无法连接内网 Oracle 时，必须先识别数据库环境并使用本地 H2 文件数据库模式开发；Oracle/达梦 DDL 仍必须同步维护，Oracle 是生产权威。
- 医院差异必须通过组织范围、配置包、字典映射、适配器绑定和 Provider 实现。
- 路径、规则、图谱、Dify 模板、字典、适配器都必须版本化、可审核、可发布、可回滚。
- 外网 AI 基准版可以生成医疗知识、字典映射、规则和图谱候选，但 AI 产物必须有来源、版本、模型记录和专业人员审核，不能直接激活。
- 院内是否具备 Dify/大模型能力不能影响核心流程；Dify 和大模型只作为提高候选映射、规则解释、证据补全和复杂复核准确性的增强能力，不能成为规则引擎的必需依赖。
- 临床建议必须可解释，并由医生最终确认。
- 前端必须同时支持配置管理、功能演示、规则校验、质控运营和运维追溯；演示和 dry-run 默认不写正式业务状态。

## 目录说明

- [zy-engine-mvp](zy-engine-mvp/README.md)：后端 MVP 工程，包含 Java/Spring Boot 代码、测试、脚本、运行说明。
- [ai-dev-input](ai-dev-input/README.md)：AI 可执行规格包，包含 OpenAPI、JSON Schema、DDL、样例数据、测试矩阵和任务卡。
- [zy-engine-mvp/docs](zy-engine-mvp/docs)：产品化总纲、AI 接手手册、AI 医疗知识工厂与字典映射方案、前端配置平台规划、API 示例、编码规范、研发任务拆解。
- [ai-dev-input/09_ai_task_cards](ai-dev-input/09_ai_task_cards)：AI 系统提示词、后端提示词模板、任务卡模板、历史 P0 任务。
- [docs/legacy-materials](docs/legacy-materials/README.md)：早期产品方案、演示原型、总控计划和历史说明，仅用于理解业务背景，不能替代当前开发约定。

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
- 组织上下文接口：`GET /api/system/org-context`，支持集团/医院/院区/站点/科室上下文解析。
- 组织目录导入、查询和树形回查接口，真实组织不包含 `PLATFORM`，`PLATFORM` 仅作为系统内置默认基线。
- AMI/STEMI 候选推荐、医生确认入径、节点流转。
- 路径版本发布、diff、回滚。
- 路径变异、节点完成率、节点滞留时长、质控聚合指标。
- 规则 DSL、规则包导入、包级 review、批量发布、执行日志和统计。
- 第三方规则引擎 API：`POST /api/rule-engine/evaluate`，按 `scenario_code`（PATHWAY_ENTRY/EMR_QC/INSURANCE_QC/ORDER_SAFETY/DRUG_INDICATION/EXAM_RATIONALITY）路由到已发布规则，支持 `rule_package_code/rule_package_version/rule_codes` 过滤，未匹配规则时返回 `NO_RULES_MATCHED` 警告而不报错。
- 第三方规则引擎批量与回查：`POST /api/rule-engine/batch-evaluate` 共享场景过滤批量评估，`GET /api/rule-engine/results` 列摘要、`GET /api/rule-engine/results/{resultId}` 取详情；评估结果在内存环形缓冲中保留最近 500 条，便于规则校验工作台抽样复演。
- 规则引擎接入组织上下文：`/api/rule-engine/*` 通过 Header/Query/Body 三方合并（Body 优先）解析集团/医院/院区/站点/科室，评估记录与审计落组织字段，列表查询支持按医院、院区、scope 等过滤。
- 规则引擎核心链路不依赖 Neo4j、Dify 或大模型；增强证据、图谱召回、AI 解释和候选规则生成必须走可降级 Provider。
- 路径运行接入组织上下文：`/api/patient-pathways/admit` 写入 tenant/医院/院区/科室/scope，路径实例、变异、质控指标和审计查询支持显式组织过滤。
- 规则配置接入组织上下文：`/api/rules/*` 支持同一规则编码在不同医院/科室独立导入、发布、回查、模拟和执行日志过滤。
- 图谱版本、节点、边、证据导入与查询，Neo4j 不可用时降级。
- Dify 工作流模板、参数映射、重试、降级输出、调用统计。
- 字典映射和适配器 Mock。
- 配置包导入、review、hash 校验、发布、导出和审计。
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

1. `PKG-001` 配置包统一模型（第一批已启动：导入、review、hash、publish、export）。
2. `ORG-001` 集团/医院/院区/站点/科室组织模型（第一批已启动：组织上下文接口和继承顺序）。
3. `TERM-001` 医嘱标准化和未映射治理。
4. `RULE-CORE-001` 规则引擎 DB-only 兼容加固，确保无 Neo4j、无 Dify、无大模型时核心规则能力完整可用。
5. `AIK-001/SRC-001/TERM-AI-001` AI 医疗知识工厂、来源注册和 AI 字典候选生成。
6. `PKG-AI-001` 外网基准医疗知识包导出、院内导入、dry-run 和激活。
7. `RULE-001` 第三方规则引擎 API，支持病历质控、医保质控、医嘱安全拦截。
8. `GRAPH-001` 图谱包发布与 Oracle 到 Neo4j 同步。
9. `DIFY-001` Dify 契约绑定和调用回放。
10. `FE-001` 前端信息架构、高保真原型和演示脚本。
11. `FE-002` 前端工程脚手架。
12. `FE-003` 功能演示与规则校验工作台。

## 每批开发硬性流程

后续 AI 开发必须遵守：

1. 先读 `AI接手执行手册.md` 和 `产品化方案与AI开发编排.md`。
2. 运行 `git status -sb`，确认工作树状态。
3. 若用户要求自主开发，按 `AI自主开发运行守则.md` 创建或更新 `ai-dev-input/12_autonomous_runs/active/<run_id>.md`。
4. 按 `AI任务认领与并行开发机制.md` 创建并推送 `ai-dev-input/10_task_claims/active/<claim_id>.md`，认领成功后才能改业务代码。
5. 执行 `.\zy-engine-mvp\scripts\detect-db-env.ps1 -BootstrapLocal`，有 Oracle 用 Oracle，无 Oracle 用 `start-local-db.ps1`。
6. 明确任务编号、边界、验收标准。
7. 实现代码时同步更新测试、样例、API 示例和文档。
8. 执行：

```powershell
.\zy-engine-mvp\scripts\run-tests.ps1
.\zy-engine-mvp\scripts\build.ps1
git diff --check
```

9. 按 `AI开发质量门禁与评审整改机制.md` 创建 `ai-dev-input/11_ai_reviews/pending/<review_id>.md`，完成自检、AI 评审、问题整改和复评。
10. 只有 `review_status=APPROVED` 且 `open_findings=0` 时，才能正式提交或合并业务代码。
11. 只暂存并提交本任务相关文件，提交信息使用清晰中文短句。
12. 每完成一个明确任务且通过质量门禁后，必须立即 `git push` 到远端当前分支，确保其它 AI 可以拉取最新项目。
13. 完成后把 claim 归档到 `ai-dev-input/10_task_claims/archive/YYYYMMDD/`，把 review 归档到 `ai-dev-input/11_ai_reviews/archive/YYYYMMDD/`；自主运行结束时同步归档 run log。
14. 最终回复必须说明改动、验证结果、review_id、open_findings、run_id、提交 hash、推送分支、数据库模式和未覆盖风险。

## 重要红线

- 不允许硬编码单医院逻辑。
- 不允许把 Neo4j/Dify 做成测试环境强依赖。
- 不允许把 Dify、大模型或 Neo4j 做成规则引擎核心判定、发布、审计和第三方调用的必需依赖。
- 不允许外部系统传任意 SQL/Cypher 到核心引擎执行。
- 不允许配置静默覆盖已发布版本。
- 不允许关键医疗建议绕过医生确认。
- 不允许发布、回滚、同步绕过审计。
