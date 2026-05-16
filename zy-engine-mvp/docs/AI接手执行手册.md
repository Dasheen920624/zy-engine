# AI 接手执行手册

## 1. 用途

本文是后续 AI 接手本项目时的第一入口。它不替代产品总纲，而是把“接手后怎么做”压缩成可执行流程。

接手 AI 必读顺序：

1. 本文。
2. `zy-engine-mvp/docs/产品化方案与AI开发编排.md`
3. `zy-engine-mvp/docs/全功能蓝图与并行开发计划.md`
4. `zy-engine-mvp/docs/多角色评审与执行计划.md`
5. `zy-engine-mvp/docs/前端配置平台规划与开发验证.md`
6. 根目录 `README.md`
7. `zy-engine-mvp/README.md`
8. `ai-dev-input/09_ai_task_cards/ai_system_prompt.md`

## 2. 当前项目一句话

这是面向集团化医院、多院区、卫生所/站点的医疗智能引擎平台。Oracle/关系型数据库是配置主数据源；Neo4j 是图谱查询投影；Dify 是 AI 工作流执行目标；没有 Neo4j 和 Dify 时，DB-only 测试环境也必须可完整验收。

## 3. 接手后 10 分钟检查

先执行：

```powershell
git status -sb
rg -n "当前优先任务池|Definition of Done|PROV-001|PKG-001|ORG-001|TERM-001|RULE-001|FE-001|FE-003" zy-engine-mvp/docs
```

再阅读：

```text
zy-engine-mvp/docs/产品化方案与AI开发编排.md
zy-engine-mvp/docs/全功能蓝图与并行开发计划.md
zy-engine-mvp/docs/前端配置平台规划与开发验证.md
zy-engine-mvp/README.md
ai-dev-input/09_ai_task_cards/ai_system_prompt.md
ai-dev-input/09_ai_task_cards/task_card_template.md
```

若工作树已有未提交改动：

- 不要回滚。
- 先判断是否与当前任务相关。
- 相关则继续兼容；无关则忽略。
- 不确定时向用户说明风险。

## 4. 开发前必须确认

每次开始前必须明确：

- 任务编号，例如 `ORG-001`、`PKG-001`、`TERM-001`、`FE-001`。
- 所属模块。
- 若涉及前端，必须确认配置界面、演示界面、规则校验工作台或质控看板的页面范围。
- 是否涉及组织隔离。
- 是否涉及配置版本。
- 是否涉及来源追溯、引用片段、医学/医保/质控依据。
- 是否涉及 Provider。
- 是否需要 Oracle/达梦 DDL。
- 是否涉及 Oracle 表结构、索引、迁移脚本、持久化 SQL 或落库行为；若涉及，必须同步真实 Oracle 并跑 Oracle 版本 smoke。
- DB-only 模式如何验收。
- 需要更新哪些测试、样例和文档。

没有明确任务编号时，优先使用 `产品化方案与AI开发编排.md` 中的“当前优先任务池”。

## 5. 任务编号约定

```text
ORG-xxx     组织模型与租户隔离
PKG-xxx     配置包和发布生命周期
TERM-xxx    标准化和字典治理
RULE-xxx    规则引擎
PATH-xxx    路径引擎
GRAPH-xxx   图谱引擎
DIFY-xxx    Dify/AI 工作流
ADAPT-xxx   第三方适配器
QC-xxx      质控指标
PROV-xxx    来源、证据、引用和可追溯性
FE-xxx      前端配置、演示校验和可视化验收
AUDIT-xxx   审计日志
OPS-xxx     运维部署
SEC-xxx     安全权限
TEST-xxx    测试与验收
DOC-xxx     文档
```

## 6. 标准开发流程

### Step 1：确认范围

```powershell
git status -sb
git log -1 --pretty=format:%H%n%s
```

阅读相关模块代码和测试。优先使用 `rg` 搜索，不要凭记忆修改。

### Step 2：形成任务计划

计划至少包含：

- 需要修改的模块。
- 需要新增/修改的接口。
- 数据模型和配置影响。
- 测试点。
- 文档点。

### Step 3：实现

实现时遵守：

- 不硬编码医院逻辑。
- 不引入 Neo4j/Dify 强依赖。
- 所有配置导入必须校验。
- 关键操作必须审计。
- 外部依赖必须 Provider 化。
- API 返回统一 `ApiResult`。
- 新增接口必须透出 `traceId`。

### Step 4：补测试

最低要求：

- 新增接口补 `EngineApiContractTests` 或等价契约测试。
- 新增配置补样例 JSON。
- 新增运行链路补 smoke 脚本或 README 验证方式。

### Step 5：补文档

按影响范围更新：

- `zy-engine-mvp/README.md`
- `zy-engine-mvp/docs/api-examples.http`
- `zy-engine-mvp/docs/产品化方案与AI开发编排.md`
- `ai-dev-input/09_ai_task_cards/*.md`
- 样例 JSON 或测试矩阵。

### Step 6：验证

必须执行：

```powershell
.\zy-engine-mvp\scripts\run-tests.ps1
.\zy-engine-mvp\scripts\build.ps1
git diff --check
```

若任务涉及 Oracle/达梦 DDL、Oracle 迁移脚本、持久化 SQL、表字段、索引、约束或 Oracle 落库行为，还必须执行真实 Oracle 验证：

```powershell
.\zy-engine-mvp\scripts\run-oracle-ddl.ps1
.\zy-engine-mvp\scripts\build.ps1
.\zy-engine-mvp\scripts\start-oracle.ps1
# 另开终端或后台启动后执行：
.\zy-engine-mvp\scripts\run-oracle-org-smoke.ps1
```

Oracle 脚本会自动读取仓库根目录 `.env.oracle.local`。`.env.oracle.local.example` 已提交用于说明 Oracle 连接目标；真实 `.env.oracle.local` 只存本机 Oracle 凭据，已被 `.gitignore` 忽略，禁止提交。若当前任务新增了新的 Oracle 业务链路，应补对应 Oracle smoke；如果不能连接客户 Oracle，最终回复必须明确说明未验证原因和风险，不能只用内存/JUnit 代替。

若只改文档，仍建议运行完整测试和构建，除非用户明确要求快速文档修改。

### Step 7：提交

每完成一个明确任务，必须提交并推送一次，保证其它 AI 可以拉取最新项目。不得把已完成任务长期留在本地未提交状态。

提交前：

```powershell
git status -sb
git diff --stat
git diff --cached --stat
```

只暂存当前任务相关文件。

提交信息格式：

```text
动词 + 模块 + 能力
```

示例：

```text
增加Provider运行状态接口
补充产品化方案与AI开发编排
增加规则包审核与批量发布
```

提交后必须推送到远端当前分支，例如：

```powershell
git push origin main
```

若因为权限、网络或远端冲突无法推送，必须在最终回复中明确说明原因、当前提交 hash 和其它 AI 获取变更的替代方式。

## 7. 每批代码 Definition of Done

完成一批任务前必须满足：

- 工作范围清晰，无无关文件混入。
- 新增能力有契约测试。
- 新增配置有样例或 API 示例。
- README 或 docs 已更新。
- `run-tests.ps1` 通过。
- `build.ps1` 通过。
- `git diff --check` 通过。
- 涉及数据库表结构、索引、约束、迁移或持久化 SQL 的任务，已执行 `run-oracle-ddl.ps1` 同步真实 Oracle，并通过 Oracle 版本 smoke；若未执行，必须说明原因和残留风险。
- 已提交本任务相关文件。
- 已推送到远端当前分支，或明确说明无法推送的原因。
- DB-only 模式仍可运行。
- 不硬编码单医院逻辑。
- 不把 Neo4j/Dify 作为强依赖。
- 最终回复包含改动、验证、风险。

## 8. 当前优先任务执行顺序

建议按以下顺序推进：

1. `PKG-001` 配置包统一模型。
2. `ORG-001` 组织模型和组织上下文。
3. `PROV-001` 来源文档、引用片段和资产绑定底座。
4. `TERM-001` 医嘱标准化。
5. `RULE-001` 第三方规则引擎 API。
6. `FE-001` 前端信息架构、高保真原型和演示脚本。
7. `FE-002` 前端工程脚手架。
8. `FE-003` 功能演示与规则校验工作台。
9. `GRAPH-001` 图谱包发布与同步。
10. `DIFY-001` Dify 契约绑定。
11. `SEC-001` 接口鉴权、签名和组织权限。
12. `OPS-001` 离线部署和运维看板。

若用户没有指定任务，优先从 `PKG-001` 或 `ORG-001` 开始，因为它们是产品化底座。若任务涉及规则、知识、路径、图谱、Dify、字典、适配器或质控结论，必须同步检查 `全功能蓝图与并行开发计划.md`。

## 9. 推荐任务切片

### PKG-001 配置包统一模型

第一批已完成模型和内存/数据库无关服务骨架：

- `ConfigPackage` 数据结构。
- `package_code/package_version/scope_level/scope_code/status/hash` 字段。
- review 返回 manifest。
- `POST /api/config-packages`
- `POST /api/config-packages/{packageCode}/{packageVersion}/review`
- `POST /api/config-packages/{packageCode}/{packageVersion}/publish`
- `POST /api/config-packages/{packageCode}/{packageVersion}/export`
- `tenant_id` 字段和 `tenantId` 查询过滤。
- review/publish 会校验组织目录中是否存在 `scope_level/scope_code`；`PLATFORM/DEFAULT` 作为系统内置默认基线保留。

下一批做：

- Oracle/达梦配置包表。
- 跨环境导出导入校验。
- 配置包回滚。
- 同步任务状态。

### ORG-001 组织模型

第一批已完成：

- 组织模型类。
- 组织上下文工具。
- `GET /api/system/org-context`。
- 默认兼容 `default/ZYHOSPITAL`。
- Header/Query 支持 `tenant_id/group_code/hospital_code/campus_code/site_code/department_code`。
- `POST /api/organizations` 导入真实组织目录。
- `GET /api/organizations` 查询组织节点。
- `GET /api/organizations/tree` 返回组织树。
- `ORG_UNIT` Oracle/达梦 DDL。

下一批做：

- 组织目录 Oracle 持久化。
- 更多配置查询继续接入组织上下文，尤其是路径配置、图谱配置、Dify 模板和适配器绑定。

### TERM-001 医嘱标准化

第一批只做：

- 医嘱映射导入。
- 单条医嘱标准化。
- 未映射返回 `PENDING_MAPPING`。
- 样例覆盖同药不同名称/规格。

### PROV-001 来源追溯底座

第一批只做：

- `SRC_DOCUMENT`、`SRC_CITATION`、`SRC_ASSET_BINDING`、`SRC_REVIEW_RECORD`、`SRC_RUNTIME_EVIDENCE` 的 Oracle/达梦 DDL 草案与迁移脚本。
- 来源样例 JSON、API 契约草案和测试矩阵。
- 配置包 review 增加 `source_review` 输出结构。
- 规则发布预留缺来源、过期来源、未审核来源的阻断点。

本批暂不做：

- 文件二进制存储。
- 全文检索。
- 前端页面。

### RULE-001 第三方规则引擎 API

第一批已落地同步接口：

```http
POST /api/rule-engine/evaluate
```

支持：

- `scenario_code`：限 `PATHWAY_ENTRY`、`EMR_QC`、`INSURANCE_QC`、`ORDER_SAFETY`、`DRUG_INDICATION`、`EXAM_RATIONALITY`。
- `rule_package_code` / `rule_package_version`：可选包过滤，未提供时按场景从全部已发布规则中匹配。
- `rule_codes`：可选规则编码白名单。
- `patient_context`：必填，须显式传入 `patient`、`encounter`、`facts`。
- 返回 `evaluated_count`、`hit_count`、`elapsed_ms`、`results[]`、`warnings[]`，未命中规则时给 `NO_RULES_MATCHED` 警告而非异常。
- 写入 `RULE_ENGINE/EVALUATE_SCENARIO` 审计与每条规则执行日志。
- 规则可通过 `scenario_codes` 数组同时挂载多个场景（如医保限定药品同时归入 `INSURANCE_QC` 与 `DRUG_INDICATION`）；老规则在缺失声明时按 `rule_type` 自动推断。

第二批已落地批量同步与结果回查：

```http
POST /api/rule-engine/batch-evaluate
GET  /api/rule-engine/results
GET  /api/rule-engine/results/{resultId}
```

- `batch-evaluate`：`items[]` 每条带可选 `case_id` 和必填 `patient_context`，共享 scenario 过滤条件，整次调用返回 `batch_id` + 每条独立 `result_id`，并写入 `RULE_ENGINE/BATCH_EVALUATE_SCENARIO` 审计。
- 单次 `/evaluate` 和 batch 内每条结果都落入内存评估环形缓冲（容量 500），`source` 字段区分 `SINGLE/BATCH`。
- `GET /results` 支持 `scenarioCode/packageCode/batchId/source/patientId/encounterId/limit/offset` 过滤，仅返回摘要字段，不带 `results/warnings` 详情。
- `GET /results/{resultId}` 返回完整 envelope，未找到返回 `VALIDATION_ERROR`。

ORG-001 第三批已织入第三方规则引擎接口：

- `/api/rule-engine/evaluate` 与 `/batch-evaluate` 通过 `OrganizationContextService.resolveWithBody` 解析组织上下文，Header（`X-Tenant-Id/X-Group-Code/X-Hospital-Code/X-Campus-Code/X-Site-Code/X-Department-Code/X-Org-Code`）与 Query 提供默认，Body 字段（同名）优先覆盖。
- 评估记录与审计明细均带 `tenant_id/group_code/hospital_code/campus_code/site_code/department_code/scope_level/scope_code/org_source`；`org_source` 取值 `HEADER/QUERY/BODY/DEFAULT/NONE`。
- `GET /api/rule-engine/results` 扩展 `tenantId/groupCode/hospitalCode/campusCode/siteCode/departmentCode/scopeLevel/scopeCode` 过滤项，便于多医院/多院区聚合复盘。

ORG-001 第四批已织入路径运行、质控和审计查询：

- `/api/patient-pathways/admit` 通过 Header/Query/Body 合并组织上下文，患者路径实例落 `tenantId/groupCode/hospitalCode/campusCode/siteCode/departmentCode/scopeLevel/scopeCode/orgSource`。
- `/api/pathway-instances*`、`/api/pathway-variations*`、`/api/quality/metrics` 和 `/api/audit-logs*` 支持显式 `tenantId/groupCode/hospitalCode/campusCode/siteCode/departmentCode/scopeLevel/scopeCode` 过滤。
- `PE_PATIENT_INSTANCE` 的活动实例唯一约束已从 encounter/pathway/status 扩展为 tenant/org/encounter/pathway/status，避免多医院同就诊号互相覆盖。

ORG-001 第五批已织入规则配置与内部执行日志：

- `/api/rules` 导入、`/api/rules/{ruleCode}` 回查、规则/规则包发布、`/api/rules/evaluate`、`/api/rules/simulate` 支持 Header/Query/Body 组织上下文。
- `RuleDefinition` 与 `RuleExecLogEntry` 带 `tenantId/groupCode/hospitalCode/campusCode/siteCode/departmentCode/scopeLevel/scopeCode/orgSource`。
- 内存规则存储键已扩展为 `tenant + scope + rule_code + version_no`，同一规则编码/版本可在不同医院或科室独立覆盖。
- 若第三方 `/api/rule-engine/*` 显式组织下没有专属规则，会回退到 legacy `default/ZYHOSPITAL` 规则，保持旧演示和基线规则可用。
- `GET /api/rules` 与 `GET /api/rules/exec-logs*` 支持显式组织过滤。

DB-ORG-001 已补齐组织上下文落库：

- `zy-engine-mvp/db/oracle/zyengine_core_ddl_with_comments.sql`、`ai-dev-input/04_database/oracle/core_ddl.sql`、`ai-dev-input/04_database/dm/core_ddl.sql` 已为 `PE_VARIATION_RECORD`、`RE_RULE_EXEC_LOG`、`ENGINE_AUDIT_LOG` 增加结构化组织字段和组织过滤索引。
- `db/oracle/zyengine_org_context_migration.sql` 可重复执行，用于已有 Oracle 库补列、建索引并升级 `UK_PE_ACTIVE_INSTANCE`。
- `EnginePersistenceService` 写入变异记录、规则执行日志和审计日志时已同步写 `tenant_id/group_code/hospital_code/campus_code/site_code/department_code/scope_level/scope_code/org_source`。
- 常规 `run-tests.cmd` 仍不连接 Oracle；真实落库验证使用 `scripts/run-oracle-org-smoke.cmd`，需先配置 `ZYENGINE_DB_CONNECT/ZYENGINE_DB_USERNAME/ZYENGINE_DB_PASSWORD` 并启动 Oracle 模式。

规则评估结果持久化、异步任务与同步任务状态留给 RULE-001 第三批继续推进。

### FE-001 前端信息架构与高保真原型

第一批只做：

- 管理台导航。
- 工作台。
- 演示与校验工作台。
- 配置包中心。
- 路径/规则配置器原型。
- AMI、病历质控、医保质控、医嘱安全演示脚本。

### FE-002 前端工程脚手架

第一批只做：

- React + TypeScript 工程。
- Layout、路由、菜单。
- API client 和统一错误处理。
- Provider 状态页。
- mock 数据和基础测试。

### FE-003 功能演示与规则校验工作台

第一批只做：

- 演示场景库。
- 患者上下文构建器。
- `POST /api/rules/simulate` 和后续 `POST /api/rule-engine/evaluate` 的结果展示组件。
- `EMR_QC`、`INSURANCE_QC`、`ORDER_SAFETY` 场景入口。
- 命中条件、证据、建议动作、标准化差异、traceId 展示。
- DB-only mock/degraded 演示闭环。

## 10. 禁止事项

- 禁止把客户医院编码、医嘱名称、院区能力写死进 Java 逻辑。
- 禁止直接让外部系统传任意 SQL/Cypher 到引擎执行。
- 禁止 Neo4j/Dify 不可用时导致核心路径/规则接口不可用。
- 禁止绕过 review/publish 直接激活配置。
- 禁止发布缺来源、来源过期或来源未审核的医学/医保/质控配置。
- 禁止配置发布后静默覆盖同版本。
- 禁止在日志中输出数据库密码、Dify API Key、患者完整隐私明文。
- 禁止回滚用户或其他 AI 的未提交改动。

## 11. 最终回复模板

建议最终回复保持简洁：

```text
已完成并验证。

主要改动：
- ...

验证：
- run-tests.ps1
- build.ps1
- git diff --check

提交与推送：
- <commit hash / branch / remote>

剩余风险：
- ...
```

如果没有提交或没有推送，必须明确说明原因。默认情况下，每个明确任务完成后都要提交并推送一次。
